package com.fezrestia.android.localcheckpointscheduler.control;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.view.LayoutInflater;

import com.fezrestia.android.localcheckpointscheduler.R;
import com.fezrestia.android.localcheckpointscheduler.Constants;
import com.fezrestia.android.localcheckpointscheduler.UserApplication;
import com.fezrestia.android.localcheckpointscheduler.service.OverlayViewService;
import com.fezrestia.android.localcheckpointscheduler.storage.StorageController;
import com.fezrestia.android.localcheckpointscheduler.util.Log;
import com.fezrestia.android.localcheckpointscheduler.view.OverlayRootView;
import com.fezrestia.android.localcheckpointscheduler.view.UserWebView;

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

    // Storage controller.
    private StorageController mStorageController = null;
    // Current set dir name.
    private String mCurrentRootDirName = "DEFAULT";
    // Screen shot set dir name pre-fix.
    private static final String SET_DIR_NAME_PREFIX = "SET_";

    // Cyclic screen shot interval.
    private static final int CYCLIC_SCREEN_SHOT_INTERVAL_SEC = 60;
    // Web page reload timeout.
    private static final int WEBVIEW_TIMEOUT_SEC = 1 * 60 * 60;

    // Cyclic screen shot task.
    private CyclicScreenShotTask mCyclicScreenShotTask = null;

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
         */
        public void requestStart(Context context) {
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
     */
    public void start(Context context) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "start() : E");

        if (mRootView != null) {
            // NOP. Already started.
            Log.logError(TAG, "Error. Already started.");
            return;
        }

        // Cache master context.
        mContext = context;

        // Load preferences.
        loadPreferences();

        // Storage controller.
        mStorageController = new StorageController(mContext);

        // Create overlay view.
        mRootView = (OverlayRootView)
                LayoutInflater.from(context).inflate(R.layout.overlay_root_view, null);
        mRootView.initialize();

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

        if (mStorageController != null) {
            mStorageController.release();
            mStorageController = null;
        }

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



    private class OnScreenShotDoneCallbackImpl implements UserWebView.OnScreenShotDoneCallback {
        private final String mDirName;

        /**
         * CONSTRUCTOR.
         *
         * @param dirName
         */
        public OnScreenShotDoneCallbackImpl(String dirName) {
            mDirName = dirName;
        }

        @Override
        public void onScreenShotDone(byte[] pngBuffer) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onScreenShotDone() : E");

            if (mStorageController != null) {
                mStorageController.storeFile(pngBuffer, mDirName);
            }

            if (Log.IS_DEBUG) Log.logDebug(TAG, "onScreenShotDone() : X");
        }
    }

    /**
     * Start cyclic screen shot task.
     */
    public void startCyclicScreenShotTask() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "startCyclicScreenShotTask()");

        // Check.
        if (mCyclicScreenShotTask != null) {
            // Already started.
            if (Log.IS_DEBUG) Log.logDebug(TAG,
                    "startCyclicScreenShotTask() : Error, already started");
            return;
        }

        // Block interaction.
        mRootView.disableInteraction();

        // Current set name.
        mCurrentRootDirName = SET_DIR_NAME_PREFIX + StorageController.getDateTimeString();

        // Start.
        mCyclicScreenShotTask = new CyclicScreenShotTask(
                new OnScreenShotDoneCallbackImpl(mCurrentRootDirName));
        mUiWorker.post(mCyclicScreenShotTask);
    }

    /**
     * Stop cyclic screen shot task.
     */
    public void stopCyclicScreenShotTask() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "stopCyclicScreenShotTask()");

        // Stop.
        if (mCyclicScreenShotTask != null) {
            mUiWorker.removeCallbacks(mCyclicScreenShotTask);
            mCyclicScreenShotTask = null;
        }

        // Recover interaction.
        mRootView.enableInteraction();
    }

    private class CyclicScreenShotTask implements Runnable {
        // Log tag.
        private final String TAG = CyclicScreenShotTask.class.getSimpleName();

        // Capture count.
        private int mCaptureCount = 0;

        // Callback.
        private final UserWebView.OnScreenShotDoneCallback mCallback;

        /**
         * CONSTRUCTOR.
         *
         * @param callback
         */
        public CyclicScreenShotTask(UserWebView.OnScreenShotDoneCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "run()");

            if (mRootView != null) {
                // Capture and reload.
                mRootView.requestCapture(mCallback);

                // Count up.
                ++mCaptureCount;

                if ((WEBVIEW_TIMEOUT_SEC / CYCLIC_SCREEN_SHOT_INTERVAL_SEC) <= mCaptureCount) {
                    // Capture count is maximum. Stop it.
                    stopCyclicScreenShotTask();

                    // Reset setting.
                    SharedPreferences.Editor editor
                            = UserApplication.getGlobalSharedPreferences().edit();
                    editor.putBoolean(Constants.SP_KEY_CYCLE_RECORD_ENABLED, false);
                    editor.apply();
                    editor.commit();

                } else {
                    // Go to next capture.
                    mUiWorker.postDelayed(this, CYCLIC_SCREEN_SHOT_INTERVAL_SEC * 1000);
                }
            }
        }
    }



}
