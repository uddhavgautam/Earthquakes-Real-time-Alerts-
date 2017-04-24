package com.liveEarthquakesAlerts.controller.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.liveEarthquakesAlerts.controller.utils.broadcastReceiver.OutgoingReceiver;
import com.liveEarthquakesAlerts.model.LocationPOJO;
import com.liveEarthquakesAlerts.model.database.DatabaseHelper;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.RiskyEarthquakes;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.POJOUSGS;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.featuresFolderUSGS.FeaturesUSGS;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.featuresFolderUSGS.insideFeaturesUSGS.GeometryUSGS;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.featuresFolderUSGS.insideFeaturesUSGS.PropertiesUSGS;
import com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.metadataFolderUSGS.MetadataUSGS;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static java.lang.Long.parseLong;

//We need database helper, so that, before we do insert new records, we clear database first

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class SaveResponseToDB { //this class updates EarthQuakes Bean


    public static boolean isInitialized = false;
    private static String locationName, jsonOriginal = null, str1;
    private static String TAG = "SaveResponseToDB";
    private static int unSuccessfulAttempts = 0;
    private static OutgoingReceiver outgoingReceiver;
    private static POJOUSGS<String, MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>, Float> items;
    private static Context context;
    private Integer sig, decimalPlace = 1;
    private long time;
    private Float longitude, latitude, depth, magnitude;

    public SaveResponseToDB() {
        DatabaseHelper.getDbHelper().clearDatabase();
    }

    public SaveResponseToDB(Context myContext) {
        context = myContext;
    }

    public static String getJson(String reqUrl) throws Exception {
        Request request = new Request.Builder().url(reqUrl).build(); //Request builder is used to get JSON url
        Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
        return response.isSuccessful() ? response.body().string() : "";
    }

    public static void updateFirebase(final String url, final DatabaseReference databaseReference) { //save every earthquake fields like magnitude, latitude etc.
        try {

            final Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(OnLineTracker.DATEFORMAT).create();
            final Type listType = new TypeToken<POJOUSGS<String, MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>, Float>>() {
            }.getType();

            try {
                jsonOriginal = getJson(url);

                if (jsonOriginal == null || jsonOriginal.length() < 1) { // JSON is null or empty , jsonOriginal.length() defines the length of string
                    return;
                }

//                        Log.i("Jsonoriginal", jsonOriginal);
                items = gson.fromJson(jsonOriginal, listType);


                if (items == null || items.getFeatures() == null || items.getFeatures().size() == 0) { //check if item null or items' FeaturesUSGS null or item's FeaturesUSGS empty
                    return;
                }

//                        update only if there is new data. This condition should not be for the first time
                if (!SaveResponseToDB.isInitialized) { //first time
                    doUpdate(items, databaseReference);
                } else {
                    Long newGeneratedTime = items.getMetadata().getGenerated();
                    Long oldGeneratedTimeInFirebase = getFirebaseTimeUsingCurl("https://earthquakesenotifications.firebaseio.com/realTimeEarthquakes/metadata/generated.json?print=pretty");

                    Log.i("Time ", " difference USGS-Firebase: " + String.valueOf(newGeneratedTime - oldGeneratedTimeInFirebase));


                    if (newGeneratedTime > oldGeneratedTimeInFirebase) {
                        doUpdate(items, databaseReference);
                    } else {
                        //do nothing
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }

    }

    private static void doUpdate(POJOUSGS<String, MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>, Float> items, DatabaseReference databaseReference) {
        databaseReference.child("realTimeEarthquakes").setValue(items); //upload jsonOriginal on new "realTimeEarthquakes" node
        String jsonString = "{  \n" +
                "      \"metaInfo\": {\n" +
                "        \"count\": \'serversCount\',\n" +
                "        \"onlineLastTime\": 1491873255092, \n" +
                "        \"needToBeServer\": \'false\'\n" +
                "      },\n" +
                "      \"servers\": [\n" +
                "        {\n" +
                "          \"id\": \"myid\",\n" +
                "          \"lastTime\": \'lastTime\'\n" +
                "        }\n" +
                "      ]\n" +
                "   }\n";

        Map<String, Object> jsonMap = new Gson().fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        databaseReference.child("serverTrack").setValue(jsonMap);//upload jsonOriginal on new "realTimeEarthquakes" node

        databaseReference.child("serverTrack").child("metaInfo").child("onlineLastTime").setValue(new Date().getTime());
        update_onlineLastTimeFirebase(databaseReference);
    }

    private static Long getFirebaseTimeUsingCurl(String urlStr) {
        Long myLong = 1l;
        Request request = new Request.Builder().url(urlStr).build(); //Request builder is used to get JSON url

        try {
            Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
            String[] str = response.body().string().split("\\n"); //because it prints with newline character with it
            myLong = parseLong(str[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myLong;

    }

    //do this based on exponential back off, otherwise collision happens
    private static void update_onlineLastTimeFirebase(final DatabaseReference databaseReference) {
        databaseReference.child("serverTrack").child("metaInfo").child("onlineLastTime").setValue(new Date().getTime()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    SaveResponseToDB.isInitialized = true; //I successfully initialized Firebase Realtime DB
//                    Pass this information to main activity

                    //send this true message via OutgoingReceiver
//                    Intent intentsda = new Intent();
//                    intentsda.setAction("SaveResponseToDB.isInitialized.Uddhav").putExtra("isInitializedAlready", SaveResponseToDB.isInitialized);
//                    Log.i(TAG, "loctracking service FROM LONG RUN");
//                    context.sendBroadcast(intentsda);
//                    Log.i(TAG, "Firebase onlineLastTime updated!");
                } else {
                    unSuccessfulAttempts++;
                    if (unSuccessfulAttempts > 2) {
                        SaveResponseToDB.isInitialized = false;
                        Log.i(TAG, "Firebase onlineLastTime tried 2 times, can't update!");
                        unSuccessfulAttempts = 0;
                        return;
                    }
                    update_onlineLastTimeFirebase(databaseReference);
                }
            }
        });

    }

    public static String getFirebaseWholeData(String urlStr) {
        String myStr = "";
        Request request = new Request.Builder().url(urlStr).build(); //Request builder is used to get JSON url

        try {
            Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
            String str[] = response.body().string().split("\\n"); //because it prints with newline character with it
            myStr = str[0];
            Log.i("FirebaseDB", " firstLine " + myStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myStr;
    }

    public void getDataFromFirebase(final DataSnapshot dataSnapshot) { //save every earthquake fields like magnitude, latitude etc.

        Thread thread = new Thread(new Runnable() { //should do network operation using separate thread; can't do from main thread
            @Override
            public void run() {
                try {
                    for (DataSnapshot feature : dataSnapshot.child("features").getChildren()) {

                        time = parseLong(feature.child("properties").child("time").getValue().toString());

                        str1 = feature.child("properties").child("place").getValue().toString().trim().toUpperCase();
                        locationName = str1.substring(str1.indexOf("of") + 3);
                        sig = Integer.parseInt(feature.child("properties").child("sig").getValue().toString());
                        magnitude = Float.parseFloat(feature.child("properties").child("mag").getValue().toString());
                        longitude = Float.parseFloat(feature.child("geometry").child("coordinates").child("0").getValue().toString());
                        latitude = Float.parseFloat(feature.child("geometry").child("coordinates").child("1").getValue().toString());
                        depth = Float.parseFloat(feature.child("geometry").child("coordinates").child("2").getValue().toString());


//                        update Earthquake object
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
                        Log.i("Bus", "posted successful event");

                        //I got the earthquakes

                        App.bus.post(new BusStatus(123)); //post event into the Otto bus
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void getPartialDataFromFirebase(final DataSnapshot dataSnapshot) { //save every earthquake fields like magnitude, latitude etc.

        Thread thread = new Thread(new Runnable() { //should do network operation using separate thread; can't do from main thread
            @Override
            public void run() {
                try {
                    for (DataSnapshot feature : dataSnapshot.child("features").getChildren()) {
                        float currentLat = (float) LocationPOJO.location.getLatitude();
                        float currentLong = (float) LocationPOJO.location.getLongitude();

                        longitude = Float.parseFloat(feature.child("geometry").child("coordinates").child("0").getValue().toString());
                        latitude = Float.parseFloat(feature.child("geometry").child("coordinates").child("1").getValue().toString());


                        if (((currentLong - 2.89 <= longitude) && (longitude <= currentLong + 2.89)) && ((currentLat - 2.91 <= latitude) && (latitude <= currentLat + 2.91))) {
                            time = parseLong(feature.child("properties").child("time").getValue().toString());

                            str1 = feature.child("properties").child("place").getValue().toString().trim().toUpperCase();
                            locationName = str1.substring(str1.indexOf("of") + 3);
                            sig = Integer.parseInt(feature.child("properties").child("sig").getValue().toString());
                            magnitude = Float.parseFloat(feature.child("properties").child("mag").getValue().toString());
                            depth = Float.parseFloat(feature.child("geometry").child("coordinates").child("2").getValue().toString());


//                        update Earthquake object
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
                        Log.i("Bus", "posted successful event");

                        //I got the earthquakes

                        App.bus.post(new BusStatus(123)); //post event into the Otto bus
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
}