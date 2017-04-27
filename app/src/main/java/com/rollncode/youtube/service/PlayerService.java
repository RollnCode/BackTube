package com.rollncode.youtube.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebView;

import com.rollncode.youtube.R;
import com.rollncode.youtube.activity.PlayerActivity;
import com.rollncode.youtube.application.AContext;
import com.rollncode.youtube.types.PlayerAction;
import com.rollncode.youtube.utility.Utils;

import java.util.Locale;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public class PlayerService extends Service {

    private static final String ACTION_OPEN = "ACTION_OPEN";
    private static final String EXTRA_LINK = "EXTRA_LINK";
    private static final int NOTIFICATION_ID = 777;

    private WindowManager.LayoutParams mParams;
    private WebView mWebView;
    private int mWebViewSizePx;
    private WindowManager mWindowManager;
    private int mWindowSizePx;

    public void onCreate() {
        super.onCreate();
        mWindowSizePx = Math.min(Resources.getSystem().getDisplayMetrics().heightPixels, Resources.getSystem().getDisplayMetrics().widthPixels);
        mWebViewSizePx = (int) (((float) mWindowSizePx) * 0.275f);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    private void initLayoutParams(boolean create) {
        if (create) {
            mParams = new WindowManager.LayoutParams(mWindowSizePx, mWindowSizePx, 2003, 288, -3);
            mParams.gravity = 8388661;
            mParams.y = AContext.getActionBarHeight() + AContext.getStatusBarHeight();
            return;
        }
        mParams = null;
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    @TargetApi(19)
    private void initWebView() {
        removeWebView();
        mWebView = new WebView(getApplicationContext());
        mWebView.getSettings().setJavaScriptEnabled(true);
    }

    @TargetApi(19)
    private void removeWebView() {
        if (mWebView != null) {
            if (mWebView.isAttachedToWindow()) {
                mWindowManager.removeView(mWebView);
            }
            mWebView.destroy();
            mWebView = null;
        }
    }

    private void loadWebViewContent(String videoId, int frameBorder) {
        mWebView.loadData(String.format(Locale.US, "<iframe width=%d height=%d src=\"http://www.youtube.com/embed/%s\" frameborder=%d allowfullscreen></iframe>", new Object[]{Integer.valueOf(this.mWebViewSizePx), Integer.valueOf(this.mWebViewSizePx), videoId, Integer.valueOf(frameBorder)}), "text/html; charset=utf-8", "UTF-8");
    }

    @TargetApi(19)
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            final PlayerAction playerAction = PlayerAction.get(action);
            switch (playerAction) {
                case START_YOUTUBE:
                    String videoId = Utils.parse(intent.getStringExtra(EXTRA_LINK));
                    Log.d("tagger", "SERVICE START_YOUTUBE");
                    initWebView();
                    initLayoutParams(true);
                    mWindowManager.addView(mWebView, mParams);
                    loadWebViewContent(videoId, 0);
                    setUpAsForeground();
                    break;

                case STOP_YOUTUBE:
                    Log.d("tagger", "SERVICE STOP_YOUTUBE");
                    stopForeground(true);
                    removeWebView();
                    initLayoutParams(false);
                    break;

                case SHOW_YOUTUBE:
                    Log.d("tagger", "SERVICE SHOW_YOUTUBE");
                    if (!(mWebView == null || mWebView.isAttachedToWindow())) {
                        initLayoutParams(true);
                        mWindowManager.addView(mWebView, mParams);
                        break;
                    }
                    break;

                case HIDE_YOUTUBE:
                    Log.d("tagger", "SERVICE HIDE_YOUTUBE");
                    if (mWebView != null && mWebView.isAttachedToWindow()) {
                        initLayoutParams(false);
                        mWindowManager.removeView(mWebView);
                        break;
                    }
                    break;

                default:
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setUpAsForeground() {
        final Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
        intent.setAction(ACTION_OPEN);
        startForeground(NOTIFICATION_ID, getNotification(intent));
    }

    private Notification getNotification(Intent intent) {
        return new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.svg_play)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.show_playback))
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .setOngoing(true)
                .build();
    }
}
