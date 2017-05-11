package com.liveEarthquakesAlerts.model.sources.pOJOFolderUSGS.insidePOJOFolderUSGS.featuresFolderUSGS;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class FeaturesUSGS<P, G> { //generic representation. Two different generic types.
// These types can be any type, PropertiesUSGS, GeometryUSGS, id are 4 attributes of feature.
    // Each feature means each earthquake.
    // Generic is only for object, not for primitive types

    //    private String type;
    private P properties; //added PropertiesUSGS as first generic type
    private G geometry; //added GeometryUSGS as second generic type
    private String id;

//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

    public P getProperties() {
        return properties;
    }

    public void setProperties(P properties) {
        this.properties = properties;
    }

    public G getGeometry() {
        return geometry;
    }

    public void setGeometry(G geometry) {
        this.geometry = geometry;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
