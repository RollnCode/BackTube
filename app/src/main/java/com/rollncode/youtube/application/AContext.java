package com.rollncode.youtube.application;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.WindowManager;

import com.rollncode.youtube.R;
import com.rollncode.youtube.utility.Utils;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public class AContext {

    // VALUES
    private final App mApp;
    private final Point mScreenSize;
    private final int mActionBarHeight;
    private final Resources mResources;
    private final LocalBroadcastManager mBroadcastManager;

    // SINGLETON
    private static AContext sInstance;

    static void init(@NonNull App app) {
        sInstance = new AContext(app);
    }

    @SuppressLint("PrivateResource")
    private AContext(@NonNull App app) {
        mApp = app;
        mResources = mApp.getResources();
        mBroadcastManager = LocalBroadcastManager.getInstance(app);

        {
            final WindowManager windowManager = (WindowManager) mApp.getSystemService(Context.WINDOW_SERVICE);
            mScreenSize = new Point();
            windowManager.getDefaultDisplay().getSize(mScreenSize);
        }

        final TypedValue value = new TypedValue();
        if (app.getTheme().resolveAttribute(R.attr.actionBarSize, value, true)) {
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(value.data, mResources.getDisplayMetrics());

        } else {
            mActionBarHeight = mResources.getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
        }
    }

    @NonNull
    public static App getApp() {
        return sInstance.mApp;
    }

    @NonNull
    public static Point getScreenSize() {
        return sInstance.mScreenSize;
    }

    @CheckResult
    public static int getActionBarHeight() {
        return sInstance.mActionBarHeight;
    }

    public static int getStatusBarHeight() {
        int resourceId = sInstance.mResources.getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? sInstance.mResources.getDimensionPixelSize(resourceId) : 0;
    }

    public static void sendLocalBroadcast(@NonNull Intent intent) {
        sInstance.mBroadcastManager.sendBroadcast(intent);
    }

    public static void subscribeLocalReceiver(@Nullable BroadcastReceiver receiver, @Nullable String... actions) {
        if (receiver == null) {
            return;
        }
        if (actions == null || actions.length == 0) {
            sInstance.mBroadcastManager.unregisterReceiver(receiver);

        } else {
            sInstance.mBroadcastManager.registerReceiver(receiver, Utils.newIntentFilter(actions));
        }
    }
}
