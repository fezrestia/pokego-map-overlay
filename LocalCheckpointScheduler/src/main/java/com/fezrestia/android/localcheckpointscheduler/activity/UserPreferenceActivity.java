package com.fezrestia.android.localcheckpointscheduler.activity;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.fezrestia.android.localcheckpointscheduler.Constants;
import com.fezrestia.android.localcheckpointscheduler.R;
import com.fezrestia.android.localcheckpointscheduler.UserApplication;
import com.fezrestia.android.localcheckpointscheduler.control.OverlayViewController;
import com.fezrestia.android.localcheckpointscheduler.util.Log;

public class UserPreferenceActivity extends PreferenceActivity {
    // Log tag.
    private static final String TAG = UserPreferenceActivity.class.getSimpleName();

    // Preference.
    private Preference mOverlayEnDis = null;
    private Preference mCycleRecordEnDis = null;

    @Override
    public void onCreate(Bundle bundle) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onCreate()");
        super.onCreate(bundle);

        // Add view finder anywhere preferences.
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onResume()");
        super.onResume();

        // Update preferences.
        updatePreferences();
        applyCurrentPreferences();

        // Overlay enabled or not.
        mOverlayEnDis = findPreference(Constants.SP_KEY_OVERLAY_VIEW_ENABLED);
        mOverlayEnDis.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        bindPreference(mOverlayEnDis);
        // Record enabled or not.
        mCycleRecordEnDis = findPreference(Constants.SP_KEY_CYCLE_RECORD_ENABLED);
        mCycleRecordEnDis.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        bindPreference(mCycleRecordEnDis);
    }

    @Override
    public void onPause() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onPause()");
        super.onPause();
        // NOP.
    }

    @Override
    public void onDestroy() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onDestroy()");
        super.onDestroy();
        // NOP.
    }

    private void updatePreferences() {


    }

    private void applyCurrentPreferences() {



    }

    private final OnPreferenceChangeListenerImpl mOnPreferenceChangeListener
            = new OnPreferenceChangeListenerImpl();
    private class OnPreferenceChangeListenerImpl
            implements  Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // NOP.
            } else if (preference instanceof CheckBoxPreference) {
                String key = preference.getKey();
                if (key == null) {
                    // NOP.
                    if (Log.IS_DEBUG) Log.logDebug(TAG, "CheckBox key == null");
                } else if (Constants.SP_KEY_OVERLAY_VIEW_ENABLED.equals(key)) {
                    final boolean isChecked = ((Boolean) value).booleanValue();

                    if (isChecked) {
                        // Update UI.
                        mCycleRecordEnDis.setEnabled(true);

                        // Start overlay.
                        OverlayViewController.LifeCycleTrigger.getInstance()
                                .requestStart(getApplicationContext());
                    } else {
                        // Update UI.
                        mCycleRecordEnDis.setEnabled(false);

                        // Stop overlay.
                        OverlayViewController.LifeCycleTrigger.getInstance()
                                .requestStop(getApplicationContext());
                    }
                } else if (Constants.SP_KEY_CYCLE_RECORD_ENABLED.equals(key)) {
                    final boolean isChecked = ((Boolean) value).booleanValue();

                    if (isChecked) {
                        // Update UI.
                        mOverlayEnDis.setEnabled(false);

                        // Start recording.
                        OverlayViewController.getInstance().startCyclicScreenShotTask();
                    } else {
                        // Update UI.
                        mOverlayEnDis.setEnabled(true);

                        // Stop recording.
                        OverlayViewController.getInstance().stopCyclicScreenShotTask();
                    }
                } else {
                    // NOP.
                    if (Log.IS_DEBUG) Log.logDebug(TAG, "Unexpected CheckBox preference.");
                }
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    }

    private void bindPreference(Preference preference) {

        String key = preference.getKey();

        switch (key) {
            case Constants.SP_KEY_OVERLAY_VIEW_ENABLED:
                boolean isCycleRecordEnabled = UserApplication.getGlobalSharedPreferences()
                        .getBoolean(Constants.SP_KEY_CYCLE_RECORD_ENABLED, false);
                if (isCycleRecordEnabled) {
                    preference.setEnabled(false);
                }
                break;

            case Constants.SP_KEY_CYCLE_RECORD_ENABLED:
                boolean isOverlayEnabled = UserApplication.getGlobalSharedPreferences()
                        .getBoolean(Constants.SP_KEY_OVERLAY_VIEW_ENABLED, false);
                if (!isOverlayEnabled) {
                    preference.setEnabled(false);
                }
                break;

            default:
                throw new IllegalArgumentException();


            // List
        // Set the listener to watch for value changes.
//        preference.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
//
//        mOnPreferenceChangeListener.onPreferenceChange(
//                preference,
//                PreferenceManager
//                        .getDefaultSharedPreferences(preference.getContext())
//                        .getString(preference.getKey(), ""));

        }
    }
}
