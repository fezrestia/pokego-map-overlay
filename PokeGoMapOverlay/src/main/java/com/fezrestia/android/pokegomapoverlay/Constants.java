package com.fezrestia.android.pokegomapoverlay;

public class Constants {
    // Intent action for start preferences activity.
    public static final String INTENT_ACTION_START_PREFERENCE_ACTIVITY
            = "com.fezrestia.android.pokegomapoverlay.intent.ACTION_START_PREFERENCE";

    // Intent action for overlay visibility switch.
    public static final String INTENT_ACTION_TOGGLE_OVERLAY_VISIBILITY
            = "com.fezrestia.android.pokegomapoverlay.intent.ACTION_TOGGLE_OVERLAY_VISIBILITY";

    /** SP Key, overlay en/disable. */
    public static final String SP_KEY_OVERLAY_VIEW_ENABLED = "is-overlay-view-enabled";
    /** SP Key, base load URL. */
    public static final String SP_KEY_BASE_LOAD_URL = "base-load-url";
    /** SP Key, webview scale ratio. */
    public static final String SP_KEY_WEBVIEW_SCALE_RATIO = "webview-scale-ratio";

    /** Default load URL. */
    public static final String DEFAULT_LOAD_URL = "https://pmap.kuku.lu";
}

