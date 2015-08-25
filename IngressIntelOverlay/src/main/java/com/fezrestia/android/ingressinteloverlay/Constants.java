package com.fezrestia.android.ingressinteloverlay;

import android.view.Gravity;

public class Constants {
    // Intent constants.
    public static final String INTENT_ACTION_START_PREFERENCE_ACTIVITY
            = "com.fezrestia.android.ingressinteloverlay.intent.ACTION_START_PREFERENCE";

    /** SP Key, overlay en/disable. */
    public static final String SP_KEY_OVERLAY_VIEW_ENABLED = "is_overlay_view_enabled";
    /** SP Key, record en/disable. */
    public static final String SP_KEY_CYCLE_RECORD_ENABLED = "is_cycle_record_enabled";
    /** SP Key, loading state detection en/disable. */
    public static final String SP_KEY_ALWAYS_RELOAD_ENABLED = "is_always_reload_enabled";
    /** SP Key, reload interval min. */
    public static final String SP_KEY_CAPTURE_INTERVAL_MIN = "capture_interval_min";
    /** SP Key, base load URL. */
    public static final String SP_KEY_BASE_LOAD_URL = "base-load-url";

    /** Font file name. */
    public static final String FONT_FILENAME_CODA = "Coda/Coda-Regular.ttf";

    /** Capture interval default. */
    public static final int DEFAULT_CAPTURE_INTERVAL_MIN = 1;

    /** Default load URL. */
    public static final String DEFAULT_LOAD_URL = "https://www.ingress.com/intel";
}
