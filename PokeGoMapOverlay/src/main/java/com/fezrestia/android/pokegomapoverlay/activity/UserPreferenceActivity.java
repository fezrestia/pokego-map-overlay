package com.fezrestia.android.pokegomapoverlay.activity;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.fezrestia.android.pokegomapoverlay.Constants;
import com.fezrestia.android.pokegomapoverlay.R;
import com.fezrestia.android.pokegomapoverlay.UserApplication;
import com.fezrestia.android.pokegomapoverlay.control.OverlayViewController;
import com.fezrestia.android.pokegomapoverlay.util.Log;

public class UserPreferenceActivity extends PreferenceActivity {
    // Log tag.
    private static final String TAG = UserPreferenceActivity.class.getSimpleName();

    // Preference.
    private Preference mOverlayEnDis = null;
    private Preference mCycleRecordEnDis = null;
    private Preference mAlwaysReloadEnDis = null;
    private Preference mReloadIntervalSetting = null;
    private Preference mBaseLoadUrlSetting = null;

    // Setting value.
    private boolean mIsAlwaysReload = false;

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
        // Base load URL.
        mBaseLoadUrlSetting = findPreference(Constants.SP_KEY_BASE_LOAD_URL);
        mBaseLoadUrlSetting.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        bindPreference(mBaseLoadUrlSetting);
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
            String key = preference.getKey();
            String stringValue = value.toString();

            switch(key) {
                case Constants.SP_KEY_OVERLAY_VIEW_ENABLED:
                    {
                        final boolean isChecked = ((Boolean) value).booleanValue();

                        if (isChecked) {
                            // Update UI.
                            mCycleRecordEnDis.setEnabled(true);

                            // Start overlay.
                            OverlayViewController.LifeCycleTrigger.getInstance()
                                    .requestStart(getApplicationContext(), mIsAlwaysReload);
                        } else {
                            // Update UI.
                            mCycleRecordEnDis.setEnabled(false);

                            // Stop overlay.
                            OverlayViewController.LifeCycleTrigger.getInstance()
                                    .requestStop(getApplicationContext());
                        }
                    }
                    break;

                case Constants.SP_KEY_BASE_LOAD_URL:
                    {
                        // Update.
                        updateBaseLoadUrlPreferenceSummary(stringValue);
                    }
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            return true;
        }
    }

    private void bindPreference(Preference preference) {

        String key = preference.getKey();

        switch (key) {
            case Constants.SP_KEY_OVERLAY_VIEW_ENABLED:
                break;

            case Constants.SP_KEY_BASE_LOAD_URL:
                String baseLoadUrl = UserApplication.getGlobalSharedPreferences()
                        .getString(Constants.SP_KEY_BASE_LOAD_URL, null);
                // Update.
                updateBaseLoadUrlPreferenceSummary(baseLoadUrl);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    private void updateBaseLoadUrlPreferenceSummary(String changedUrl) {
        if (changedUrl != null) {
            if (changedUrl.isEmpty()) {
                // Empty.
                mBaseLoadUrlSetting.setSummary(
                        "Empty is DEFAULT:\n" + Constants.DEFAULT_LOAD_URL);
            } else {
                // Valid URL.
                mBaseLoadUrlSetting.setSummary("Your URL:\n" + changedUrl);
            }
        } else {
            mBaseLoadUrlSetting.setSummary(
                    "Indicates your URL. Empty is reset to DEFAULT.");
        }
    }
}
