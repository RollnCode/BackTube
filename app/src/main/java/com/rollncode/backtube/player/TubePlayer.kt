package com.rollncode.backtube.player

import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import com.rollncode.backtube.api.TubeApi
import com.rollncode.backtube.logic.NotificationController
import com.rollncode.backtube.logic.PlayerController
import com.rollncode.backtube.logic.TUBE_PLAYLIST
import com.rollncode.backtube.logic.TUBE_VIDEO
import com.rollncode.backtube.logic.TubeState
import com.rollncode.backtube.logic.TubeUri
import com.rollncode.backtube.logic.ViewController
import com.rollncode.backtube.logic.whenDebug
import com.rollncode.utility.ICoroutines
import com.rollncode.utility.receiver.ObjectsReceiver
import com.rollncode.utility.receiver.ReceiverBus
import kotlinx.coroutines.experimental.Job
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

object TubePlayer : ObjectsReceiver, ICoroutines {

    private lateinit var context: Application
    private var service: Reference<Service> = WeakReference<Service>(null)

    override val jobs = mutableSetOf<Job>()

    @SuppressLint("StaticFieldLeak")
    private var notificationController: NotificationController? = null
    private var playerController: PlayerController? = null
    private var viewController: ViewController? = null

    init {
        ReceiverBus.subscribe(this,
                TubeState.PLAY, TubeState.PAUSE, TubeState.SERVICE_START, TubeState.SERVICE_STOP, TubeState.RELEASE,
                TubeState.WINDOW_SHOW, TubeState.WINDOW_HIDE, TubeState.PREVIOUS, TubeState.NEXT)
    }

    fun onServiceCreated(service: Service) {
        context = service.application
        this.service = SoftReference(service)

        if (notificationController == null) {
            notificationController = NotificationController(service)
            playerController = PlayerController(notificationController ?: throw NullPointerException())
            viewController = ViewController(context, playerController ?: throw NullPointerException())
        }
        notificationController?.context = service
        notificationController?.showNotification()
    }

    fun onTubeUri(uri: TubeUri) {
        ReceiverBus.notify(TubeState.WINDOW_SHOW)
        TubeState.currentUri = uri.original

        when (uri.type) {
            TUBE_VIDEO    -> execute {
                TubeApi.requestVideo(uri.id,
                        { playerController?.play(it) },
                        { ReceiverBus.notify(TubeState.SERVICE_STOP) })
            }
            TUBE_PLAYLIST -> execute {
                TubeApi.requestPlaylist(uri.id,
                        { playerController?.play(it) },
                        { ReceiverBus.notify(TubeState.SERVICE_STOP) })
            }
            else          -> ReceiverBus.notify(TubeState.SERVICE_STOP)
        }
    }

    fun onServiceDestroy() {
        service.get()?.run {
            stopForeground(false)
            service.clear()
        }
        notificationController?.context = context
        notificationController?.showNotification()
    }

    override fun onObjectsReceive(event: Int, vararg objects: Any) {
        when (event) {
            TubeState.PLAY          -> playerController?.play()
            TubeState.PAUSE         -> playerController?.pause()
            TubeState.PREVIOUS      -> playerController?.previous()
            TubeState.NEXT          -> playerController?.next()

            TubeState.WINDOW_SHOW   -> viewController?.show()
            TubeState.WINDOW_HIDE   -> viewController?.hide()

            TubeState.SERVICE_START -> TubeService.start(context)
            TubeState.SERVICE_STOP  -> onServiceDestroy()
            TubeState.RELEASE       -> release()

            else                    -> whenDebug { throw IllegalStateException("Unknown event: $event") }
        }
    }

    private fun release() {
        ReceiverBus.notify(TubeState.CLOSE_APP)
        service.clear()
        cancelJobs()

        notificationController?.release()
        playerController?.release()
        viewController?.release()

        notificationController = null
        playerController = null
        viewController = null
    }
}