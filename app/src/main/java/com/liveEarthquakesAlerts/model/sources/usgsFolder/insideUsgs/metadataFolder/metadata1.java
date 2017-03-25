package com.liveEarthquakesAlerts.model.sources.usgsFolder.insideUsgs.metadataFolder;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */
public class metadata1 {

    private long generated;
    private String url;
    private String title;
    private int status;
    private String api;
    private int totalCount;

    public long getGenerated() {
        return generated;
    }

    public void setGenerated(long generated) {
        this.generated = generated;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubTitle() { //shows title of Earthquake in ListView
        return title;
    }

    public void setSubTitle(String subTitle) {
        this.title = subTitle;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
