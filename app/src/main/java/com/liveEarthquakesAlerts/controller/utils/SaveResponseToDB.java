package com.liveEarthquakesAlerts.controller.utils;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.liveEarthquakesAlerts.model.database.DatabaseHelper;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.RiskyEarthquakes;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.POJOUSGS;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.featuresFolderUSGS.FeaturesUSGS;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.featuresFolderUSGS.insideFeaturesUSGS.GeometryUSGS;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.featuresFolderUSGS.insideFeaturesUSGS.PropertiesUSGS;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.metadataFolderUSGS.MetadataUSGS;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//We need database helper, so that, before we do insert new records, we clear database first

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class SaveResponseToDB { //this class updates EarthQuakes Bean


    private String locationName, jsonOriginal = null, str1;
    private Integer sig, decimalPlace = 1;
    private Long time;
    private Float longitude, latitude, depth, magnitude;

    public SaveResponseToDB() {
        DatabaseHelper.getDbHelper().clearDatabase();
    }

    public static String getJson(String reqUrl) throws Exception {
        Request request = new Request.Builder().url(reqUrl).build(); //Request builder is used to get JSON url
        Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
        return response.isSuccessful() ? response.body().string() : "";
    }

    public void getDataFromFirebase(final DataSnapshot dataSnapshot) { //save every earthquake fields like magnitude, latitude etc.

        new Thread(new Runnable() { //should do network operation using separate thread; can't do from main thread
            @Override
            public void run() {
                try {

                    for (DataSnapshot feature : dataSnapshot.child("features").getChildren()) {

                        //get all required data

                        time = Long.parseLong(feature.child("properties").child("time").getValue().toString());
                        if(new Date().getTime() - time > 11000) { //old data
                            //upload the JSON again
                            updateFirebase(CreateRequestUrl.URL_USGS(0), FirebaseDatabase.getInstance().getReference().getRoot());
                            return;
                        }

                        str1 = feature.child("properties").child("place").getValue().toString().trim().toUpperCase();
                        locationName = str1.substring(str1.indexOf("of") + 3);
                        sig = Integer.parseInt(feature.child("properties").child("sig").getValue().toString());
                        magnitude = Float.parseFloat(feature.child("properties").child("mag").getValue().toString());
                        longitude = Float.parseFloat(feature.child("geometry").child("coordinates").child("0").getValue().toString());
                        latitude = Float.parseFloat(feature.child("geometry").child("coordinates").child("1").getValue().toString());
                        depth = Float.parseFloat(feature.child("geometry").child("coordinates").child("2").getValue().toString());


                        //update Earthquake object
                        EarthQuakes eq = new EarthQuakes(); //EarthQuakes is a bean
                        eq.setSig(sig);
                        eq.setDateMilis(time);
                        eq.setDepth(round(depth, decimalPlace)); //depth means altitude. In this way, database is getting updated
                        eq.setLatitude(latitude);
                        eq.setLongitude(longitude);
                        eq.setLocationName(locationName);
                        eq.setMagnitude(round(magnitude, decimalPlace));

                        eq.Insert();

                        if (CheckRiskEarthquakes.checkRisky(latitude, longitude, sig)) {
                            RiskyEarthquakes riskyEarthquakes = new RiskyEarthquakes();
                            riskyEarthquakes.setSig(sig);
                            riskyEarthquakes.setDateMilis(time);
                            riskyEarthquakes.setDepth(round(depth, decimalPlace)); //depth means altitude. In this way, database is getting updated
                            riskyEarthquakes.setLatitude(latitude);
                            riskyEarthquakes.setLongitude(longitude);
                            riskyEarthquakes.setLocationName(locationName);
                            riskyEarthquakes.setMagnitude(round(magnitude, decimalPlace));
                            riskyEarthquakes.Insert();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void updateFirebase(final String url, final DatabaseReference databaseReference) { //save every earthquake fields like magnitude, latitude etc.

        try {

            final Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(OnLineTracker.DATEFORMAT).create();
            final Type listType = new TypeToken<POJOUSGS<String, MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>, Float>>() {
            }.getType();


            new Thread(new Runnable() { //should do network operation using separate thread; can't do from main thread
                @Override
                public void run() {
                    try {
                        jsonOriginal = getJson(url);

                        if (jsonOriginal == null || jsonOriginal.length() < 1) { // JSON is null or empty , jsonOriginal.length() defines the length of string
                            return;
                        }

                        Log.i("Jsonoriginal", jsonOriginal);
                        POJOUSGS<String, MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>, Float> items = gson.fromJson(jsonOriginal, listType);


                        if (items == null || items.getFeatures() == null || items.getFeatures().size() == 0) { //check if item null or items' FeaturesUSGS null or item's FeaturesUSGS empty
                            return;
                        }

                        Log.i("items", items.toString());

                        databaseReference.child("realTimeEarthquakes").setValue(items); //upload jsonOriginal on new "realTimeEarthquakes" node
                        String jsonString = "{  \n" +
                                "      \"metaInfo\": {\n" +
                                "        \"count\": \"serversCount\",\n" +
                                "        \"needToBeServer\": \"false\"\n" +
                                "      },\n" +
                                "      \"servers\": [\n" +
                                "        {\n" +
                                "          \"id\": \"myid\",\n" +
                                "          \"lastOnline\": \"timeMilis\"\n" +
                                "        }\n" +
                                "      ]\n" +
                                "   }\n";
                        Map<String, Object> jsonMap = new Gson().fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {
                        }.getType());


                        databaseReference.child("serverTrack").setValue(jsonMap); //upload jsonOriginal on new "realTimeEarthquakes" node


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();


        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }

    }

    public float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

}