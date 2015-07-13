package com.fezrestia.android.localcheckpointscheduler.view;

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

import com.fezrestia.android.localcheckpointscheduler.Constants;
import com.fezrestia.android.localcheckpointscheduler.R;
import com.fezrestia.android.localcheckpointscheduler.UserApplication;
import com.fezrestia.android.localcheckpointscheduler.control.OverlayViewController;
import com.fezrestia.android.localcheckpointscheduler.util.Log;

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
    private ImageView mCloseButton = null;

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

    // Screen shot generator.
    private ScreenShotGenerator mScreenShotGenerator = null;

    // Capture delay. e.g. waiting for java script execution done.
    private static final int CAPTURE_DELAY_MILLIS = 5000;

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
        mCloseButton = new ImageView(getContext());
        mCloseButton.setOnTouchListener(mHideButtonOnTouchListenerImpl);
        mCloseButton.setImageResource(R.drawable.close_button);
        FrameLayout.LayoutParams hideButtonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        hideButtonParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        hideButtonParams.rightMargin = 20;
        hideButtonParams.bottomMargin = 20;
        mInteractionViewContainer.addView(mCloseButton, hideButtonParams);

        // Screen shot generator.
        mScreenShotGenerator = new ScreenShotGenerator(
                mUserWebViewContainer,
                mHudViewContainer,
                isLoadingDetectionEnabled);
        mUserWebView.setLoadingStateCallback(mScreenShotGenerator);
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
            mUserWebView.setLoadingStateCallback(null);
            mUserWebView.release();
            mUserWebView = null;
        }
        mEdgeFrame = null;
        mUserWebViewContainer = null;
        mHudViewContainer = null;
        if (mCloseButton != null) {
            mCloseButton.setOnTouchListener(null);
            mCloseButton = null;
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

        FrameLayout.LayoutParams edgeFrameLayoutParams = (FrameLayout.LayoutParams)
                mEdgeFrame.getLayoutParams();
        edgeFrameLayoutParams.width = mDisplayShortLineLength;
        edgeFrameLayoutParams.height = mDisplayShortLineLength;
        mEdgeFrame.setLayoutParams(edgeFrameLayoutParams);
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



}
