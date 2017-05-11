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

@DatabaseTable(tableName = "LastTimeEarthquakes")
public class LastTimeEarthquakes implements Parcelable, Comparator<LastTimeEarthquakes> {

    public static final Creator<LastTimeEarthquakes> CREATOR = new Creator<LastTimeEarthquakes>() {
        @Override
        public LastTimeEarthquakes createFromParcel(Parcel in) {
            return new LastTimeEarthquakes(in);
        }

        @Override
        public LastTimeEarthquakes[] newArray(int size) {
            return new LastTimeEarthquakes[size];
        }
    };
    @DatabaseField(id = true)
    private int id;
    @DatabaseField
    private Long DateMilis;

    public LastTimeEarthquakes() {
        this.id = 1;
    }

    protected LastTimeEarthquakes(Parcel in) {
        id = in.readInt();
    }

    public void Insert() {

        try {
            Dao<LastTimeEarthquakes, Integer> Missionsinsert = (DatabaseHelper.getDbHelper()).getLastEarthquakeDateDataHelper();
            LastTimeEarthquakes existenceCheck = Missionsinsert.queryForId(this.id);

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
        LastTimeEarthquakes lastDate = null;
        try {
            Dao<LastTimeEarthquakes, Integer> dao = DatabaseHelper.getDbHelper().getLastEarthquakeDateDataHelper();
            lastDate = dao.queryForId(1);
        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }
        return lastDate.getDateMilis();
    }

    public int GetRowCount() {
        int count = 0;

        try {
            Dao<LastTimeEarthquakes, Integer> dao = DatabaseHelper.getDbHelper().getLastEarthquakeDateDataHelper();
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
    public int compare(LastTimeEarthquakes lhs, LastTimeEarthquakes rhs) {
        return (int) (lhs.DateMilis - rhs.DateMilis);
    }

}