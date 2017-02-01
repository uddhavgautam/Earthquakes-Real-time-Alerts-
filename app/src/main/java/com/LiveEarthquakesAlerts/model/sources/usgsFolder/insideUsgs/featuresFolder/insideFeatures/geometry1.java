package com.LiveEarthquakesAlerts.model.sources.usgsFolder.insideUsgs.featuresFolder.insideFeatures;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */

import java.util.List;

public class geometry1 {

    private String type;
    private List<Float> coordinates;

    public String getType() { //return type of Geometry. eg, point, line, Polygon etc. Here it is always point
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Float> getCoordinates() {
        return coordinates; //coordinate must be array. Here it is List. Since all arrays are list.
    } //lat, long, alt of point

    public void setCoordinate(List<Float> coordinates) {
        this.coordinates = coordinates;
    }

}
