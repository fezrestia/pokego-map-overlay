package com.fezrestia.android.localcheckpointscheduler.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.fezrestia.android.localcheckpointscheduler.Constants;
import com.fezrestia.android.localcheckpointscheduler.R;
import com.fezrestia.android.localcheckpointscheduler.UserApplication;
import com.fezrestia.android.localcheckpointscheduler.control.OverlayViewController;
import com.fezrestia.android.localcheckpointscheduler.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class OverlayRootView extends FrameLayout {
    // Log tag.
    private static final String TAG = OverlayRootView.class.getSimpleName();

    // UI thread worker.
    private Handler mUiWorker = new Handler();

    // Web.
    private FrameLayout mUserWebViewContainer = null;
    private UserWebView mUserWebView = null;

    // UI component.
    private ImageView mHideButton = null;
    private TextView mClockIndicator = null;

    // Time.
    private static final SimpleDateFormat TIME_INDICATOR_SDF
            = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    // Clock update cycle.
    private static final int CLOCK_INDICATOR_UPDATE_INTERVAL_MILLIS = 20000;

    // Display coordinates.
    private int mDisplayLongLineLength = 0;
    private int mDisplayShortLineLength = 0;

    // Overlay window orientation.
    private int mOrientation = Configuration.ORIENTATION_UNDEFINED;

    // Window.
    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mWindowLayoutParams = null;
    private static final int INTERACTIVE_WINDOW_FLAGS = 0 // Dummy
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                ;
    private static final int UNINTERACTIVE_WINDOW_FLAGS = 0 // Dummy
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                ;

    // UI scale.
    private static final float mUiScaleRate = 0.5f;

    // CONSTRUCTOR.
    public OverlayRootView(final Context context) {
        this(context, null);
        if (Log.IS_DEBUG) Log.logDebug(TAG, "CONSTRUCTOR");
        // NOP.
    }

    // CONSTRUCTOR.
    public OverlayRootView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        if (Log.IS_DEBUG) Log.logDebug(TAG, "CONSTRUCTOR");
        // NOP.
    }

    // CONSTRUCTOR.
    public OverlayRootView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (Log.IS_DEBUG) Log.logDebug(TAG, "CONSTRUCTOR");
        // NOP.
    }

    /**
     * Initialize all of configurations.
     */
    public void initialize() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "initialize() : E");

        // Cache instance references.
        initializeInstances();

        // Load setting.
        loadPreferences();

        // Window related.
        createWindowParameters();

        // Update UI.
        updateTotalUserInterface();

        if (Log.IS_DEBUG) Log.logDebug(TAG, "initialize() : X");
    }

    private void initializeInstances() {
        // Web view container.
        mUserWebViewContainer = (FrameLayout) findViewById(R.id.web_view_container);

        // Web view.
        mUserWebView = new UserWebView(getContext());
        mUserWebView.initialize(mUserWebViewContainer);

        ViewGroup.LayoutParams webViewParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mUserWebViewContainer.addView(mUserWebView, webViewParams);

        // Clock.
        mClockIndicator = new TextView(getContext());
        int textColor = Color.rgb(235, 194, 62);
        mClockIndicator.setShadowLayer(5.0f, 1.0f, 1.0f, textColor);
        mClockIndicator.setTextColor(textColor);
        mClockIndicator.setTextSize(18.0f);
        mClockIndicator.setSingleLine();
        mClockIndicator.setPadding(10, 0, 0, 0);
        mClockIndicator.setText(getTimeString());
        mClockIndicator.setTypeface(
                Typeface.createFromAsset(getContext().getAssets(), Constants.FONT_FILENAME_CODA));
        FrameLayout.LayoutParams timeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        timeParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        mUserWebViewContainer.addView(mClockIndicator, timeParams);
        // Start clock.
        mUiWorker.postDelayed(mUpdateClockIndicatorTask, CLOCK_INDICATOR_UPDATE_INTERVAL_MILLIS);

        // Hide button.
        mHideButton = new ImageView(getContext());
        mHideButton.setOnTouchListener(mHideButtonOnTouchListenerImpl);
        mHideButton.setBackgroundColor(Color.RED);
        FrameLayout.LayoutParams hideButtonParams = new FrameLayout.LayoutParams(
                150,
                150);
        hideButtonParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        hideButtonParams.rightMargin = 20;
        hideButtonParams.bottomMargin = 20;
        addView(mHideButton, hideButtonParams);


    }

    private void loadPreferences() {
        SharedPreferences sp = UserApplication.getGlobalSharedPreferences();



    }

    private void createWindowParameters() {
        mWindowManager = (WindowManager)
                getContext().getSystemService(Context.WINDOW_SERVICE);

        mWindowLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                INTERACTIVE_WINDOW_FLAGS,
                PixelFormat.TRANSLUCENT);
    }

    private void enableOverlayInteraction() {
        mWindowLayoutParams.flags = INTERACTIVE_WINDOW_FLAGS;
        mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    private void disableOverlayInteraction() {
        mWindowLayoutParams.flags = UNINTERACTIVE_WINDOW_FLAGS;
        mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    /**
     * Release all resources.
     */
    public void release() {
        mUiWorker.removeCallbacks(mUpdateClockIndicatorTask);
        mUiWorker.removeCallbacks(mShowViewTask);
        mUiWorker.removeCallbacks(mHideViewTask);

        if (mUserWebView != null) {
            mUserWebView.release();
            mUserWebView = null;
        }
        mUserWebViewContainer = null;
        if (mHideButton != null) {
            mHideButton.setOnTouchListener(null);
            mHideButton = null;
        }

        setOnTouchListener(null);

        mWindowManager = null;
        mWindowLayoutParams = null;
    }

    /**
     * Add this view to WindowManager layer.
     */
    public void addToOverlayWindow() {
        // Window parameters.
        updateWindowParams();

        // Add to WindowManager.
        WindowManager winMng = (WindowManager)
                getContext().getSystemService(Context.WINDOW_SERVICE);
        winMng.addView(this, mWindowLayoutParams);
    }

    private void updateWindowParams() {
        mWindowLayoutParams.width = mDisplayShortLineLength;
        mWindowLayoutParams.height = mDisplayShortLineLength;
        mWindowLayoutParams.gravity = Gravity.CENTER;
        mWindowLayoutParams.x = 0;
        mWindowLayoutParams.y = 0;

        if (Log.IS_DEBUG) Log.logDebug(TAG,
                "updateWindowParams() : WinSizeWxH="
                + mWindowLayoutParams.width + "x" + mWindowLayoutParams.height);

        if (isAttachedToWindow()) {
            mWindowManager.updateViewLayout(this, mWindowLayoutParams);
        }
    }

    private void updateLayoutParams() {
        mUserWebView.setScaleX(mUiScaleRate);
        mUserWebView.setScaleY(mUiScaleRate);
        FrameLayout.LayoutParams webViewLayoutParams = (FrameLayout.LayoutParams)
                mUserWebView.getLayoutParams();
        webViewLayoutParams.width = (int) (mDisplayShortLineLength / mUiScaleRate);
        webViewLayoutParams.height = (int) (mDisplayShortLineLength / mUiScaleRate);
        webViewLayoutParams.gravity = Gravity.CENTER;
        mUserWebView.setLayoutParams(webViewLayoutParams);
    }

    /**
     * Remove this view from WindowManager layer.
     */
    public void removeFromOverlayWindow() {
        // Remove from to WindowManager.
        WindowManager winMng = (WindowManager)
                getContext().getSystemService(Context.WINDOW_SERVICE);
        winMng.removeView(this);
    }

    private void updateTotalUserInterface() {
        // Screen configuration.
        calculateScreenConfiguration();
        // Window layout.
        updateWindowParams();
        // UI layout.
        updateLayoutParams();
    }

    private void calculateScreenConfiguration() {
        // Get display size.
        Display display = mWindowManager.getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        final int width = screenSize.x;
        final int height = screenSize.y;
        mDisplayLongLineLength = Math.max(width, height);
        mDisplayShortLineLength = Math.min(width, height);

        if (Log.IS_DEBUG) Log.logDebug(TAG,
                "calculateScreenConfiguration() : ScreenWxH=" + width + "x" + height);

        // Get display orientation.
        if (height < width) {
            mOrientation = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            mOrientation = Configuration.ORIENTATION_PORTRAIT;
        }
    }



    /**
     * Request to show overlay.
     */
    public void requestShowOverlay() {
        mUiWorker.removeCallbacks(mShowViewTask);
        mUiWorker.removeCallbacks(mHideViewTask);
        mShowViewTask.reset();
        mUiWorker.post(mShowViewTask);

        enableOverlayInteraction();
    }

    /**
     * Request to hide overlay.
     */
    public void requestHideOverlay() {
        mUiWorker.removeCallbacks(mShowViewTask);
        mUiWorker.removeCallbacks(mHideViewTask);
        mHideViewTask.reset();
        mUiWorker.post(mHideViewTask);

        disableOverlayInteraction();
    }

    private abstract class ViewVisibilityControlTask implements Runnable {
        // Log tag.
        private final String TAG = ViewVisibilityControlTask.class.getSimpleName();

        // Alpha def.
        private final float SHOWN_ALPHA = 1.0f;
        private final float HIDDEN_ALPHA = 0.0f;

        // Actual view finder alpha.
        private float mActualAlpha = SHOWN_ALPHA;

        // Alpha stride.
        private static final float ALPHA_DELTA = 0.05f;
        // Alpha animation interval.
        private static final int FADE_INTERVAL_MILLIS = 16;

        abstract protected boolean isFadeIn();

        /**
         * Reset state. Call this before post.
         */
        public void reset() {
            mActualAlpha = getAlpha();
        }

        /**
         * Control immediately.
         */
        public void directControl() {
            if (isFadeIn()) {
                setAlpha(SHOWN_ALPHA);
            } else {
                setAlpha(HIDDEN_ALPHA);
            }
        }

        @Override
        public void run() {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "run()");

            boolean isNextTaskRequired = true;

            if (isFadeIn()) {
                mActualAlpha += ALPHA_DELTA;
                if (SHOWN_ALPHA < mActualAlpha) {
                    mActualAlpha = SHOWN_ALPHA;
                    isNextTaskRequired = false;
                }
            } else {
                mActualAlpha -= ALPHA_DELTA;
                if (mActualAlpha < HIDDEN_ALPHA) {
                    mActualAlpha = HIDDEN_ALPHA;
                    isNextTaskRequired = false;
                }
            }

            setAlpha(mActualAlpha);

            if (isNextTaskRequired) {
                mUiWorker.postDelayed(this, FADE_INTERVAL_MILLIS);
            }
        }
    }

    private final ShowViewTask mShowViewTask = new ShowViewTask();
    private class ShowViewTask extends ViewVisibilityControlTask {
        @Override
        protected boolean isFadeIn() {
            return true;
        }
    }

    private final HideViewTask mHideViewTask = new HideViewTask();
    private class HideViewTask extends ViewVisibilityControlTask {
        @Override
        protected boolean isFadeIn() {
            return false;
        }
    }



    private final HideButtonOnTouchListenerImpl mHideButtonOnTouchListenerImpl
            = new HideButtonOnTouchListenerImpl();
    private class HideButtonOnTouchListenerImpl implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    // fall-through.
                case MotionEvent.ACTION_CANCEL:
                    OverlayViewController.getInstance().pause();
                    break;

                default:
                    // NOP;
                    break;
            }

            return true;
        }
    }



    /**
     * Request reload.
     */
    public void requestReload() {
        if (mUserWebView != null) {
            mUserWebView.reload();
        }
    }

    /**
     * Request screen shot.
     *
     * @param callback
     */
    public void requestCapture(UserWebView.OnScreenShotDoneCallback callback) {
        if (mUserWebView != null) {
            mUserWebView.requestScreenShot(callback);
        }
    }

    /**
     * Request screen shot, after then reload automatically.
     *
     * @param callback
     */
    public void requestCaptureAndReload(UserWebView.OnScreenShotDoneCallback callback) {
        if (mUserWebView != null) {
            mUserWebView.requestScreenShotAndReload(callback);
        }
    }

    /**
     * Enable interaction with this view.
     */
    public void enableInteraction() {
        if (mUserWebView != null) {
            mUserWebView.enableInteraction();
        }
    }

    /**
     * Disable interaction with this view.
     */
    public void disableInteraction() {
        if (mUserWebView != null) {
            mUserWebView.disableInteraction();
        }
    }



    private String getTimeString() {
        Calendar calendar = Calendar.getInstance();
        return TIME_INDICATOR_SDF.format(calendar.getTime());
    }

    private final UpdateClockIndicatorTask mUpdateClockIndicatorTask
            = new UpdateClockIndicatorTask();
    private class UpdateClockIndicatorTask implements Runnable {
        @Override
        public void run() {
            if (mClockIndicator != null) {
                // Update time.
                mClockIndicator.setText(getTimeString());

                // Next.
                mUiWorker.postDelayed(this, CLOCK_INDICATOR_UPDATE_INTERVAL_MILLIS);
            }
        }
    }



    @Override
    public void onFinishInflate() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onFinishInflate()");
        super.onFinishInflate();
        // NOP.
    }

    @Override
    public void onAttachedToWindow() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onAttachedToWindow()");
        super.onAttachedToWindow();
        // NOP.
    }

    @Override
    public void onDetachedFromWindow() {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onDetachedFromWindow()");
        super.onDetachedFromWindow();
        // NOP.
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Log.IS_DEBUG) Log.logDebug(TAG,
                "onConfigurationChanged() : [Config=" + newConfig.toString());
        super.onConfigurationChanged(newConfig);

        // Update UI.
        updateTotalUserInterface();
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        if (Log.IS_DEBUG) Log.logDebug(TAG,
//                "onLayout() : [Changed=" + changed + "] [Rect="
//                 + left + ", " + top + ", " + right + ", " + bottom + "]");
        super.onLayout(changed, left, top, right, bottom);
        // NOP.
    }

    @Override
    public void onSizeChanged(int curW, int curH, int nxtW, int nxtH) {
//        if (Log.IS_DEBUG) Log.logDebug(TAG,
//                "onSizeChanged() : [CUR=" + curW + "x" + curH + "] [NXT=" +  nxtW + "x" + nxtH + "]");
        super.onSizeChanged(curW, curH, nxtW, nxtH);
        // NOP.
    }



}
