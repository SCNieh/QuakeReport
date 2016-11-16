package com.example.android.quakereport;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class EarthquakeActivity extends AppCompatActivity {

    public static final String LOG_TAG = EarthquakeActivity.class.getName();
    private static final String USGS_REQUEST_URL = "http://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&";
    private static final Float CONNECTION_COMPLETE = (float) 200;
    private static final Float CONNECTION_FAILED = (float) 201;
    private static final Float JSON_REQUIRED = (float) 202;
    private static final Float JSON_REQUIRED_FAILED = (float) 203;
    private QuakeAsyncTask task = null;
    private Toast toast = null;
    private String userInputUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.earthquake_activity);

    }

    public void beginSearch(View view) {
        TextView processView = (TextView)findViewById(R.id.progress_connection);
        if(processView.getTextColors().getDefaultColor() == ContextCompat.getColor(this ,R.color.connectionSucceed)){
            processView.setTextColor(ContextCompat.getColor(this, R.color.textColorEarthquakeDetails));
        }
        processView.setText("processing...");
        userInputUrl = SetUserInputUrl();
        if (task == null) {
            task = new QuakeAsyncTask();
            task.execute();
        } else {
            task.cancel(true);
            if(task.isCancelled()){
                if (toast == null) {
                    toast = Toast.makeText(this, "Thread Canceled", Toast.LENGTH_SHORT);
                } else {
                    toast.setText("Thread Cancelded");
                }
                toast.show();
            }
            task = new QuakeAsyncTask();
            task.execute();
        }

    }

    private String SetUserInputUrl() {
        String startime, endtime, userUrl;
        startime = findDate(R.id.from_year, R.id.from_month, R.id.from_day);
        endtime = findDate(R.id.to_year, R.id.to_month, R.id.to_day);
        if (startime == null || endtime == null) {
            return null;
        }
        userUrl = USGS_REQUEST_URL + "starttime=" + startime + "&" + "endtime=" + endtime;
        return userUrl;
    }

    private String findDate(int year, int month, int day) {
        String date = "";
        EditText edittext = (EditText) findViewById(year);
        try {
            date = edittext.getText().toString();
            edittext = (EditText) findViewById(month);
            date = date + "-" + edittext.getText().toString();
            edittext = (EditText) findViewById(day);
            date = date + "-" + edittext.getText().toString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.v(LOG_TAG, "getText nullPointer");
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setLenient(false);
            Date formatDate = format.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Illegal Date", Toast.LENGTH_SHORT).show();
            return null;
        }
        return date;
    }

    private void updateUi(final ArrayList<EarthquakeData> earthquakes) {
        TextView itemText = (TextView) findViewById(R.id.item_acquired);
        itemText.setText(earthquakes.size() + " items acquired");

        ListView earthquakeListView = (ListView) findViewById(R.id.list);

        // Create a new {@link ArrayAdapter} of earthquakes
        EarthquakeAdapter adapter = new EarthquakeAdapter(this, earthquakes);
        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        earthquakeListView.setAdapter(adapter);
        earthquakeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(earthquakes.get(i).getUrl()));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
    }

    private void updateProgress(Float value) {
        if (value == CONNECTION_COMPLETE) {
            TextView progressText = (TextView) findViewById(R.id.progress_connection);
            progressText.setTextColor(ContextCompat.getColor(this, R.color.connectionSucceed));
            progressText.setText(R.string.connection_succeed);
        } else if (value == JSON_REQUIRED) {
            TextView progressText = (TextView) findViewById(R.id.progress_json);
            progressText.setTextColor(ContextCompat.getColor(this, R.color.connectionSucceed));
            progressText.setText(R.string.json_required);
        } else if (value == CONNECTION_FAILED) {
            TextView progressText = (TextView) findViewById(R.id.progress_connection);
            progressText.setTextColor(ContextCompat.getColor(this, R.color.connectionFailed));
            progressText.setText(R.string.connection_failed);
        } else if (value == JSON_REQUIRED_FAILED) {
            TextView progressText = (TextView) findViewById(R.id.progress_json);
            progressText.setTextColor(ContextCompat.getColor(this, R.color.connectionFailed));
            progressText.setText(R.string.json_required_failed);
        } else {
            String progress = "Loading list..." + Integer.toString(Math.round(value)) + "%";
            TextView progressText = (TextView) findViewById(R.id.progress_text);
            progressText.setText(progress);
        }
    }

    private class QuakeAsyncTask extends AsyncTask<URL, Float, ArrayList<EarthquakeData>> {
        @Override
        protected ArrayList<EarthquakeData> doInBackground(URL... urls) {
            if (userInputUrl == null) {
                return null;
            }
            URL url = creatUrl(userInputUrl);
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                // TODO Handle the IOException
                Log.v("MainActivity", "jsonException");
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object
            ArrayList<EarthquakeData> earthquake = extractFeatureFromJson(jsonResponse);

            // Return the {@link Event} object as the result fo the {@link TsunamiAsyncTask}
            return earthquake;
        }

        @Override
        protected void onPostExecute(ArrayList<EarthquakeData> earthquake) {
            if (earthquake == null) {
                return;
            }

            updateUi(earthquake);
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            updateProgress(values[0]);
        }

        private ArrayList<EarthquakeData> extractFeatureFromJson(String earthquakeJSON) {
            ArrayList<EarthquakeData> earthquakes = new ArrayList<>();
            if (TextUtils.isEmpty(earthquakeJSON)) {
                publishProgress(JSON_REQUIRED_FAILED);
                return null;
            }
            try {
                publishProgress(JSON_REQUIRED);
                JSONObject jsonObject = new JSONObject(earthquakeJSON);
                JSONArray jsonArray = jsonObject.getJSONArray("features");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObjectFeatures = jsonArray.getJSONObject(i).getJSONObject("properties");
                    double mag;
                    try {
                        mag = jsonObjectFeatures.getDouble("mag");
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                        Log.v(LOG_TAG, "The Mag of NO." + i + "item us wrong", ex);
                        mag = 0.0;
                    }
                    String place = jsonObjectFeatures.getString("place");
                    long time = jsonObjectFeatures.getLong("time");
                    String url = jsonObjectFeatures.getString("url");
                    earthquakes.add(new EarthquakeData(mag, place, time, url));
                    publishProgress((i / (float) jsonArray.length()) * 100);
                }
                return earthquakes;
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
            }
            return null;
        }

        private URL creatUrl(String StringUrl) {
            URL url = null;
            try {
                url = new URL(StringUrl);
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error with creating URL", e);
                return null;
            }
            return url;
        }

        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            if (url == null) {
                return jsonResponse;
            }
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    publishProgress(CONNECTION_COMPLETE);
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                }
            } catch (IOException e) {
                // TODO: Handle the exception
                publishProgress(CONNECTION_FAILED);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }
    }

}
