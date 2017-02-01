package com.LiveEarthquakesAlerts.controller.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.LiveEarthquakesAlerts.R;
import com.LiveEarthquakesAlerts.model.database.EarthQuakes;
import com.LiveEarthquakesAlerts.model.database.LastEarthquakeDate;
import com.LiveEarthquakesAlerts.view.MainActivity;

import java.util.List;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class SyncService extends Service {

    public static boolean isServiceRunning = false;
    public static Context AppContextService;
    private int mStartMode;
    private IBinder mBinder;
    private boolean mAllowRebind;
    private Handler handler;

    @Override
    public void onCreate() {
        AppContextService = getApplicationContext();
        App.bus.register(this);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isServiceRunning = true;

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (isServiceRunning) {

                    if (OnLineTracker.isOnline(AppContextService)) {

                        Log.i("SyncService", "Service Running");

                        updateSyncPeriod(); //based on Firebase'r response

                        int day = AppSettings.getInstance().getTimeInterval();
                        SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor

                        if (AppSettings.getInstance().getProximityMiles() == 0) { //world-wide Json
                            clientHelper.saveDatabaseUsgs(CreateRequestUrl.URL_USGS(day));
                        } else if (AppSettings.getInstance().getProximityMiles() == 200) { //user-customized json
                            clientHelper.saveDatabaseUsgs(CreateRequestUrl.URL_USGS(day, 200, MainActivity.currentLoc)); //Json of 200 miles bbox
                        } else {
                            //do nothing
                        }


                        notificationHandler();

                        App.bus.post(new BusStatus(123)); //post event into the Otto bus

                    } else {
                        App.bus.post(new BusStatus(999));
                    }

                    try {
                        Thread.sleep(1000 * 60 * OnLineTracker.syncPeriod);
                    } catch (InterruptedException e) {
                        OnLineTracker.catchException(e);
                    }
                }

            }
        }).start();

        return Service.START_STICKY;
    }

    private void updateSyncPeriod() {
        //updates whenever firebase tells

        OnLineTracker.syncPeriod = 2; //in every two minutes

    }

    private void notificationHandler() {

        handler.post(new Runnable() {
            @Override
            public void run() {
                showNotification();
            }
        });
    }

    private void showNotification() {
        List<EarthQuakes> newEarthquakes = new EarthQuakes().newEarthquakes();

        if (newEarthquakes.size() > 0) { //if there are earthquakes

            if (AppSettings.getInstance().isNotifications()) {
                if (riskyEarthquakes()) {
                    createNotification(getString(R.string.EarthquakesDetect), "" + newEarthquakes.get(0).getMagnitude() + "  |  " + newEarthquakes.get(0).getLocationName());
                    sendMsgToEmergencyContacts();
                }
            }

            LastEarthquakeDate led = new LastEarthquakeDate();
            led.setDateMilis(new EarthQuakes().GetLastEarthQuakeDate());
            led.Insert();
        }
    }

    private void sendMsgToEmergencyContacts() {
        //if automated async send is successful, for each successful message say "I am ok" tapping "Hey, I am OK" button
    }

    private boolean riskyEarthquakes() {

        return true;
    }

    private void createNotification(String strContentTitle, String strContentText) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(SyncService.this) //
                .setSmallIcon(R.drawable.icon1) //
                .setContentTitle(strContentTitle) //
                .setContentText(strContentText);

        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);
        builder.setAutoCancel(true);
        builder.setLights(Color.BLUE, 500, 500);

        if (AppSettings.getInstance().isVibration()) {
            long[] pattern = {500, 500};
            builder.setVibrate(pattern);
        }
        if (AppSettings.getInstance().isSound()) {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(alarmSound);
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
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
        isServiceRunning = false;
        App.bus.unregister(this);
    }

}