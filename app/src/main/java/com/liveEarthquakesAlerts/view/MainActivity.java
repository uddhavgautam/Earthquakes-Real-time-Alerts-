package com.liveEarthquakesAlerts.view;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import com.liveEarthquakesAlerts.controller.utils.broadcastReceiver.OutgoingReceiver;
import com.liveEarthquakesAlerts.model.LocationPOJO;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;
import com.liveEarthquakesAlerts.model.database.LastEarthquakeDate;
import com.liveEarthquakesAlerts.model.database.LastEarthquakeDateRisky;
import com.liveEarthquakesAlerts.model.database.RiskyEarthquakes;
import com.odoo.FavoriteNumberBean;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener, OnScrollListener {

    public static String bannerText;
    public static Context mainApplicationContext;
    public static Intent locInitServiceIntent;
    private final String TAG = "MainActivity";
    public String messageEarthquake;
    private ProgressDialog pd;
    private ListView list;
    private int currentScrollState, currentFirstVisibleItem, currentVisibleItemCount, currentTotalItemCount;
    private ListViewAdapter adapter;
    private TextView tvEmptyMessage;
    private TextView tvBanner;
    private boolean isConnectToInternet = true;
    private IncomingReceiver incomingReceiver;
    private OutgoingReceiver outgoingReceiver;
    private MyOwnCustomLog myOwnCustomLog = new MyOwnCustomLog();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mainApplicationContext = getApplicationContext();
        super.onCreate(savedInstanceState);

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
        list.setOnScrollListener(this);
        list.setOnItemClickListener(this);


    }

    private void initializeFirebaseRealtimeDB() {
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().getRoot();
        Log.i("DbReference", databaseReference + "");

        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //if there is data. Don't do null checking using "==" operator. This understanding is wrong

                //Note: Firebase writes "null" string for null

                //below myVarData can throw null\n. Please remember this
                String myVarData = SaveResponseToDB.getFirebaseWholeData("https://earthquakesenotifications.firebaseio.com/realTimeEarthquakes.json?print=pretty");
                Log.i("myVarData", "\"" + myVarData + "\"" + " hello gautam! " + myVarData.equals(null));
                if ((!myVarData.equals("null"))) { //realtime db already exists
                    Log.i("else", "inside elsewer, realtime DB already exists");
//                    create Firebase Realtime DB jsonOriginal structure and upload earthquake JSON
                    SaveResponseToDB.isInitialized = true;
                    Intent intent = new Intent();
                    intent.setAction("SaveResponseToDB.isInitialized.Uddhav").putExtra("isInitializedAlready", SaveResponseToDB.isInitialized);
                    sendBroadcast(intent);


                } else {
                    Log.i("else", "no real time db");
                    SaveResponseToDB.isInitialized = false;//initialized but not properly. Therefore isInitialized = false
                    SaveResponseToDB clientHelper = new SaveResponseToDB(); //clears the database in constructor
                    SaveResponseToDB.updateFirebase(CreateRequestUrl.URL_USGS(), databaseReference);
                }

            }
        });
        newThread.start();
    }

    @Override
    protected void onStart() { //main thread
        super.onStart();

        //show the log
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        String simpleName = this.getClass().getSimpleName();
        myOwnCustomLog.addLog(simpleName, Thread.currentThread().getStackTrace()[2].getMethodName().toString(), stackTraces);

        //make sure firebase realtime DB initialized completed. Why? Because as LocTrackService gets the location
        //I start fetching from firebase. There I create reference. If I don't have already initialized, then it throws null pointer
        //exception on those references
        locInitServiceIntent = new Intent(this, LocTrackService.class);

        //main thread should not be blocked. Therefore, two solutions here:
        //1) Either I create another thread and run startService(locInitServiceIntent) from there. Make the created thread never dies and listen from the thread when I updated firebase database
        //2) BroadcastReceiver. Since there is chain like "main thread> another threadX > main therad > another thread> another thread> main thread> another threadZ. Our situation is like communication threadX and threadZ.

        incomingReceiver = new IncomingReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra("isInitializedAlready", false)) { //it means already initialized
                    Log.i(TAG, "Successfully loctracking service is triggered!");
                    startService(locInitServiceIntent);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SaveResponseToDB.isInitialized.Uddhav");
        registerReceiver(incomingReceiver, intentFilter);


        initializeFirebaseRealtimeDB(); //completed


        App.bus.register(this);

//        pd = new ProgressDialog(MainActivity.this); //show progressbar
//        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        pd.setTitle(getString(R.string.PleaseWait));
//        pd.setMessage(getString(R.string.DatasLoading));
//        pd.setCancelable(true);
//        pd.setIndeterminate(true);
//        pd.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(incomingReceiver);
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
        } else {


            RiskyEarthquakes riskyEarthquakes = new RiskyEarthquakes();
            List<RiskyEarthquakes> allRiskyEarthquakes = riskyEarthquakes.GetAllData();

//          update the RiskyEarthquakes
            for (RiskyEarthquakes r : allRiskyEarthquakes) {
                if (!CheckRiskEarthquakes.checkRisky(r)) {
                    r.DeleteRow(r.getDateMilis());
                }
            }

            for (RiskyEarthquakes r : allRiskyEarthquakes) {
                while (CheckRiskEarthquakes.checkRisky(r)) {
                    //Notify user
                    notificationHandler();
                    //Notify emergency only one time, then pop the "I am Ok" button to click and push messages "I am ok"
                    sendMsgToEmergencyContacts();
                }
            }


//update the adapter
            List<EarthQuakes> EarthQuakeList;
            Log.i("ConnectInternet", "true");
            if (AppSettings.getInstance().getProximity() == 1) {
                EarthQuakeList = new EarthQuakes().GetAllDataUserProximity();
                if (EarthQuakeList.size() > 0) {
                    adapter = new ListViewAdapter(MainActivity.this, EarthQuakeList);
                    adapter.notifyDataSetChanged();
                    list.setAdapter(adapter);
                    list.setSelectionFromTop(currentFirstVisibleItem, 0); // (x,y)
                }
            }

            if (AppSettings.getInstance().getProximity() == 0) {
                EarthQuakeList = new EarthQuakes().GetAllData();

                if (EarthQuakeList.size() > 0) {
                    adapter = new ListViewAdapter(MainActivity.this, EarthQuakeList);
                    adapter.notifyDataSetChanged();
                    list.setAdapter(adapter);
                    list.setSelectionFromTop(currentFirstVisibleItem, 0); // (x,y)
                }
            }
        }

        //stop the progress bar

