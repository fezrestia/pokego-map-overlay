package com.fezrestia.android.pokegomapoverlay.control;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;

import com.fezrestia.android.pokegomapoverlay.R;
import com.fezrestia.android.pokegomapoverlay.service.OverlayViewService;
import com.fezrestia.android.pokegomapoverlay.view.OverlayRootView;
import com.fezrestia.android.util.Log;

public class OverlayViewController {
    // Log tag.
    private static final String TAG = "OverlayViewController";

    // Singleton instance
    private static final OverlayViewController INSTANCE = new OverlayViewController();

    // Overlay view.
    private OverlayRootView mRootView = null;

    /**
     * Life cycle trigger interface.
     */
    public static class LifeCycleTrigger {
        private static final String TAG = "LifeCycleTrigger";
        private static final LifeCycleTrigger INSTANCE = new LifeCycleTrigger();

        // CONSTRUCTOR.
        private LifeCycleTrigger() {
            // NOP.
        }

        /**
         * Get accessor.
         *
         * @return Life cycle trigger instance.
         */
        public static LifeCycleTrigger getInstance() {
            return INSTANCE;
        }

        /**
         * Start.
         *
         * @param context Master context.
         */
        public void requestStart(Context context) {
            Intent service = new Intent(context, OverlayViewService.class);
            ComponentName component = context.startService(service);

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
         * @param context Master context.
         */
        public void requestStop(Context context) {
            Intent service = new Intent(context, OverlayViewService.class);
            boolean isSuccess = context.stopService(service);

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
     * @return Controller singleton instance.
     */
    public static synchronized OverlayViewController getInstance() {
        return INSTANCE;
    }

    /**
     * Start overlay view finder.
     *
     * @param context Master context.
     */
    public void start(Context context) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "start() : E");

        if (mRootView != null) {
            // NOP. Already started.
            Log.logError(TAG, "Error. Already started.");
            return;
        }

        // Load preferences.
        loadPreferences();

        // Create overlay view.
        mRootView = (OverlayRootView)
                LayoutInflater.from(context).inflate(R.layout.overlay_root_view, null);
        mRootView.initialize();

        // Add to window.
        mRootView.addToOverlayWindow();

        if (Log.IS_DEBUG) Log.logDebug(TAG, "start() : X");
    }

    private void loadPreferences() {
        // NOP.
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
        mRootView.release();
        mRootView.removeFromOverlayWindow();
        mRootView = null;

        if (Log.IS_DEBUG) Log.logDebug(TAG, "stop() : X");
    }

    /**
     * Toggle overlay view visibility.
     */
    public void toggleVisibility() {
        if (mRootView != null) {
            if (mRootView.isOverlayShown()) {
                mRootView.hide();
            } else {
                mRootView.show(false);
            }
        }
    }
}
