package com.rollncode.youtube;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.rollncode.youtube.application.AContext;

/**
 * This is temporary service realization to experiment
 * how works alert window in this way
 */
@Deprecated
public class YoutubeService extends Service {

    private WebView mWebView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(getBaseContext(), "onCreate", Toast.LENGTH_LONG).show();
        initWebView();
        loadWebViewContent("d9-zYbhDbPo", 160, 150, 20);
        initAlerWindow();
    }

    private void initAlerWindow() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
//                600,
                AContext.getScreenSize().x,
//                600,
                AContext.getScreenSize().y,
//                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.END | Gravity.TOP;
        params.setTitle("Load Average");
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mWebView, params);
    }

    /**
     * Get the html to load it in webView with several params - temporary implementation function inside
     * @param videoId an id of current video
     * @param width   a width of iframe
     * @param height  a height of iframe
     * @param frameBorder  a width of iframe border
     */
    private void loadWebViewContent(String videoId, int width, int height, int frameBorder) {

        String str = String.format(
                "<iframe width=%d height=%d " + "src=\"http://www.youtube.com/embed/%s?autoplay=1" + "&enablejsapi=1\" frameborder=%d allowfullscreen></iframe>",
                width, height, videoId, frameBorder);

        mWebView.loadData(str, "text/html; charset=utf-8", "UTF-8");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        mWebView = new WebView(getApplicationContext());
        WebSettings settings = mWebView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setPluginState(WebSettings.PluginState.ON);

        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setVerticalScrollBarEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(), "onDestroy", Toast.LENGTH_LONG).show();
        if (mWebView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mWebView);
            mWebView = null;
        }
    }
}
