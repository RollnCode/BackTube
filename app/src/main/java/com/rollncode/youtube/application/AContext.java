package com.rollncode.youtube.application;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.WindowManager;

import com.rollncode.youtube.R;

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

    // SINGLETON
    private static AContext sInstance;

    static void init(@NonNull App app) {
        sInstance = new AContext(app);
    }

    public static AContext getInstance() {
        return sInstance;
    }

    @SuppressLint("PrivateResource")
    private AContext(@NonNull App app) {
        mApp = app;
        mResources = mApp.getResources();
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
}
