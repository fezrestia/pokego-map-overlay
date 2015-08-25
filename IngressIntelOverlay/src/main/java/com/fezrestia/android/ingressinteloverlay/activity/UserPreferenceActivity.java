package com.fezrestia.android.ingressinteloverlay.activity;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.fezrestia.android.ingressinteloverlay.Constants;
import com.fezrestia.android.ingressinteloverlay.R;
import com.fezrestia.android.ingressinteloverlay.UserApplication;
import com.fezrestia.android.ingressinteloverlay.control.OverlayViewController;
import com.fezrestia.android.ingressinteloverlay.util.Log;

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
    private int mScreenShotIntervalMin = Constants.DEFAULT_CAPTURE_INTERVAL_MIN;

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
        // Always reload after capture or not.
        mAlwaysReloadEnDis = findPreference(Constants.SP_KEY_ALWAYS_RELOAD_ENABLED);
        mAlwaysReloadEnDis.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        bindPreference(mAlwaysReloadEnDis);
        // Reload interval setting.
        mReloadIntervalSetting = findPreference(Constants.SP_KEY_CAPTURE_INTERVAL_MIN);
        mReloadIntervalSetting.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        bindPreference(mReloadIntervalSetting);
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

                case Constants.SP_KEY_CYCLE_RECORD_ENABLED:
                    {
                        final boolean isChecked = ((Boolean) value).booleanValue();

                        if (isChecked) {
                            // Update UI.
                            mOverlayEnDis.setEnabled(false);

                            // Show overlay view.
                            OverlayViewController.getInstance().resume();

                            // Start recording.
                            OverlayViewController.getInstance().startCyclicScreenShotTask(
                                    mScreenShotIntervalMin);
                            // Finish own self.
                            UserPreferenceActivity.this.finish();
                        } else {
                            // Update UI.
                            mOverlayEnDis.setEnabled(true);

                            // Stop recording.
                            OverlayViewController.getInstance().stopCyclicScreenShotTask();

                            // Show overlay view.
                            OverlayViewController.getInstance().resume();
                        }
                    }
                    break;

                case Constants.SP_KEY_ALWAYS_RELOAD_ENABLED:
                    {
                        // NOP.
                    }
                    break;

                case Constants.SP_KEY_CAPTURE_INTERVAL_MIN:
                    {
                        mScreenShotIntervalMin = Integer.parseInt(stringValue);
                    }
                    break;

                case Constants.SP_KEY_BASE_LOAD_URL:
                    {
                        // Update.
                        updateBaseLoadUrlPreferenceSummary(stringValue);

                        // Check intel map based URL or not.
//                        final boolean isValid = stringValue.startsWith(
//                                Constants.DEFAULT_LOAD_URL);
//                        if (isValid) {
//                            mBaseLoadUrlSetting.setSummary(stringValue);
//                        } else {
//                            // Invalid.
//                            return false;
//                        }
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

            case Constants.SP_KEY_ALWAYS_RELOAD_ENABLED:
                boolean isAlwaysReloadEnabled = UserApplication.getGlobalSharedPreferences()
                        .getBoolean(Constants.SP_KEY_ALWAYS_RELOAD_ENABLED, false);
                ((CheckBoxPreference) preference).setChecked(isAlwaysReloadEnabled);
                mIsAlwaysReload = isAlwaysReloadEnabled;
                break;

            case Constants.SP_KEY_CAPTURE_INTERVAL_MIN:
                String captureIntervalString
                        = UserApplication.getGlobalSharedPreferences().getString(
                                Constants.SP_KEY_CAPTURE_INTERVAL_MIN,
                                String.valueOf(Constants.DEFAULT_CAPTURE_INTERVAL_MIN));
                mScreenShotIntervalMin = Integer.parseInt(captureIntervalString);
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
