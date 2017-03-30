package com.liveEarthquakesAlerts.controller.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.liveEarthquakesAlerts.R;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;

import java.util.Date;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class MarkerInfoAdapter implements InfoWindowAdapter { // this is to mark location via google maps when user clicks earthquake records

    LayoutInflater inflater = null;

    TextView tvLoc, tvMag, tvDepth, tvDate, tvLat, tvLng;

    public MarkerInfoAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public View getInfoContents(Marker marker) {

        View info = inflater.inflate(R.layout.marker_info, null);

        tvLoc = (TextView) info.findViewById(R.id.tv1);
        tvMag = (TextView) info.findViewById(R.id.tv2);
        tvDate = (TextView) info.findViewById(R.id.tv3);
        tvDepth = (TextView) info.findViewById(R.id.tv4);
        tvLat = (TextView) info.findViewById(R.id.tv5);
        tvLng = (TextView) info.findViewById(R.id.tv6);

        String snippet = marker.getSnippet();

        EarthQuakes earthQuakes = new EarthQuakes().getEarthquakesById(Long.parseLong(snippet));

        tvLoc.setText(" : " + earthQuakes.getLocationName());
        tvMag.setText(" : " + earthQuakes.getMagnitude());
        tvDepth.setText(" : " + earthQuakes.getDepth() + " KM");
        tvDate.setText(" : " + new Date(earthQuakes.getDateMilis()).toLocaleString());
        tvLat.setText(" : " + earthQuakes.getLatitude());
        tvLng.setText(" : " + earthQuakes.getLongitude());

        return info;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        // TODO Auto-generated method stub
        return null;
    }

}
