package com.liveEarthquakesAlerts.controller.utils;

import android.location.Location;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class CreateRequestUrl {

    public static String URL_USGS(int day) {

        String str = "all_hour";

        if (day == 0) {
            str = "all_hour";
        } else if (day == 1) {
            str = "all_day";
        } else if (day == 2) {
            str = "all_week";
        }

        return "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/" + str + ".geojson";
        //https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson
    }

    public static String URL_USGS(int day, int i, Location currentLoc) {
        int bboxRadius = i;

        String str = "all_hour";
        if (day == 0) {
            str = "all_hour";
        } else if (day == 1) {
            str = "all_day";
        } else if (day == 2) {
            str = "all_week";
        }

        return "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/" + str + ".geojson";
    }
}
