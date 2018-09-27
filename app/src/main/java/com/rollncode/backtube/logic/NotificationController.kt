package com.rollncode.backtube.logic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.rollncode.backtube.R
import com.rollncode.backtube.api.TubeApi
import com.rollncode.backtube.api.TubePlaylist
import com.rollncode.backtube.api.TubeVideo
import com.rollncode.backtube.screen.TubeActivity
import com.rollncode.utility.ICoroutines
import kotlinx.coroutines.experimental.Job

class NotificationController(private val service: Service) : OnPlayerControllerListener, ICoroutines {

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

    private val defaultLargeIcon by lazy { BitmapFactory.decodeResource(service.resources, R.mipmap.ic_launcher) }
    override val jobs = mutableSetOf<Job>()
    private var state = NotificationState()
    private val notificationId = 0xA

    fun release() {
        cancelJobs()
    }

    fun startForeground(onBuilder: NotificationCompat.Builder.() -> Unit = { }) =
            NotificationCompat.Builder(service, channelId)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.notification_play)
                .setColor(getColor(R.color.colorPrimary))
                .setLargeIcon(defaultLargeIcon)

                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(TubeActivity.newInstance(service).toPendingActivity(service))

                .run {
                    onBuilder.invoke(this)
                    service.startForeground(notificationId, build())
                }

    private fun getString(@StringRes stringRes: Int) = service.getString(stringRes)
    private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(service, colorRes)

    override fun onNewVideo(video: TubeVideo, playlist: TubePlaylist?) {
        state = NotificationState(
                video.title,
                if (playlist == null)
                    ""
                else
                    "${playlist.title} - ${playlist.videos.indexOf(video) + 1} of ${playlist.videos.size}")
        execute {
            attempt(2) {
                state.largeIcon = TubeApi.requestBitmap(video.thumbnail)
                redrawNotification()
            }
        }
    }

    override fun onPlay(video: TubeVideo, playlist: TubePlaylist?) {
        state.play = true
        state.smallIcon = R.drawable.notification_play

        redrawNotification()
    }

    override fun onPause(video: TubeVideo, playlist: TubePlaylist?) {
        state.play = false
        state.smallIcon = R.drawable.notification_pause

        redrawNotification()
    }

    private fun redrawNotification() = startForeground {
        setSmallIcon(state.smallIcon)
        setLargeIcon(state.largeIcon)
        setContentTitle(state.videoTitle)
        setContentText(state.playlistTitle)

        setDeleteIntent(TubeActivity.newInstance(service).toPendingActivity(service))

        if (state.play) {
            addAction(R.drawable.svg_pause, R.string.pause, TubeState.ACTION_PAUSE)
            addAction(R.drawable.svg_toggle, R.string.toggle, TubeState.ACTION_TOGGLE)

        } else {
            addAction(R.drawable.svg_play, R.string.play, TubeState.ACTION_PLAY)
            addAction(R.drawable.svg_close, R.string.close, TubeState.ACTION_CLOSE)
        }
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
}

private data class NotificationState(
        val videoTitle: String = "",
        val playlistTitle: String = "",
        @DrawableRes var smallIcon: Int = R.drawable.notification_play,
        var largeIcon: Bitmap? = null,
        var play: Boolean = false
                                    )