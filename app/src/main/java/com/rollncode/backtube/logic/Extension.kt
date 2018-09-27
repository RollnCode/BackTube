package com.rollncode.backtube.logic

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import com.rollncode.backtube.BuildConfig
import com.rollncode.backtube.R.attr
import com.rollncode.backtube.R.dimen

fun toLog(a: Any?, tag: String = "aLog"): Unit = whenDebug {
    when (a) {
        is String    -> {
            Log.d(tag, a)

            Unit
        }
        is Throwable -> {
            Log.d(tag, "error", a)

            Unit
        }
        else         -> toLog(a.toString(), tag)
    }
}

inline fun whenDebug(block: () -> Unit) {
    if (BuildConfig.DEBUG)
        block()
}

inline val Activity.canDrawOverlays: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

fun Intent.toPendingActivity(context: Context, requestCode: Int = 0xA): PendingIntent =
        PendingIntent.getActivity(context, requestCode, this, PendingIntent.FLAG_UPDATE_CURRENT)
                ?: PendingIntent.getActivity(context, requestCode, this, 0)

fun Intent.toPendingBroadcast(context: Context, requestCode: Int = 0xB): PendingIntent =
        PendingIntent.getBroadcast(context, requestCode, this, PendingIntent.FLAG_UPDATE_CURRENT)
                ?: PendingIntent.getBroadcast(context, requestCode, this, 0)

val Context.topPoint: Int
    @SuppressLint("PrivateResource")
    get() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight =
                if (resourceId > 0)
                    resources.getDimensionPixelSize(resourceId)
                else
                    0

        val value = TypedValue()
        val actionBarHeight =
                if (theme.resolveAttribute(attr.actionBarSize, value, true))
                    TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
                else
                    resources.getDimensionPixelSize(dimen.abc_action_bar_default_height_material)

        return statusBarHeight + actionBarHeight
    }

inline fun attempt(count: Int = 0, block: () -> Unit): Unit =
        attempt(count, block, { })

inline fun attempt(count: Int,
                   onTry: () -> Unit,
                   onException: (Exception) -> Unit) {
    var exception: Exception = IllegalStateException()
    (0..count).forEach { _ ->
        try {
            onTry()
            return

        } catch (e: Exception) {
            exception = e
            toLog(e)
        }
    }
    onException(exception)
}

fun Intent.startActivity(context: Context) = context.startActivity(this)