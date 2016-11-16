package com.example.android.quakereport;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.ServiceConfigurationError;

/**
 * Created by Shichao Nie on 2016/11/13.
 */

public class EarthquakeAdapter extends ArrayAdapter<EarthquakeData> {
    public EarthquakeAdapter(Activity context, ArrayList<EarthquakeData> arrayList){
        super(context, 0, arrayList);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItemView = convertView;
        if(listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }
        EarthquakeData quakedata = getItem(position);

        TextView textMag = (TextView) listItemView.findViewById(R.id.text_mag);
        textMag.setText(new DecimalFormat("0.0").format(quakedata.getMagnitude()));

        // 为震级圆圈设置正确的背景颜色。
        // 从 TextView 获取背景，该背景是一个 GradientDrawable。
        GradientDrawable magnitudeCircle = (GradientDrawable) textMag.getBackground();

        // 根据当前的地震震级获取相应的背景颜色
        int magnitudeColor = getMagnitudeColor(quakedata.getMagnitude());

        // 设置震级圆圈的颜色
        magnitudeCircle.setColor(ContextCompat.getColor(getContext(), magnitudeColor));

        TextView textMainPlace = (TextView) listItemView.findViewById(R.id.text_main_place);
        TextView textSubPlace = (TextView) listItemView.findViewById(R.id.text_sub_place);
        if(quakedata.getCity().contains("km") && quakedata.getCity().contains("of")){
            textMainPlace.setText(getMainPlace(quakedata.getCity()));
            textSubPlace.setText(getSubPlace(quakedata.getCity()));
        }
        else{
            textMainPlace.setText(quakedata.getCity());
            textSubPlace.setText("Near the");
        }

        TextView textDate = (TextView) listItemView.findViewById(R.id.text_date);
        String dateText;
        Calendar date = quakedata.getDate();
        dateText = (new SimpleDateFormat("MMM d, yyyy")).format(date.getTime());
        textDate.setText(dateText);

        TextView textTime = (TextView) listItemView.findViewById(R.id.time);
        String timeText;
        timeText = (new SimpleDateFormat("h:mm, a")).format(date.getTime());
        textTime.setText(timeText);
        return listItemView;
    }
    public String getMainPlace(String place){
        int index = place.indexOf("of");
        return place.substring(index + 3);
    }
    public String getSubPlace(String place){
        int index = place.indexOf("of");
        return place.substring(0, index + 2);
    }
    private int getMagnitudeColor(double mag){
        int magFormat = (int) Math.floor(mag);
        switch (magFormat) {
            case 0:
            case 1:
                return R.color.magnitude1;
            case 2:
                return R.color.magnitude2;
            case 3:
                return R.color.magnitude3;
            case 4:
                return R.color.magnitude4;
            case 5:
                return R.color.magnitude5;
            case 6:
                return R.color.magnitude6;
            case 7:
                return R.color.magnitude7;
            case 8:
                return R.color.magnitude8;
            case 9:
                return R.color.magnitude9;
            default:
                return R.color.magnitude10plus;
        }

    }
}
