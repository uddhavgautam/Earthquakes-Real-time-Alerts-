package com.liveEarthquakesAlerts.model.sources.usgsFolder.insideUsgs.featuresFolder;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class features1<P, G> { //generic representation. Two different generic types.
// These types can be any type, properties, geometry, id are 4 attributes of feature.
    // Each feature means each earthquake.
    // Generic is only for object, not for primitive types

    private String type;
    private P properties; //added properties as first generic type
    private G geometry; //added geometry as second generic type
    private String id;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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
