package com.liveEarthquakesAlerts.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.liveEarthquakesAlerts.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar2);
        mToolbar.setTitle("Settings");

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


////        remove the left margin from the logo
        mToolbar.setPadding(2, 0, 0, 0);//for tab otherwise give space in tab
        mToolbar.setContentInsetsAbsolute(0, 0);

        setSupportActionBar(mToolbar);


        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(R.id.blankFragment, new SettingsFragment())
                .commit();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return super.onOptionsItemSelected(menuItem);
    }

}