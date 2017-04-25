package com.liveEarthquakesAlerts.model.database;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;
import com.liveEarthquakesAlerts.controller.utils.AppSettings;
import com.liveEarthquakesAlerts.controller.utils.OnLineTracker;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
@DatabaseTable(tableName = "RiskyEarthquakes")
public class RiskyEarthquakes implements Parcelable, Comparator<RiskyEarthquakes> {

    public static final Creator<RiskyEarthquakes> CREATOR = new Creator<RiskyEarthquakes>() { // earthquakes ko chunk space is CREATOR
        @Override
        public RiskyEarthquakes createFromParcel(Parcel in) {
            return new RiskyEarthquakes(in);
        }

        @Override
        public RiskyEarthquakes[] newArray(int size) {
            return new RiskyEarthquakes[size];
        }
    };

    @DatabaseField(id = true)
    private Long DateMilis;
    @DatabaseField
    private String LocationName;
    @DatabaseField
    private double Latitude;
    @DatabaseField
    private double Longitude;
    @DatabaseField
    private float Magnitude;
    @DatabaseField
    private float Depth;
    @DatabaseField
    private int sig;
    @DatabaseField
    private int Day;
    @DatabaseField
    private int Month;

    public RiskyEarthquakes() {
    }

    protected RiskyEarthquakes(Parcel in) {
        LocationName = in.readString();
        Latitude = in.readDouble();
        Longitude = in.readDouble();
        Magnitude = in.readFloat();
        Depth = in.readFloat();
        sig = in.readInt();
        Day = in.readInt();
        Month = in.readInt();
    }

    public static Long backDate() {

        int value = AppSettings.getInstance().getTimeInterval();

        int goBack = 7;

        if (value == 0) {
            goBack = 0; //last hour
        } else if (value == 1) {
            goBack = 1; //last 1 day
        } else if (value == 2) {
            goBack = 7; //last 7 days
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -goBack);
        return cal.getTimeInMillis();
    }

    public int getSig() {
        return sig;
    }

    public void setSig(int sig) {
        this.sig = sig;
    }

    public void Insert() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(DateMilis);
        Day = cal.get(Calendar.DAY_OF_MONTH);
        Month = cal.get(Calendar.MONTH) + 1;

