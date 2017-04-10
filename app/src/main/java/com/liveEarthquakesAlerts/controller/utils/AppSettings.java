package com.liveEarthquakesAlerts.controller.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.liveEarthquakesAlerts.R;

import java.util.Map;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class AppSettings {

    public static AppSettings pojoPref = null;
    private static Context ctx = App.AppContext; //Get the same context of App class. Context is like security permission manager

    private int TimeInterval, Proximity, Magnitude, Sorting;
    private boolean Notifications, Emergency, Vibration, Sound; // for checking notification, vibration and sound whether to do ON/OFF

    private String Key_TimeInterval, Key_Magnitude, Key_Proxmity, Key_Sorting;

    private String Key_Notifications, Key_Vibration, Key_Sound, Key_Emergency;

    AppSettings() { // constructor but no public. Because this class is based on Singleton design pattern

        Key_Proxmity = ctx.getResources().getString(R.string.listPref_Key_Proximity);
        Key_TimeInterval = ctx.getResources().getString(R.string.listPref_Key_TimeInterval);
        Key_Magnitude = ctx.getResources().getString(R.string.listPref_Key_Magnitude);
        Key_Sorting = ctx.getResources().getString(R.string.listPref_Key_Sorting);


        Key_Notifications = ctx.getResources().getString(R.string.CheckBoxPref_Key_Notifications);
        Key_Vibration = ctx.getResources().getString(R.string.CheckBoxPref_Key_Vibration);
        Key_Sound = ctx.getResources().getString(R.string.CheckBoxPref_Key_Sound);
        Key_Emergency = ctx.getResources().getString(R.string.CheckBoxPref_Key_Emergency);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        Map<String, ?> allEntries = pref.getAll(); //generic hashmap<string as key, anything as value>

        Log.i("Allentries", allEntries.toString());

        TimeInterval = Integer.parseInt((String) allEntries.get(Key_TimeInterval)); //return all values of  time interval (eg, last 24 hours)
        Magnitude = Integer.parseInt((String) allEntries.get(Key_Magnitude));
        Proximity = Integer.parseInt((String) allEntries.get(Key_Proxmity));
        Sorting = Integer.parseInt((String) allEntries.get(Key_Sorting));

        Notifications = (Boolean) allEntries.get(Key_Notifications);
        Vibration = (Boolean) allEntries.get(Key_Vibration);
        Sound = (Boolean) allEntries.get(Key_Sound);
        Log.i("Sound", Sound + "");
        Emergency = (Boolean) allEntries.get(Key_Emergency);
        Log.i("Emergency Sound", Emergency + "");


    }

    public static void setDefaultSettings() {
        PreferenceManager.setDefaultValues(ctx, R.xml.pref, false); //false tells not to read again.

    }

    public static AppSettings getInstance() {
        return pojoPref == null ? new AppSettings() : pojoPref;
    }

    public int getTimeInterval() {
        return TimeInterval;
    }

    public void setTimeInterval(int timeInterval) {
        TimeInterval = timeInterval;
    }

    public int getProximity() {
        return Proximity;
    }

    public void setProximity(int proximity) {
        Proximity = proximity;
    }

    public int getMagnitude() {
        return Magnitude;
    }

    public void setMagnitude(int magnitude) {
        Magnitude = magnitude;
    }

    public int getSorting() {
        return Sorting;
    }

    public void setSorting(int sorting) {
        Sorting = sorting;
    }

    public boolean isSound() {
        return Sound;
    } //for boolean getter, it starts with "is" not "get"

    public void setSound(boolean sound) {
        Sound = sound;
    }

    public boolean isVibration() {
        return Vibration;
    }

    public void setVibration(boolean vibration) {
        Vibration = vibration;
    }

    public boolean isNotifications() {
        return Notifications;
    }

    public void setNotifications(boolean notifications) {
        Notifications = notifications;
    }

    public boolean isEmergency() {
        return Emergency;
    }

    public void setEmergency(boolean emergency) {
        Emergency = emergency;
    }
}