//        if (pd != null && pd.isShowing()) {
//            Log.i("Inside pd", "pd is running");
//            pd.dismiss();
//            pd = null;
//        }
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


    public void notificationHandler() {
        showNotification();
    }

    public void showNotification() {
        List<RiskyEarthquakes> newEarthquakes = new RiskyEarthquakes().newEarthquakes();

        if (newEarthquakes.size() > 0) { //if there are earthquakes

            if (AppSettings.getInstance().isNotifications()) {
                createNotification(getString(R.string.EarthquakesDetect), "" + newEarthquakes.get(0).getMagnitude() + "  |  " + newEarthquakes.get(0).getLocationName());
                messageEarthquake = "Earthquake Hit !!" + newEarthquakes.get(0).getMagnitude() + "  |  " + newEarthquakes.get(0).getLocationName();

            }


            LastEarthquakeDate led = new LastEarthquakeDate();
            led.setDateMilis(new EarthQuakes().GetLastEarthQuakeDate());
            led.Insert();
        }
    }

    public void sendMsgToEmergencyContacts() {
        FavoriteNumberBean favoriteNumberBean = new FavoriteNumberBean(false);
        ArrayList<String> mobileList = favoriteNumberBean.getMobileNumber();
        SmsManager smsManager = SmsManager.getDefault();
        //send every emergency contacts the messages
        for (String phone : mobileList) {
            smsManager.sendTextMessage(phone, null, messageEarthquake, null, null);
        }
        //now create "I am Ok button", tap it to send "I am Ok" messages to every emergency contacts

        //create a floating button
        FloatingActionButton floatingActionButton = new FloatingActionButton(getApplicationContext());
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FavoriteNumberBean favoriteNumberBean = new FavoriteNumberBean(false);
                ArrayList<String> mobileList = favoriteNumberBean.getMobileNumber();
                SmsManager smsManager = SmsManager.getDefault();
                //send every emergency contacts the messages
                for (String phone : mobileList) {
                    smsManager.sendTextMessage(phone, null, "I am Ok!", null, null);
                }
            }
        });
    }

    public void createNotification(String strContentTitle, String strContentText) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext()) //
                .setSmallIcon(R.drawable.icon1) //
                .setContentTitle(strContentTitle) //
                .setContentText(strContentText);

        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);
        builder.setAutoCancel(true);
        builder.setLights(Color.BLUE, 500, 500);

        if (AppSettings.getInstance().isVibration()) {
            long[] pattern = {500, 500};
            builder.setVibrate(pattern);
        }
        if (AppSettings.getInstance().isSound()) {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(alarmSound);
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}