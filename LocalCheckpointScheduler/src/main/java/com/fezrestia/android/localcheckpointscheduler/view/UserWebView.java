package com.fezrestia.android.localcheckpointscheduler.view;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fezrestia.android.localcheckpointscheduler.util.Log;

import java.io.BufferedReader;
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

    // Is interactive mode or not.
    private boolean mIsInInteractiveMode = true;

    // Initial load target URL.
    private static final String INITIAL_LOAD_URL = "https://www.ingress.com/intel";

    // JS done timeout.
    private static final int JS_DONE_TIMEOUT = 1000;

    // Java Script -> Native Java interface.
    private static final String INJECTED_JAVA_SCRIPT_NATIVE_INTERFACE_OBJECT_NAME = "jsni";

    // JS file.
    private static final String JS_INGRESS_INTEL_HIDE_HEAD_UP_DISPLAY
            = "ingress_intel_hide_head_up_display.js";
    private ExecuteJsTask mLoadHideHudJsTask = null;
    private static final String JS_INGRESS_INTEL_SHOW_HEAD_UP_DISPLAY
            = "ingress_intel_show_head_up_display.js";
    private ExecuteJsTask mLoadShowHudJsTask = null;
    private static final String JS_LOAD_CONTENT_HTML
            = "load_content_html.js";
    private ExecuteJsTask mLoadContentHtmlTask = null;
    private static final String JS_INGRESS_INTEL_CHECK_LOADING_INDICATOR_VISIBILITY
            = "ingress_intel_check_loading_indicator_visibility.js";
    private ExecuteJsTask mLoadCheckLoadingIndicatorVisibilityTask = null;

    private class JsExecutionCallback implements ValueCallback<String> {
        // Log tag.
        private final String TAG = JsExecutionCallback.class.getSimpleName();

        @Override
        public void onReceiveValue(String value) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onReceiveValue() : " + value);
            // NOP.
        }
    }

    // Reload task.
    private ReloadTask mReloadTask = null;

    // Loading checker.
    private CheckLoadingIndicatorVisibilityTask mLoadingChecker = null;

    // Loading callback.
    private LoadingStateCallback mLoadingStateCallback = null;

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
     */
    public void initialize() {
        setWebViewClient(mUserWebViewClient);

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

        // Java Script.
        String script;
        script = loadJs(JS_INGRESS_INTEL_HIDE_HEAD_UP_DISPLAY);
        mLoadHideHudJsTask = new ExecuteJsTask(script, new JsExecutionCallback());
        script = loadJs(JS_INGRESS_INTEL_SHOW_HEAD_UP_DISPLAY);
        mLoadShowHudJsTask = new ExecuteJsTask(script, new JsExecutionCallback());
        script = loadJs(JS_LOAD_CONTENT_HTML);
        mLoadContentHtmlTask = new ExecuteJsTask(script, new JsExecutionCallback());

        // Loading checker.
        mLoadingChecker = new CheckLoadingIndicatorVisibilityTask();
        script = loadJs(JS_INGRESS_INTEL_CHECK_LOADING_INDICATOR_VISIBILITY);
        mLoadCheckLoadingIndicatorVisibilityTask = new ExecuteJsTask(script, mLoadingChecker);

        // Tasks.
        mReloadTask = new ReloadTask();

        // Load.
        String intelUrl = INITIAL_LOAD_URL;
        loadUrl(intelUrl);

        // Continuous check task.
        mUiWorker.post(mLoadingChecker);
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
     * Set loading state callback.
     *
     * @param callback
     */
    public void setLoadingStateCallback(LoadingStateCallback callback) {
        mLoadingStateCallback = callback;
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

        mLoadingStateCallback = null;

        if (mUiWorker != null) {
            if (mLoadHideHudJsTask != null) {
                mUiWorker.removeCallbacks(mLoadHideHudJsTask);
                mLoadHideHudJsTask = null;
            }
            if (mLoadShowHudJsTask != null) {
                mUiWorker.removeCallbacks(mLoadShowHudJsTask);
                mLoadShowHudJsTask = null;
            }
            if (mLoadContentHtmlTask != null) {
                mUiWorker.removeCallbacks(mLoadContentHtmlTask);
                mLoadContentHtmlTask = null;
            }
            if (mLoadCheckLoadingIndicatorVisibilityTask != null) {
                mUiWorker.removeCallbacks(mLoadCheckLoadingIndicatorVisibilityTask);
                mLoadCheckLoadingIndicatorVisibilityTask = null;
            }
            if (mReloadTask != null) {
                mUiWorker.removeCallbacks(mReloadTask);
                mReloadTask = null;
            }
            if (mLoadingChecker != null) {
                mUiWorker.removeCallbacks(mLoadingChecker);
                mLoadingChecker = null;
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


    private class ReloadTask implements Runnable {
        @Override
        public void run() {
            reload();
        }
    }

    private class ExecuteJsTask implements Runnable {
        private final String mScript;
        private final ValueCallback mCallback;

        /**
         * CONSTRUCTOR.
         *
         * @param script
         * @param callback
         */
        public ExecuteJsTask(String script, ValueCallback callback) {
            mScript = script;
            mCallback = callback;
        }

        @Override
        public void run() {
            evaluateJavascript(mScript, mCallback);
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
            if (Log.IS_DEBUG) Log.logDebug(TAG, "HTML = \n" + htmlSrc);
            // NOP.
        }
    }

    private final UserWebViewClient mUserWebViewClient = new UserWebViewClient();
    private class UserWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onPageFinished()");

            mUiWorker.post(mLoadContentHtmlTask);
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

    /**
     * Enable interactive mode.
     */
    public void enableInteraction() {
        mIsInInteractiveMode = true;

        // Show HUD.
        mUiWorker.post(mLoadShowHudJsTask);
    }

    /**
     * Disable interactive mode.
     */
    public void disableInteraction() {
        mIsInInteractiveMode = false;

        // Hide HUD.
        mUiWorker.post(mLoadHideHudJsTask);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mIsInInteractiveMode) {
            return super.dispatchTouchEvent(event);
        } else {
            return false;
        }
    }

    /**
     * Web  view loading state callback.
     */
    public interface LoadingStateCallback {
        /**
         * Web view is now on loading or not.
         *
         * @param isLoading
         */
        void onLoading(boolean isLoading);
    }

    private class CheckLoadingIndicatorVisibilityTask
            implements
                    Runnable,
                    ValueCallback<String> {
        // Log tag.
        private final String TAG = CheckLoadingIndicatorVisibilityTask.class.getSimpleName();

        // Check interval.
        private final int CHECK_INTERVAL_MILLIS = 1000;

        // Now on loading or not.
        private boolean mIsLoading = true;

        // Not loading display value.
        private final String NOT_LOADING_DISPLAY_VALUE = "\"none\"";

        @Override
        public void run() {
            if (mUiWorker != null && mLoadCheckLoadingIndicatorVisibilityTask != null) {
                mUiWorker.post(mLoadCheckLoadingIndicatorVisibilityTask);
                mUiWorker.postDelayed(this, CHECK_INTERVAL_MILLIS);
            }
        }

        @Override
        public void onReceiveValue(String value) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onReceiveValue() : " + value);

            if (mLoadingStateCallback != null) {
                if (NOT_LOADING_DISPLAY_VALUE.equals(value)) {
                    mLoadingStateCallback.onLoading(false);
                } else {
                    mLoadingStateCallback.onLoading(true);
                }
            }
        }
    }



}