        try {
            Dao<RiskyEarthquakes, Long> MissionsInsert = (DatabaseHelperRisky.getDbHelper()).getEarthQuakesDataHelperRisky();
            RiskyEarthquakes existenceCheck = MissionsInsert.queryForId(this.DateMilis);

            if (existenceCheck != null) {
                MissionsInsert.update(this); //delete and then create
            } else {
                MissionsInsert.create(this);
            }

        } catch (SQLException e) {
            OnLineTracker.catchException(e);
        }
    }

    public List<RiskyEarthquakes> GetAllData() {

        List<RiskyEarthquakes> data = new ArrayList<>();

        try {

            Dao<RiskyEarthquakes, Long> dao = DatabaseHelperRisky.getDbHelper().getEarthQuakesDataHelperRisky();
            QueryBuilder<RiskyEarthquakes, Long> qBuilder = dao.queryBuilder();

            int sortingType = AppSettings.getInstance().getSorting();
            Long backdate = backDate();

            qBuilder.where()//
                    .gt("Magnitude", AppSettings.getInstance().getMagnitude()) //
                    .and()//
                    .gt("DateMilis", backdate);


            if (sortingType == 0) {
                qBuilder.orderBy("DateMilis", true);
            } else if (sortingType == 1) {
                qBuilder.orderBy("DateMilis", false);
            } else if (sortingType == 2) {
                qBuilder.orderBy("Magnitude", true);
            } else if (sortingType == 3) {
                qBuilder.orderBy("Magnitude", false);
            }

            PreparedQuery<RiskyEarthquakes> pQuery = qBuilder.prepare();
            data = dao.query(pQuery);

        } catch (SQLException e) {
            OnLineTracker.catchException(e);
        }

//        Log.i("Row1", data.get(0).getLocationName());
        return data;
    }

    public Long GetLastEarthQuakeDate() {

        List<RiskyEarthquakes> data = new ArrayList<>();

        try {

            Dao<RiskyEarthquakes, Long> dao = DatabaseHelperRisky.getDbHelper().getEarthQuakesDataHelperRisky();
            QueryBuilder<RiskyEarthquakes, Long> qBuilder = dao.queryBuilder();

            qBuilder.orderBy("DateMilis", false);

            PreparedQuery<RiskyEarthquakes> pQuery = qBuilder.prepare();
            data = dao.query(pQuery);

        } catch (SQLException e) {
            OnLineTracker.catchException(e);
        }

        Log.i("NewDateMilis", String.valueOf(data.get(0).getDateMilis()));

        return data.get(0).getDateMilis();
    }

    public List<RiskyEarthquakes> newEarthquakes() {

        List<RiskyEarthquakes> data = new ArrayList<>();


        try {

            Dao<RiskyEarthquakes, Long> dao = DatabaseHelperRisky.getDbHelper().getEarthQuakesDataHelperRisky();
            QueryBuilder<RiskyEarthquakes, Long> qBuilder = dao.queryBuilder();


            qBuilder.distinct().where()
                    .gt("Magnitude", AppSettings.getInstance().getMagnitude()) //
                    .and()//
                    .gt("DateMilis", new LastTimeEarthquake().GetLastEarthquakeMilisDate());


            qBuilder.orderBy("DateMilis", false);

            PreparedQuery<RiskyEarthquakes> pQuery = qBuilder.prepare();
            data = dao.query(pQuery);

        } catch (SQLException e) {
            OnLineTracker.catchException(e);
        }

        return data;
    }

    public int GetRowCount() {
        int count = 0;

        try {
            Dao<RiskyEarthquakes, Long> dao = DatabaseHelperRisky.getDbHelper().getEarthQuakesDataHelperRisky();
            count = (int) dao.countOf();
        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }

        return count;
    }

    public void DeleteRow(long deleteId) {
        try {

            Dao<RiskyEarthquakes, Long> dao = DatabaseHelperRisky.getDbHelper().getEarthQuakesDataHelperRisky();
            DeleteBuilder<RiskyEarthquakes, Long> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq("DateMilis", deleteId);
            deleteBuilder.delete();
        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }
    }

    public RiskyEarthquakes getEarthquakesById(Long DateMilis) {

        List<RiskyEarthquakes> eqList = new ArrayList<>();

        try {
            Dao<RiskyEarthquakes, Long> dao = DatabaseHelperRisky.getDbHelper().getEarthQuakesDataHelperRisky();
            QueryBuilder<RiskyEarthquakes, Long> qBuilder = dao.queryBuilder();
            qBuilder.distinct().where().eq("DateMilis", DateMilis);
            PreparedQuery<RiskyEarthquakes> pQuery = qBuilder.prepare();
            eqList = dao.query(pQuery);

        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }

        return eqList.get(0);
    }

    public List<RiskyEarthquakes> getEarthquakesByDay(int day, int month) {

        List<RiskyEarthquakes> data = new ArrayList<>();

        try {

            Dao<RiskyEarthquakes, Long> dao = DatabaseHelperRisky.getDbHelper().getEarthQuakesDataHelperRisky();
            QueryBuilder<RiskyEarthquakes, Long> qBuilder = dao.queryBuilder();

            int sortingType = AppSettings.getInstance().getSorting();

            qBuilder.where()//
                    .eq("Source", 0) //
                    .and()//
                    .gt("Magnitude", AppSettings.getInstance().getMagnitude());


            if (sortingType == 0) {
                qBuilder.orderBy("DateMilis", true);
            } else if (sortingType == 1) {
                qBuilder.orderBy("DateMilis", false);
            } else if (sortingType == 2) {
                qBuilder.orderBy("Magnitude", true);
            } else if (sortingType == 3) {
                qBuilder.orderBy("Magnitude", false);
            }

            PreparedQuery<RiskyEarthquakes> pQuery = qBuilder.prepare();
            data = dao.query(pQuery);

        } catch (SQLException e) {
            OnLineTracker.catchException(e);
        }

        return data;
    }

    public List<RiskyEarthquakes> getColumn(String ColumnName) throws SQLException {
        Dao<RiskyEarthquakes, Long> dao = DatabaseHelperRisky.getDbHelper().getEarthQuakesDataHelperRisky();
        List<RiskyEarthquakes> results = dao.queryBuilder().distinct().selectColumns(ColumnName).query();
        return results;
    }

    public Long getDateMilis() {
        return DateMilis;
    }

    public void setDateMilis(Long dateMilis) {
        DateMilis = dateMilis;
    }

    public String getLocationName() {
        return LocationName;
    }

    public void setLocationName(String locationName) {
        LocationName = locationName;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public float getMagnitude() {
        return Magnitude;
    }

    public void setMagnitude(float magnitude) {
        Magnitude = magnitude;
    }

    public float getDepth() {
        return Depth;
    }

    public void setDepth(float depth) {
        Depth = depth;
    }

    public int getDay() {
        return Day;
    }

    public void setDay(int day) {
        Day = day;
    }

    public int getMonth() {
        return Month;
    }

    public void setMonth(int month) {
        Month = month;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(LocationName);
        dest.writeDouble(Latitude);
        dest.writeDouble(Longitude);
        dest.writeFloat(Magnitude);
        dest.writeFloat(Depth);
        dest.writeInt(sig);
        dest.writeInt(Day);
        dest.writeInt(Month);
    }

    @Override
    public int compare(RiskyEarthquakes lhs, RiskyEarthquakes rhs) {
        return (int) (lhs.getDateMilis() - rhs.getDateMilis());
    }


}
