package com.liveEarthquakesAlerts.controller.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.liveEarthquakesAlerts.model.database.DatabaseHelper;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.RiskyEarthquakes;
import com.liveEarthquakesAlerts.model.sources.usgsFolder.insideUsgs.featuresFolder.features1;
import com.liveEarthquakesAlerts.model.sources.usgsFolder.insideUsgs.featuresFolder.insideFeatures.geometry1;
import com.liveEarthquakesAlerts.model.sources.usgsFolder.insideUsgs.featuresFolder.insideFeatures.properties1;
import com.liveEarthquakesAlerts.model.sources.usgsFolder.insideUsgs.metadataFolder.metadata1;
import com.liveEarthquakesAlerts.model.sources.usgsFolder.usgs;

import java.lang.reflect.Type;
import java.math.BigDecimal;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//We need database helper, so that, before we do insert new records, we clear database first

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class SaveResponseToDB { //this class updates EarthQuakes Bean

    private int decimalPlace = 1;

    public SaveResponseToDB() {
        DatabaseHelper.getDbHelper().clearDatabase();
    }

    public void saveDatabaseUsgs(String url) { //save every earthquake fields like magnitude, latitude etc.

        try {

            Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(OnLineTracker.DATEFORMAT).create();
            Type listType = new TypeToken<usgs<String, metadata1, features1<properties1, geometry1>, Float>>() {
            }.getType();


            String json = getJson(url);

            if (json == null || json.length() < 10) { // JSON is null or empty , json.length() defines the length of string
                return;
            }

            usgs<String, metadata1, features1<properties1, geometry1>, Float> items = gson.fromJson(json, listType);


            if (items == null || items.getFeatures() == null || items.getFeatures().size() == 0) { //check if item null or items' features null or item's features empty
                return;
            }

            for (features1<properties1, geometry1> item : items.getFeatures()) { //each item means each feature, which is each earthquake record

                String str1 = item.getProperties().getPlace().trim().toUpperCase();

                String str2 = str1.substring(str1.indexOf("of") + 3); //look json, the title is like "228km NW of Saumlaki, Indonesia"

                EarthQuakes eq = new EarthQuakes(); //EarthQuakes is a bean
                eq.setSig(item.getProperties().getSig());
                eq.setDateMilis(Long.parseLong(item.getProperties().getTime()));
                eq.setDepth(round(item.getGeometry().getCoordinates().get(2), decimalPlace)); //depth means altitude. In this way, database is getting updated
                eq.setLatitude(item.getGeometry().getCoordinates().get(1));
                eq.setLongitude(item.getGeometry().getCoordinates().get(0));
                eq.setLocationName(str2);
                eq.setMagnitude(round(item.getProperties().getMag(), decimalPlace));
                eq.Insert();
                Log.i("Sig", String.valueOf(eq.getSig()));


                if (CheckRiskEarthquakes.checkRisky(eq)) {


                    String str11 = item.getProperties().getPlace().trim().toUpperCase();

                    String str22 = str1.substring(str1.indexOf("of") + 3); //look json, the title is like "228km NW of Saumlaki, Indonesia"

                    RiskyEarthquakes riskyEarthquakes = new RiskyEarthquakes(); //RiskyEarthQuakes is a bean
                    riskyEarthquakes.setSig(item.getProperties().getSig());
                    riskyEarthquakes.setDateMilis(Long.parseLong(item.getProperties().getTime()));
                    riskyEarthquakes.setDepth(round(item.getGeometry().getCoordinates().get(2), decimalPlace)); //depth means altitude. In this way, database is getting updated
                    riskyEarthquakes.setLatitude(item.getGeometry().getCoordinates().get(1));
                    riskyEarthquakes.setLongitude(item.getGeometry().getCoordinates().get(0));
                    riskyEarthquakes.setLocationName(str2);
                    riskyEarthquakes.setMagnitude(round(item.getProperties().getMag(), decimalPlace));
                    riskyEarthquakes.Insert();
                }
            }

        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }

    }

    private String getJson(String reqUrl) throws Exception {
        Request request = new Request.Builder().url(reqUrl).build(); //Request builder is used to get JSON url
        Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
        return response.isSuccessful() ? response.body().string() : "";
    }

    public float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

}