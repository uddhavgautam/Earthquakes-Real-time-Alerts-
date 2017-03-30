package com.liveEarthquakesAlerts.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
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
import com.liveEarthquakesAlerts.controller.services.earthquakes.EarthquakesDataSyncService;
import com.liveEarthquakesAlerts.controller.services.locations.LocationTracker;
import com.liveEarthquakesAlerts.controller.utils.Animator;
import com.liveEarthquakesAlerts.controller.utils.App;
import com.liveEarthquakesAlerts.controller.utils.AppSettings;
import com.liveEarthquakesAlerts.controller.utils.BusStatus;
import com.liveEarthquakesAlerts.controller.utils.CheckRiskEarthquakes;
import com.liveEarthquakesAlerts.controller.utils.CreateRequestUrl;
import com.liveEarthquakesAlerts.controller.utils.OnLineTracker;
import com.liveEarthquakesAlerts.controller.utils.SaveResponseToDB;
import com.liveEarthquakesAlerts.model.LocationPOJO;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.LastEarthquakeDate;
import com.liveEarthquakesAlerts.model.database.LastEarthquakeDateRisky;
import com.squareup.otto.Subscribe;

import java.util.List;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener, OnScrollListener {

    public static String bannerText;
    private final String TAG = "MainActivity";
    private ProgressDialog pd;
    private ListView list;
    private int currentScrollState, currentFirstVisibleItem, currentVisibleItemCount, currentTotalItemCount;
    private ListViewAdapter adapter;
    private TextView tvEmptyMessage;
    private TextView tvBanner;
    private boolean isConnectToInternet = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar1);


////        remove the left margin from the logo
        mToolbar.setPadding(2, 0, 0, 0);//for tab otherwise give space in tab
        mToolbar.setContentInsetsAbsolute(0, 0);

        setSupportActionBar(mToolbar);
//
////        set the logo icon
        mToolbar.setLogo(R.drawable.icon1);

        AppSettings.setDefaultSettings(); //SingleFragmentActivity -- AppSettings -- other classes

        if (new LastEarthquakeDate().GetRowCount() == 0) { //if no earthquakes already in our database, then find total no of new records by querying JSON
            //checking just one earthquake record exists if enough to tell, this apk has previously synced the earthquakes
            LastEarthquakeDate led = new LastEarthquakeDate();
            //sets datemilis for any arbitrary record
            led.setDateMilis(606175200000l); //some date of 1989. This is starting datemilis
            Log.i("Datemilis", String.valueOf(led.getDateMilis()));
            led.Insert(); //this ultimately creates a earthquake row
            //this ensures the LastEarthquakeDate table is not null

        }

        if (new LastEarthquakeDateRisky().GetRowCount() == 0) {
            LastEarthquakeDateRisky led = new LastEarthquakeDateRisky();
            led.setDateMilis(606175200000l);
            Log.i("Datemilis", String.valueOf(led.getDateMilis()));
            led.Insert();

        }


        tvEmptyMessage = (TextView) findViewById(R.id.tv_empty_message);
        tvBanner = (TextView) findViewById(R.id.mile_banner);
        tvBanner.setText(" ");
        list = (ListView) findViewById(R.id.list2); //adds ListView in this, MainActivity. This list is for storing
        // earthquakes record in GUI

        list.setOnItemLongClickListener(this); // adds listeners on that ListView
        list.setOnScrollListener(this); //
        list.setOnItemClickListener(this);

//        start service to track user location
        if (!LocationTracker.isServiceRunning) { //if service not running
            Intent intent = new Intent(getApplicationContext(), LocationTracker.class); //start service
            startService(intent);
        }

        initializeFirebaseRealtimeDB();


//        start service to fetch earthquakes

        Intent intent = new Intent(getBaseContext(), EarthquakesDataSyncService.class); //start service
        startService(intent);

        pd = new ProgressDialog(MainActivity.this); //show progressbar
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setTitle(getString(R.string.PleaseWait));
        pd.setMessage(getString(R.string.DatasLoading));
        pd.setCancelable(true);
        pd.setIndeterminate(true);
        pd.show();

    }

    private void initializeFirebaseRealtimeDB() {
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().getRoot();
        Log.i("reference1", databaseReference.toString());

        final ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.getChildren().iterator().hasNext()) {//empty

                    //create Firebase Realtime DB jsonOriginal structure and upload earthquake JSON

                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                    clientHelper.initializeFirebase(CreateRequestUrl.URL_USGS(0), databaseReference);

//                    unregister valueEventListener
                    databaseReference.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReference.addValueEventListener(valueEventListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        App.bus.register(this); //registration of Otto Bus
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        Log.i("MainActivity", event.getStatus() + " ");

        if (event.getStatus() == 999) {
            isConnectToInternet = false;
            list.setEmptyView(tvEmptyMessage);
            list.setAdapter(null);
        } else {
            isConnectToInternet = true;
            List<EarthQuakes> EarthQuakeList;
            Log.i("ConnectInternet", "true");
            if (AppSettings.getInstance().getProximityMiles() == 0) {
                EarthQuakeList = new EarthQuakes().GetAllDataUserProximity();
            } else {
                EarthQuakeList = new EarthQuakes().GetAllData();
            }
            if (EarthQuakeList.size() > 0) {
                Log.i("EarthquakeData", "Yes");
                adapter = new ListViewAdapter(MainActivity.this, EarthQuakeList);
                adapter.notifyDataSetChanged();
                list.setAdapter(adapter);
                list.setSelectionFromTop(currentFirstVisibleItem, 0); // (x,y)
            }

        }

        if (pd != null && pd.isShowing()) {
            Log.i("Inside pd", "pd is running");
            pd.dismiss();
            pd = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//            Show the Emergency contact icon
        Log.i("Visible", String.valueOf(View.VISIBLE));
        if (AppSettings.getInstance().isFavourite()) {
            menu.getItem(0).setVisible(true);
//            On every emergency contacts enabled, animate the Emergency contact icon. Will do later

        } else {
            menu.getItem(0).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_main) {

            Intent i1 = new Intent(MainActivity.this, SettingsActivity.class); //It is actually communicating with SettingsActivity's fragment
            //explicit intent to start SingleFragmentActivity
            startActivityForResult(i1, 7777); //Update Menu from this activity. //(Intent, requestCode)

            return true;
        }

        if (item.getItemId() == R.id.e_contact) {

            Intent i2 = new Intent(MainActivity.this, com.odoo.HomeActivity.class); //explicit intent to start SingleFragmentActivity
            startActivity(i2);
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
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent1, View view, int position, long id) {
        EarthQuakes eq = (EarthQuakes) parent1.getAdapter().getItem(position);

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


}