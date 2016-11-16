package com.example.android.quakereport;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.example.android.quakereport.QueryUtils.extractEarthquakes;

public class EarthquakeActivity extends AppCompatActivity {

    public static final String LOG_TAG = EarthquakeActivity.class.getName();
    private static final String USGS_REQUEST_URL = "http://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2016-11-01&endtime=2016-11-02";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.earthquake_activity);

        QuakeAsyncTask task = new QuakeAsyncTask();
        task.execute();
    }
    private void updateUi(final ArrayList<EarthquakeData> earthquakes) {
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
    private class QuakeAsyncTask extends AsyncTask<URL, Void, ArrayList<EarthquakeData>>{
        @Override
        protected ArrayList<EarthquakeData> doInBackground(URL... urls) {
            URL url = creatUrl(USGS_REQUEST_URL);
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
        private ArrayList<EarthquakeData> extractFeatureFromJson(String earthquakeJSON){
        ArrayList<EarthquakeData> earthquakes = new ArrayList<>();
        if (TextUtils.isEmpty(earthquakeJSON)) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(earthquakeJSON);
            JSONArray jsonArray = jsonObject.getJSONArray("features");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObjectFeatures = jsonArray.getJSONObject(i).getJSONObject("properties");
                double mag = jsonObjectFeatures.getDouble("mag");
                String place = jsonObjectFeatures.getString("place");
                long time = jsonObjectFeatures.getLong("time");
                String url = jsonObjectFeatures.getString("url");
                earthquakes.add(new EarthquakeData(mag, place, time, url));
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
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            }
        } catch (IOException e) {
            // TODO: Handle the exception
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
