package com.liveEarthquakesAlerts.controller.utils;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class CreateRequestUrl {

    public static String URL_USGS() {
        return "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson";
        //https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson
    }

}
