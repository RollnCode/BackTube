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
import com.rollncode.utility.receiver.ReceiverBus
import kotlinx.coroutines.experimental.Job

class NotificationController(var context: Context) : OnPlayerControllerListener, ICoroutines {

    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val channelId by lazy {
        val channelId = "com.rollncode.backtube"
        if (VERSION.SDK_INT >= VERSION_CODES.O) notificationManager.createNotificationChannel(
                NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        lightColor = getColor(R.color.colorPrimary)
                    })

        channelId
    }

    override val jobs = mutableSetOf<Job>()
    private val notificationId = 0xA

    private var state = NotificationState(getString(R.string.app_name),
            getString(R.string.loading),
            largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))

    fun release() {
        context = context.applicationContext
        cancelJobs()
    }

    fun showNotification(onBuilder: NotificationCompat.Builder.() -> Unit = { }) =
            NotificationCompat.Builder(context, channelId)
                .setColor(getColor(R.color.colorPrimary))
                .setSmallIcon(state.smallIcon)
                .setLargeIcon(state.largeIcon)
                .setContentTitle(state.videoTitle)
                .setContentText(state.playlistTitle)

                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setDeleteIntent(getPendingBroadcast(TubeState.ACTION_CLOSE))
                .setContentIntent(getPendingBroadcast(TubeState.ACTION_TOGGLE))

                .run {
                    val (layoutSmall, layoutBig) =
                            if (state.single)
                                R.layout.notification_player_single_small to R.layout.notification_player_single_big
                            else
                                R.layout.notification_player_playlist_small to R.layout.notification_player_playlist_big

                    val modifications: RemoteViews.() -> Unit = {
                        setViewVisibility(R.id.btn_pause, if (state.play) View.VISIBLE else View.GONE)
                        setViewVisibility(R.id.btn_play, if (state.play) View.GONE else View.VISIBLE)

                        setTextViewText(R.id.tv_video, state.videoTitle)
                        setTextViewText(R.id.tv_playlist, state.playlistTitle)

                        if (state.largeIcon != null)
                            setImageViewBitmap(R.id.iv_cover, state.largeIcon)

                        setOnClickPendingIntent(R.id.btn_previous, getPendingBroadcast(TubeState.ACTION_PREVIOUS))
                        setOnClickPendingIntent(R.id.btn_play, getPendingBroadcast(TubeState.ACTION_PLAY))
                        setOnClickPendingIntent(R.id.btn_pause, getPendingBroadcast(TubeState.ACTION_PAUSE))
                        setOnClickPendingIntent(R.id.btn_next, getPendingBroadcast(TubeState.ACTION_NEXT))
                    }
                    RemoteViews(context.packageName, layoutSmall)
                        .apply(modifications)
                        .run { setCustomContentView(this) }

                    RemoteViews(context.packageName, layoutBig)
                        .apply(modifications)
                        .run { setCustomBigContentView(this) }

                    onBuilder.invoke(this)

                    val ctx = context
                    if (ctx is Service)
                        ctx.startForeground(notificationId, build())
                    else
                        notificationManager.notify(notificationId, build())
                }

    private fun getString(@StringRes stringRes: Int) = context.getString(stringRes)
    private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(context, colorRes)

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
                showNotification()
            }
        }
    }

    override fun onPlay(video: TubeVideo, playlist: TubePlaylist?) {
        state.play = true
        state.smallIcon = R.drawable.notification_play

        showNotification()
        ReceiverBus.notify(TubeState.SERVICE_START)
    }

    override fun onPause(video: TubeVideo, playlist: TubePlaylist?) {
        state.play = false
        state.smallIcon = R.drawable.notification_pause

        showNotification()
        ReceiverBus.notify(TubeState.SERVICE_STOP)
    }

    private fun getPendingBroadcast(action: String) = Intent(action).toPendingBroadcast(context, action.hashCode())
}

private data class NotificationState(
        val videoTitle: String = "",
        val playlistTitle: String = "",
        val single: Boolean = true,
        @DrawableRes var smallIcon: Int = R.drawable.notification_play,
        var largeIcon: Bitmap? = null,
        var play: Boolean = false
                                    )