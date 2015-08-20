package com.fezrestia.android.localcheckpointscheduler;

import android.view.Gravity;

public class Constants {
    // Intent constants.
    public static final String INTENT_ACTION_OVERLAY_VIEW_TRIGGER
            = "com.fezrestia.android.localcheckpointscheduler.intent.ACTION_OVERLAY_VIEW_TRIGGER";

    /** SP Key, overlay en/disable. */
    public static final String SP_KEY_OVERLAY_VIEW_ENABLED = "is_overlay_view_enabled";
    /** SP Key, record en/disable. */
    public static final String SP_KEY_CYCLE_RECORD_ENABLED = "is_cycle_record_enabled";
    /** SP Key, loading state detection en/disable. */
    public static final String SP_KEY_ALWAYS_RELOAD_ENABLED = "is_always_reload_enabled";
    /** SP Key, reload interval min. */
    public static final String SP_KEY_CAPTURE_INTERVAL_MIN = "capture_interval_min";

    /** Font file name. */
    public static final String FONT_FILENAME_CODA = "Coda/Coda-Regular.ttf";

    /** Capture interval default. */
    public static final int DEFAULT_CAPTURE_INTERVAL_MIN = 1;



}
