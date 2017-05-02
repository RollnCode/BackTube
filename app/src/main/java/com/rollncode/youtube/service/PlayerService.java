package com.rollncode.youtube.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;

import com.rollncode.youtube.R;
import com.rollncode.youtube.activity.PlayerActivity;
import com.rollncode.youtube.application.AContext;
import com.rollncode.youtube.types.PlayerAction;
import com.rollncode.youtube.utility.Utils;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public class PlayerService extends Service {

    private static final int NOTIFICATION_ID = 777;
    private static final float DELTA = 0.275f;

    private static final String FORMAT = "<iframe width=%1$d height=%2$d src=\"http://www.youtube.com/embed/%3$s\" frameborder=%4$d allowfullscreen></iframe>";
    private static final String MIME_TYPE = "text/html; charset=utf-8";
    private static final String ENCODING = "UTF-8";
    private static final String YOUTUBE_LINK = "YOUTUBE_LINK";

    private BroadcastReceiver mReceiver;
    private LayoutParams mParams;
    private LayoutParams mHideParams;
    private WebView mWebView;
    private int mWebViewSizePx;
    private WindowManager mWindowManager;

    public static void start(@NonNull Context context, @NonNull String url) {
        final Intent intent = new Intent(context, PlayerService.class);
        intent.setAction(PlayerAction.START_YOUTUBE.getName());
        intent.putExtra(YOUTUBE_LINK, url);
        context.startService(intent);
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    public void onCreate() {
        super.onCreate();
        final int windowSizePx = Math.min(Resources.getSystem().getDisplayMetrics().heightPixels, Resources.getSystem().getDisplayMetrics().widthPixels);
        mWebViewSizePx = (int) (((float) windowSizePx) * DELTA);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mParams = new LayoutParams(windowSizePx, windowSizePx, LayoutParams.TYPE_SYSTEM_ALERT, 288, PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.END | Gravity.TOP;
        mParams.y = AContext.getActionBarHeight() + AContext.getStatusBarHeight();

        mHideParams = new LayoutParams(1, 1, LayoutParams.TYPE_SYSTEM_ALERT, 288, PixelFormat.TRANSLUCENT);

        mWebView = new WebView(this);
        mWebView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        mWebView.getSettings().setJavaScriptEnabled(true);

        AContext.subscribeLocalReceiver(mReceiver = newBroadcastReceiver(),
                PlayerAction.STOP_YOUTUBE.getName(),
                PlayerAction.SHOW_YOUTUBE.getName(),
                PlayerAction.HIDE_YOUTUBE.getName());
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AContext.subscribeLocalReceiver(mReceiver);
        mReceiver = null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        if (action != null) {
            final PlayerAction playerAction = PlayerAction.get(action);
            if (playerAction.equals(PlayerAction.START_YOUTUBE)) {
                final String videoId = Utils.parse(intent.getStringExtra(YOUTUBE_LINK));
                mWindowManager.addView(mWebView, mParams);
                loadWebViewContent(videoId, 0);

                setUpAsForeground();
            }
        }
        return START_NOT_STICKY;
    }

    @SuppressLint("DefaultLocale")
    private void loadWebViewContent(String videoId, int frameBorder) {
        mWebView.loadData(String.format(FORMAT, mWebViewSizePx, mWebViewSizePx, videoId, frameBorder), MIME_TYPE, ENCODING);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void showYoutubePlayer() {
        if (mWebView != null && mWebView.isAttachedToWindow()) {
            mWindowManager.removeView(mWebView);
            mWindowManager.addView(mWebView, mParams);
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void hideYoutubePlayer() {
        if (mWebView != null && mWebView.isAttachedToWindow()) {
            mWindowManager.removeView(mWebView);
            mWindowManager.addView(mWebView, mHideParams);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void removeWebView() {
        if (mWebView != null) {
            if (mWebView.isAttachedToWindow()) {
                mWindowManager.removeView(mWebView);
            }
            mWebView.destroy();
            mWebView = null;
        }
    }

    private void setUpAsForeground() {
        final Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(PlayerAction.START_YOUTUBE.getName());
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

    @NonNull
    private BroadcastReceiver newBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                final PlayerAction playerAction = PlayerAction.get(action);
                switch (playerAction) {
                    case STOP_YOUTUBE:
                        stopForeground(true);
                        removeWebView();
                        break;

                    case SHOW_YOUTUBE:
                        showYoutubePlayer();
                        break;

                    case HIDE_YOUTUBE:
                        hideYoutubePlayer();
                        break;

                    default:
                        break;
                }
            }
        };
    }
}
