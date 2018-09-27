package com.rollncode.backtube.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayerView
import com.rollncode.backtube.R.color
import com.rollncode.backtube.R.drawable
import com.rollncode.backtube.R.mipmap
import com.rollncode.backtube.R.string
import com.rollncode.backtube.api.TubeApi
import com.rollncode.backtube.api.TubePlaylist
import com.rollncode.backtube.api.TubeVideo
import com.rollncode.backtube.logic.OnPlayerControllerListener
import com.rollncode.backtube.logic.PlayerController
import com.rollncode.backtube.logic.TubeState
import com.rollncode.backtube.logic.attempt
import com.rollncode.backtube.logic.toLog
import com.rollncode.backtube.logic.toPendingActivity
import com.rollncode.backtube.logic.toPendingBroadcast
import com.rollncode.backtube.logic.topPoint
import com.rollncode.backtube.logic.whenDebug
import com.rollncode.backtube.screen.TubeActivity
import com.rollncode.utility.ICoroutines
import com.rollncode.utility.receiver.ObjectsReceiver
import com.rollncode.utility.receiver.ReceiverBus
import kotlinx.coroutines.experimental.Job

class TubeService : Service(),
        ObjectsReceiver,
        OnPlayerControllerListener,
        ICoroutines {

    companion object {

        private const val EXTRA_URI = "EXTRA_0"

        fun start(context: Context, uri: String) {
            val intent = Intent(context, TubeService::class.java)
                .putExtra(EXTRA_URI, uri)

            if (VERSION.SDK_INT >= VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }
    }

    override val jobs = mutableSetOf<Job>()

    private val events by lazy {
        intArrayOf(TubeState.PLAY, TubeState.PAUSE, TubeState.STOP, TubeState.WINDOW_SHOW, TubeState.WINDOW_HIDE)
    }
    private val playController by lazy { PlayerController(this) }

    private val view by lazy {
        YouTubePlayerView(this).apply {
            initialize(playController, true)
            playerUIController.run {
                showFullscreenButton(false)
                showYouTubeButton(false)
                showCustomAction1(false)
                showCustomAction2(false)
                showMenuButton(false)
            }
        }
    }

    private val wm by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private val hideParams by lazy {
        createWindowLayoutParams(1, 1)
    }
    private val showParams by lazy {
        val metrics = resources.displayMetrics
        val min = Math.min(metrics.heightPixels, metrics.widthPixels)

        createWindowLayoutParams(min).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            y = topPoint
        }
    }

    override fun onCreate() {
        super.onCreate()

        startForeground()
        ReceiverBus.subscribe(this, *events)

        toLog("onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) when {
            intent.hasExtra(EXTRA_URI) -> {
                val uri = Uri.parse(intent.getStringExtra(EXTRA_URI))
                var id = uri.getQueryParameter("v")
                if (id.isNullOrEmpty())
                    id = uri.lastPathSegment

                ReceiverBus.notify(TubeState.WINDOW_SHOW)
                when {
                    id?.contains("list") == true -> execute {
                        val listId = uri.getQueryParameter("list")
                        if (listId == null)
                            ReceiverBus.notify(TubeState.STOP)
                        else
                            TubeApi.requestPlaylist(listId,
                                    { playController.play(it) },
                                    { ReceiverBus.notify(TubeState.STOP) })
                    }
                    id?.isNotBlank() == true     -> execute {
                        TubeApi.requestVideo(id,
                                { playController.play(it) },
                                { ReceiverBus.notify(TubeState.STOP) })
                    }
                    else                         -> ReceiverBus.notify(TubeState.STOP)
                }
                toLog(id)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        super.stopForeground(true)

        cancelJobs()

        TubeState.currentVideoId = ""
        TubeState.windowShowed = false

        ReceiverBus.unSubscribe(this, *events)
        ReceiverBus.notify(TubeState.CLOSE_APP)

        playController.recycle()

        attempt {
            view.release()
            wm.removeViewImmediate(view)
        }
        toLog("onDestroy")
    }

    override fun onObjectsReceive(event: Int, vararg objects: Any) {
        when (event) {
            TubeState.PLAY        -> playController.play()
            TubeState.PAUSE       -> playController.pause()
            TubeState.STOP        -> super.stopSelf()
            TubeState.WINDOW_SHOW -> {
                toLog("WINDOW_SHOW")
                TubeState.windowShowed = true

                if (view.layoutParams == null)
                    wm.addView(view, showParams)
                else
                    wm.updateViewLayout(view, showParams)
            }
            TubeState.WINDOW_HIDE -> {
                toLog("WINDOW_HIDE")
                TubeState.windowShowed = false

                if (view.layoutParams == null)
                    wm.addView(view, hideParams)
                else
                    wm.updateViewLayout(view, hideParams)
            }
            else                  -> whenDebug { throw IllegalStateException("Unknown event: $event") }
        }
    }

    override fun onLoadVideo(video: TubeVideo, playlist: TubePlaylist?) {
        TubeState.currentVideoId = video.id
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

    private fun startForeground(block: NotificationCompat.Builder.() -> Unit = {}) =
            NotificationCompat.Builder(this, createNotificationChannel())
                .setSmallIcon(drawable.notification_play)
                .setContentTitle(getString(string.app_name))
                .setColor(ContextCompat.getColor(this, color.colorPrimary))
                .setLargeIcon(BitmapFactory.decodeResource(resources, mipmap.ic_launcher))

                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(TubeActivity.newInstance(this).toPendingActivity(this))

                .run {
                    block.invoke(this)
                    super.startForeground(0xA, build())
                }

    private fun createNotificationChannel(): String {
        val channelId = "backTubePlayer"
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_NONE)
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            channel.lightColor = Color.BLUE

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return channelId
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

    private fun NotificationCompat.Builder.addAction(@DrawableRes drawableRes: Int,
                                                     @StringRes stringRes: Int,
                                                     action: String
                                                    ) = addAction(
            NotificationCompat.Action.Builder(
                    drawableRes,
                    getString(stringRes),
                    Intent(action).toPendingBroadcast(applicationContext, action.hashCode()))
                .build())
}