package com.liveEarthquakesAlerts.view;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.liveEarthquakesAlerts.R;
import com.liveEarthquakesAlerts.controller.adapters.MarkerInfoAdapter;
import com.liveEarthquakesAlerts.controller.utils.OnLineTracker;
import com.liveEarthquakesAlerts.model.LocationPOJO;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;

import java.util.ArrayList;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener, LocationSource, LocationListener {

    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1972;
    // location accuracy settings
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_LOW_POWER);
    private final String TAG = "MapsActivity";
    MarkerOptions mOptions = new MarkerOptions();
    private GoogleMap myMap;
    private MarkerInfoAdapter infoAdapter;
    private long itemId;
    private SupportMapFragment mapFragment;
    private long DateMilis;
    private LatLng currentLatLng;
    private LatLng destLatLng1;
    private Location location;
    private GoogleApiClient mGoogleApiClient;
    private OnLocationChangedListener mMapLocationListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        itemId = getIntent().getLongExtra("selectedItem", 0);

        if (itemId == 0) {
            finish();
        } else {
            DateMilis = itemId;
        }
        location = LocationPOJO.location;
        Log.i(TAG, " Location: " + location);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    private void setUpMapIfNeeded() {
        if (myMap == null) {
            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(getApplicationContext(), "myMap null!", Toast.LENGTH_LONG).show();
        }
    }

    private void setUpMap() {

        try {
            if (location != null) {
                myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                infoAdapter = new MarkerInfoAdapter(MapsActivity.this.getLayoutInflater());
                myMap.clear();
                myMap.setInfoWindowAdapter(infoAdapter);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                myMap.setMyLocationEnabled(true);
                myMap.setOnMapClickListener(this);
                myMap.setOnMapLongClickListener(this);
                myMap.setOnMarkerClickListener(this);
                myMap.setOnMarkerDragListener(this);

                EarthQuakes currentEarthquakes = new EarthQuakes().getEarthquakesById(itemId);

//            gets user LatLng
                this.currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mOptions.position(currentLatLng); //reach the position of the LatLng for that mark
                mOptions.title("Current Location");
                mOptions.visible(true); //makes visible
                mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                myMap.addMarker(mOptions);

                for (EarthQuakes earthQuakes : new EarthQuakes().GetAllData()) {
//                gets marker LatLng

                    LatLng destLatLng = new LatLng(earthQuakes.getLatitude(), earthQuakes.getLongitude());


                    mOptions.position(destLatLng); //reach the position of the LatLng for that mark
                    mOptions.snippet(Long.toString(earthQuakes.getDateMilis())); //takes whatever descriptions
                    mOptions.visible(true); //makes visible


                    float magnitude = earthQuakes.getMagnitude();

                    if (magnitude < 3) {
                        mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    } else if (magnitude >= 3 && magnitude < 5) {
                        mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    } else if (magnitude >= 5) {
                        mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }

                    if (earthQuakes.getDateMilis().equals(currentEarthquakes.getDateMilis())) {
                        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 6));
                        Marker marker = myMap.addMarker(mOptions);
                        marker.showInfoWindow();
                    } else {
                        myMap.addMarker(mOptions);
                    }


//                add the line only between selected earthQuake and current location
                    this.destLatLng1 = new LatLng(earthQuakes.getEarthquakesById(DateMilis).getLatitude(), earthQuakes.getEarthquakesById(DateMilis).getLongitude());

                    ArrayList<LatLng> locList = new ArrayList<LatLng>();
                    locList.add(currentLatLng);
                    locList.add(destLatLng1);

                    int setColor = Color.BLUE;
                    if (magnitude < 3) {
                        setColor = Color.GREEN;
                    } else if (magnitude >= 3 && magnitude < 5) {
                        setColor = Color.YELLOW;
                    } else if (magnitude >= 5) {
                        setColor = Color.RED;
                    }

                    Polyline pl = myMap.addPolyline((new PolylineOptions()).addAll(locList)
                            .width(15)
                            .color(setColor)
                            .geodesic(false));
                    pl.setClickable(true);


                    myMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
                        @Override
                        public void onPolylineClick(Polyline polyline) {

                            String val = " Hi ";
                            if (MainActivity.bannerText != null) {
                                val = MainActivity.bannerText;
                            }

                            try {
                                Toast toast = Toast.makeText(getBaseContext(), val, Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), "Oh Gautam!", Toast.LENGTH_LONG).show();
                            }

                        }
                    });
                }

            }

        } catch (Exception e) {
            OnLineTracker.catchException(e);
            Toast.makeText(this, getString(R.string.MapCanNotBeDisplayed), Toast.LENGTH_LONG).show();
            finish();
        }


    }

    @Override
    public void onMapReady(GoogleMap gmap) {
        //DO WHATEVER YOU WANT WITH GOOGLEMAP

        myMap = gmap;
//        if (myMap != null) {
//
//            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
//
//            if (resultCode != ConnectionResult.SUCCESS) {
//                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 1);
//                dialog.show();
//                return;
//            } else {
//                setUpMap();
//            }
//        }

        myMap.setLocationSource(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        myMap.setMyLocationEnabled(true);

        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, Activity.RESULT_OK, null);
        } else if (api.isUserResolvableError(code) &&
                api.showErrorDialogFragment(this, code, REQUEST_GOOGLE_PLAY_SERVICES)) {
            // wait for onActivityResult call (see below)
        } else {
            Toast.makeText(this, api.getErrorString(code), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    setUpMap();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    @Override
    public void onBackPressed() {
        finish();
    }


    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mMapLocationListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mMapLocationListener = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mMapLocationListener != null) {
            mMapLocationListener.onLocationChanged(location);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                REQUEST,
                this);  // LocationListener
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}