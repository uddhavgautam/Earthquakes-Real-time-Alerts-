package com.liveEarthquakesAlerts.controller.utils;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class BusStatus { //to main the OttoBus status

    private int status;

    public BusStatus(int s) { //constructor working as a setter
        status = s;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
