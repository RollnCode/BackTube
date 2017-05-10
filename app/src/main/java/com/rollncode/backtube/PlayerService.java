package com.rollncode.backtube;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;

import com.rollncode.backtube.type.LinkType;
import com.rollncode.backtube.type.ServiceAction;

import java.util.Locale;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */
public final class PlayerService extends Service {

    private static final int NOTIFICATION_ID = 0xA;

    private static final String FORMAT = "<iframe width=%1$d height=%2$d src=\"http://www.youtube.com/embed/%3$s\" frameborder=%4$d allowfullscreen></iframe>";
    private static final String MIME_TYPE = "text/html; charset=utf-8";
    private static final String ENCODING = "UTF-8";

    private BroadcastReceiver mReceiver;

    private WindowManager mWindowManager;
    private LayoutParams mHideParams;
    private LayoutParams mParams;

    private int mWebViewSizePx;
    private WebView mWebView;

    public static void start(@NonNull Context context, @NonNull String url) {
        context.startService(new Intent(context, PlayerService.class)
                .setAction(ServiceAction.START)
                .putExtra(Intent.EXTRA_STREAM, url));
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    public void onCreate() {
        super.onCreate();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        final int min = Math.min(metrics.heightPixels, metrics.widthPixels);

        mWebViewSizePx = (int) (min / metrics.density);
        {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            mHideParams = newLayoutParams(1);

            mParams = newLayoutParams(min);
            mParams.gravity = Gravity.END | Gravity.TOP;
            mParams.y = getTopY(metrics);
        }
        mWebView = new WebView(this);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.getSettings().setJavaScriptEnabled(true);
        {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ServiceAction.STOP);
            filter.addAction(ServiceAction.SHOW);
            filter.addAction(ServiceAction.HIDE);

            LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver = newBroadcastReceiver(), filter);
        }
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.stopForeground(true);

        resetView(null);
        mWebView.destroy();
        mWebView = null;
    }

    @SuppressLint("PrivateResource")
    private int getTopY(@NonNull DisplayMetrics metrics) {
        final int statusBarHeight;
        final int actionBarHeight;
        {
            final int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            statusBarHeight = resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
        }
        final TypedValue value = new TypedValue();

        if (super.getTheme().resolveAttribute(R.attr.actionBarSize, value, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(value.data, metrics);

        } else {
            actionBarHeight = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
        }
        return statusBarHeight + actionBarHeight;
    }

    @CheckResult
    private static LayoutParams newLayoutParams(int size) {
        return new LayoutParams(size, size
                , WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                , WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                , PixelFormat.TRANSLUCENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ServiceAction.START:
                    final String videoId = getVideoId(intent.getStringExtra(Intent.EXTRA_STREAM));
                    if (mWebView != null) {
                        resetView(mParams);
                        mWebView.loadData(String.format(Locale.ENGLISH, FORMAT, mWebViewSizePx, mWebViewSizePx, videoId, 0), MIME_TYPE, ENCODING);
                        {
                            intent = new Intent(this, PlayerActivity.class).putExtra(Intent.EXTRA_INTENT, true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            pi = pi != null ? pi : PendingIntent.getActivity(this, 0, intent, 0);

                            super.startForeground(NOTIFICATION_ID, new Builder(this)
                                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                                    .setContentText(getString(R.string.Show_application))
                                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setSmallIcon(R.drawable.svg_play)
                                    .setContentIntent(pi)
                                    .setOngoing(true)
                                    .build());
                        }
                    }
                    break;

                case ServiceAction.HIDE:
                case ServiceAction.SHOW:
                case ServiceAction.STOP:
                default:
                    throw new IllegalStateException();
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    private static String getVideoId(@NonNull String string) {
        final Uri uri = Uri.parse(string);
        return LinkType.SHORT.equals(uri.getAuthority()) ? uri.getLastPathSegment() : uri.getQueryParameter("v");
    }

    private boolean isAttachedToWindow() {
        return mWebView != null && (VERSION.SDK_INT < VERSION_CODES.KITKAT ? mWebView.getParent() != null : mWebView.isAttachedToWindow());
    }

    private void resetView(@Nullable LayoutParams params) {
        if (isAttachedToWindow()) {
            try {
                mWindowManager.removeView(mWebView);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (params != null) {
            mWindowManager.addView(mWebView, params);
        }
    }

    @CheckResult
    private BroadcastReceiver newBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ServiceAction.HIDE:
                        if (isAttachedToWindow()) {
                            resetView(mHideParams);
                        }
                        break;

                    case ServiceAction.SHOW:
                        if (isAttachedToWindow()) {
                            resetView(mParams);
                        }
                        break;

                    case ServiceAction.STOP:
                        stopSelf();
                        break;

                    case ServiceAction.START:
                    default:
                        throw new IllegalStateException();
                }
            }
        };
    }
}