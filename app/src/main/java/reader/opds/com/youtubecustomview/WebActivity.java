package reader.opds.com.youtubecustomview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends AppCompatActivity implements ValueCallback<String> {

    private static final String TAG = WebActivity.class.getName();
    private WebView mWebView;

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
        mWebView = (WebView) findViewById(R.id.webView);
        WebSettings settings = mWebView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setPluginState(WebSettings.PluginState.ON);
        mWebView.setKeepScreenOn(true);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setVerticalScrollBarEnabled(false);

//        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @TargetApi(Build.VERSION_CODES.KITKAT)
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "WebViewClient onPageFinished url: " + url);

                mWebView.evaluateJavascript("document.getElementsByTagName(\"iframe\")[0].contentWindow" +
                        ".document.getElementsByClassName(\"html5-video-player ytp-new-infobar ytp-hide-controls ytp-small-mode ytp-native-controls playing-mode ytp-touch-mode\")" +
                        "[0].click();", WebActivity.this);
            }
        });

        final String mimeType = "text/html";
        final String encoding = "UTF-8";
        String html = getHTML();
        mWebView.loadDataWithBaseURL("", html, mimeType, encoding, "");
    }

    @Override
    public void onReceiveValue(String value) {
        Log.d(TAG, "Evaluate javascript: " + value);
    }

    private String getHTML() {
        return "<iframe id=iframe1\" width=\"420\" height=\"315\" src=\"http://www.youtube.com/embed/d9-zYbhDbPo" +
                "?rel=0&autoplay=1&enablejsapi=1 frameborder=\"0\" allowfullscreen></iframe>";
    }

}
