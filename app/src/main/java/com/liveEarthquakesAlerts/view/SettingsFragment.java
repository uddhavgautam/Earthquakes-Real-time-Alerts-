package com.liveEarthquakesAlerts.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.view.MenuItem;

import com.liveEarthquakesAlerts.R;
import com.liveEarthquakesAlerts.controller.utils.AppSettings;


/**
 * Created by  Uddhav Gautam  on 1/5/17.
 */
public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private String Key_TimeInterval, Key_Magnitude, Key_Proximity;
    private String Key_Sorting, Key_EmergencyPhoneContactEnabled, Key_Notifications, Key_Vibration, Key_Sound;
    private ListPreference lpTimeInterval, lpMagnitude, lpSorting, lpProximity;
    private CheckBoxPreference cbNotifications, cbVibration, cbSound, cbEmergencyContacts;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);

        Key_Proximity = getResources().getString(R.string.listPref_Key_Proximity);

        Key_TimeInterval = getResources().getString(R.string.listPref_Key_TimeInterval);
        Key_Magnitude = getResources().getString(R.string.listPref_Key_Magnitude);
        Key_Sorting = getResources().getString(R.string.listPref_Key_Sorting);
        Key_Notifications = getResources().getString(R.string.CheckBoxPref_Key_Notifications);
        Key_Vibration = getResources().getString(R.string.CheckBoxPref_Key_Vibration);
        Key_Sound = getResources().getString(R.string.CheckBoxPref_Key_Sound);
        Key_EmergencyPhoneContactEnabled = getResources().getString(R.string.CheckBoxPref_Key_Phone);

        lpProximity = (ListPreference) findPreference(Key_Proximity);

        lpTimeInterval = (ListPreference) findPreference(Key_TimeInterval);
        lpMagnitude = (ListPreference) findPreference(Key_Magnitude);
        lpSorting = (ListPreference) findPreference(Key_Sorting);
        cbNotifications = (CheckBoxPreference) findPreference(Key_Notifications);
        cbVibration = (CheckBoxPreference) findPreference(Key_Vibration);
        cbSound = (CheckBoxPreference) findPreference(Key_Sound);
        cbEmergencyContacts = (CheckBoxPreference) findPreference(Key_EmergencyPhoneContactEnabled);


        lpProximity.setOnPreferenceChangeListener(this);

        lpTimeInterval.setOnPreferenceChangeListener(this);
        lpMagnitude.setOnPreferenceChangeListener(this);
        cbNotifications.setOnPreferenceChangeListener(this);
        cbVibration.setOnPreferenceChangeListener(this);
        cbSound.setOnPreferenceChangeListener(this);
        cbEmergencyContacts.setOnPreferenceChangeListener(this);

        initSummary(getPreferenceScreen());


        if (cbNotifications.isChecked()) {
            cbVibration.setEnabled(true);
            cbSound.setEnabled(true);
            cbEmergencyContacts.setEnabled(true);
        } else {
            cbVibration.setEnabled(false);
            cbSound.setEnabled(false);
            cbEmergencyContacts.setEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) { //if p is instance of top level
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }

        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }

        if (p instanceof CheckBoxPreference) { //preference p can be instantiated from CheckBoxPreference
            if (p instanceof PreferenceCategory) {
                PreferenceCategory pCat = (PreferenceCategory) p;
                if (pCat.getTitle().equals("Notifications")) {
                    CheckBoxPreference checkBoxPref = (CheckBoxPreference) p;
                    p.setSummary(checkBoxPref.isChecked() ? getResources().getString(R.string.statu_on) : getResources().getString(R.string.statu_off));
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (AppSettings.getInstance().isFavourite()) {
            Intent i = new Intent(getActivity(), MainActivity.class);
            i.putExtra("str", true); //(key,value)
            getActivity().setResult(getActivity().RESULT_OK, i); //100 is request code
            getActivity().finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        String value = newValue.toString();
        if (key.equalsIgnoreCase(Key_TimeInterval)) { //time interval
            lpTimeInterval.setSummary(lpTimeInterval.getEntries()[Integer.parseInt(value)]);
        } else if (key.equalsIgnoreCase(Key_Proximity)) { //proximity
            lpProximity.setSummary(lpProximity.getEntries()[Integer.parseInt(value)]);
            Log.i("Summary", lpProximity.getEntries()[Integer.parseInt(value)].toString());
        } else if (key.equalsIgnoreCase(Key_Magnitude)) { //magnitude
            lpMagnitude.setSummary(lpMagnitude.getEntries()[Integer.parseInt(value)]);
        } else if (key.equalsIgnoreCase(Key_Sorting)) { //sorting
            lpSorting.setSummary(lpSorting.getEntries()[Integer.parseInt(value)]);
        } else if (key.equalsIgnoreCase(Key_Notifications)) {
            if (value.equals("true")) {
                cbNotifications.setSummary(getResources().getString(R.string.statu_on));
                cbVibration.setEnabled(true);
                cbSound.setEnabled(true);
                cbEmergencyContacts.setEnabled(true);
            } else {
                cbNotifications.setSummary(getResources().getString(R.string.statu_off));
                cbVibration.setEnabled(false);
                cbVibration.setChecked(false);
                cbVibration.setSummary(getResources().getString(R.string.statu_off));
                cbSound.setEnabled(false);
                cbSound.setChecked(false);
                cbSound.setSummary(getResources().getString(R.string.statu_off));
                cbEmergencyContacts.setEnabled(false);
                cbEmergencyContacts.setChecked(false);
                cbEmergencyContacts.setSummary(getResources().getString(R.string.statu_off));
            }
        } else if (key.equalsIgnoreCase(Key_Vibration)) {
            if (value.equals("true")) {
                cbVibration.setSummary(getResources().getString(R.string.statu_on));
            } else {
                cbVibration.setSummary(getResources().getString(R.string.statu_off));
            }
        } else if (key.equalsIgnoreCase(Key_Sound)) {
            if (value.equals("true")) {
                cbSound.setSummary(getResources().getString(R.string.statu_on));
            } else {
                cbSound.setSummary(getResources().getString(R.string.statu_off));
            }
        } else if (key.equalsIgnoreCase(Key_EmergencyPhoneContactEnabled)) {
            if (value.equals("true")) {
                cbEmergencyContacts.setSummary(getResources().getString(R.string.sync_on));
            } else {
                cbEmergencyContacts.setSummary(getResources().getString(R.string.sync_off));
            }
        }
        return true;
    }
}
