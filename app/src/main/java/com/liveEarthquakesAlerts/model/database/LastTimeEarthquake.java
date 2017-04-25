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

@DatabaseTable(tableName = "LastTimeEarthquake")
public class LastTimeEarthquake implements Parcelable, Comparator<LastTimeEarthquake> {

    public static final Creator<LastTimeEarthquake> CREATOR = new Creator<LastTimeEarthquake>() {
        @Override
        public LastTimeEarthquake createFromParcel(Parcel in) {
            return new LastTimeEarthquake(in);
        }

        @Override
        public LastTimeEarthquake[] newArray(int size) {
            return new LastTimeEarthquake[size];
        }
    };
    @DatabaseField(id = true)
    private int id;
    @DatabaseField
    private Long DateMilis;

    public LastTimeEarthquake() {
        this.id = 1;
    }

    protected LastTimeEarthquake(Parcel in) {
        id = in.readInt();
    }

    public void Insert() {

        try {
            Dao<LastTimeEarthquake, Integer> Missionsinsert = (DatabaseHelper.getDbHelper()).getLastEarthquakeDateDataHelper();
            LastTimeEarthquake existenceCheck = Missionsinsert.queryForId(this.id);

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
        LastTimeEarthquake lastDate = null;
        try {
            Dao<LastTimeEarthquake, Integer> dao = DatabaseHelper.getDbHelper().getLastEarthquakeDateDataHelper();
            lastDate = dao.queryForId(1);
        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }
        return lastDate.getDateMilis();
    }

    public int GetRowCount() {
        int count = 0;

        try {
            Dao<LastTimeEarthquake, Integer> dao = DatabaseHelper.getDbHelper().getLastEarthquakeDateDataHelper();
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
    public int compare(LastTimeEarthquake lhs, LastTimeEarthquake rhs) {
        return (int) (lhs.DateMilis - rhs.DateMilis);
    }

}