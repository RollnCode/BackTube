package com.rollncode.backtube.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
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
import com.rollncode.backtube.logic.ViewController
import com.rollncode.backtube.logic.toLog
import com.rollncode.backtube.logic.toPendingActivity
import com.rollncode.backtube.logic.toPendingBroadcast
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

        private const val ACTION_START = "com.rollncode.backtube.player.ACTION_START"

        fun start(context: Context, uri: Uri) {
            val intent = Intent(context, TubeService::class.java)
                .setAction(ACTION_START)
                .setData(uri)

            if (VERSION.SDK_INT >= VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }
    }

    override val jobs = mutableSetOf<Job>()
    private val events = intArrayOf(TubeState.PLAY, TubeState.PAUSE, TubeState.STOP, TubeState.WINDOW_SHOW, TubeState.WINDOW_HIDE)

    private val playerController by lazy { PlayerController(this) }
    private val viewController by lazy { ViewController(application, playerController) }

    override fun onCreate() {
        super.onCreate()

        startForeground()
        ReceiverBus.subscribe(this, *events)

        toLog("TubeService.onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_START) {
            ReceiverBus.notify(TubeState.WINDOW_SHOW)

            val uri = intent.data ?: throw java.lang.IllegalStateException()
            var id = uri.getQueryParameter("v")
            if (id.isNullOrEmpty())
                id = uri.lastPathSegment

            when {
                id?.contains("list") == true -> execute {
                    TubeState.currentVideoId = id

                    val listId = uri.getQueryParameter("list")
                    if (listId == null)
                        ReceiverBus.notify(TubeState.STOP)
                    else
                        TubeApi.requestPlaylist(listId,
                                { playerController.play(it) },
                                { ReceiverBus.notify(TubeState.STOP) })
                }
                id?.isNotBlank() == true     -> execute {
                    TubeState.currentVideoId = id

                    TubeApi.requestVideo(id,
                            { playerController.play(it) },
                            { ReceiverBus.notify(TubeState.STOP) })
                }
                else                         -> ReceiverBus.notify(TubeState.STOP)
            }
            toLog("TubeService.ACTION_START: $id")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        super.stopForeground(true)

        cancelJobs()

        ReceiverBus.unSubscribe(this, *events)
        ReceiverBus.notify(TubeState.CLOSE_APP)

        playerController.release()
        viewController.release()

        toLog("TubeService.onDestroy")
    }

    override fun onObjectsReceive(event: Int, vararg objects: Any) {
        when (event) {
            TubeState.PLAY        -> playerController.play()
            TubeState.PAUSE       -> playerController.pause()
            TubeState.STOP        -> super.stopSelf()
            TubeState.WINDOW_SHOW -> viewController.show()
            TubeState.WINDOW_HIDE -> viewController.hide()

            else                  -> whenDebug { throw IllegalStateException("Unknown event: $event") }
        }
    }

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