package com.liveEarthquakesAlerts.controller.services.earthquakes;


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
import com.liveEarthquakesAlerts.controller.utils.AppSettings;
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
    }


    private void fetchFromFirebase() {

        final ValueEventListener valueEventListenerEarthquake = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (OnLineTracker.isOnline(getApplicationContext())) { //check every time online
                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
//                    clientHelper.getDataFromFirebase(dataSnapshot); //makes download data from Firebase
                    clientHelper.getDataFromUSGS();

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

    private void fetchPartialDataFromFirebase() {

        final ValueEventListener valueEventListenerEarthquake = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (OnLineTracker.isOnline(getApplicationContext())) { //check every time online
                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
//                    clientHelper.getPartialDataFromFirebase(dataSnapshot); //downloads data from Firebase
                    clientHelper.getPartialDataFromUSGS();

                } else {
                    App.bus.post(new BusStatus(999));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//        referenceEarthquakes = FirebaseDatabase.getInstance().getReference().getRoot().child("realTimeEarthquakes");
        referenceEarthquakes = FirebaseDatabase.getInstance().getReference().getRoot().child("serverTrack").child("metaInfo");
//metaInfo changes on every Firebase Update
        Thread wrapperThread = new Thread(new Runnable() { //UI thread is not getting blocked
            @Override
            public void run() {
                while (true) {
                    Thread insideThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            //before fetching check if real-time database has not been deleted since after you initialized
                            String myVarData = SaveResponseToDB.checkIfFirebaseHasData("https://earthquakesenotifications.firebaseio.com/realTimeEarthquakes.json?print=pretty");
                            Log.i("myVarData", "\"" + myVarData + "\"" + " hello gautam! " + myVarData.equals(null));
                            if ((!myVarData.equals("null"))) { //realtime db already exists
                                if (AppSettings.getInstance().getProximity() == 0) {
//                                    fetchFromFirebase(); //if data changed then it fetches the earthquakes automatically
                                    Log.i("fullfetch", "dlfjs");
                                    fetchFromUSGS();
                                } else {
//                                    fetchPartialDataFromFirebase(); //if data changed then it fetches the earthquakes automatically
                                    Log.i("Partialfetch", "dlfjs");
                                    fetchPartialDataFromUSGS();
                                }
                                firebaseTime = getFirebaseTimeUsingCurl();
                                if (((new Date().getTime()) - firebaseTime) > 11000) {
                                    SaveResponseToDB.updateFirebase(CreateRequestUrl.URL_USGSAlwaysFullUpdate(), FirebaseDatabase.getInstance().getReference().getRoot());
                                    try {
                                        Thread.currentThread().sleep(11000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                SaveResponseToDB.isInitialized = false;//initialized but not properly. Therefore isInitialized = false
                                SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                                SaveResponseToDB.updateFirebase(CreateRequestUrl.URL_USGSAlwaysFullUpdate(), FirebaseDatabase.getInstance().getReference().getRoot());
                            }
                        }
                    });
                    try {
                        insideThread.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(11000); //11 seconds //Let's change it to 51 seconds (doing this optimizes my processor), because millions of clients update this.
                        //When they update, I get the new data.
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        wrapperThread.setPriority(Thread.MAX_PRIORITY);
        wrapperThread.start();

        return START_STICKY;
    }

    private void fetchPartialDataFromUSGS() {

        if (OnLineTracker.isOnline(getApplicationContext())) { //check every time online
            SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
//                    clientHelper.getPartialDataFromFirebase(dataSnapshot); //downloads data from Firebase
            clientHelper.getPartialDataFromUSGS();

        } else {
            App.bus.post(new BusStatus(999));
        }
    }

    private void fetchFromUSGS() {
        if (OnLineTracker.isOnline(getApplicationContext())) { //check every time online
            SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
//                    clientHelper.getDataFromUSGS(dataSnapshot); //downloads data from Firebase
            clientHelper.getDataFromUSGS();

        } else {
            App.bus.post(new BusStatus(999));
        }
    }

    private long getFirebaseTimeUsingCurl() {

        Request request = new Request.Builder().url("https://earthquakesenotifications.firebaseio.com/serverTrack/metaInfo/onlineLastTime.json?print=pretty").build(); //Request builder is used to get JSON url

        try {
            Response response = new OkHttpClient().newCall(request).execute(); //OkHttpClient is HTTP client to request
            String[] str = response.body().string().split("\\n"); //because it prints with newline character with it
            for (String sttsf : str) {
                Log.i("strsfdas", sttsf);
            }
//            Log.i("myLong", str[0]);
//            myLong = Long.parseLong(str[0]);
//            Log.i("myLong", myLong + "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 234l;

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