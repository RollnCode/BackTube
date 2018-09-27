package com.rollncode.backtube.player

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import com.rollncode.backtube.api.TubeApi
import com.rollncode.backtube.logic.NotificationController
import com.rollncode.backtube.logic.PlayerController
import com.rollncode.backtube.logic.TUBE_PLAYLIST
import com.rollncode.backtube.logic.TUBE_VIDEO
import com.rollncode.backtube.logic.TubeState
import com.rollncode.backtube.logic.TubeUri
import com.rollncode.backtube.logic.ViewController
import com.rollncode.backtube.logic.toLog
import com.rollncode.backtube.logic.whenDebug
import com.rollncode.utility.ICoroutines
import com.rollncode.utility.receiver.ObjectsReceiver
import com.rollncode.utility.receiver.ReceiverBus
import kotlinx.coroutines.experimental.Job

class TubeService : Service(),
        ObjectsReceiver,
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
    private val events = intArrayOf(TubeState.PLAY, TubeState.PAUSE, TubeState.STOP,
            TubeState.WINDOW_SHOW, TubeState.WINDOW_HIDE, TubeState.PREVIOUS, TubeState.NEXT)

    private val notificationController by lazy { NotificationController(this) }
    private val playerController by lazy { PlayerController(notificationController) }
    private val viewController by lazy { ViewController(application, playerController) }

    override fun onCreate() {
        super.onCreate()
        toLog("TubeService.onCreate")

        notificationController.startForeground()
        ReceiverBus.subscribe(this, *events)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_START) {
            ReceiverBus.notify(TubeState.WINDOW_SHOW)

            val uri = TubeUri(intent)
            TubeState.currentUri = uri.original

            when (uri.type) {
                TUBE_VIDEO    -> execute {
                    TubeApi.requestVideo(uri.id,
                            { playerController.play(it) },
                            { ReceiverBus.notify(TubeState.STOP) })
                }
                TUBE_PLAYLIST -> execute {
                    TubeApi.requestPlaylist(uri.id,
                            { playerController.play(it) },
                            { ReceiverBus.notify(TubeState.STOP) })
                }
                else          -> ReceiverBus.notify(TubeState.STOP)
            }
            toLog("TubeService.ACTION_START: $uri")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        toLog("TubeService.onDestroy")

        super.stopForeground(true)
        cancelJobs()

        ReceiverBus.notify(TubeState.CLOSE_APP)
        ReceiverBus.unsubscribe(this, *events)

        notificationController.release()
        playerController.release()
        viewController.release()
    }

    override fun onObjectsReceive(event: Int, vararg objects: Any) {
        when (event) {
            TubeState.PLAY        -> playerController.play()
            TubeState.PAUSE       -> playerController.pause()
            TubeState.STOP        -> super.stopSelf()
            TubeState.PREVIOUS    -> playerController.previous()
            TubeState.NEXT        -> playerController.next()
            TubeState.WINDOW_SHOW -> viewController.show()
            TubeState.WINDOW_HIDE -> viewController.hide()

            else                  -> whenDebug { throw IllegalStateException("Unknown event: $event") }
        }
    }
}