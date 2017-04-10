package com.liveEarthquakesAlerts.controller.services.earthquakes;


import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.liveEarthquakesAlerts.controller.utils.App;
import com.liveEarthquakesAlerts.controller.utils.BusStatus;
import com.liveEarthquakesAlerts.controller.utils.CreateRequestUrl;
import com.liveEarthquakesAlerts.controller.utils.OnLineTracker;
import com.liveEarthquakesAlerts.controller.utils.SaveResponseToDB;

import java.io.IOException;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class EarthquakeIntentService extends IntentService {

    private static final String TAG = "EarthquakeIntentService";
    private static long firebaseTime;
    private DatabaseReference referenceEarthquakes;
    private DatabaseReference referenceUpdateTime;


    public EarthquakeIntentService() {
        super("EarthquakeIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "EarthquakeIntentService created!");
    }


    private void fetchFromFirebase() {
        Log.i("fetch from", "databases updateasdf");

        final ValueEventListener valueEventListenerEarthquake = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (OnLineTracker.isOnline(getApplicationContext())) { //check every time online
                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                    Log.i("Inside", "on start command!");
                    clientHelper.getDataFromFirebase(dataSnapshot);

                } else {
                    App.bus.post(new BusStatus(999));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        referenceEarthquakes.addValueEventListener(valueEventListenerEarthquake);
    }

    @Override
    public void setIntentRedelivery(boolean enabled) {
        super.setIntentRedelivery(enabled);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        referenceEarthquakes = FirebaseDatabase.getInstance().getReference().getRoot().child("realTimeEarthquakes");
        referenceUpdateTime = FirebaseDatabase.getInstance().getReference().getRoot().child("serverTrack").child("metaInfo").child("onlineLastTime");

        fetchFromFirebase(); //if data changed then it fetches the earthquakes automatically


        while (true) {
            Thread ttttfs = new Thread(new Runnable() {
                @Override
                public void run() {
                    firebaseTime = getFirebaseTimeUsingCurl("https://earthquakesenotifications.firebaseio.com/serverTrack/metaInfo/onlineLastTime.json?print=pretty");
                    if (((new Date().getTime()) - firebaseTime) > 11000) {
                        Log.i("Periodic", " updateasdf!");
                        SaveResponseToDB.updateFirebase(CreateRequestUrl.URL_USGS(), FirebaseDatabase.getInstance().getReference().getRoot());
                    }
                }
            });
            ttttfs.start();
            try {
                ttttfs.sleep(11000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

//        Looper.loop(); //keep current thread alive

// curl 'https://earthquakesenotifications.firebaseio.com/serverTrack/metaInfo/onlineLastTime.json?print=pretty'

    }

    private Long getFirebaseTimeUsingCurl(String urlStr) {
        Long myLong = 1l;
        Request request = new Request.Builder().url(urlStr).build(); //Request builder is used to get JSON url

        try {
            Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
            String[] str = response.body().string().split("\\n"); //because it prints with newline character with it
            myLong = Long.parseLong(str[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myLong;

    }

    @Override
    public void onDestroy() {
        App.bus.unregister(this);
        Log.i(TAG, "EarthquakeIntentService destroyed!");
    }

}