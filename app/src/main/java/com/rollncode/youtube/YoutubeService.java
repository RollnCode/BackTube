package com.rollncode.youtube;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * This is temporary service realization to experiment how works alert window
 * in this way
 */

public class YoutubeService extends Service {

    private WebView webView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(getBaseContext(),"onCreate", Toast.LENGTH_LONG).show();
        initWebView();
        loadWebViewContent();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                600,
                600,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                0,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.setTitle("Load Average");
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(webView, params);
    }

    private void loadWebViewContent() {
        String str = "<html>\n" +
                "\n" +
                "<head>\n" +
                "<script>" +
                "function def() {document.getElementsByClassName(\"" +
                "html5-video-player ytp-new-infobar ytp-hide-controls ytp-small-mode ytp-native-controls playing-mode ytp-touch-mode" +
                "\")[0].click();}" +
                "</script>" +
                "</head>\n" +
                "\n" +
                "<body onLoad=\"def()\">\n" +
                "\n" +
                "<iframe id=\"myFrame\" width=\"420\" height=\"315\"\n" +
                "        src=\"http://www.youtube.com/embed/d9-zYbhDbPo?autoplay=1&enablejsapi=1\"\n" +
                "        frameborder=\"0\" allowfullscreen></iframe>\n" +
                "\n" +
                "</body>\n" +
                "\n" +
                "</html>";

        webView.loadData(str, "text/html; charset=utf-8", "UTF-8");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webView = new WebView(getApplicationContext());
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setPluginState(WebSettings.PluginState.ON);

        settings.setBuiltInZoomControls(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(),"onDestroy", Toast.LENGTH_LONG).show();
        if(webView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(webView);
            webView = null;
        }
    }
}
