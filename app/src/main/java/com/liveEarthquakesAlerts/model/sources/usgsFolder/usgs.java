package com.liveEarthquakesAlerts.model.sources.usgsFolder;

import java.util.List;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class usgs<String, M, F, Float> {

    private String type;
    private M metadata; //added metadata as M type generic
    private List<F> features; //added features as List of F type.
    private List<Float> bbox;

    public List<Float> getBbox() {
        return bbox;
    }

    public void setBbox(List<Float> bbox) {
        this.bbox = bbox;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public M getMetadata() {
        return metadata;
    }

    public void setMetadata(M metadata) {
        this.metadata = metadata;
    }

    public List<F> getFeatures() {
        return features;
    }

    public void setFeatures(List<F> features) {
        this.features = features;
    }

}
