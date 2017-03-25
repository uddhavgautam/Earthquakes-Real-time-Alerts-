package com.liveEarthquakesAlerts.controller.utils;

import android.location.Location;

import com.liveEarthquakesAlerts.model.LocationPOJO;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.RiskyEarthquakes;

/**
 * Created by uddhav on 3/24/17.
 */

public class CheckRiskEarthquakes {


    public static boolean checkRisky(EarthQuakes item) {
        Location userLocation = LocationPOJO.location;

        Location finalLoc = new Location("Risky Earthquake");
        finalLoc.setLatitude(item.getLatitude());
        finalLoc.setLongitude(item.getLongitude());

        boolean status = false;

        if (userLocation != null) {
            double distanceInMeters = finalLoc.distanceTo(userLocation);
            double distanceValInMiles = distanceInMeters * 0.000621371;


            if (distanceValInMiles < 200 && item.getSig() > 500) { //we assume risky, need collaboration with GeoScientist
                status = true;
            }
        }
        return status;
    }

    public static boolean checkRisky(RiskyEarthquakes item) {
        Location userLocation = LocationPOJO.location;

        Location finalLoc = new Location("Risky Earthquake");
        finalLoc.setLatitude(item.getLatitude());
        finalLoc.setLongitude(item.getLongitude());

        boolean status = false;

        if (userLocation != null) {
            double distanceInMeters = finalLoc.distanceTo(userLocation);
            double distanceValInMiles = distanceInMeters * 0.000621371;


            if (distanceValInMiles < 200 && item.getSig() > 500) { //we assume risky, need collaboration with GeoScientist
                status = true;
            }
        }
        return status;
    }
}
