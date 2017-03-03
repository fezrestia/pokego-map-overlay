package com.fezrestia.android.pokegomapoverlay.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.fezrestia.android.pokegomapoverlay.Constants;
import com.fezrestia.android.pokegomapoverlay.R;
import com.fezrestia.android.pokegomapoverlay.UserApplication;
import com.fezrestia.android.util.FrameSize;
import com.fezrestia.android.util.Log;

public class OverlayRootView extends FrameLayout {
    // Log tag.
    private static final String TAG = "OverlayRootView";

    // UI thread worker.
    private Handler mUiWorker = UserApplication.getUiThreadHandler();

    // Web.
    private FrameLayout mUserWebViewContainer = null;
    private UserWebView mUserWebView = null;

    // HUD.
    private FrameLayout mHudViewContainer = null;
    private ImageView mEdgeFrame = null;

    // UI interaction.
    private FrameLayout mInteractionViewContainer = null;

    // Grip.
    private View mSliderGrip = null;

    // Screen coordinates.
    private FrameSize mScreenSize = null;

    // Overlay window orientation.
    private int mOrientation = Configuration.ORIENTATION_UNDEFINED;

    // Window.
    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mWindowLayoutParams = null;
    @SuppressWarnings("PointlessBitwiseExpression")
    private static final int INTERACTIVE_WINDOW_FLAGS = 0 // Dummy
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                ;
    @SuppressWarnings("PointlessBitwiseExpression")
    private static final int NOT_INTERACTIVE_WINDOW_FLAGS = 0 // Dummy
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                ;

    // UI scale.
    private float mUiScaleRate = 1.0f; // Default.

    // Grip width.
    private static final int SLIDER_GRIP_WIDTH_PIX = 1080 - 960;

    // Window position.
    private int mWinOpenPosX = 0;
    private int mWinClosePosX = 0;

    // Hidden window position constants.
    private static final int WINDOW_HIDDEN_POS_X = -5000;

    // Window position correction animation.
    private WindowPositionCorrectionTask mWindowPositionCorrectionTask = null;

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

    @SuppressLint("RtlHardcoded")
    private void initializeInstances() {
        // Web view container.
        mUserWebViewContainer = (FrameLayout) findViewById(R.id.web_view_container);
        // HUD view container.
        mHudViewContainer = (FrameLayout) findViewById(R.id.hud_view_container);
        // ITX view container.
        mInteractionViewContainer = (FrameLayout) findViewById(R.id.interaction_view_container);

        // Web view.
        mUserWebView = new UserWebView(getContext());
        mUserWebView.initialize();
        // Load URL.
        String url = UserApplication.getGlobalSharedPreferences()
                .getString(Constants.SP_KEY_BASE_LOAD_URL, Constants.DEFAULT_LOAD_URL);
        if (url.isEmpty()) {
            url = Constants.DEFAULT_LOAD_URL;
        }
        mUserWebView.loadWebPase(url);

        ViewGroup.LayoutParams webViewParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mUserWebViewContainer.addView(mUserWebView, webViewParams);

        // Edge frame.
        mEdgeFrame = new ImageView(getContext());
        mEdgeFrame.setImageResource(R.drawable.overlay_edge_frame);
        mEdgeFrame.setScaleType(ImageView.ScaleType.FIT_XY);
        mHudViewContainer.addView(mEdgeFrame, webViewParams);

        // Slider grip.
        mSliderGrip = findViewById(R.id.slider_grip_container);
        mSliderGrip.setOnTouchListener(new SliderGripTouchEventHandler());
    }

