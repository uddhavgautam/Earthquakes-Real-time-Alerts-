package com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS;

import java.util.List;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class POJOUSGS<String, M, F, Float> {

    private String type;
    private M metadata; //added MetadataUSGS as M type generic. M becomes MetadataUSGS when
    //I call                         POJOUSGS<String, MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>, Float> items = gson.fromJson(jsonOriginal, listType);
//  where listType is as:            final Type listType = new TypeToken<POJOUSGS<String, MetadataUSGS, FeaturesUSGS<PropertiesUSGS, GeometryUSGS>, Float>>() {

    private List<F> features; //added FeaturesUSGS as List of F type.
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
