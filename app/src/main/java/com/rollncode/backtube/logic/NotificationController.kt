package com.rollncode.backtube.logic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.rollncode.backtube.R
import com.rollncode.backtube.R.color
import com.rollncode.backtube.R.drawable
import com.rollncode.backtube.R.mipmap
import com.rollncode.backtube.R.string
import com.rollncode.backtube.api.TubePlaylist
import com.rollncode.backtube.api.TubeVideo
import com.rollncode.backtube.screen.TubeActivity

class NotificationController(private val service: Service) : OnPlayerControllerListener {

    private val channelId by lazy {
        val channelId = "com.rollncode.backtube"
        if (VERSION.SDK_INT >= VERSION_CODES.O)
            (service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(
                        channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        lightColor = getColor(R.color.colorPrimary)
                    })
        channelId
    }

    fun startForeground(block: NotificationCompat.Builder.() -> Unit = {}) =
            NotificationCompat.Builder(service, channelId)
                .setSmallIcon(drawable.notification_play)
                .setContentTitle(getString(string.app_name))
                .setColor(getColor(color.colorPrimary))
                .setLargeIcon(BitmapFactory.decodeResource(service.resources, mipmap.ic_launcher))

                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(TubeActivity.newInstance(service).toPendingActivity(service))

                .run {
                    block.invoke(this)
                    service.startForeground(0xA, build())
                }

    private fun NotificationCompat.Builder.addAction(@DrawableRes drawableRes: Int,
                                                     @StringRes stringRes: Int,
                                                     action: String
                                                    ) = addAction(
            NotificationCompat.Action.Builder(
                    drawableRes,
                    getString(stringRes),
                    Intent(action).toPendingBroadcast(service, action.hashCode()))
                .build())

    private fun getString(@StringRes stringRes: Int) = service.getString(stringRes)
    private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(service, colorRes)

    override fun onLoadVideo(video: TubeVideo, playlist: TubePlaylist?) {
    }

    override fun onPlay(video: TubeVideo, playlist: TubePlaylist?) {
        startForeground {
            setSmallIcon(drawable.notification_play)

            addAction(drawable.svg_pause, string.pause, TubeState.ACTION_PAUSE)
            addAction(drawable.svg_toggle, string.toggle, TubeState.ACTION_TOGGLE)
        }
    }

    override fun onPause(video: TubeVideo, playlist: TubePlaylist?) {
        startForeground {
            setSmallIcon(drawable.notification_pause)

            addAction(drawable.svg_play, string.play, TubeState.ACTION_PLAY)
            addAction(drawable.svg_close, string.close, TubeState.ACTION_CLOSE)
        }
    }
}