    private void loadPreferences() {
        SharedPreferences sp = UserApplication.getGlobalSharedPreferences();

        // UI scale rate.
        String uiScaleValue = sp.getString(Constants.SP_KEY_WEBVIEW_SCALE_RATIO, null);
        if (uiScaleValue != null) {
            mUiScaleRate = Float.parseFloat(uiScaleValue);
        }
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
        mWindowLayoutParams.flags = NOT_INTERACTIVE_WINDOW_FLAGS;
        mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    /**
     * Release all resources.
     */
    public void release() {
        if (mSliderGrip != null) {
            mSliderGrip.setOnTouchListener(null);
            mSliderGrip = null;
        }
        if (mUserWebView != null) {
            mUserWebView.release();
            mUserWebView = null;
        }
        mEdgeFrame = null;
        mUserWebViewContainer = null;
        mHudViewContainer = null;

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
        mWindowLayoutParams.width = mScreenSize.getShortLineSize();
        mWindowLayoutParams.height = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;
        mWindowLayoutParams.gravity = Gravity.CENTER;

        // Window show/hide constants.
        mWinOpenPosX = 0;
        mWinClosePosX = -1 * (mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX);

        mWindowLayoutParams.x = mWinOpenPosX;
        mWindowLayoutParams.y = 0;

        if (Log.IS_DEBUG) Log.logDebug(TAG,
                "updateWindowParams() : WinSizeWxH="
                + mWindowLayoutParams.width + "x" + mWindowLayoutParams.height);

        if (isAttachedToWindow()) {
            mWindowManager.updateViewLayout(this, mWindowLayoutParams);
        }
    }

    private void updateLayoutParams() {
        updateUiWebViewContainerLayoutParams();
        updateHudViewContainerLayoutParams();
        updateInteractionViewContainerLayoutParams();
    }

    private void updateUiWebViewContainerLayoutParams() {
        // Container size.
        mUserWebViewContainer.getLayoutParams().width
                = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;
        mUserWebViewContainer.getLayoutParams().height
                = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;

        // Contents size.
        mUserWebView.setScaleX(mUiScaleRate);
        mUserWebView.setScaleY(mUiScaleRate);
        FrameLayout.LayoutParams webViewLayoutParams = (FrameLayout.LayoutParams)
                mUserWebView.getLayoutParams();
        webViewLayoutParams.width = (int)
                ((mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX) / mUiScaleRate);
        webViewLayoutParams.height = (int)
                ((mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX) / mUiScaleRate);
        webViewLayoutParams.gravity = Gravity.CENTER;
        mUserWebView.setLayoutParams(webViewLayoutParams);

        FrameLayout.LayoutParams edgeFrameLayoutParams = (FrameLayout.LayoutParams)
                mEdgeFrame.getLayoutParams();
        edgeFrameLayoutParams.width = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;
        edgeFrameLayoutParams.height = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;
        mEdgeFrame.setLayoutParams(edgeFrameLayoutParams);
    }

    private void updateHudViewContainerLayoutParams() {
        // Container size.
        mHudViewContainer.getLayoutParams().width
                = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;
        mHudViewContainer.getLayoutParams().height
                = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;
    }

    private void updateInteractionViewContainerLayoutParams() {
        // Container size.
        mInteractionViewContainer.getLayoutParams().width
                = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;
        mInteractionViewContainer.getLayoutParams().height
                = mScreenSize.getShortLineSize() - SLIDER_GRIP_WIDTH_PIX;
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

        // Ignore Landscape configuration.
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isAttachedToWindow()) {
                hide();
            }
        }
    }

    private void calculateScreenConfiguration() {
        // Get display size.
        Display display = mWindowManager.getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);

        mScreenSize = new FrameSize(screenSize.x, screenSize.y);

        if (Log.IS_DEBUG) Log.logDebug(TAG,
                "calculateScreenConfiguration() : " + mScreenSize.toString());

        // Get display orientation.
        if (mScreenSize.height < mScreenSize.width) {
            mOrientation = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            mOrientation = Configuration.ORIENTATION_PORTRAIT;
        }
    }

    /**
     * Overlay view is shown or not.
     *
     * @return Show/Hide flag.
     */
    public boolean isOverlayShown() {
        return mWindowLayoutParams.x != WINDOW_HIDDEN_POS_X;
    }

    /**
     * Show overlay view.
     *
     * @param isOpened Overlay view initial state.
     */
    public void show(boolean isOpened) {
        if (isOpened) {
            mWindowLayoutParams.x = mWinOpenPosX;
            enableOverlayInteraction();
        } else {
            mWindowLayoutParams.x = mWinClosePosX;
            disableOverlayInteraction();
        }
        mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    /**
     * Hide overlay view.
     */
    public void hide() {
        mWindowLayoutParams.x = WINDOW_HIDDEN_POS_X;
        disableOverlayInteraction();
        mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Log.IS_DEBUG) Log.logDebug(TAG,
                "onConfigurationChanged() : [Config=" + newConfig.toString());
        super.onConfigurationChanged(newConfig);

        // Update UI.
        updateTotalUserInterface();
    }

    private class SliderGripTouchEventHandler implements View.OnTouchListener {
        private int mOnDownWinPosX = 0;
        private int mOnDownBasePosX = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mOnDownBasePosX = (int) event.getRawX();
                    mOnDownWinPosX = mWindowLayoutParams.x;
                    break;

                case MotionEvent.ACTION_MOVE:
                    int diffX = ((int) event.getRawX()) - mOnDownBasePosX;
                    int nextWinPosX = mOnDownWinPosX + diffX;

                    if (isAttachedToWindow()) {
                        // Check limit.
                        if (nextWinPosX < mWinClosePosX) {
                            nextWinPosX = mWinClosePosX;
                        }
                        if (mWinOpenPosX < nextWinPosX) {
                            nextWinPosX = mWinOpenPosX;
                        }

                        // Update.
                        mWindowLayoutParams.x = nextWinPosX;
                        mWindowManager.updateViewLayout(OverlayRootView.this, mWindowLayoutParams);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    // fall-through.
                case MotionEvent.ACTION_CANCEL:
                    // Reset.
                    mOnDownBasePosX = 0;
                    mOnDownWinPosX = 0;

                    // Check.
                    if (mWindowPositionCorrectionTask != null) {
                        mUiWorker.removeCallbacks(mWindowPositionCorrectionTask);
                    }

                    // Fixed position.
                    Point targetPoint;
                    if (mScreenSize.getShortLineSize() / 2 < event.getRawX()) {
                        // To be opened.
                        targetPoint = new Point(mWinOpenPosX, mWindowLayoutParams.y);

                        enableOverlayInteraction();
                    } else {
                        // To be closed.
                        targetPoint = new Point(mWinClosePosX, mWindowLayoutParams.y);

                        disableOverlayInteraction();
                    }

                    // Start fix.
                    mWindowPositionCorrectionTask = new WindowPositionCorrectionTask(
                            OverlayRootView.this,
                            targetPoint,
                            mWindowManager,
                            mWindowLayoutParams,
                            mUiWorker);
                    mUiWorker.post(mWindowPositionCorrectionTask);
                    break;

                default:
                    // NOP. Unexpected.
                    break;
            }

            return true;
        }
    }

    private static class WindowPositionCorrectionTask implements Runnable {
        // Environment.
        private final WindowManager mWinMng;
        private final WindowManager.LayoutParams mWinParams;
        private final Handler mHandler;

        // Target.
        private final View mTargetView;
        private final Point mTargetWindowPosit;

        // Proportional gain.
        private static final float P_GAIN = 0.2f;

        // Animation refresh interval.
        private static final int WINDOW_ANIMATION_INTERVAL_MILLIS = 16;

        // Last delta.
        private int mLastDeltaX = 0;
        private int mLastDeltaY = 0;

        /**
         * CONSTRUCTOR.
         *
         * @param targetView Control target view.
         * @param targetPos Target position.
         * @param winMng WindowManager instance.
         * @param winParams Window layout params.
         * @param handler UI thread handler.
         */
        WindowPositionCorrectionTask(
                View targetView,
                Point targetPos,
                WindowManager winMng,
                WindowManager.LayoutParams winParams,
                Handler handler) {
            mTargetView = targetView;
            mTargetWindowPosit = targetPos;
            mWinMng = winMng;
            mWinParams = winParams;
            mHandler = handler;
        }

        @Override
        public void run() {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "WindowPositionCorrectionTask.run() : E");

            final int dX = mTargetWindowPosit.x - mWinParams.x;
            final int dY = mTargetWindowPosit.y - mWinParams.y;

            // Update layout.
            mWinParams.x += (int) (dX * P_GAIN);
            mWinParams.y += (int) (dY * P_GAIN);

            if (mTargetView.isAttachedToWindow()) {
                mWinMng.updateViewLayout(
                        mTargetView,
                        mWinParams);
            } else {
                // Already detached from window.
                if (Log.IS_DEBUG) Log.logDebug(TAG, "Already detached from window.");
                return;
            }

            // Check next.
            if (mLastDeltaX == dX && mLastDeltaY == dY) {
                // Correction is already convergent.
                if (Log.IS_DEBUG) Log.logDebug(TAG, "Already position fixed.");

                // Fix position.
                mWinParams.x = mTargetWindowPosit.x;
                mWinParams.y = mTargetWindowPosit.y;

                mWinMng.updateViewLayout(
                        mTargetView,
                        mWinParams);
                return;
            }
            mLastDeltaX = dX;
            mLastDeltaY = dY;

            // Next.
            mHandler.postDelayed(this, WINDOW_ANIMATION_INTERVAL_MILLIS);

            if (Log.IS_DEBUG) Log.logDebug(TAG, "WindowPositionCorrectionTask.run() : X");
        }
    }

    private BackKeyLongPressDetectionTask mBackKeyLongPressDetectionTask = null;
    private static final int LONG_PRESS_DETECTION_MILLIS = 1000;

    private class BackKeyLongPressDetectionTask implements Runnable {
        @Override
        public void run() {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "KEYCODE_BACK : DOWN LONG PRESS");

            if (mUserWebView != null) {
                if (Log.IS_DEBUG) Log.logDebug(TAG, "Do reload.");
                mUserWebView.reload();

                // Release reference.
                mBackKeyLongPressDetectionTask = null;
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        if (Log.IS_DEBUG) Log.logDebug(TAG, "KEYCODE_BACK : DOWN");

                        // Long press detector.
                        if (mBackKeyLongPressDetectionTask == null) {
                            mBackKeyLongPressDetectionTask = new BackKeyLongPressDetectionTask();
                            getHandler().postDelayed(
                                    mBackKeyLongPressDetectionTask,
                                    LONG_PRESS_DETECTION_MILLIS);
                        }

                        break;

                    case KeyEvent.ACTION_UP:
                        if (Log.IS_DEBUG) Log.logDebug(TAG, "KEYCODE_BACK : UP");

                        if (mBackKeyLongPressDetectionTask != null) {
                            // Not long-pressed.
                            if (Log.IS_DEBUG) Log.logDebug(TAG, "KEYCODE_BACK : Not Long Pressed");

                            // Cancel long-press detector.
                            if (mBackKeyLongPressDetectionTask != null) {
                                getHandler().removeCallbacks(mBackKeyLongPressDetectionTask);
                                mBackKeyLongPressDetectionTask = null;
                            }

                            // Go back on WebView.
                            if (mUserWebView.canGoBack()) {
                                mUserWebView.goBack();
                            }
                        } else {
                            // Long-press detection task is triggered.
                            if (Log.IS_DEBUG) Log.logDebug(TAG, "KEYCODE_BACK : Long Pressed");

                            // NOP.
                        }

                        break;

                    default:
                        // NOP.
                        break;
                }

            default:
                // NOP.
                break;
        }

        return true;
    }
}
