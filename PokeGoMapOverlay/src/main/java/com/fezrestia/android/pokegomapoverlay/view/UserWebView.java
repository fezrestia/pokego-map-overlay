package com.fezrestia.android.pokegomapoverlay.view;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fezrestia.android.pokegomapoverlay.UserApplication;
import com.fezrestia.android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserWebView extends WebView {
    // Log tag.
    private static final String TAG = "UserWebView";

    // UI thread handler.
    private final Handler mUiWorker = UserApplication.getUiThreadHandler();

    // Background worker thread.
    private ExecutorService mBackWorker = null;

    // Is interactive mode or not.
    private boolean mIsInInteractiveMode = true;

    // JS done timeout.
    private static final int JS_DONE_TIMEOUT = 1000;

    // Java Script -> Native Java interface.
    private static final String INJECTED_JAVA_SCRIPT_NATIVE_INTERFACE_OBJECT_NAME = "jsni";

    // JS file.
    //
    // NO JavaScript.
    //

    private class JsExecutionCallback implements ValueCallback<String> {
        // Log tag.
        private final String TAG = "JsExecutionCallback";

        @Override
        public void onReceiveValue(String value) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onReceiveValue() : " + value);
            // NOP.
        }
    }

    // Reload task.
    private ReloadTask mReloadTask = null;

    /**
     * CONSTRUCTOR.
     *
     * @param context
     */
    public UserWebView(Context context) {
        super(context);
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
//        String script;
//        script = loadJs("JS FILE_PATH");
//        mJsTask = new ExecuteJsTask(script, new JsExecutionCallback());

        // Tasks.
        mReloadTask = new ReloadTask();
    }

    /**
     * Request to load URL.
     *
     * @param url
     */
    public void loadWebPase(String url) {
        loadUrl(url);
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

        if (mUiWorker != null) {
            if (mReloadTask != null) {
                mUiWorker.removeCallbacks(mReloadTask);
                mReloadTask = null;
            }
        }
        if (mBackWorker != null) {
            mBackWorker.shutdown();
            mBackWorker = null;
        }
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
        private final String TAG = "JavaScriptNativeInterface";

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
            // NOP.
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
