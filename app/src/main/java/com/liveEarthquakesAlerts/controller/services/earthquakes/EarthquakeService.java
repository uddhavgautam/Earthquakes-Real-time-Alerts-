package com.liveEarthquakesAlerts.controller.services.earthquakes;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.liveEarthquakesAlerts.controller.utils.CreateRequestUrl;
import com.liveEarthquakesAlerts.controller.utils.SaveResponseToDB;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class EarthquakeService extends Service {

    private static final String TAG = "EarthquakeService";

    public static Thread insideThread;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//metaInfo changes on every Firebase Update
        Thread wrapperThread = new Thread(new Runnable() { //UI thread is not getting blocked
            @Override
            public void run() {
                while (true) {
                    insideThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().getRoot();

                            String myVarData = SaveResponseToDB.checkIfFirebaseHasData("https://earthquakesenotifications.firebaseio.com/realTimeEarthquakes.json?print=pretty");
                            if ((!myVarData.equals("null"))) {
                                SaveResponseToDB.DoFirebaseUpdateOnNeed(SaveResponseToDB.isInitialized, CreateRequestUrl.requestUSGSUsingHttp(), databaseReference);
                            } else {
                                SaveResponseToDB.isInitialized = false;
                                SaveResponseToDB.DoFirebaseUpdateOnNeed(SaveResponseToDB.isInitialized, CreateRequestUrl.requestUSGSUsingHttp(), databaseReference);
                            }
                        }
                    });
                    insideThread.start();
                    try {
                        Thread.sleep(11000); //If I keep getting earthquakes Updates from Firebase, I keep sleeping this thread
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

    @Override
    public void onDestroy() {
        Log.i(TAG, "EarthquakeService destroyed!");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}