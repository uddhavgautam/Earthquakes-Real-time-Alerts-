package com.liveEarthquakesAlerts.controller.services.locations;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.liveEarthquakesAlerts.R;
import com.liveEarthquakesAlerts.controller.utils.AppSettings;
import com.liveEarthquakesAlerts.controller.utils.CheckRiskEarthquakes;
import com.liveEarthquakesAlerts.model.LocationPOJO;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.LastEarthquakeDate;
import com.liveEarthquakesAlerts.model.database.RiskyEarthquakes;
import com.liveEarthquakesAlerts.view.MainActivity;
import com.odoo.FavoriteNumberBean;

import java.util.ArrayList;
import java.util.List;

/* The IntentService class provides a straightforward structure for running an operation on a single background thread. */
public class LocationTracker extends Service
        implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = "LocationTracker";
    public static boolean isServiceRunning = false;
    private Handler handler;
    private List<RiskyEarthquakes> riskyEarthquakes;
    private int count = 0;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private LocationSettingsRequest.Builder builderLocationSettings;

    private Location location; // location
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationPOJO locationPOJO;
    private String messageEarthquake;

    public LocationTracker() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "On Create");
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        handler = new Handler(Looper.getMainLooper());
        super.onCreate();


    }

    protected void createLocationRequest() {
        //remove location updates so that it resets
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this); //Import should not be android.Location.LocationListener
        //import should be import com.google.android.gms.location.LocationListener;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setSmallestDisplacement(500); //500 meters changed
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //restart location updates with the new interval
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    protected synchronized void buildLocationSettingsRequest() {
        builderLocationSettings = new LocationSettingsRequest.Builder(); //null builder
        builderLocationSettings.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builderLocationSettings.build();

    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { //this calls onStart()

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed!");
        isServiceRunning = false;
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        count++;
        Log.i(TAG, "GoogleApiClient connected!");
//        buildLocationSettingsRequest();
        createLocationRequest();
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LocationPOJO.location = location;
        Log.i(TAG, " LocationWhat: " + count + " " + LocationPOJO.location); //may return null because, I can't guarantee location
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient failed!");

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location Changed!");
        this.location = location;

        //update bean on every location changed
        LocationPOJO.location = location;

        //keep tracking of every risky earthquakes until they are no more dangerous to victim
        RiskyEarthquakes riskyEarthquakes = new RiskyEarthquakes();
        List<RiskyEarthquakes> allRiskyEarthquakes = riskyEarthquakes.GetAllData();

        //update the RiskyEarthquakes
        for (RiskyEarthquakes r : allRiskyEarthquakes) {
            if (!CheckRiskEarthquakes.checkRisky(r)) {
                r.DeleteRow(r.getDateMilis());
            }
        }

        for (RiskyEarthquakes r : allRiskyEarthquakes) {
            while (CheckRiskEarthquakes.checkRisky(r)) {
                //Notify user
                notificationHandler();
                //Notify emergency only one time, then pop the "I am Ok" button to click and push messages "I am ok"
                sendMsgToEmergencyContacts();
            }
        }
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
        List<RiskyEarthquakes> newEarthquakes = new RiskyEarthquakes().newEarthquakes();

        if (newEarthquakes.size() > 0) { //if there are earthquakes

            if (AppSettings.getInstance().isNotifications()) {
                createNotification(getString(R.string.EarthquakesDetect), "" + newEarthquakes.get(0).getMagnitude() + "  |  " + newEarthquakes.get(0).getLocationName());
                messageEarthquake = "Earthquake Hit !!" + newEarthquakes.get(0).getMagnitude() + "  |  " + newEarthquakes.get(0).getLocationName();

            }


            LastEarthquakeDate led = new LastEarthquakeDate();
            led.setDateMilis(new EarthQuakes().GetLastEarthQuakeDate());
            led.Insert();
        }
    }

    private void sendMsgToEmergencyContacts() {
        FavoriteNumberBean favoriteNumberBean = new FavoriteNumberBean(false);
        ArrayList<String> mobileList = favoriteNumberBean.getMobileNumber();
        SmsManager smsManager = SmsManager.getDefault();
        //send every emergency contacts the messages
        for (String phone : mobileList) {
            smsManager.sendTextMessage(phone, null, messageEarthquake, null, null);
        }
        //now create "I am Ok button", tap it to send "I am Ok" messages to every emergency contacts

        handler.post(new Runnable() {
            @Override
            public void run() {
                //create a floating button
                FloatingActionButton floatingActionButton = new FloatingActionButton(getApplicationContext());
                floatingActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FavoriteNumberBean favoriteNumberBean = new FavoriteNumberBean(false);
                        ArrayList<String> mobileList = favoriteNumberBean.getMobileNumber();
                        SmsManager smsManager = SmsManager.getDefault();
                        //send every emergency contacts the messages
                        for (String phone : mobileList) {
                            smsManager.sendTextMessage(phone, null, "I am Ok!", null, null);
                        }
                    }
                });
            }
        });
    }

    private void createNotification(String strContentTitle, String strContentText) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext()) //
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
}