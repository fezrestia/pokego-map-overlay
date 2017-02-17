package com.fezrestia.android.pokegomapoverlay;

import android.view.Gravity;

public class Constants {
    // Intent constants.
    public static final String INTENT_ACTION_START_PREFERENCE_ACTIVITY
            = "com.fezrestia.android.pokegomapoverlay.intent.ACTION_START_PREFERENCE";

    /** SP Key, overlay en/disable. */
    public static final String SP_KEY_OVERLAY_VIEW_ENABLED = "is_overlay_view_enabled";
    /** SP Key, base load URL. */
    public static final String SP_KEY_BASE_LOAD_URL = "base-load-url";
    /** SP Key, webview scale ratio. */
    public static final String SP_KEY_WEBVIEW_SCALE_RATIO = "webview_scale_ratio";

    /** Default load URL. */
    public static final String DEFAULT_LOAD_URL = "https://www.ingress.com/intel";
}

