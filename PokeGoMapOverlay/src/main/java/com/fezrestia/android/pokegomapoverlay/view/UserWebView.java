package com.fezrestia.android.pokegomapoverlay.view;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fezrestia.android.pokegomapoverlay.UserApplication;
import com.fezrestia.android.util.Log;

import java.io.BufferedReader;
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

    // JS done timeout.
    private static final int JS_DONE_TIMEOUT = 1000;

    // Java Script -> Native Java interface.
    private static final String INJECTED_JAVA_SCRIPT_NATIVE_INTERFACE_OBJECT_NAME = "jsni";

    // JS file.
    private static final String JS_P_GO_SEARCH_HIDE_AD = "p_go_search_hide_ad.js";
    private ExecuteJsTask mHideAdTask = null;

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
     * @param context View context.
     */
    public UserWebView(Context context) {
        super(context);
        mBackWorker = Executors.newSingleThreadExecutor();
    }

    /**
     * Initialize.
     */
    public void initialize() {
        // Web callback.
        setWebViewClient(mUserWebViewClient);
        setWebChromeClient(mUserWebChromeClient);

        // Debug.
        setWebContentsDebuggingEnabled(true);

        // Web setting.
        WebSettings webSettings = getSettings();
//        webSettings.setAllowContentAccess(true);
//        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAppCacheEnabled(true);
//        webSettings.setBlockNetworkImage(false);
//        webSettings.setBlockNetworkLoads(false);
//        webSettings.setBuiltInZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setJavaScriptEnabled(true);
//        webSettings.setLoadsImagesAutomatically(true);
//        webSettings.setUseWideViewPort(true);
//        webSettings.setUserAgentString("Desktop");

        addJavascriptInterface(mJSNI, INJECTED_JAVA_SCRIPT_NATIVE_INTERFACE_OBJECT_NAME);

        // Java Script.
        String script;
        script = loadJs(JS_P_GO_SEARCH_HIDE_AD);
        mHideAdTask = new ExecuteJsTask(script, new JsExecutionCallback());

        // Tasks.
        mReloadTask = new ReloadTask();
    }

    /**
     * Request to load URL.
     *
     * @param url Load target URL.
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

        if (mUiWorker != null) {
            if (mReloadTask != null) {
                mUiWorker.removeCallbacks(mReloadTask);
                mReloadTask = null;
            }
            if (mHideAdTask != null) {
                mUiWorker.removeCallbacks(mHideAdTask);
                mHideAdTask = null;
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
        private final ValueCallback<String> mCallback;

        /**
         * CONSTRUCTOR.
         *
         * @param script JavaScript source codes.
         * @param callback JavaScript done callback.
         */
        public ExecuteJsTask(String script, ValueCallback<String> callback) {
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
         * @param htmlSrc Loaded HTML source codes.
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

            mUiWorker.post(mHideAdTask);
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

    private final UserWebChromeClient mUserWebChromeClient = new UserWebChromeClient();
    private class UserWebChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(
                String origin,
                GeolocationPermissions.Callback callback) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onGeolocationPermissionShowPrompt()");

            callback.invoke(origin, true, true); // Allow to use geo API and retain this.
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onPermissionRequest()");

            if (Log.IS_DEBUG) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    String[] resources = request.getResources();

                    for (String resource : resources) {
                        Log.logDebug(TAG, "    Permission=" + resource);
                    }
                } else {
                    Log.logDebug(TAG, "PermissionRequest.getResources() is not supported.");
                }
            }
        }
    }
}
