package com.liveEarthquakesAlerts.controller.services.earthquakes;


import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
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
public class EarthquakeService extends Service {

    private static final String TAG = "EarthquakeService";
    private static long firebaseTime;
    private static long myLong = 1l;
    private DatabaseReference referenceEarthquakes;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "EarthquakeService created!");
    }


    private void fetchFromFirebase() {
        Log.i("fetch from", "databases updateasdf");

        final ValueEventListener valueEventListenerEarthquake = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (OnLineTracker.isOnline(getApplicationContext())) { //check every time online
                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                    clientHelper.getDataFromFirebase(dataSnapshot);

                } else {
                    App.bus.post(new BusStatus(999));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };

        referenceEarthquakes.addValueEventListener(valueEventListenerEarthquake);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        referenceEarthquakes = FirebaseDatabase.getInstance().getReference().getRoot().child("realTimeEarthquakes");



        Thread thdsds = new Thread(new Runnable() { //UI thread is not getting blocked
            @Override
            public void run() {
                Log.i("Thread watchout: ", Thread.currentThread().getName() + "");
                Log.i("Process watchout1: ", ActivityManager.RunningAppProcessInfo.class.getCanonicalName());

                int counttt = 0;

                while (true) {

                    Thread thsdfdsfds = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            Log.i("Thread watchoutfhghf: ", Thread.currentThread().getName() + "");
                            //before fetching check if real-time database has not been deleted since after you initialized
                            String myVarData = SaveResponseToDB.getFirebaseWholeData("https://earthquakesenotifications.firebaseio.com/realTimeEarthquakes.json?print=pretty");
                            if ((!myVarData.equals("null"))) { //realtime db already exists
                                fetchFromFirebase(); //if data changed then it fetches the earthquakes automatically

                                firebaseTime = getFirebaseTimeUsingCurl();
                                if (((new Date().getTime()) - firebaseTime) > 11000) {
                                    Log.i("Periodic", " updateasdf!");
                                    SaveResponseToDB.updateFirebase(CreateRequestUrl.URL_USGS(), FirebaseDatabase.getInstance().getReference().getRoot());
                                    try {
                                        Thread.currentThread().sleep(11000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                Log.i("else", "no real time db");
                                SaveResponseToDB.isInitialized = false;//initialized but not properly. Therefore isInitialized = false
                                SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                                SaveResponseToDB.updateFirebase(CreateRequestUrl.URL_USGS(), FirebaseDatabase.getInstance().getReference().getRoot());
                            }
                        }
                    });

                    try {
                        thsdfdsfds.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(21000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thdsds.setPriority(Thread.MAX_PRIORITY);
        thdsds.start();

        return START_STICKY;
    }

    private long getFirebaseTimeUsingCurl() {

        Request request = new Request.Builder().url("https://earthquakesenotifications.firebaseio.com/serverTrack/metaInfo/onlineLastTime.json?print=pretty").build(); //Request builder is used to get JSON url

        try {
            Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
            String[] str = response.body().string().split("\\n"); //because it prints with newline character with it
            Log.i("myLong", str[0]);

            myLong = Long.parseLong(str[0]);
            Log.i("myLong", myLong + "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myLong;

    }

    @Override
    public void onDestroy() {
//        App.bus.unregister(this);
        Log.i(TAG, "EarthquakeService destroyed!");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}