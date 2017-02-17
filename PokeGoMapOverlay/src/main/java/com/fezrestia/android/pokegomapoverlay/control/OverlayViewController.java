package com.fezrestia.android.pokegomapoverlay.control;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.view.LayoutInflater;

import com.fezrestia.android.pokegomapoverlay.Constants;
import com.fezrestia.android.pokegomapoverlay.R;
import com.fezrestia.android.pokegomapoverlay.service.OverlayViewService;
import com.fezrestia.android.pokegomapoverlay.util.Log;
import com.fezrestia.android.pokegomapoverlay.view.OverlayRootView;

public class OverlayViewController {
    // Log tag.
    private static final String TAG = OverlayViewController.class.getSimpleName();

    // Master context.
    private  Context mContext;

    // UI thread worker.
    private Handler mUiWorker = new Handler();

    // Singleton instance
    private static final OverlayViewController INSTANCE = new OverlayViewController();

    // Overlay view.
    private OverlayRootView mRootView = null;

    // Always reload after capture screen shot.
    private boolean mIsAlwaysReloadEnabled = false;

    /**
     * Life cycle trigger interface.
     */
    public static class LifeCycleTrigger {
        private static final String TAG = LifeCycleTrigger.class.getSimpleName();
        private static final LifeCycleTrigger INSTANCE = new LifeCycleTrigger();

        // Wake lock.
        private PowerManager.WakeLock mWakeLock = null;

        // CONSTRUCTOR.
        private LifeCycleTrigger() {
            // NOP.
        }

        /**
         * Get accessor.
         *
         * @return
         */
        public static LifeCycleTrigger getInstance() {
            return INSTANCE;
        }

        /**
         * Start.
         *
         * @param context
         * @param isAlwaysReloadEnabled
         */
        public void requestStart(Context context, boolean isAlwaysReloadEnabled) {
            Intent service = new Intent(context, OverlayViewService.class);
            ComponentName component = context.startService(service);

            // Wake lock.
            if (mWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        context.getPackageName());
                mWakeLock.acquire();
            }

            if (Log.IS_DEBUG) {
                if (component != null) {
                    Log.logDebug(TAG, "requestStart() : Component = " + component.toString());
                } else {
                    Log.logDebug(TAG, "requestStart() : Component = NULL");
                }
            }
        }

        /**
         * Stop.
         *
         * @param context
         */
        public void requestStop(Context context) {
            Intent service = new Intent(context, OverlayViewService.class);
            boolean isSuccess = context.stopService(service);

            // Wake lock.
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }

            if (Log.IS_DEBUG) Log.logDebug(TAG, "requestStop() : isSuccess = " + isSuccess);
        }
    }

    /**
     * CONSTRUCTOR.
     */
    private OverlayViewController() {
        // NOP.
    }

    /**
     * Get singleton controller instance.
     *
     * @return
     */
    public static synchronized OverlayViewController getInstance() {
        return INSTANCE;
    }

    /**
     * Start overlay view finder.
     *
     * @param context
     * @param isAlwaysReloadEnabled
     */
    public void start(Context context, boolean isAlwaysReloadEnabled) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "start() : E");

        if (mRootView != null) {
            // NOP. Already started.
            Log.logError(TAG, "Error. Already started.");
            return;
        }

        // Cache master context.
        mContext = context;

        // Flag.
        mIsAlwaysReloadEnabled = isAlwaysReloadEnabled;

        // Load preferences.
        loadPreferences();

        // Create overlay view.
        mRootView = (OverlayRootView)
                LayoutInflater.from(context).inflate(R.layout.overlay_root_view, null);
        mRootView.initialize(false); //TODO:Impl loading detection

        // Add to window.
        mRootView.addToOverlayWindow();

        if (Log.IS_DEBUG) Log.logDebug(TAG, "start() : X");
    }

    private void loadPreferences() {



    }

    /**
     * Resume overlay view finder.
     */
    public void resume() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "resume() : E");

        mRootView.requestShowOverlay();

        if (Log.IS_DEBUG) Log.logDebug(TAG, "resume() : X");
    }

    /**
     * Overlay UI is active or not.
     *
     * @return
     */
    public boolean isOverlayActive() {
        return (mRootView != null);
    }

    /**
     * Pause overlay view finder.
     */
    public void pause() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "pause() : E");

        mRootView.requestHideOverlay();

        if (Log.IS_DEBUG) Log.logDebug(TAG, "pause() : X");
    }

    /**
     * Stop overlay view finder.
     */
    public void stop() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "stop() : E");

        if (mRootView == null) {
            // NOP. Already stopped.
            Log.logError(TAG, "Error. Already stopped.");
            return;
        }

        // Release references.
        mContext = null;
        if (mRootView != null) {
            mRootView.release();
            mRootView.removeFromOverlayWindow();
            mRootView = null;
        }

        if (Log.IS_DEBUG) Log.logDebug(TAG, "stop() : X");
    }
}
