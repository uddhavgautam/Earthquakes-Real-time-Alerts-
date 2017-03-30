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
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */

@DatabaseTable(tableName = "LastEarthquakeDateRisky")
public class LastEarthquakeDateRisky implements Parcelable, Comparator<LastEarthquakeDateRisky> {

    public static final Creator<LastEarthquakeDateRisky> CREATOR = new Creator<LastEarthquakeDateRisky>() {
        @Override
        public LastEarthquakeDateRisky createFromParcel(Parcel in) {
            return new LastEarthquakeDateRisky(in);
        }

        @Override
        public LastEarthquakeDateRisky[] newArray(int size) {
            return new LastEarthquakeDateRisky[size];
        }
    };
    @DatabaseField(id = true)
    private int id;
    @DatabaseField
    private Long DateMilis;

    public LastEarthquakeDateRisky() {
        this.id = 1;
    }

    protected LastEarthquakeDateRisky(Parcel in) {
        id = in.readInt();
    }

    public void Insert() {

        try {
            Dao<LastEarthquakeDateRisky, Integer> Missionsinsert = (DatabaseHelperRisky.getDbHelper()).getLastEarthquakeDateDataHelperRisky();
            LastEarthquakeDateRisky existenceCheck = Missionsinsert.queryForId(this.id);

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
        LastEarthquakeDateRisky lastDate = null;
        try {
            Dao<LastEarthquakeDateRisky, Integer> dao = DatabaseHelperRisky.getDbHelper().getLastEarthquakeDateDataHelperRisky();
            lastDate = dao.queryForId(1);
        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }
        return lastDate.getDateMilis();
    }

    public int GetRowCount() {
        int count = 0;

        try {
            Dao<LastEarthquakeDateRisky, Integer> dao = DatabaseHelperRisky.getDbHelper().getLastEarthquakeDateDataHelperRisky();
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
    public int compare(LastEarthquakeDateRisky lhs, LastEarthquakeDateRisky rhs) {
        return (int) (lhs.DateMilis - rhs.DateMilis);
    }

}