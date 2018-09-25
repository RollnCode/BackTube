package com.rollncode.backtube

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
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants.PlaybackQuality
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants.PlaybackRate
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants.PlayerError
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants.PlayerState
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerInitListener
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerListener
import com.rollncode.receiver.ObjectsReceiver
import com.rollncode.receiver.ReceiverBus

class TubeService : Service(),
        ObjectsReceiver,
        YouTubePlayerInitListener,
        YouTubePlayerListener {

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

    private val events by lazy {
        intArrayOf(TubeState.PLAY, TubeState.PAUSE, TubeState.STOP, TubeState.WINDOW_SHOW, TubeState.WINDOW_HIDE)
    }
    private val view by lazy {
        YouTubePlayerView(this).apply {
            initialize(this@TubeService, true)
            playerUIController.run {
                showFullscreenButton(false)
                showYouTubeButton(false)
                showCustomAction1(false)
                showCustomAction2(false)
                showMenuButton(false)
            }
        }
    }

    private var player: YouTubePlayer? = null
    private var playerReady = false
    private var fromStart = false
    private var videoId = ""
        set(value) {
            field = value
            TubeState.currentVideoId = value
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
                var success = false
                attempt {
                    val uri = Uri.parse(intent.getStringExtra(EXTRA_URI))
                    var id = uri.getQueryParameter("v")
                    if (id.isNullOrEmpty())
                        id = uri.lastPathSegment

                    if (id?.isNotBlank() == true) {
                        if (videoId == id) {
                            return super.onStartCommand(intent, flags, startId)

                        } else {
                            success = true

                            fromStart = true
                            videoId = id
                        }
                    }
                    toLog(id)
                }
                if (success) {
                    ReceiverBus.notify(TubeState.WINDOW_SHOW)
                    ReceiverBus.notify(TubeState.PLAY)

                } else {
                    ReceiverBus.notify(TubeState.STOP)
                    ReceiverBus.notify(TubeState.CLOSE_APP)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        super.stopForeground(true)

        videoId = ""
        ReceiverBus.unSubscribe(this, *events)

        attempt {
            player?.pause()
            player?.removeListener(this)
            player = null
        }
        attempt {
            wm.removeViewImmediate(view)
            view.release()
        }
        toLog("onDestroy")
    }

    override fun onObjectsReceive(event: Int, vararg objects: Any) {
        when (event) {
            TubeState.PLAY        -> player?.let {
                if (!playerReady || videoId.isBlank())
                    return
                else
                    toLog("PLAY: $videoId\t$fromStart")

                if (fromStart) {
                    fromStart = false
                    it.loadVideo(videoId, 0F)

                } else {
                    it.play()
                }
            }
            TubeState.PAUSE       -> {
                player?.pause()
            }
            TubeState.STOP        -> {
                super.stopSelf()
            }
            TubeState.WINDOW_SHOW -> {
                toLog("WINDOW_SHOW")

                if (view.layoutParams == null)
                    wm.addView(view, showParams)
                else
                    wm.updateViewLayout(view, showParams)
            }
            TubeState.WINDOW_HIDE -> {
                toLog("WINDOW_HIDE")

                if (view.layoutParams == null)
                    wm.addView(view, hideParams)
                else
                    wm.updateViewLayout(view, hideParams)
            }
            else                  -> whenDebug { throw IllegalStateException("Unknown event: $event") }
        }
    }

    override fun onInitSuccess(player: YouTubePlayer) {
        toLog("onInitSuccess")

        playerReady = false
        this.player = player
        player.addListener(this)

        ReceiverBus.notify(TubeState.PLAY)
    }

    override fun onReady() {
        toLog("onReady")

        playerReady = true
        ReceiverBus.notify(TubeState.PLAY)
    }

    override fun onPlaybackQualityChange(playbackQuality: PlaybackQuality) =
            toLog("onPlaybackQualityChange: $playbackQuality", "pLog")

    override fun onVideoDuration(duration: Float) =
            toLog("onVideoDuration: $duration", "pLog")

    override fun onCurrentSecond(second: Float) =
            toLog("onCurrentSecond: $second", "pLog")

    override fun onVideoLoadedFraction(loadedFraction: Float) =
            toLog("onVideoLoadedFraction: $loadedFraction", "pLog")

    override fun onPlaybackRateChange(playbackRate: PlaybackRate) =
            toLog("onPlaybackRateChange: $playbackRate", "pLog")

    override fun onVideoId(videoId: String) =
            toLog("onVideoId: $videoId", "pLog")

    override fun onApiChange() =
            toLog("onApiChange", "pLog")

    override fun onError(error: PlayerError) =
            toLog("onError: $error", "pLog")

    override fun onStateChange(state: PlayerState) =
            toLog("onStateChange: $state", "pLog")

    private fun startForeground(block: (builder: NotificationCompat.Builder) -> Unit = {}) =
            NotificationCompat.Builder(this, createNotificationChannel())
                .setSmallIcon(R.drawable.notification_play)
                .setContentTitle(getString(R.string.app_name))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))

                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(TubeActivity.newInstance(this).toPending(this))

                .run {
                    block(this)
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
}