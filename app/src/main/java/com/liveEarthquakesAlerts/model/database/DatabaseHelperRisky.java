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
 * Created by  Uddhav Gautam  on 3/23/17.
 */

public class DatabaseHelperRisky extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "lastearthquakesRisky.db";
    private static final int DATABASE_VERSION = 1;
    private static DatabaseHelperRisky dbHelper;
    private static Object syncObject = new Object();
    private final Context myContext;
    private Dao<RiskyEarthquakes, Long> EarthQuakesDataHelperRisky = null;
    private Dao<LastEarthquakeDateRisky, Integer> LastEarthquakeDateDataHelperRisky = null;

    public DatabaseHelperRisky(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.myContext = context;
    }

    public static DatabaseHelperRisky getDbHelper() {
        synchronized (syncObject) {
            if (dbHelper == null) {
                dbHelper = new DatabaseHelperRisky(App.AppContext);
            }
        }
        return dbHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, RiskyEarthquakes.class);
            TableUtils.createTable(connectionSource, LastEarthquakeDateRisky.class);
        } catch (java.sql.SQLException e) {
            OnLineTracker.catchException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, RiskyEarthquakes.class, true);
            TableUtils.dropTable(connectionSource, LastEarthquakeDateRisky.class, true);
            onCreate(db, connectionSource);
        } catch (java.sql.SQLException e) {
            OnLineTracker.catchException(e);
        }
    }

    public void clearDatabase() {
        ConnectionSource connectionSource = getConnectionSource();
        try {
            TableUtils.clearTable(connectionSource, RiskyEarthquakes.class);
        } catch (SQLException e) {
            OnLineTracker.catchException(e);
        }
    }

    public Dao<RiskyEarthquakes, Long> getEarthQuakesDataHelperRisky() throws SQLException {
        if (EarthQuakesDataHelperRisky == null) {
            EarthQuakesDataHelperRisky = getDao(RiskyEarthquakes.class);
        }
        return EarthQuakesDataHelperRisky;
    }

    public Dao<LastEarthquakeDateRisky, Integer> getLastEarthquakeDateDataHelperRisky() throws SQLException {
        if (LastEarthquakeDateDataHelperRisky == null) {
            LastEarthquakeDateDataHelperRisky = getDao(LastEarthquakeDateRisky.class);
        }
        return LastEarthquakeDateDataHelperRisky;
    }

}