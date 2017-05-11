package com.liveEarthquakesAlerts.model.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.liveEarthquakesAlerts.controller.utils.App;
import com.liveEarthquakesAlerts.controller.utils.OnLineTracker;

import java.sql.SQLException;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "lastearthquakes.db";
    private static final int DATABASE_VERSION = 1;
    private static DatabaseHelper dbHelper;
    private static Object syncObject = new Object();
    private final Context myContext;
    private Dao<EarthQuakes, Long> EarthQuakesDataHelper = null;
    private Dao<LastTimeEarthquakes, Integer> LastEarthquakeDateDataHelper = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.myContext = context;
    }

    public static DatabaseHelper getDbHelper() {
        synchronized (syncObject) {
            if (dbHelper == null) {
                dbHelper = new DatabaseHelper(App.AppContext);
            }
        }
        return dbHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, EarthQuakes.class);
            TableUtils.createTable(connectionSource, LastTimeEarthquakes.class);
        } catch (java.sql.SQLException e) {
            OnLineTracker.catchException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, EarthQuakes.class, true);
            TableUtils.dropTable(connectionSource, LastTimeEarthquakes.class, true);
            onCreate(db, connectionSource);
        } catch (java.sql.SQLException e) {
            OnLineTracker.catchException(e);
        }
    }

    public void clearDatabase() {
        ConnectionSource connectionSource = getConnectionSource();
        try {
            TableUtils.clearTable(connectionSource, EarthQuakes.class);
        } catch (SQLException e) {
            OnLineTracker.catchException(e);
        }
    }

    public Dao<EarthQuakes, Long> getEarthQuakesDataHelper() throws SQLException {
        if (EarthQuakesDataHelper == null) {
            EarthQuakesDataHelper = getDao(EarthQuakes.class);
        }
        return EarthQuakesDataHelper;
    }

    public Dao<LastTimeEarthquakes, Integer> getLastEarthquakeDateDataHelper() throws SQLException {
        if (LastEarthquakeDateDataHelper == null) {
            LastEarthquakeDateDataHelper = getDao(LastTimeEarthquakes.class);
        }
        return LastEarthquakeDateDataHelper;
    }

}