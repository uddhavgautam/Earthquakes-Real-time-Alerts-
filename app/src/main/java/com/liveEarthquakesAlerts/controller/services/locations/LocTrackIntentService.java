package com.liveEarthquakesAlerts.controller.services.locations;

import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.liveEarthquakesAlerts.controller.services.earthquakes.EarthquakeIntentService;
import com.liveEarthquakesAlerts.controller.utils.MyOwnCustomLog;
import com.liveEarthquakesAlerts.model.LocationPOJO;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocTrackIntentService extends Service
        implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "LocTrackIntentService";
    public static Handler mHandler;
    public static boolean isLocationUpdated = false;
    public int count = 0;
    public LocationRequest mLocationRequest;
    public GoogleApiClient mGoogleApiClient;
    public LocationSettingsRequest.Builder builderLocationSettings;
    public Location location; // location
    public LocationSettingsRequest mLocationSettingsRequest;
    private MyOwnCustomLog myOwnCustomLog = new MyOwnCustomLog();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "On Create");
    }

    public void createLocationRequest() { //in main thread

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

                            //now I got the location, start EarthquakeIntentService to fetch earthquakes

                            if (LocationPOJO.location != null)
                                getApplicationContext().startService(new Intent(getApplicationContext(), EarthquakeIntentService.class));


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
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "LocTrackIntentService destroyed!");
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

        Log.i(TAG, "inside thread of LocTrackIntentService!");

        buildGoogleApiClient();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        } else {
            Log.i(TAG, "GoogleApiClient couldn't build!");
        }

        return START_STICKY;
    }

//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) { //onHandleIntent thread
//
//        LocTrackIntentService.isRunning = true;
//
//        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
//        String simpleName = this.getClass().getSimpleName();
//        myOwnCustomLog.addLog(simpleName, Thread.currentThread().getStackTrace()[2].getMethodName().toString(), stackTraces);
//
////        if (intent != null) {
////            mReceiver = intent.getParcelableExtra("receiver");
////        } //got the same resultReceiver of Activity
//
//        buildGoogleApiClient();
//        if (mGoogleApiClient != null) {
//            mGoogleApiClient.connect();
//        } else {
//            Log.i(TAG, "GoogleApiClient couldn't build!");
//        }
//
//        mHandler = new Handler(Looper.myLooper()) {
//            @Override
//            public void handleMessage(Message msg) {
//                Bundle bundle = msg.getData();
//                if (bundle.getBoolean("locationStatus")) { //wait the response for the main thread
////                    Looper.myLooper().quit();
//                } else {
//                    Log.i(TAG, "Couldn't get Location TOP");
//                }
//            }
//        };
//        Looper.loop(); //keep current thread alive
//    }


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
    }


}