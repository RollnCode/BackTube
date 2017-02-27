package com.rollncode.youtube;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends AppCompatActivity implements ValueCallback<String> {

    /**
     * This is constants for tags and class names from youtube video html page, which must be clicked
     */
    private final String mTagName = "\"iframe\"";
    private final String mClassName = "\"html5-video-player ytp-new-infobar ytp-hide-controls ytp-small-mode ytp-native-controls playing-mode ytp-touch-mode\"";

    private static final String TAG = WebActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            { WebView.setWebContentsDebuggingEnabled(true); }
        }
        setWebView();
    }

    @SuppressLint({"NewApi", "SetJavaScriptEnabled"})
    private void setWebView() {
        WebView webView = (WebView) findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setPluginState(WebSettings.PluginState.ON);
        webView.setKeepScreenOn(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);

//        mWebView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @TargetApi(Build.VERSION_CODES.KITKAT)
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "WebViewClient onPageFinished url: " + url);

                String evaluateString = String.format("document.getElementsByTagName(%s)[0]" +
                                ".contentWindow.document.getElementsByClassName(%s)[0].click();",
                        mTagName, mClassName);

                view.evaluateJavascript(evaluateString, WebActivity.this);
            }
        });

        final String videoId = "d9-zYbhDbPo";
        final String iframeId = "iframe1";
        final String autoPlay = "0";
        final String enableJsApi = "1";
        final String mimeType = "text/html";
        final String encoding = "UTF-8";

        String html = getHTML(iframeId ,videoId, autoPlay, enableJsApi);
        webView.loadDataWithBaseURL("", html, mimeType, encoding, "");
    }

    @Override
    public void onReceiveValue(String value) {
        Log.d(TAG, "Evaluate javascript: " + value);
    }

    /**
     * Get the html to load it in webView with several params
     * @param iframeId an id of iframe tag
     * @param videoId an id of current video
     * @param autoPlay enable autoplay the video or not
     * @param enableJsApi enable Javascript API or not
     * @return complete string which we can load to webView
     */
    private String getHTML(String iframeId, String videoId, String autoPlay, String enableJsApi) {
        return String.format("<iframe id=%s crossorigin=\"anonymous\" width=\"420\" height=\"315\" " +
                "src=\"http://www.youtube.com/embed/%s?rel=0&autoplay=%s&enablejsapi=%s frameborder=\"0\" allowfullscreen></iframe>",
                iframeId, videoId, autoPlay, enableJsApi) ;
    }

}
