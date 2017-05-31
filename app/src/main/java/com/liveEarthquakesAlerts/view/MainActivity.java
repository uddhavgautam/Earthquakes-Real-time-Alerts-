package com.liveEarthquakesAlerts.view;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.liveEarthquakesAlerts.R;
import com.liveEarthquakesAlerts.controller.adapters.ListViewAdapter;
import com.liveEarthquakesAlerts.controller.services.locations.LocTrackService;
import com.liveEarthquakesAlerts.controller.utils.Animator;
import com.liveEarthquakesAlerts.controller.utils.App;
import com.liveEarthquakesAlerts.controller.utils.AppSettings;
import com.liveEarthquakesAlerts.controller.utils.BusStatus;
import com.liveEarthquakesAlerts.controller.utils.CheckRiskEarthquakes;
import com.liveEarthquakesAlerts.controller.utils.CreateRequestUrl;
import com.liveEarthquakesAlerts.controller.utils.MyOwnCustomLog;
import com.liveEarthquakesAlerts.controller.utils.OnLineTracker;
import com.liveEarthquakesAlerts.controller.utils.SaveResponseToDB;
import com.liveEarthquakesAlerts.controller.utils.broadcastReceiver.IncomingReceiver;
import com.liveEarthquakesAlerts.model.LocationPOJO;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.LastTimeEarthquakes;
import com.liveEarthquakesAlerts.model.database.LastTimeRiskyEarthquakes;
import com.squareup.otto.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener, OnScrollListener {

    public static final DatabaseReference realTimeEarthquakes = FirebaseDatabase.getInstance().getReference().getRoot().child("realTimeEarthquakes");
    public static String bannerText;
    public static Intent locInitServiceIntent;
    public static boolean isRegistered = false;
    public static Intent broadcastIntent = new Intent();
    public static String modifiedCheckTime;
    private static IncomingReceiver incomingReceiver;
    private final String TAG = "MainActivity";
    private ProgressDialog pd;
    private ListView list;
    private int currentScrollState, currentFirstVisibleItem, currentVisibleItemCount, currentTotalItemCount;
    private ListViewAdapter adapter;
    private TextView tvEmptyMessage;
    private TextView tvBanner;
    private boolean isConnectToInternet = true;
    private MyOwnCustomLog myOwnCustomLog = new MyOwnCustomLog();

    public static void FirebaseSync(final Context context, DatabaseReference realTimeEarthquakes) {
        final ValueEventListener valueEventListenerEarthquake = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (AppSettings.getInstance().getProximity() == 0) {
                    Log.i("FirebaseDb", " World-wide");
                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                    clientHelper.getDataFromUSGSCalledFromDataListener();

                } else { //user-proximity
                    Log.i("FirebaseDb", " User-proximity");
                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                    clientHelper.getPartialDataFromUSGSCalledFromDataListener();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        realTimeEarthquakes.addValueEventListener(valueEventListenerEarthquake);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SimpleDateFormat dateFormat2 = new SimpleDateFormat("E',' dd MMM yyyy kk:mm:ss 'GMT'"); //instead of hh, use kk for 24 hours format
        TimeZone timeZone2 = TimeZone.getTimeZone("GMT");
        dateFormat2.setTimeZone(timeZone2);
        Calendar cal1 = Calendar.getInstance(timeZone2);
        cal1.add(Calendar.MINUTE, -1000); //get time of 1000 minutes before
        Date dateBeforeTenMinute2 = cal1.getTime();
        modifiedCheckTime = dateFormat2.format(dateBeforeTenMinute2); //current GMT time of 10 minutes before
        Log.i("ModifiedTm ", MainActivity.modifiedCheckTime);


        SaveResponseToDB saveResponseToDB = new SaveResponseToDB(this); //pass the context. Don't use static, it will leak the memory
        LocTrackService locTrackService = new LocTrackService(this); //pass the context

        //I did broadcast receiver registration for my intent filter in main thread

        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        String simpleName = this.getClass().getSimpleName();
        myOwnCustomLog.addLog(simpleName, Thread.currentThread().getStackTrace()[2].getMethodName().toString(), stackTraces);


        setContentView(R.layout.activity_main);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar1);

////        remove the left margin from the logo
        mToolbar.setPadding(2, 0, 0, 0);//for tab otherwise give space in tab
        mToolbar.setContentInsetsAbsolute(0, 0);

        setSupportActionBar(mToolbar);
//
////        set the logo icon
        mToolbar.setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setSubtitle("Real-time Alerts!");

        AppSettings.setDefaultSettings(); //SingleFragmentActivity -- AppSettings -- other classes

        if (new LastTimeEarthquakes().GetRowCount() == 0) { //if no earthquakes already in our database, then find total no of new records by querying JSON
            //checking just one earthquake record exists if enough to tell, this apk has previously synced the earthquakes
            LastTimeEarthquakes led = new LastTimeEarthquakes();
            //sets datemilis for any arbitrary record
            led.setDateMilis(606175200000l); //some date of 1989. This is starting datemilis
            led.Insert(); //this ultimately creates a earthquake row
            //this ensures the LastTimeEarthquakes table is not null
        }

        if (new LastTimeRiskyEarthquakes().GetRowCount() == 0) {
            LastTimeRiskyEarthquakes led = new LastTimeRiskyEarthquakes();
            led.setDateMilis(606175200000l);
            led.Insert();
        }

        tvEmptyMessage = (TextView) findViewById(R.id.tv_empty_message);
        tvBanner = (TextView) findViewById(R.id.mile_banner);
//        tvBanner.setText("");
        list = (ListView) findViewById(R.id.list2); //adds ListView in this, MainActivity. This list is for storing
        // earthquakes record in GUI

        list.setOnItemLongClickListener(this); // adds listeners on that ListView
        list.setOnScrollListener(this);
        list.setOnItemClickListener(this);

        locInitServiceIntent = new Intent(this, LocTrackService.class);

        incomingReceiver = new IncomingReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra("isInitializedAlready", false /* default value */) && intent.getBooleanExtra("LocationPermissionAlready", false)) { //it means already initialized
                    Log.i("Broadcast", " receiver executed!");
                    startService(locInitServiceIntent); //started service
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SaveResponseToDB.isInitialized.Uddhav");
        if (!isRegistered) {
            registerReceiver(incomingReceiver, intentFilter);
            isRegistered = true;
        }

    }

    private void initializeFirebaseRealtimeDB() { //onStart() calling this
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().getRoot();

        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {

                String myVarData = SaveResponseToDB.checkIfFirebaseHasData("https://earthquakesenotifications.firebaseio.com/realTimeEarthquakes.json?print=pretty");
                if ((!myVarData.equals("null"))) { //database in Firebase exists
                    Log.i("Initialize", " Firebase Database data exists!");
                    FirebaseSync(getApplicationContext(), realTimeEarthquakes);
                    SaveResponseToDB.isInitialized = true;
                    broadcastIntent.setAction("SaveResponseToDB.isInitialized.Uddhav").putExtra("isInitializedAlready", SaveResponseToDB.isInitialized);
                    sendBroadcast(broadcastIntent);

                } else {
                    Log.i("Initialize", " Firebase Database data doesn't exists!");
                    SaveResponseToDB.isInitialized = false;
                    SaveResponseToDB.DoFirebaseUpdateOnNeed(SaveResponseToDB.isInitialized, CreateRequestUrl.requestUSGSUsingHttp(), databaseReference);
                }
            }
        });
        newThread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("case1", "inside99");

        switch (requestCode) {
            case 99: {
                Log.i("case2", "inside99");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("case3", "inside99");

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Log.i("case4", "inside99");

                        broadcastIntent.setAction("SaveResponseToDB.isInitialized.Uddhav").putExtra("LocationPermissionAlready", true);
                        sendBroadcast(broadcastIntent);

                    } else { // if user does not enable location settings

                        broadcastIntent.setAction("SaveResponseToDB.isInitialized.Uddhav").putExtra("LocationPermissionAlready", false);
                        sendBroadcast(broadcastIntent);

                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.

                    }
                    return;
                }
            }
        }
    }

    @Override
    protected void onStart() { //main thread
        super.onStart();

        //show the log
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        String simpleName = this.getClass().getSimpleName();
        myOwnCustomLog.addLog(simpleName, Thread.currentThread().getStackTrace()[2].getMethodName().toString(), stackTraces);

        //first work: to check location permission, if not enabled, ask user to enable

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 99);
        } else {
            broadcastIntent.setAction("SaveResponseToDB.isInitialized.Uddhav").putExtra("LocationPermissionAlready", true);
            sendBroadcast(broadcastIntent);
        }

        initializeFirebaseRealtimeDB(); //completed

        App.bus.register(this);

