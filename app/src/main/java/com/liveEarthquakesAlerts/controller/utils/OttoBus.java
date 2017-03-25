package com.liveEarthquakesAlerts.controller.utils;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class OttoBus extends Bus { //this is Otto Bus

    private static OttoBus ottoBus = new OttoBus();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private OttoBus() {
    }

    public static OttoBus getOttoBus() {
        return ottoBus;
    }

    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {

            super.post(event);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    OttoBus.super.post(event);
                }
            });
        }
    }
}