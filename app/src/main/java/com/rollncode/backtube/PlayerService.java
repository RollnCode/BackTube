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
import android.view.ViewGroup;
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

    private static final String FORMAT = "<iframe id=\"ytplayer\" type=\"text/html\" width=\"%2$d\" height=\"%2$d\" frameborder=\"0\" src=\"http://www.youtube.com/embed%1$s\"/>";
    private static final String MIME_TYPE = "text/html; charset=utf-8";
    private static final String ENCODING = "UTF-8";

    private BroadcastReceiver mReceiver;

    private WindowManager mWindowManager;
    private LayoutParams mHideParams;
    private LayoutParams mParams;

    private int mPlayerSize;
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

        mWebView = new WebView(this);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.getSettings().setJavaScriptEnabled(true);
        {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            mHideParams = newLayoutParams(1);

            final DisplayMetrics metrics = getResources().getDisplayMetrics();
            final int min = Math.min(metrics.heightPixels, metrics.widthPixels);

            mPlayerSize = (int) (min / metrics.density);

            mParams = newLayoutParams(min);
            mParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            mParams.y = getTopY(metrics);
        }
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceAction.STOP);
        filter.addAction(ServiceAction.SHOW);
        filter.addAction(ServiceAction.HIDE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver = newBroadcastReceiver(), filter);
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

        resetViewParams(null);
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
                    if (mWebView == null) {
                        throw new IllegalStateException();
                    }
                    final Uri uri = Uri.parse(intent.getStringExtra(Intent.EXTRA_STREAM));
                    final String videoId = getVideoId(uri);

                    final String suffix;
                    if (videoId == null) {
                        suffix = "/nsDjNnFlFYE";

                    } else if (videoId.startsWith("PL")) {
                        suffix = "?listType=playlist&list=" + videoId;

                    } else {
                        final String startTime = getStartTime(uri);
                        suffix = "/" + videoId + (startTime == null ? "" : "?start=" + startTime);
                    }
                    resetViewParams(mParams);
                    mWebView.loadData(String.format(Locale.ENGLISH, FORMAT, suffix, mPlayerSize), MIME_TYPE, ENCODING);
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
    private static String getVideoId(@NonNull Uri uri) {
        final String videoId = LinkType.SHORT.equals(uri.getAuthority()) ? uri.getLastPathSegment() : uri.getQueryParameter("v");
        return videoId != null ? videoId : uri.getQueryParameter("list");
    }

    @Nullable
    public static String getStartTime(@NonNull Uri uri) {
        return uri.getQueryParameter("t");
    }

    private void resetViewParams(@Nullable LayoutParams params) {
        if (mWebView == null) {
            return;
        }
        final ViewGroup.LayoutParams layoutParams = mWebView.getLayoutParams();
        if (layoutParams == null) {
            mWindowManager.addView(mWebView, params);

        } else if (params == null) {
            mWindowManager.removeView(mWebView);

        } else if (layoutParams != params) {
            mWindowManager.updateViewLayout(mWebView, params);
        }
    }

    @CheckResult
    private BroadcastReceiver newBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ServiceAction.HIDE:
                        resetViewParams(mHideParams);
                        break;

                    case ServiceAction.SHOW:
                        resetViewParams(mParams);
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