//        pd = new ProgressDialog(MainActivity.this); //show progressbar
//        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        pd.setTitle(getString(R.string.PleaseWait));
//        pd.setMessage(getString(R.string.DataLoading));
//        pd.setCancelable(true);
//        pd.setIndeterminate(true);
//        pd.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isRegistered) {
            unregisterReceiver(incomingReceiver);
            isRegistered = false;
        }
        App.bus.unregister(this); //Unregister of Otto Bus
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isConnectToInternet) {
            List<EarthQuakes> EarthQuakeList = new EarthQuakes().GetAllData();

            if (EarthQuakeList.size() > 0) {
                adapter = new ListViewAdapter(MainActivity.this, EarthQuakeList);
                adapter.notifyDataSetChanged();
                list.setAdapter(adapter);
                list.setSelectionFromTop(currentFirstVisibleItem, 0);
            }
        }

    }

    @Subscribe
    public void messageReceived(BusStatus event) {
        Log.i("Bus", "after msg received");

        Log.i("MainActivity", event.getStatus() + " ");

        if (event.getStatus() == 999) {
            isConnectToInternet = false;
            list.setEmptyView(tvEmptyMessage);
            list.setAdapter(null);
        } else if (event.getStatus() == 1234) {
            broadcastIntent.setAction("SaveResponseToDB.isInitialized.Uddhav").putExtra("isInitializedAlready", SaveResponseToDB.isInitialized);
            sendBroadcast(broadcastIntent);
        } else if (event.getStatus() == 123) {


//update the adapter
            List<EarthQuakes> EarthQuakeList;
            Log.i("ConnectInternet", "true");
            if (AppSettings.getInstance().getProximity() == 1) { //user-proximity
                list.setAdapter(null); //clears everything
                EarthQuakeList = new EarthQuakes().GetAllDataUserProximity();
                if (EarthQuakeList.size() > 0) {
                    adapter = new ListViewAdapter(MainActivity.this, EarthQuakeList);
                    adapter.notifyDataSetChanged();
                    list.setAdapter(adapter);
                    list.setSelectionFromTop(currentFirstVisibleItem, 0); // (x,y)
                }
            }

            if (AppSettings.getInstance().getProximity() == 0) { //world-wide
                list.setAdapter(null); //clears everything
                EarthQuakeList = new EarthQuakes().GetAllData();

                if (EarthQuakeList.size() > 0) {
                    adapter = new ListViewAdapter(MainActivity.this, EarthQuakeList);
                    adapter.notifyDataSetChanged();
                    list.setAdapter(adapter);
                    list.setSelectionFromTop(currentFirstVisibleItem, 0); // (x,y)
                }
            }
        }

//        stop the progress bar

//        if (pd != null && pd.isShowing()) {
//            Log.i("Inside pd", "pd is running");
//            pd.dismiss();
//            pd = null;
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//            Show the Emergency contact icon
        Log.i("Visible", String.valueOf(View.VISIBLE));
        if (AppSettings.getInstance().isEmergency()) {
            menu.getItem(0).setVisible(true);
//            On every emergency contacts enabled, animate the Emergency contact icon. Will do later

        } else {
            menu.getItem(0).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.e_contact) {

            Intent i2 = new Intent(MainActivity.this, com.odoo.HomeActivity.class); //explicit intent to start SingleFragmentActivity
            startActivity(i2);
            return true;
        }

        if (item.getItemId() == R.id.action_main) {

            Intent i3 = new Intent(MainActivity.this, SettingsActivity.class); //It is actually communicating with SettingsActivity's fragment
            //explicit intent to start SingleFragmentActivity
            startActivity(i3); //Update Menu from this activity. //(Intent, requestCode)

            return true;
        }

        if (item.getItemId() == R.id.action_help) {

            Intent i4 = new Intent(MainActivity.this, HelpActivity.class); //It is actually communicating with SettingsActivity's fragment
            //explicit intent to start SingleFragmentActivity
            startActivity(i4);//Update Menu from this activity. //(Intent, requestCode)

            return true;
        }

        if (item.getItemId() == R.id.action_about) {

            Intent i5 = new Intent(MainActivity.this, AboutActivity.class); //It is actually communicating with SettingsActivity's fragment
            //explicit intent to start SingleFragmentActivity
            startActivity(i5); //Update Menu from this activity. //(Intent, requestCode)

            return true;
        }


        return super.onOptionsItemSelected(item); //This is default method calling which simply return false. This makes sure item is not selected
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check which request we're responding to
        if (data != null && resultCode == RESULT_OK && requestCode == 7777) {

            Boolean boolVal = data.getBooleanExtra("str", false); //default value is false
            if (boolVal) {
                invalidateOptionsMenu();
            }

        }

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.currentScrollState = scrollState;
        this.isScrollCompleted();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {
        this.currentFirstVisibleItem = firstVisibleItem;
        this.currentVisibleItemCount = visibleItemCount;
        this.currentTotalItemCount = totalItemCount;
    }

    private void isScrollCompleted() {

        if (currentFirstVisibleItem + currentVisibleItemCount >= currentTotalItemCount) {
            if (this.currentVisibleItemCount > 0 && this.currentScrollState == OnScrollListener.SCROLL_STATE_IDLE) {

            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        EarthQuakes eq = (EarthQuakes) parent.getAdapter().getItem(position);
        view.setSelected(true);

        if (CheckRiskEarthquakes.checkRisky(eq)) {
            Animator animator = Animator.getAnimator(tvBanner); //get the Animator to do the animation for tvBanner TextView
            if (animator.isSetAnimation) {
                tvBanner.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                animator.stopAnimation(tvBanner);
            }

        }
        Location userLocation = LocationPOJO.location;

        Location finalLoc = new Location("Risky Earthquake");
        finalLoc.setLatitude(eq.getLatitude());
        finalLoc.setLongitude(eq.getLongitude());


        if (userLocation != null) {
            double distanceInMeters = finalLoc.distanceTo(userLocation);
            double distanceValInMiles = distanceInMeters * 0.000621371;
            bannerText = String.format("%.2f", distanceValInMiles) + " miles far!";
            tvBanner.setText(bannerText);

        } else {
            Log.i(TAG, "User location null!");
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent1, View view, int position, long id) {
        EarthQuakes eq = (EarthQuakes) parent1.getAdapter().getItem(position);
        view.setSelected(true);

        if (CheckRiskEarthquakes.checkRisky(eq)) {
            Animator animator = Animator.getAnimator(tvBanner); //get the Animator to do the animation for tvBanner TextView
            if (animator.isSetAnimation) {
                tvBanner.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                animator.stopAnimation(tvBanner);
            }

        }
        Location userLocation = LocationPOJO.location;

        Location finalLoc = new Location("Risky Earthquake");
        finalLoc.setLatitude(eq.getLatitude());
        finalLoc.setLongitude(eq.getLongitude());

        if (userLocation != null) {
            double distanceInMeters = finalLoc.distanceTo(userLocation);
            double distanceValInMiles = distanceInMeters * 0.000621371;
            bannerText = String.format("%.2f", distanceValInMiles) + " miles far!";
            tvBanner.setText(bannerText);

        }

//Display the Google Maps
        try {
            Intent i = new Intent(MainActivity.this, MapsActivity.class); //explicit intent
            i.putExtra("selectedItem", eq.getDateMilis());
            startActivity(i);
        } catch (Exception e) {
            OnLineTracker.catchException(e);
        }
        return false;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}