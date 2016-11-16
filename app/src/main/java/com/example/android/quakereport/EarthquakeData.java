package com.example.android.quakereport;

import android.widget.EditText;

import java.util.Calendar;
import java.util.Date;


/**
 * Created by Shichao Nie on 2016/11/13.
 */

public class EarthquakeData {
    private String mCity;
    private double mMagnitude;
    private Calendar mDate = Calendar.getInstance();
    private String mUrl;

    public EarthquakeData(double magnitude, String city, long time, String url){
        mMagnitude = magnitude;
        mCity = city;
        mDate.setTime(new Date(time));
        mUrl = url;
    }
    public String getCity(){
        return mCity;
    }
    public double getMagnitude(){
        return mMagnitude;
    }
    public Calendar getDate(){
        return mDate;
    }
    public String getUrl(){
        return mUrl;
    }
}
