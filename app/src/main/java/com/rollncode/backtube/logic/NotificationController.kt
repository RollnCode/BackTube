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
import android.view.View
import android.widget.RemoteViews
import com.rollncode.backtube.R
import com.rollncode.backtube.api.TubeApi
import com.rollncode.backtube.api.TubePlaylist
import com.rollncode.backtube.api.TubeVideo
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

    override val jobs = mutableSetOf<Job>()

    private val defaultLargeIcon by lazy { BitmapFactory.decodeResource(service.resources, R.mipmap.ic_launcher) }
    private var state = NotificationState(getString(R.string.app_name), getString(R.string.loading))

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
                .setContentIntent(getPendingBroadcast(TubeState.ACTION_TOGGLE))

                .run {
                    val (layoutSmall, layoutBig) =
                            if (state.single)
                                R.layout.notification_player_single_small to R.layout.notification_player_single_big
                            else
                                R.layout.notification_player_playlist_small to R.layout.notification_player_playlist_big

                    val modifications: RemoteViews.() -> Unit = {
                        setViewVisibility(R.id.btn_pause, if (state.play) View.VISIBLE else View.GONE)
                        setViewVisibility(R.id.btn_close, if (state.play) View.INVISIBLE else View.VISIBLE)
                        setViewVisibility(R.id.btn_play, if (state.play) View.GONE else View.VISIBLE)

                        setTextViewText(R.id.tv_video, state.videoTitle)
                        setTextViewText(R.id.tv_playlist, state.playlistTitle)

                        if (state.largeIcon != null)
                            setImageViewBitmap(R.id.iv_cover, state.largeIcon)

                        setOnClickPendingIntent(R.id.btn_previous, getPendingBroadcast(TubeState.ACTION_PREVIOUS))
                        setOnClickPendingIntent(R.id.btn_play, getPendingBroadcast(TubeState.ACTION_PLAY))
                        setOnClickPendingIntent(R.id.btn_pause, getPendingBroadcast(TubeState.ACTION_PAUSE))
                        setOnClickPendingIntent(R.id.btn_next, getPendingBroadcast(TubeState.ACTION_NEXT))
                        setOnClickPendingIntent(R.id.btn_close, getPendingBroadcast(TubeState.ACTION_CLOSE))
                    }
                    RemoteViews(service.packageName, layoutSmall)
                        .apply(modifications)
                        .run { setCustomContentView(this) }

                    RemoteViews(service.packageName, layoutBig)
                        .apply(modifications)
                        .run { setCustomBigContentView(this) }

                    onBuilder.invoke(this)
                    service.startForeground(0xA, build())
                }

    private fun getString(@StringRes stringRes: Int) = service.getString(stringRes)
    private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(service, colorRes)

    override fun onNewVideo(video: TubeVideo, playlist: TubePlaylist?) {
        state = NotificationState(
                video.title,
                if (playlist == null)
                    ""
                else
                    "${playlist.title} - ${playlist.videos.indexOf(video) + 1} of ${playlist.videos.size}",
                playlist == null)
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
    }

    private fun getPendingBroadcast(action: String) = Intent(action).toPendingBroadcast(service, action.hashCode())
}

private data class NotificationState(
        val videoTitle: String = "",
        val playlistTitle: String = "",
        val single: Boolean = true,
        @DrawableRes var smallIcon: Int = R.drawable.notification_play,
        var largeIcon: Bitmap? = null,
        var play: Boolean = false
                                    )