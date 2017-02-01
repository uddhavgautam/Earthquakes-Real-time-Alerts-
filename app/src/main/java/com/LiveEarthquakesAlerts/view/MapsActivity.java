package com.LiveEarthquakesAlerts.view;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.widget.Toast;

import com.LiveEarthquakesAlerts.R;
import com.LiveEarthquakesAlerts.controller.adapters.MarkerInfoAdapter;
import com.LiveEarthquakesAlerts.controller.utils.OnLineTracker;
import com.LiveEarthquakesAlerts.model.database.EarthQuakes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener {

    MarkerOptions mOptions = new MarkerOptions();
    private GoogleMap myMap;
    private String Depth, Lat, Lng, Loc, Mag, Date;
    private MarkerInfoAdapter infoAdapter;
    private long itemId;
    private SupportMapFragment mapFragment;
    private long DateMilis;

    private LatLng currentLatLng;
    private LatLng destLatLng1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        itemId = getIntent().getLongExtra("selectedItem", 0);

        if (itemId == 0) {
            finish();
        } else {
            DateMilis = itemId;
        }
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        if (myMap == null) {
//            myMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            //static fragment mapFragment
            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this); //this calling onMapReady() callback of this class. 'this' means GoogleMap instance
        } else {
            Toast.makeText(getApplicationContext(), "myMap null!", Toast.LENGTH_LONG).show();
        }
    }

    private void setUpMap() {

        try {

            myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            infoAdapter = new MarkerInfoAdapter(MapsActivity.this.getLayoutInflater());
            myMap.clear();
            myMap.setInfoWindowAdapter(infoAdapter);

            myMap.setMyLocationEnabled(true);
            myMap.setOnMapClickListener(this);
            myMap.setOnMapLongClickListener(this);
            myMap.setOnMarkerClickListener(this);
            myMap.setOnMarkerDragListener(this);

            EarthQuakes currentEarthquakes = new EarthQuakes().getEarthquakesById(itemId);

//            gets user LatLng
            this.currentLatLng = new LatLng(MainActivity.currentLoc.getLatitude(), MainActivity.currentLoc.getLongitude());
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
                        if (MainActivity.cc != null) {
                            val = MainActivity.cc;
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
        if (myMap != null) {

            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

            if (resultCode != ConnectionResult.SUCCESS) {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 1);
                dialog.show();
                return;
            } else {
                setUpMap();
            }
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


}