package com.rollncode.backtube.logic

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.LayoutInflater
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayerView
import com.rollncode.backtube.R

@SuppressLint("InflateParams")
class ViewController(context: Application, playerController: PlayerController) {

    private val wm by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val view by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_player, null, false)
    }

    private val viewPlayer by lazy {
        view.findViewById<YouTubePlayerView>(R.id.view_player).apply {
            initialize(playerController, true)
            playerUIController.run {
                showFullscreenButton(false)
                showYouTubeButton(false)
                showCustomAction1(false)
                showCustomAction2(false)
                showMenuButton(false)
            }
            val params = layoutParams
            if (params is MarginLayoutParams)
                params.topMargin = context.actionBarHeight + context.statusBarHeight
        }
    }

    private val hideParams by lazy {
        createWindowLayoutParams(1, 1)
    }

    private val showParams by lazy {
        val metrics = context.resources.displayMetrics
        val min = Math.min(metrics.heightPixels, metrics.widthPixels)

        createWindowLayoutParams(min)
    }

    fun show() {
        toLog("ViewController.show")
        TubeState.windowShowed = true

        if (view.layoutParams == null)
            wm.addView(view, showParams)
        else
            wm.updateViewLayout(view, showParams)

        viewPlayer
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

        viewPlayer.release()
        wm.removeViewImmediate(view)
    }

    @Suppress("DEPRECATION")
    private fun createWindowLayoutParams(width: Int = WindowManager.LayoutParams.MATCH_PARENT,
                                         height: Int = WindowManager.LayoutParams.MATCH_PARENT
                                        ) = WindowManager.LayoutParams(width, height,
            if (VERSION.SDK_INT >= VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT)
}