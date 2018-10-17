package com.rollncode.backtube.player

import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.content.Context
import android.net.Uri
import com.rollncode.backtube.api.TubeApi
import com.rollncode.backtube.api.TubeVideo
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
                TubeState.PLAY, TubeState.PAUSE, TubeState.SERVICE_START,
                TubeState.SERVICE_STOP, TubeState.RELEASE,
                TubeState.WINDOW_SHOW, TubeState.WINDOW_HIDE,
                TubeState.LIST_SHOW, TubeState.LIST_HIGHLIGHT_ITEM,
                TubeState.PREVIOUS, TubeState.NEXT)
    }

    fun attachContext(context: Context) {
        this.context = context.applicationContext as Application

        if (notificationController == null) {
            notificationController = NotificationController(this.context)
            playerController = PlayerController(notificationController ?: throw NullPointerException())
            viewController = ViewController(this.context, playerController ?: throw NullPointerException())
        }
        if (context is Service) {
            this.service = SoftReference(context)
            notificationController?.context = context
        }
        notificationController?.showNotification()
    }

    fun play(context: Context, uri: Uri) {
        attachContext(context)

        val tubeUri = TubeUri(uri)
        TubeState.currentUri = tubeUri.original

        when (tubeUri.type) {
            TUBE_VIDEO    -> execute {
                TubeApi.requestVideo(tubeUri.videoId,
                        {
                            toLog("requestVideo: $it")

                            playerController?.play(it, tubeUri.timeReference)
                            ReceiverBus.notify(TubeState.LIST_SHOW, emptyList<TubeVideo>())
                        },
                        {
                            ReceiverBus.notify(TubeState.SERVICE_STOP)
                        })
            }
            TUBE_PLAYLIST -> execute {
                TubeApi.requestPlaylist(tubeUri.playlistId,
                        {
                            toLog("requestPlaylist: $it")

                            var index: Int? = null
                            var startSeconds = 0
                            var i = 0

                            for (video in it.videos) {
                                if (video.id == tubeUri.videoId) {
                                    index = i
                                    startSeconds = tubeUri.timeReference
                                    break
                                }
                                i++
                            }
                            playerController?.play(it, index, startSeconds)
                            ReceiverBus.notify(TubeState.LIST_SHOW, it.videos)
                        },
                        {
                            ReceiverBus.notify(TubeState.SERVICE_STOP)
                        })
            }
            else          -> ReceiverBus.notify(TubeState.SERVICE_STOP)
        }
        ReceiverBus.notify(TubeState.WINDOW_SHOW)
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
            TubeState.PLAY                -> when (objects.size) {
                0 -> playerController?.play()
                1 -> playerController?.play(objects.first() as Int)
                2 -> playerController?.play(objects.first() as Int, objects[1] as Int)
            }
            TubeState.PAUSE               -> playerController?.pause()
            TubeState.PREVIOUS            -> playerController?.previous()
            TubeState.NEXT                -> playerController?.next()

            TubeState.WINDOW_SHOW         -> viewController?.show()
            TubeState.WINDOW_HIDE         -> viewController?.hide()

            TubeState.LIST_SHOW           -> {
                @Suppress("UNCHECKED_CAST")
                viewController?.showList(objects.first() as List<TubeVideo>)
            }
            TubeState.LIST_HIGHLIGHT_ITEM -> {
                viewController?.highlightCurrent(objects.first() as Int)
            }
            TubeState.SERVICE_START       -> TubeService.start(context)
            TubeState.SERVICE_STOP        -> onServiceDestroy()
            TubeState.RELEASE             -> release()

            else                          -> whenDebug { throw IllegalStateException("Unknown event: $event") }
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