package com.fezrestia.android.localcheckpointscheduler.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fezrestia.android.localcheckpointscheduler.Constants;
import com.fezrestia.android.localcheckpointscheduler.control.OverlayViewController;
import com.fezrestia.android.localcheckpointscheduler.util.Log;

public class OverlayViewTriggerReceiver extends BroadcastReceiver {
    // Log tag.
    private static final String TAG = OverlayViewTriggerReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onReceive()");

        String action = intent.getAction();
        if (Log.IS_DEBUG) Log.logDebug(TAG, "ACTION = " + action);

        if (action == null) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "ACTION is NULL.");
            // NOP.
        } else if (Constants.INTENT_ACTION_OVERLAY_VIEW_TRIGGER.equals(action)) {
            // Resume overlay.
            OverlayViewController.getInstance().resume();
        } else {
            // Unexpected Action.
            throw new IllegalArgumentException("Unexpected Action : " + action);
        }
    }
}
