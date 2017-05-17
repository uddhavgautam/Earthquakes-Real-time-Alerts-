package com.liveEarthquakesAlerts.controller.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import com.liveEarthquakesAlerts.view.MainActivity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static java.lang.Long.parseLong;

//We need database helper, so that, we clear database before we insert new records

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class SaveResponseToDB { //this class updates EarthQuakes Bean


    public static boolean isInitialized = false;
    private static String locationName, str1;
    private static String TAG = "SaveResponseToDB";
    private static OutgoingReceiver outgoingReceiver;
    private static Context context;
    private static Integer sig;
    private static Integer decimalPlace = 1;
    private static long time;
    private static Float longitude;
    private static Float latitude;
    private static Float depth;
    private static Float magnitude;


    public SaveResponseToDB() {
        DatabaseHelper.getDbHelper().clearDatabase();
    }

    public SaveResponseToDB(Context myContext) {
        context = myContext;
    }

    public static String getJson(String reqUrl) {
        Request request = new Request.Builder().url(reqUrl).build(); //Request builder is used to get JSON url
        try {
            Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
            if (response.isSuccessful()) {
                String json = response.body().string(); //because it prints with newline character with it
                Log.i("jsonStr", json);
                return json;
            } else return "";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void DoFirebaseUpdateOnNeed(boolean isInitialized, final String url /* from */, final DatabaseReference databaseReference /* to */) { //save every earthquake fields like magnitude, latitude etc.
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(OnLineTracker.DATEFORMAT).create();

            Type listType = new TypeToken<POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>>>() {
            }.getType();
            POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>> items;

            try {
//               update Firebase only when there is old data. This condition should not be for the first time
                if (!isInitialized) { // no data in Firebase
                    String json = getJson(url); //first time or when it knows there is no data in Firebase, so without checking if-modified-since
                    Log.i("Broadcast", "  executed!");

                    if (json == null || json.length() < 1) { // JSON is null or empty , jsonOriginal.length() defines the length of string
                        return;
                    }
                    items = gson.fromJson(json, listType);

                    if (items == null || items.getFeatures() == null || items.getFeatures().size() == 0) { //check if item null or items' FeaturesUSGS null or item's FeaturesUSGS empty
                        return;
                    }
                    updateFirebaseDatabase(items.getMetadata(), databaseReference); //After firebase upload, do always update Local database also

                } else {
                    String json = "";
                    if (USGSGotNewData(MainActivity.modifiedCheckTime)) { //modifiedCheckTime gets updated on each Firebase update
                        json = getJson(url);
                    } else return;
                    Log.i("Broadcast", " receiverdfgdfgdfgdg executed!");

                    if (json == null || json.length() < 1) { // JSON is null or empty , jsonOriginal.length() defines the length of string
                        return;
                    }
                    items = gson.fromJson(json, listType);

                    if (items == null || items.getFeatures() == null || items.getFeatures().size() == 0) { //check if item null or items' FeaturesUSGS null or item's FeaturesUSGS empty
                        return;
                    }
                    //decision making before updating Firebase
                    Long newGeneratedTime = items.getMetadata().getGenerated();
                    Log.i("newGenerate ", "GenerateTime " + newGeneratedTime + "");
                    Long oldGeneratedTimeInFirebase = getDeneratedTimeFirebase();

                    Log.i("oldGenerate ", "GenerateTime " + oldGeneratedTimeInFirebase + "");
                    Log.i("oldGenerate", newGeneratedTime - oldGeneratedTimeInFirebase + " difference");


                    if (newGeneratedTime > oldGeneratedTimeInFirebase) { //If our data is newer than Firebase
                        updateFirebaseDatabase(items.getMetadata(), databaseReference);
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

    private static void updateLocalDatabases(POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>> items) {
        //update local database also
        for (FeaturesUSGS<PropertiesUSGS, GeometryUSGS> item : items.getFeatures()) { //each item means each feature, which is each earthquake record

            time = parseLong(item.getProperties().getTime());
            str1 = item.getProperties().getPlace().toString().trim().toUpperCase();
            locationName = str1.substring(str1.indexOf("of") + 3);
            sig = item.getProperties().getSig();
            magnitude = item.getProperties().getMag();
            longitude = item.getGeometry().getCoordinates().get(0);
            latitude = item.getGeometry().getCoordinates().get(1);
            depth = item.getGeometry().getCoordinates().get(2);


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

    private static void updateFirebaseDatabase(final MetadataUSGS items, final DatabaseReference databaseReference) {

        databaseReference.child("realTimeEarthquakes").setValue(items);

        String jsonString = "{\"metaInfo\":{\"onlineLastTime\":1491873255092}}";

        Map<String, Object> jsonMap = new Gson().fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        databaseReference.child("serverTrack").setValue(jsonMap);   /* upload jsonOriginal on new "serverTrack" node */

        databaseReference.child("serverTrack").child("metaInfo").child("onlineLastTime").setValue(new Date().getTime()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    SaveResponseToDB.isInitialized = true;
                    App.bus.post(new BusStatus(1234)); //post event into the Otto bus // for broadcasting

                    //update modifiedCheckTime
                    SimpleDateFormat dateFormat2 = new SimpleDateFormat("E',' dd MMM yyyy kk:mm:ss 'GMT'"); //instead of hh, use kk for 24 hours format
                    TimeZone timeZone2 = TimeZone.getTimeZone("GMT");
                    dateFormat2.setTimeZone(timeZone2);
                    Calendar cal1 = Calendar.getInstance(timeZone2);
                    Date firebaseUpdatedTime = cal1.getTime();
                    MainActivity.modifiedCheckTime = dateFormat2.format(firebaseUpdatedTime);
                    Log.i("ModifiedTm ", MainActivity.modifiedCheckTime);

                    DatabaseReference realTimeEarthquakes = FirebaseDatabase.getInstance().getReference().getRoot().child("realTimeEarthquakes");

                    MainActivity.FirebaseSync(context, realTimeEarthquakes);
                }
            }
        });
    }
    public static String checkIfFirebaseHasData(String urlStr) { //download from Firebase
        String myStr = "";
        Request request = new Request.Builder().url(urlStr).build(); //Request builder is used to get JSON url

        try {
            Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
            String str[] = response.body().string().split("\\n"); //because it prints with newline character with it
            myStr = str[0];
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("Checkif", myStr);
        return myStr;
    }

    private static Long getDeneratedTimeFirebase() {
        Long myLong = 1l;
        Log.i("GenerateTIme", "check");
        Request request = new Request.Builder().url("https://earthquakesenotifications.firebaseio.com/realTimeEarthquakes/generated.json?print=pretty").build(); //Request builder is used to get JSON url

        try {
            Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request

            if (response.isSuccessful()) {
                String[] str = response.body().string().split("\\n"); //because it prints with newline character with it
                myLong = Long.parseLong(str[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("GeneratedFirebase ", myLong + "");
        return myLong;

    }


    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    // Using HTTP_NOT_MODIFIED
    public static boolean USGSGotNewData(String modifiedCheckTime) {
        OkHttpClient httpClient2 = new OkHttpClient();
        Request request2 = new Request.Builder()  //note, Builder Design Pattern, it can make out of memory
                .url("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_hour.geojson")
                .addHeader("If-Modified-Since", modifiedCheckTime)
                .build();
        Response response = null;
        try {
            response = httpClient2.newCall(request2).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response.isSuccessful()) { //if modified
            Log.i("Request234", "successful!");
            return true;

        } else { //if not modified
            Log.i("Request234", "Unsuccessful!");
            return false;
        }
    }

    public void getDataFromUSGSCalledFromDataListener() { //don't update firebase here
        new Thread(new Runnable() {
            @Override
            public void run() {
                Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(OnLineTracker.DATEFORMAT).create();
                Type listType = new TypeToken<POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>>>() {
                }.getType();

                String url = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_hour.geojson";
                String json = getJson(url); //don't need to check http if-modified-since

                if (json == null || json.length() < 1) { // JSON is null or empty , json.length() defines the length of string
                    return;
                }

                POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>> items = gson.fromJson(json, listType);
                if (items == null || items.getFeatures() == null || items.getFeatures().size() == 0) { //check if item null or items' features null or item's features empty
                    return;
                }
                updateLocalDatabases(items);
            }
        }).start();

    }

    public void getPartialDataFromUSGS() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(OnLineTracker.DATEFORMAT).create();

            Type listType = new TypeToken<POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>>>() {
            }.getType();

            float currentLat = (float) LocationPOJO.location.getLatitude();
            float currentLong = (float) LocationPOJO.location.getLongitude();
            float minLatValue, minLongValue, maxLatValue, maxLongValue;
            minLatValue = (float) (currentLat - 2.91);
            maxLatValue = (float) (currentLat + 2.91);
            minLongValue = (float) (currentLong - 2.89);
            maxLongValue = (float) (currentLong + 2.91);

            String url = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=JSON&minlatitude=-" + minLatValue + "&minlongitude=-" + minLongValue + "&maxlatitude=-" + maxLatValue + "&maxlongitude=-" + maxLongValue;
            String json = "";
            if (USGSGotNewData(MainActivity.modifiedCheckTime)) {
                json = getJson(url);
            } else return;

            if (json == null || json.length() < 1) { // JSON is null or empty , json.length() defines the length of string
                return;
            }

            POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>> items = gson.fromJson(json, listType);

            if (items == null || items.getFeatures() == null || items.getFeatures().size() == 0) { //check if item null or items' features null or item's features empty
                return;
            }
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().getRoot();

            updateFirebaseDatabase(items.getMetadata(), databaseReference); //update only metadata
            updateLocalDatabases(items); //update USGS also. It means you are updating just metadata for notification

        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }
    }

    public void getPartialDataFromUSGSCalledFromDataListener() {
        try {

            Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(OnLineTracker.DATEFORMAT).create();

            Type listType = new TypeToken<POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>>>() {
            }.getType();

            float currentLat = (float) LocationPOJO.location.getLatitude();
            float currentLong = (float) LocationPOJO.location.getLongitude();
            float minLatValue, minLongValue, maxLatValue, maxLongValue;
            minLatValue = (float) (currentLat - 2.91);
            maxLatValue = (float) (currentLat + 2.91);
            minLongValue = (float) (currentLong - 2.89);
            maxLongValue = (float) (currentLong + 2.91);

            String url = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=JSON&minlatitude=-" + minLatValue + "&minlongitude=-" + minLongValue + "&maxlatitude=-" + maxLatValue + "&maxlongitude=-" + maxLongValue;
            String json = "";
            if (USGSGotNewData(MainActivity.modifiedCheckTime)) {
                json = getJson(url);
            } else return;

            if (json == null || json.length() < 1) { // JSON is null or empty , json.length() defines the length of string
                return;
            }
            POJOUSGS<MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>> items = gson.fromJson(json, listType);

            if (items == null || items.getFeatures() == null || items.getFeatures().size() == 0) { //check if item null or items' features null or item's features empty
                return;
            }

            updateLocalDatabases(items); //update USGS also. It means you are updating just metadata for notification

        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }
    }

}