package com.liveEarthquakesAlerts.model.database;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.liveEarthquakesAlerts.controller.utils.OnLineTracker;

import java.sql.SQLException;
import java.util.Comparator;

/**
 * Created by uddhav Gautam on 7.3.2016. upgautam@ualr.edu
 */

@DatabaseTable(tableName = "LastTimeRiskyEarthquakes")
public class LastTimeRiskyEarthquakes implements Parcelable, Comparator<LastTimeRiskyEarthquakes> {

    public static final Creator<LastTimeRiskyEarthquakes> CREATOR = new Creator<LastTimeRiskyEarthquakes>() {
        @Override
        public LastTimeRiskyEarthquakes createFromParcel(Parcel in) {
            return new LastTimeRiskyEarthquakes(in);
        }

        @Override
        public LastTimeRiskyEarthquakes[] newArray(int size) {
            return new LastTimeRiskyEarthquakes[size];
        }
    };
    @DatabaseField(id = true)
    private int id;
    @DatabaseField
    private Long DateMilis;

    public LastTimeRiskyEarthquakes() {
        this.id = 1;
    }

    protected LastTimeRiskyEarthquakes(Parcel in) {
        id = in.readInt();
    }

    public void Insert() {

        try {
            Dao<LastTimeRiskyEarthquakes, Integer> Missionsinsert = (DatabaseHelperRisky.getDbHelper()).getLastEarthquakeDateDataHelperRisky();
            LastTimeRiskyEarthquakes existenceCheck = Missionsinsert.queryForId(this.id);

            if (existenceCheck != null) {
                Missionsinsert.update(this);
            } else {
                Missionsinsert.create(this);
            }

        } catch (SQLException e) {
            OnLineTracker.catchException(e);
        }
    }

    public Long GetLastEarthquakeMilisDate() {
        LastTimeRiskyEarthquakes lastDate = null;
        try {
            Dao<LastTimeRiskyEarthquakes, Integer> dao = DatabaseHelperRisky.getDbHelper().getLastEarthquakeDateDataHelperRisky();
            lastDate = dao.queryForId(1);
        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }
        return lastDate.getDateMilis();
    }

    public int GetRowCount() {
        int count = 0;

        try {
            Dao<LastTimeRiskyEarthquakes, Integer> dao = DatabaseHelperRisky.getDbHelper().getLastEarthquakeDateDataHelperRisky();
            count = (int) dao.countOf();
        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }

        return count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getDateMilis() {
        return DateMilis;
    }

    public void setDateMilis(Long dateMilis) {
        DateMilis = dateMilis;
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // TODO Auto-generated method stub

        dest.writeInt(id);
    }

    @Override
    public int compare(LastTimeRiskyEarthquakes lhs, LastTimeRiskyEarthquakes rhs) {
        return (int) (lhs.DateMilis - rhs.DateMilis);
    }

}