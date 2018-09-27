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
import com.rollncode.backtube.logic.TubeState
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
    private val events = intArrayOf(TubeState.PLAY, TubeState.PAUSE, TubeState.STOP, TubeState.WINDOW_SHOW, TubeState.WINDOW_HIDE)

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

            var uri = intent.data ?: throw java.lang.IllegalStateException()
            if (!uri.isHierarchical) {
                val uriString = uri.toString()
                uri = Uri.parse(uriString.substring(uriString.indexOf(": ") + 2))
            }
            TubeState.currentUri = uri

            var id = uri.getQueryParameter("v")
            if (id.isNullOrEmpty())
                id = uri.lastPathSegment

            when {
                id?.contains("list") == true -> execute {
                    val listId = uri.getQueryParameter("list")
                    if (listId == null)
                        ReceiverBus.notify(TubeState.STOP)
                    else
                        TubeApi.requestPlaylist(listId,
                                { playerController.play(it) },
                                { ReceiverBus.notify(TubeState.STOP) })
                }
                id?.isNotBlank() == true     -> execute {
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
        toLog("TubeService.onDestroy")

        super.stopForeground(true)
        cancelJobs()

        ReceiverBus.unsubscribe(this, *events)
        ReceiverBus.notify(TubeState.CLOSE_APP)

        playerController.release()
        viewController.release()
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
}