package com.fezrestia.android.localcheckpointscheduler.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fezrestia.android.localcheckpointscheduler.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserWebView extends WebView {
    // Log tag.
    private static final String TAG = UserWebView.class.getSimpleName();

    // UI thread handler.
    private Handler mUiWorker = null;

    // Background worker thread.
    private ExecutorService mBackWorker = null;

    // Screen shot task.
    private ScreenShotTask mScreenShotTask = null;

    // Container view.
    private View mContainer = null;

    // Is interactive mode or not.
    private boolean mIsInInteractiveMode = true;

    // Javascript wait timeout.
    private static final int JS_DONE_TIMEOUT_MILLIS = 3000;

    // Initial load target URL.
    private static final String INITIAL_LOAD_URL = "https://www.ingress.com/intel";

    // JS file.
    private static final String INJECTED_JAVA_SCRIPT_NATIVE_INTERFACE_OBJECT_NAME = "jsni";
    private static final String JS_INGRESS_INTEL_HIDE_HEAD_UP_DISPLAY
            = "ingress_intel_hide_head_up_display.js";
    private ExecuteJsTask mLoadHideHudJsTask = null;
    private static final String JS_LOAD_CONTENT_HTML
            = "load_content_html.js";
    private ExecuteJsTask mLoadContentHtmlTask = null;
    private static final String JS_ESCAPE_LOADING_MSG
            = "ingress_intel_escape_loading_indicator.js";
    private ExecuteJsTask mEscapeLoadingMsgTask = null;

    private class JsIngressIntelHideHudCallback implements ValueCallback<String> {
        // Log tag.
        private final String TAG = JsIngressIntelHideHudCallback.class.getSimpleName();
        @Override
        public void onReceiveValue(String value) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onReceiveValue() : " + value);
        }
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

    /**
     * CONSTRUCTOR.
     *
     * @param context
     */
    public UserWebView(Context context) {
        super(context);
        mUiWorker = new Handler();
        mBackWorker = Executors.newSingleThreadExecutor();
    }

    /**
     * Initialize.
     *
     * @param parentView
     */
    public void initialize(View parentView) {
        setDrawingCacheEnabled(true);
        setDrawingCacheQuality(DRAWING_CACHE_QUALITY_HIGH);
        setWebViewClient(mUserWebViewClient);
        setPictureListener(mPictureListener);

        // Debug.
        setWebContentsDebuggingEnabled(true);

        // Web setting.
        WebSettings webSettings = getSettings();
//        webSettings.setAllowContentAccess(true);
//        webSettings.setAllowFileAccess(true);
//        webSettings.setAllowFileAccessFromFileURLs(true);
//        webSettings.setAllowUniversalAccessFromFileURLs(true);
//        webSettings.setBlockNetworkImage(false);
//        webSettings.setBlockNetworkLoads(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setGeolocationEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        addJavascriptInterface(mJSNI, INJECTED_JAVA_SCRIPT_NATIVE_INTERFACE_OBJECT_NAME);

        // Container.
        mContainer = parentView;
        mContainer.setDrawingCacheEnabled(true);
        mContainer.setDrawingCacheQuality(DRAWING_CACHE_QUALITY_HIGH);

        // Java Script.
        String script;
        script = loadJs(JS_INGRESS_INTEL_HIDE_HEAD_UP_DISPLAY);
        mLoadHideHudJsTask = new ExecuteJsTask(script);
        script = loadJs(JS_LOAD_CONTENT_HTML);
        mLoadContentHtmlTask = new ExecuteJsTask(script);
        script = loadJs(JS_ESCAPE_LOADING_MSG);
        mEscapeLoadingMsgTask = new ExecuteJsTask(script);

        // Load.
        String intelUrl = INITIAL_LOAD_URL;
        loadUrl(intelUrl);
    }

    private String loadJs(String assetsName) {
        String script = "";

        try {
            InputStream fis = getContext().getAssets().open(assetsName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                script = script + tmp + '\n';
            }
            reader.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return script;
    }


    /**
     * Release all references.
     */
    public void release() {
        stopLoading();
        clearCache(true);
        destroy();

        setWebViewClient(null);
        setWebChromeClient(null);
        setPictureListener(null);

        mContainer = null;

        if (mUiWorker != null) {
            if (mScreenShotTask != null) {
                mUiWorker.removeCallbacks(mScreenShotTask);
            }
            if (mLoadHideHudJsTask != null) {
                mUiWorker.removeCallbacks(mLoadHideHudJsTask);
            }
            if (mLoadContentHtmlTask != null) {
                mUiWorker.removeCallbacks(mLoadContentHtmlTask);
            }
            if (mReloadTask != null) {
                mUiWorker.removeCallbacks(mReloadTask);
            }
            if (mEscapeLoadingMsgTask != null) {
                mUiWorker.removeCallbacks(mEscapeLoadingMsgTask);
            }
            mUiWorker = null;
        }
        if (mBackWorker != null) {
            mBackWorker.shutdown();
            mBackWorker = null;
        }
    }

    @Override
    public void loadUrl(String url) {
        super.loadUrl(url);
    }

    @Override
    public void reload() {
        stopLoading();
        super.reload();
    }

    private final ReloadTask mReloadTask = new ReloadTask();
    private class ReloadTask implements Runnable {
        @Override
        public void run() {
            reload();
        }
    }

    private class ExecuteJsTask implements Runnable {
        private final String mScript;

        /**
         * CONSTRUCTOR.
         *
         * @param script
         */
        public ExecuteJsTask(String script) {
            mScript = script;
        }

        @Override
        public void run() {
            evaluateJavascript(mScript, new JsIngressIntelHideHudCallback());
        }
    }



    private final JavaScriptNativeInterface mJSNI = new JavaScriptNativeInterface();
    private class JavaScriptNativeInterface {
        // Log tag.
        private final String TAG = JavaScriptNativeInterface.class.getSimpleName();

        /**
         * Called on content HTML loaded.
         *
         * @param htmlSrc
         */
        @JavascriptInterface
        public final void onContentHtmlLoaded(final String htmlSrc) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onPageFinished()");
            if (Log.IS_DEBUG) Log.logDebug(TAG, "HTML = " + htmlSrc);
            // NOP.
        }
    }

    private final UserWebViewClient mUserWebViewClient = new UserWebViewClient();
    private class UserWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onPageFinished()");

            mUiWorker.post(mLoadContentHtmlTask);
            mUiWorker.post(mEscapeLoadingMsgTask);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "shouldOverrideUrlLoading() : E");
            if (Log.IS_DEBUG) Log.logDebug(TAG, "URL=" + url);

            UserWebView.this.loadUrl(url);

            if (Log.IS_DEBUG) Log.logDebug(TAG, "shouldOverrideUrlLoading() : X");
            return true;
        }
    }

    private final PictureListenerImpl mPictureListener = new PictureListenerImpl();
    private class PictureListenerImpl implements WebView.PictureListener {
        @Override
        public void onNewPicture(WebView webView, Picture picture) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onNewPicture() : E");
            // NOP.
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onNewPicture() : X");
        }
    }



    private long currentTimestamp() {
        return SystemClock.uptimeMillis();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onCreateInputConnection() : E");
        // NOP.
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onCreateInputConnection() : X");
        return super.onCreateInputConnection(outAttrs);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onDraw() : E");
        // NOP.
        if (Log.IS_DEBUG) Log.logDebug(TAG, "onDraw() : X");
        super.onDraw(canvas);
    }

    /**
     * Request to capture screen shot, after then, reload automatically.
     *
     * @param callback
     */
    public void requestScreenShotAndReload(OnScreenShotDoneCallback callback) {
        if (mScreenShotTask == null) {
            // Hide HUD.
            mUiWorker.post(mLoadHideHudJsTask);

            // Capture screen shot.
            mScreenShotTask = new ScreenShotTask(callback);
            mUiWorker.postDelayed(mScreenShotTask, JS_DONE_TIMEOUT_MILLIS);

            // Reload.
            mUiWorker.postDelayed(mReloadTask, JS_DONE_TIMEOUT_MILLIS + 500);
        } else {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "requestCapture() : Error, already requested.");
        }
    }



    private class ScreenShotTask implements Runnable {
        // Log tag.
        private final String TAG = ScreenShotTask.class.getSimpleName();

        // Callback.
        private final OnScreenShotDoneCallback mCallback;

        /**
         * CONSTRUCTOR.
         *
         * @param callback
         */
        public ScreenShotTask(OnScreenShotDoneCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "run() : E");

            // Create drawing cache.
            buildDrawingCache();
            mContainer.buildDrawingCache(true);
            Bitmap bmp = Bitmap.createBitmap(mContainer.getDrawingCache(true));
            destroyDrawingCache();
            mContainer.destroyDrawingCache();

            if (bmp == null) {
                if (Log.IS_DEBUG) Log.logError(TAG, "Cache is NULL.");
                // NOP.
            } else {
                if (Log.IS_DEBUG) Log.logDebug(TAG, "Cache is available.");

                // Done successfully.
                Runnable task = new Bmp2PngTask(bmp, mCallback);
                mBackWorker.execute(task);

                // Reset.
                mScreenShotTask = null;
            }

            if (Log.IS_DEBUG) Log.logDebug(TAG, "run() : X");
        }
    }

    private class Bmp2PngTask implements Runnable {
        // Log tag.
        private final String TAG = Bmp2PngTask.class.getSimpleName();

        // Target bitmap.
        private Bitmap mBitmap = null;

        // Callback.
        private final OnScreenShotDoneCallback mCallback;

        /**
         * CONSTRUCTOR.
         *
         * @param bmp
         * @param callback
         */
        public Bmp2PngTask(Bitmap bmp, OnScreenShotDoneCallback callback) {
            mBitmap = bmp;
            mCallback = callback;
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

            if (Log.IS_DEBUG) Log.logDebug(TAG, "run() : X");
        }
    }


    /**
     * Enable interactive mode.
     */
    public void enableInteraction() {
        mIsInInteractiveMode = true;
    }

    /**
     * Disable interactive mode.
     */
    public void disableInteraction() {
        mIsInInteractiveMode = false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mIsInInteractiveMode) {
            return super.dispatchTouchEvent(event);
        } else {
            return false;
        }
    }



}
