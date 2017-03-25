package com.liveEarthquakesAlerts.controller.utils;

import android.app.Application;
import android.content.Context;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class App extends Application { //App class for: 1) providing application context 2)Starting Otto Bus

    public static Context AppContext;
    public static OttoBus bus;
    //bus is just a Otto bus. It uses handler for the communication between activity and fragments or activity and services.

    @Override
    public void onCreate() {
        super.onCreate();

        AppContext = getApplicationContext(); //Now, AppContext is the context of the Application class (App class here)

//		Mint.initAndStartSession(App.this, getString(R.string.Mint_apiKey));

//        Mint.initAndStartSession(App.this, "29463cb0"); //I am using Splunk Mint SDK to initialize and do start session
        // of this App providing the Splunk Mint API key. 29463cb0 is the key. Splunk Mint, here, I am using for the Data collector

        bus = OttoBus.getOttoBus(); //Class.method. This technique doesn't initialize the constructor
    }
}
