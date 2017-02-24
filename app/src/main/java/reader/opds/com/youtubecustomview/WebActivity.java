package reader.opds.com.youtubecustomview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

public class WebActivity extends AppCompatActivity implements ValueCallback<String> {

    private static final String TAG = "sldfkjslk";
    private WebView wv;

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
        wv = (WebView) findViewById(R.id.webView);

        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setPluginState(WebSettings.PluginState.ON);
        wv.setKeepScreenOn(true);
        wv.setHorizontalScrollBarEnabled(false);
        wv.setVerticalScrollBarEnabled(false);
        wv.getSettings().setBuiltInZoomControls(true);

        wv.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return super.onConsoleMessage(consoleMessage);
            }
        });

        wv.setWebViewClient(new WebViewClient() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "WebViewClient onPageFinished url: " + url);

                wv.evaluateJavascript("document.getElementByTagName(\"iframe\")[0].contentWindow" +
                        ".document.getElementsByClassName(\"html5-video-player ytp-new-infobar ytp-hide-controls ytp-small-mode ytp-native-controls playing-mode ytp-touch-mode\")" +
                        "[0].click();", WebActivity.this);
            }
        });

        final String mimeType = "text/html";
        final String encoding = "UTF-8";
        String html = getHTML();
        wv.loadDataWithBaseURL("", html, mimeType, encoding, "");
    }

    @Override
    public void onReceiveValue(String value) {
        Log.d(TAG, "Evaluate javascript: " + value);
    }

    public String getHTML() {
        String html = "<iframe id=iframe1\" width=\"420\" height=\"315\" src=\"http://www.youtube.com/embed/d9-zYbhDbPo" +
                "?rel=0&autoplay=1&origin=http:///www.youtube.com\" frameborder=\"0\" allowfullscreen></iframe>";

        return html;
    }

}
