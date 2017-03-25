package com.liveEarthquakesAlerts.controller.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

//import com.splunk.mint.Mint;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class OnLineTracker {

    public static int syncPeriod = 1000 * 15; //15 seconds
    public static String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
    public static String DATEFORMAT_SEISMICPORTAL = "yyyy-MM-dd'T'HH:mm:ss.S'Z'";

    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static void catchException(Exception ex) {
        ex.printStackTrace();
//        Mint.logException(ex);
    }
}
