package com.liveEarthquakesAlerts.controller.services.locations;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
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
    public static boolean isLocationUpdated = false;
    public static GoogleApiClient mGoogleApiClient;
    public int count = 0;
    public LocationRequest mLocationRequest;
    public LocationSettingsRequest.Builder builderLocationSettings;
    public Location location; // location
    public LocationSettingsRequest mLocationSettingsRequest;
    private MyOwnCustomLog myOwnCustomLog = new MyOwnCustomLog();
    private String messageEarthquake;
    private Handler handler;
    private Context context1;
    private boolean buildFlag = false;

    public LocTrackService(Context mainActivityContext) {
        this.context1 = mainActivityContext;
    }

    public LocTrackService() {
    }

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
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        PendingResult<Status> pendingStatus = LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        pendingStatus.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    if (ContextCompat.checkSelfPermission(LocTrackService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(LocTrackService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

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

        buildFlag = true;
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

        if (!buildFlag) {
            buildGoogleApiClient();
        }
        if (mGoogleApiClient != null) {
            if (!mGoogleApiClient.isConnected()) { /* if already not connected then only connect */
                mGoogleApiClient.connect();
            } else {
                if (isLocationUpdated) {
                    if (LocationPOJO.location != null) {
                        getApplicationContext().startService(new Intent(getApplicationContext(), EarthquakeService.class));
                    }
                }
            }
        } else {
            Log.i(TAG, "inside thread of sfasdfafas null GoogleApiClient !");
            Log.i(TAG, "GoogleApiClient couldn't build!");
        }

        return START_STICKY;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) { //This is Async request, obviously should be from the main thread. Because we are not sure,
        //after how many minutes it will get connected. Other threads are possible to die but not main thread
        count++;
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        String simpleName = this.getClass().getSimpleName();
        myOwnCustomLog.addLog(simpleName, Thread.currentThread().getStackTrace()[2].getMethodName().toString(), stackTraces);

        buildLocationSettingsRequest();

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mGoogleApiClient != null) {
                    if (!mGoogleApiClient.isConnected()) {/* if already not connected then only connect */
                        mGoogleApiClient.connect();

                    } else {
                        createLocationRequest(); //Because LocationRequest permission can only be done from the Activity
                    }
                }
            }
        });

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
         
            return;
        }
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

        //track the most risky earthquake

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

    public void showNotification() {
        List<RiskyEarthquakes> newEarthquakes = new RiskyEarthquakes().newEarthquakes();

        if (newEarthquakes.size() > 0) { //if there are earthquakes

            if (AppSettings.getInstance().isNotifications()) {
                createNotification(getString(R.string.EarthquakesDetect), "" + newEarthquakes.get(0).getMagnitude() + "  |  " + newEarthquakes.get(0).getLocationName());
                messageEarthquake = "Earthquake Hit !!" + newEarthquakes.get(0).getMagnitude() + "  |  " + newEarthquakes.get(0).getLocationName();

            }

            LastTimeEarthquake led = new LastTimeEarthquake();
            led.setDateMilis(new EarthQuakes().GetLastEarthQuakeDate());
            led.Insert();
        }
    }

    private void sendMsgToEmergencyContacts() {
        FavoriteNumberBean favoriteNumberBean = new FavoriteNumberBean(false);
        ArrayList<String> mobileList = favoriteNumberBean.getMobileNumber();
        final SmsManager[] smsManager = {SmsManager.getDefault()};
//send every emergency contacts the messages
        for (String phone : mobileList) {
            smsManager[0].sendTextMessage(phone, null, messageEarthquake, null, null);
        }
//now create "I am Ok button", tap it to send "I am Ok" messages to every emergency contacts
        handler.post(new Runnable() {
            @Override
            public void run() {
//create a floating button
                FloatingActionButton floatingActionButton = getFAB();
                floatingActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FavoriteNumberBean favoriteNumberBean = new FavoriteNumberBean(false);
                        ArrayList<String> mobileList = favoriteNumberBean.getMobileNumber();
                        smsManager[0] = SmsManager.getDefault();
//send every emergency contacts the messages
                        for (String phone : mobileList) {
                            smsManager[0].sendTextMessage(phone, null, "I am Ok!", null, null);
                        }
                    }
                });
            }
        });
    }

    private FloatingActionButton getFAB() {
        FloatingActionButton fab = new FloatingActionButton(context1);
        return fab;
    }


    private void createNotification(String strContentTitle, String strContentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext()) //
                .setSmallIcon(R.drawable.ic_action_eq) //
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


