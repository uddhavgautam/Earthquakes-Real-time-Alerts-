package com.liveEarthquakesAlerts.controller.services.locations;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.liveEarthquakesAlerts.R;
import com.liveEarthquakesAlerts.controller.services.earthquakes.EarthquakeService;
import com.liveEarthquakesAlerts.controller.utils.AppSettings;
import com.liveEarthquakesAlerts.controller.utils.CheckRiskEarthquakes;
import com.liveEarthquakesAlerts.controller.utils.MyOwnCustomLog;
import com.liveEarthquakesAlerts.model.LocationPOJO;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.LastEarthquakeDate;
import com.liveEarthquakesAlerts.model.database.RiskyEarthquakes;
import com.liveEarthquakesAlerts.view.MainActivity;
import com.odoo.FavoriteNumberBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocTrackService extends Service
        implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "LocTrackService";
    public static Handler mHandler;
    public static boolean isLocationUpdated = false;
    public int count = 0;
    public LocationRequest mLocationRequest;
    public GoogleApiClient mGoogleApiClient;
    public LocationSettingsRequest.Builder builderLocationSettings;
    public Location location; // location
    public LocationSettingsRequest mLocationSettingsRequest;
    private MyOwnCustomLog myOwnCustomLog = new MyOwnCustomLog();
    private String messageEarthquake;
    private Handler handler;


    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        Log.i(TAG, "On Create");
    }

    public void createLocationRequest() { //in main thread
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); //10 seconds
        mLocationRequest.setSmallestDisplacement(500); //500 meters changed
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        PendingResult<Status> pendingStatus = LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        pendingStatus.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    if (LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient) != null) {
                        isLocationUpdated = true;
                        LocationPOJO.location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        Log.i(TAG, String.valueOf(LocationPOJO.location.getLatitude() + " " + LocationPOJO.location.getLongitude()));

                        //Get GeoCoder
                        Geocoder geocoder;
                        List<Address> addresses;
                        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                        try {
                            addresses = geocoder.getFromLocation(LocationPOJO.location.getLatitude(), LocationPOJO.location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                            String city = addresses.get(0).getLocality();
                            String state = addresses.get(0).getAdminArea();
                            String country = addresses.get(0).getCountryName();
                            String postalCode = addresses.get(0).getPostalCode();
                            Log.i(TAG, address + ", " + city + ", " + state + ", " + postalCode + ", " + country);

                            //now I got the location, start EarthquakeService to fetch earthquakes

                            if (LocationPOJO.location != null)
                                getApplicationContext().startService(new Intent(getApplicationContext(), EarthquakeService.class));


                        } catch (IOException e) {
                            Log.i("GoogleAPIClient", " not connected yet sir!");
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public synchronized void buildLocationSettingsRequest() {
        builderLocationSettings = new LocationSettingsRequest.Builder(); //null builder
        builderLocationSettings.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builderLocationSettings.build();

    }

    public synchronized void buildGoogleApiClient() { //onHandleIntent thread
        //We need to monitor because
        mGoogleApiClient = new GoogleApiClient.Builder(this) //Note: GoogleApiClient is not thread safe
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "LocTrackService destroyed!");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { //main thread
        super.onStartCommand(intent, flags, startId);

        Log.i(TAG, "inside thread of LocTrackService!");

        buildGoogleApiClient();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        } else {
            Log.i(TAG, "GoogleApiClient couldn't build!");
        }

        return START_STICKY;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) { //This is Async request, obviously should be from the main thread. Because we are not sure,
        //after how many minutes it will get connected. Other threads are possible to die but not main thread
        count++;
        Log.i(TAG, "Inside GoogleApiClient onConnected");
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        String simpleName = this.getClass().getSimpleName();
        myOwnCustomLog.addLog(simpleName, Thread.currentThread().getStackTrace()[2].getMethodName().toString(), stackTraces);

        buildLocationSettingsRequest();
        createLocationRequest();

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient suspended!");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient failed!");

    }

    @Override
    public void onLocationChanged(Location location) { //main thread

        if (LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient) != null) {
            LocationPOJO.location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.i(TAG, String.valueOf(LocationPOJO.location.getLatitude() + " " + LocationPOJO.location.getLongitude()));

            //Get GeoCoder
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(this, Locale.getDefault());

            try {
                addresses = geocoder.getFromLocation(LocationPOJO.location.getLatitude(), LocationPOJO.location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                Log.i(TAG, address + ", " + city + ", " + state + ", " + postalCode + ", " + country);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        RiskyEarthquakes riskyEarthquakes = new RiskyEarthquakes();
        List<RiskyEarthquakes> allRiskyEarthquakes = riskyEarthquakes.GetAllData();

//          update the RiskyEarthquakes
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

    public void notificationHandler() {
        showNotification();
    }

    public void showNotification() {
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


