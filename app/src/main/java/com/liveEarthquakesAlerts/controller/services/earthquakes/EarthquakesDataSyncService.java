package com.liveEarthquakesAlerts.controller.services.earthquakes;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.liveEarthquakesAlerts.controller.utils.App;
import com.liveEarthquakesAlerts.controller.utils.BusStatus;
import com.liveEarthquakesAlerts.controller.utils.OnLineTracker;
import com.liveEarthquakesAlerts.controller.utils.SaveResponseToDB;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class EarthquakesDataSyncService extends Service {

    public static Context AppContextService;
    private int mStartMode;
    private IBinder mBinder;
    private boolean mAllowRebind;
    private DatabaseReference databaseReference;
//    private Handler handler;

    @Override
    public void onCreate() {
        AppContextService = getApplicationContext();
        App.bus.register(this);
        databaseReference = FirebaseDatabase.getInstance().getReference().getRoot().child("realTimeEarthquakes");

//        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { //this will call onStart()
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (OnLineTracker.isOnline(AppContextService)) { //check every time online
                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                    Log.i("Inside", "on start command!");
                    clientHelper.getDataFromFirebase(dataSnapshot);
                    App.bus.post(new BusStatus(123)); //post event into the Otto bus

                } else {
                    App.bus.post(new BusStatus(999));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        databaseReference.addValueEventListener(valueEventListener);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {

    }

    @Override
    public void onDestroy() {
        App.bus.unregister(this);
    }

}