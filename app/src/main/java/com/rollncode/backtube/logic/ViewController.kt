package com.rollncode.backtube.logic

import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.support.v4.view.GravityCompat
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayerView

class ViewController(context: Application, playerController: PlayerController) {

    private val wm by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val view by lazy {
        YouTubePlayerView(context).apply {
            initialize(playerController, true)
            playerUIController.run {
                showFullscreenButton(false)
                showYouTubeButton(false)
                showCustomAction1(false)
                showCustomAction2(false)
                showMenuButton(false)
            }
        }
    }

    private val hideParams by lazy {
        createWindowLayoutParams(1, 1)
    }

    private val showParams by lazy {
        val metrics = context.resources.displayMetrics
        val min = Math.min(metrics.heightPixels, metrics.widthPixels)

        createWindowLayoutParams(min).apply {
            gravity = GravityCompat.START or Gravity.TOP
            y = context.actionBarHeight + context.statusBarHeight
        }
    }

    fun show() {
        toLog("ViewController.show")
        TubeState.windowShowed = true

        if (view.layoutParams == null)
            wm.addView(view, showParams)
        else
            wm.updateViewLayout(view, showParams)
    }

    fun hide() {
        toLog("ViewController.hide")

        TubeState.windowShowed = false

        if (view.layoutParams == null)
            wm.addView(view, hideParams)
        else
            wm.updateViewLayout(view, hideParams)
    }

    fun release() = attempt {
        toLog("ViewController.release")
        TubeState.windowShowed = false

        view.release()
        wm.removeViewImmediate(view)
    }

    @Suppress("DEPRECATION")
    private fun createWindowLayoutParams(width: Int = WindowManager.LayoutParams.MATCH_PARENT,
                                         height: Int = WindowManager.LayoutParams.WRAP_CONTENT
                                        ) = WindowManager.LayoutParams(width, height,
            if (VERSION.SDK_INT >= VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT)
}