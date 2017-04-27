package com.rollncode.youtube;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

@Deprecated
public class WebActivity extends AppCompatActivity
        implements ValueCallback<String> {

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
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
        setWebView();
    }

    @SuppressLint({"NewApi", "SetJavaScriptEnabled"})
    private void setWebView() {
        WebView webView = (WebView) findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setPluginState(WebSettings.PluginState.ON);

        settings.setBuiltInZoomControls(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);

//        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @TargetApi(Build.VERSION_CODES.KITKAT)
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "WebViewClient onPageFinished url: " + url);
//                evaluateJs(view);
            }

            private void evaluateJs(WebView view) {
                String evaluateString = String.format("document.getElementsByTagName(%s)[0]" +
                                ".contentWindow.document.getElementsByClassName(%s)[0].click();",
                        mTagName, mClassName);
                view.evaluateJavascript(evaluateString, WebActivity.this);
            }
        });

        loadDataFromString(webView);

//        loadDataFromFile(webView);
    }

    private void loadDataFromString(WebView webView) {
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

    private void loadDataFromFile(WebView webView) {
        String htmlFilename = "page.html";
        AssetManager mgr = getBaseContext().getAssets();
        try {
            InputStream in = mgr.open(htmlFilename, AssetManager.ACCESS_BUFFER);
            String htmlContentInStringFormat = streamToString(in);
            in.close();
            webView.loadData(htmlContentInStringFormat, "text/html; charset=utf-8", "UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String streamToString(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {

        }
        return writer.toString();
    }

    @Override
    public void onReceiveValue(String value) {
        Log.d(TAG, "Evaluate javascript: " + value);
    }

}
