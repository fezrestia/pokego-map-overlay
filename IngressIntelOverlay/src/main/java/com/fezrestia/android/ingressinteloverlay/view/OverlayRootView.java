package com.fezrestia.android.ingressinteloverlay.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

import com.fezrestia.android.ingressinteloverlay.Constants;
import com.fezrestia.android.ingressinteloverlay.R;
import com.fezrestia.android.ingressinteloverlay.UserApplication;
import com.fezrestia.android.ingressinteloverlay.control.OverlayViewController;
import com.fezrestia.android.ingressinteloverlay.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverlayRootView extends FrameLayout {
    // Log tag.
    private static final String TAG = OverlayRootView.class.getSimpleName();

    // UI thread worker.
    private Handler mUiWorker = new Handler();

    // Web.
    private FrameLayout mUserWebViewContainer = null;
    private UserWebView mUserWebView = null;

    // HUD.
    private FrameLayout mHudViewContainer = null;
    private ImageView mEdgeFrame = null;
    private TextView mClockIndicator = null;

    // UI interaction.
    private FrameLayout mInteractionViewContainer = null;
//    private ImageView mCloseButton = null;
    private ImageView mCaptureButton = null;
    private ImageView mReloadButton = null;

    // Grip.
    private View mSliderGrip = null;

    // Time.
    private static final SimpleDateFormat TIME_INDICATOR_SDF
            = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    // Clock update cycle.
    private static final int CLOCK_INDICATOR_UPDATE_INTERVAL_MILLIS = 10000;

    // Display coordinates.
    private int mDisplayLongLineLength = 0;
    private int mDisplayShortLineLength = 0;

    // Overlay window orientation.
    private int mOrientation = Configuration.ORIENTATION_UNDEFINED;

    // Window.
    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mWindowLayoutParams = null;
    private static final int INTERACTIVE_WINDOW_FLAGS = 0 // Dummy
                // NOTICE:
                //   If hardware acceleration is enabled, portal indicator will not be rendered
                //   in drawing cache. (on UI, portal indicator is shown normally)
                //   This is IntelMap or WebView specification maybe,
                //   so, disable hardware acceleration.
//                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                ;
    private static final int UNINTERACTIVE_WINDOW_FLAGS = 0 // Dummy
//                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                ;

    // UI scale.
    private static final float mUiScaleRate = 0.6f;

    // Screen shot generator.
    private ScreenShotGenerator mScreenShotGenerator = null;

    // Capture delay. e.g. waiting for java script execution done.
    private static final int CAPTURE_DELAY_MILLIS = 5000;

    // Grip width.
    private static final int SLIDER_GRIP_WIDTH_PIX = 1080 - 960;

    // Window position constants.
    private int mWinOpenPosX = 0;
    private int mWinClosePosX = 0;

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
     *
     * @param isLoadingDetectionEnabled
     */
    public void initialize(boolean isLoadingDetectionEnabled) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "initialize() : E");

        // Cache instance references.
        initializeInstances(isLoadingDetectionEnabled);

        // Load setting.
        loadPreferences();

        // Window related.
        createWindowParameters();

        // Update UI.
        updateTotalUserInterface();

        if (Log.IS_DEBUG) Log.logDebug(TAG, "initialize() : X");
    }

    private void initializeInstances(boolean isLoadingDetectionEnabled) {
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
        mHudViewContainer.addView(mClockIndicator, timeParams);
        // Start clock.
        mUiWorker.postDelayed(mUpdateClockIndicatorTask, CLOCK_INDICATOR_UPDATE_INTERVAL_MILLIS);

        // Hide button.
//        mCloseButton = new ImageView(getContext());
//        mCloseButton.setOnTouchListener(mHideButtonOnTouchListenerImpl);
//        mCloseButton.setImageResource(R.drawable.close_button);
//        FrameLayout.LayoutParams hideButtonParams = new FrameLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT);
//        hideButtonParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
//        hideButtonParams.rightMargin = 20;
//        hideButtonParams.bottomMargin = 20;
//        mInteractionViewContainer.addView(mCloseButton, hideButtonParams);

        // Capture button.
        mCaptureButton = new ImageView(getContext());
        mCaptureButton.setOnTouchListener(mCaptureButtonOnTouchListenerImpl);
        mCaptureButton.setImageResource(R.drawable.capture_button);
        FrameLayout.LayoutParams captureButtonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        captureButtonParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        captureButtonParams.rightMargin = 20;
        captureButtonParams.bottomMargin = 20;
        mInteractionViewContainer.addView(mCaptureButton, captureButtonParams);

        // Reload button.
        mReloadButton = new ImageView(getContext());
        mReloadButton.setOnTouchListener(mReloadButtonOnTouchListenerImpl);
        mReloadButton.setImageResource(R.drawable.reload_button);
        FrameLayout.LayoutParams reloadButtonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        reloadButtonParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        reloadButtonParams.leftMargin = 20;
        reloadButtonParams.bottomMargin = 20;
        mInteractionViewContainer.addView(mReloadButton, reloadButtonParams);

        // Screen shot generator.
        mScreenShotGenerator = new ScreenShotGenerator(
                mUserWebViewContainer,
                mHudViewContainer,
                isLoadingDetectionEnabled);
        mUserWebView.setLoadingStateCallback(mScreenShotGenerator);

        // Slider grip.
        mSliderGrip = findViewById(R.id.slider_grip_container);
        mSliderGrip.setOnTouchListener(new SliderGripTouchEventHandler());
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

        if (mSliderGrip != null) {
            mSliderGrip.setOnTouchListener(null);
            mSliderGrip = null;
        }

        if (mUserWebView != null) {
            mUserWebView.setLoadingStateCallback(null);
            mUserWebView.release();
            mUserWebView = null;
        }
        mEdgeFrame = null;
        mUserWebViewContainer = null;
        mHudViewContainer = null;
//        if (mCloseButton != null) {
//            mCloseButton.setOnTouchListener(null);
//            mCloseButton = null;
//        }
        if (mCaptureButton != null) {
            mCaptureButton.setOnTouchListener(null);
            mCaptureButton = null;
        }
        if (mReloadButton != null) {
            mReloadButton.setOnTouchListener(null);
            mReloadButton = null;
        }

        if (mScreenShotGenerator != null) {
            mScreenShotGenerator.release();
            mScreenShotGenerator = null;
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
        mWindowLayoutParams.height = mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX;
        mWindowLayoutParams.gravity = Gravity.CENTER;

        // Window show/hide constants.
        mWinOpenPosX = 0;
        mWinClosePosX = -1 * (mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX);

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
                = mDisplayShortLineLength  - SLIDER_GRIP_WIDTH_PIX;
        mUserWebViewContainer.getLayoutParams().height
                = mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX;

        // Contents size.
        mUserWebView.setScaleX(mUiScaleRate);
        mUserWebView.setScaleY(mUiScaleRate);
        FrameLayout.LayoutParams webViewLayoutParams = (FrameLayout.LayoutParams)
                mUserWebView.getLayoutParams();
        webViewLayoutParams.width = (int)
                ((mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX) / mUiScaleRate);
        webViewLayoutParams.height = (int)
                ((mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX) / mUiScaleRate);
        webViewLayoutParams.gravity = Gravity.CENTER;
        mUserWebView.setLayoutParams(webViewLayoutParams);

        FrameLayout.LayoutParams edgeFrameLayoutParams = (FrameLayout.LayoutParams)
                mEdgeFrame.getLayoutParams();
        edgeFrameLayoutParams.width = mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX;
        edgeFrameLayoutParams.height = mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX;
        mEdgeFrame.setLayoutParams(edgeFrameLayoutParams);
    }

    private void updateHudViewContainerLayoutParams() {
        // Container size.
        mHudViewContainer.getLayoutParams().width
                = mDisplayShortLineLength  - SLIDER_GRIP_WIDTH_PIX;
        mHudViewContainer.getLayoutParams().height
                = mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX;
    }

    private void updateInteractionViewContainerLayoutParams() {
        // Container size.
        mInteractionViewContainer.getLayoutParams().width
                = mDisplayShortLineLength  - SLIDER_GRIP_WIDTH_PIX;
        mInteractionViewContainer.getLayoutParams().height
                = mDisplayShortLineLength - SLIDER_GRIP_WIDTH_PIX;
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
            disableOverlayInteraction();

            if (isAttachedToWindow()) {
                mWindowLayoutParams.x = -5000;
                mWindowManager.updateViewLayout(this, mWindowLayoutParams);
            }
        }
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



//    private final HideButtonOnTouchListenerImpl mHideButtonOnTouchListenerImpl
//            = new HideButtonOnTouchListenerImpl();
//    private class HideButtonOnTouchListenerImpl implements OnTouchListener {
//        @Override
//        public boolean onTouch(View v, MotionEvent event) {
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_UP:
//                    // fall-through.
//                case MotionEvent.ACTION_CANCEL:
//                    OverlayViewController.getInstance().pause();
//                    break;
//
//                default:
//                    // NOP;
//                    break;
//            }
//
//            return true;
//        }
//    }

    private final CaptureButtonOnTouchListenerImpl mCaptureButtonOnTouchListenerImpl
            = new CaptureButtonOnTouchListenerImpl();
    private class CaptureButtonOnTouchListenerImpl implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    OverlayViewController.getInstance().captureScreenShot();
                    break;

                default:
                    // NOP;
                    break;
            }

            return true;
        }
    }

    private final ReloadButtonOnTouchListenerImpl mReloadButtonOnTouchListenerImpl
            = new ReloadButtonOnTouchListenerImpl();
    private class ReloadButtonOnTouchListenerImpl implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_UP:
                    mUserWebView.stopLoading();
                    mUserWebView.reload();
                    break;

                default:
                    // NOP.
                    break;
            }

            return true;
        }
    }

    /**
     * Request screen shot.
     *
     * @param isReloadRequired
     * @param callback
     */
    public void requestCapture(boolean isReloadRequired, OnScreenShotDoneCallback callback) {
        disableInteraction();

        Runnable task = new RequestStartScreenShotTask(isReloadRequired, callback);
        mUiWorker.postDelayed(task, CAPTURE_DELAY_MILLIS);
    }

    private class RequestStartScreenShotTask implements Runnable {
        // Reload is required.
        private final boolean mIsReloadRequired;
        // Callback.
        private final OnScreenShotDoneCallback mCallback;

        /**
         * CONSTRUCTOR.
         *
         * @param isReloadRequired
         * @param callback
         */
        RequestStartScreenShotTask(boolean isReloadRequired, OnScreenShotDoneCallback callback) {
            mIsReloadRequired = isReloadRequired;
            mCallback = callback;
        }

        @Override
        public void run() {
            if (mScreenShotGenerator != null) {
                mScreenShotGenerator.request(mCallback);
            }

            if (mIsReloadRequired && mUserWebView != null) {
                mUserWebView.reload();
            }
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

                // NOTICE:
                //   WebView is not rendered correctly after time indicator is updated
                //   like as SurfaceView. So, invalidate to refresh WebView area.
                mUserWebViewContainer.invalidate();

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



    /**
     * Screen shot done callback interface.
     */
    public interface OnScreenShotDoneCallback {
        /**
         * Screen shot is done.
         *
         * @param pngBuffer
         */
        void onScreenShotDone(byte[] pngBuffer);
    }

    private static class ScreenShotGenerator implements UserWebView.LoadingStateCallback {
        // Log tag.
        private final String TAG = ScreenShotGenerator.class.getSimpleName();

        // Back worker.
        private ExecutorService mBackWorker = null;

        // Web view container.
        private View mWebView = null;
        // Overlay HUD view.
        private View mHudView = null;

        // Web view layer screen shot.
        private Bitmap mWebLayerBmp = null;
        // Overlay HUD layer screen shot.
        private Bitmap mHudLayerBmp = null;

        // Client callback.
        private OnScreenShotDoneCallback mCallback = null;

        // Currently, web view is loading or not.
        private boolean mIsWebViewOnLoading = false;

        // Loading detection enabled or not.
        private boolean mIsLoadingDetectionEnabled = false;

        /**
         * CONSTRUCTOR.
         *
         * @param webLayerLayout
         * @param hudLayerLayout
         * @param isLoadingDetectionEnabled
         */
        ScreenShotGenerator(
                ViewGroup webLayerLayout,
                ViewGroup hudLayerLayout,
                boolean isLoadingDetectionEnabled) {
            mWebView = webLayerLayout;
            mHudView = hudLayerLayout;

            mIsLoadingDetectionEnabled = isLoadingDetectionEnabled;

            // Worker thread.
            mBackWorker = Executors.newSingleThreadExecutor();
        }

        /**
         * Release all references.
         */
        void release() {
            mWebView = null;
            mHudView = null;

            if (mWebLayerBmp != null) {
                if (!mWebLayerBmp.isRecycled()) {
                    mWebLayerBmp.recycle();
                }
                mWebLayerBmp = null;
            }
            if (mHudLayerBmp != null) {
                if (!mHudLayerBmp.isRecycled()) {
                    mHudLayerBmp.recycle();
                }
                mHudLayerBmp = null;
            }

            if (mBackWorker != null && !mBackWorker.isShutdown()) {
                mBackWorker.shutdown();
                mBackWorker = null;
            }
        }

        @Override
        public void onLoading(boolean isLoading) {
            mIsWebViewOnLoading = isLoading;
        }

        private boolean canCaptureScreenShot() {
            if (mIsLoadingDetectionEnabled) {
                // Loading state is checked.
                if (mIsWebViewOnLoading) {
                    return false;
                } else {
                    return true;
                }
            } else {
                // Loading state is ignored.
                return true;
            }
        }

        /**
         * Request to generate latest valid screen shot.
         *
         * @param callback
         * @return request is succeeded or not
         */
        boolean request(OnScreenShotDoneCallback callback) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "request() : E");

            boolean isSuccess = false;

            // Check.
            if (mCallback == null) {
                // Do request.

                mCallback = callback;

                // Web view screen shot.
                if (mWebLayerBmp == null || canCaptureScreenShot()) {
                    // Release old bitmap before capture.
                    if (mWebLayerBmp != null && !mWebLayerBmp.isRecycled()) {
                        mWebLayerBmp.recycle();
                    }

                    // Get screen shot.
                    mWebLayerBmp = getDrawingCache(mWebView);
                } else {
                    // 1st capture, or currently web view is loading.
                    if (Log.IS_DEBUG) Log.logDebug(TAG, "Web view is now on loading.");
                }

                // HUD view screen shot.
                // Release old bitmap before capture.
                if (mHudLayerBmp != null && !mHudLayerBmp.isRecycled()) {
                    mHudLayerBmp.recycle();
                }

                // Get screen shot.
                mHudLayerBmp = getDrawingCache(mHudView);

                // Fringe task.
                FringeLayerTask fringeTask = new FringeLayerTask(mWebLayerBmp, mHudLayerBmp);
                mBackWorker.execute(fringeTask);

            } else {
                // NOP. Currently, another request is processing.
            }

            if (Log.IS_DEBUG) Log.logDebug(TAG, "request() : X");
            return isSuccess;
        }

        private Bitmap getDrawingCache(View view) {
            Bitmap screenShot;

            // Generate drawable cache.
            view.setDrawingCacheEnabled(true);
            view.setDrawingCacheQuality(DRAWING_CACHE_QUALITY_HIGH);
            view.buildDrawingCache(true);

            // Capture.
            screenShot = Bitmap.createBitmap(view.getDrawingCache(true));
            view.destroyDrawingCache();

            return screenShot;
        }

        private class FringeLayerTask implements Runnable {
            private final Bitmap mBaseBmp;
            private final Bitmap mOverlayBmp;

            private final Paint mPaint;

            /**
             * CONSTRUCTOR.
             *
             * @param baseBmp
             * @param overlayBmp
             */
            FringeLayerTask(Bitmap baseBmp, Bitmap overlayBmp) {
                mBaseBmp = baseBmp;
                mOverlayBmp = overlayBmp;
                mPaint = new Paint();
            }

            @Override
            public void run() {
                Bitmap fringed = Bitmap.createBitmap(mBaseBmp);
                Canvas c = new Canvas(fringed);
                c.drawBitmap(mOverlayBmp, 0, 0, mPaint);

                // Bitmap will be recycled in Bmp2PngTask.
                Bmp2PngTask bmp2PngTask = new Bmp2PngTask(fringed);
                mBackWorker.execute(bmp2PngTask);
            }
        }

        private class Bmp2PngTask implements Runnable {
            // Log tag.
            private final String TAG = Bmp2PngTask.class.getSimpleName();

            // Target bitmap.
            private Bitmap mBitmap = null;

            /**
             * CONSTRUCTOR.
             *
             * @param bmp
             */
            public Bmp2PngTask(Bitmap bmp) {
                mBitmap = bmp;
            }

            @Override
            public void run() {
                if (Log.IS_DEBUG) Log.logDebug(TAG, "run() : E");

                // Cache is available.
                byte[] pngBuffer = null;
                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 95, os);
                    pngBuffer = os.toByteArray();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mBitmap.recycle();
                mBitmap = null;

                // Callback.
                mCallback.onScreenShotDone(pngBuffer);

                // Reset.
                mCallback = null;

                if (Log.IS_DEBUG) Log.logDebug(TAG, "run() : X");
            }
        }
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
                    if (mDisplayShortLineLength / 2 < event.getRawX()) {
                        targetPoint = new Point(mWinOpenPosX, mWindowLayoutParams.y);

                        // Enable interaction.
                        enableOverlayInteraction();
                    } else {
                        targetPoint = new Point(mWinClosePosX, mWindowLayoutParams.y);

                        // Disable interaction.
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
         * @param targetView
         * @param targetPos
         * @param winMng
         * @param winParams
         * @param handler
         */
        public WindowPositionCorrectionTask(
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
}
