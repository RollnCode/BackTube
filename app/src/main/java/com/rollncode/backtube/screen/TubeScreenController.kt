package com.rollncode.backtube.screen

import android.app.Activity
import android.arch.lifecycle.Lifecycle.Event.ON_CREATE
import android.arch.lifecycle.Lifecycle.Event.ON_DESTROY
import android.arch.lifecycle.Lifecycle.Event.ON_PAUSE
import android.arch.lifecycle.Lifecycle.Event.ON_RESUME
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.rollncode.backtube.R
import com.rollncode.backtube.logic.TubeState
import com.rollncode.backtube.logic.attempt
import com.rollncode.backtube.logic.canDrawOverlays
import com.rollncode.backtube.logic.whenDebug
import com.rollncode.backtube.player.TubeService
import com.rollncode.utility.receiver.ObjectsReceiver
import com.rollncode.utility.receiver.ReceiverBus

class TubeScreenController(private val activity: AppCompatActivity,
                           private val savedInstanceState: Bundle?) :
        ObjectsReceiver, LifecycleObserver {

    companion object {

        private const val EXTRA_INTERNAL = "EXTRA_INTERNAL"

        fun <A : Activity> newInstance(context: Context, cls: Class<A>): Intent =
                Intent(context, cls)
                    .putExtra(EXTRA_INTERNAL, true)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private val events = intArrayOf(TubeState.OPEN_YOUTUBE, TubeState.REQUEST_OVERDRAW, TubeState.OPEN_TUBE, TubeState.CLOSE_APP)
    private val requestOverlay = 0xA

    init {
        activity.lifecycle.addObserver(this)
        ReceiverBus.subscribe(this, *events)
    }

    @OnLifecycleEvent(ON_RESUME)
    private fun showPlayer() = ReceiverBus.notify(TubeState.WINDOW_SHOW)

    @OnLifecycleEvent(ON_PAUSE)
    private fun hidePlayer() = ReceiverBus.notify(TubeState.WINDOW_HIDE)

    @OnLifecycleEvent(ON_DESTROY)
    private fun unsubscribeEvents() = ReceiverBus.unsubscribe(this, *events)

    @OnLifecycleEvent(ON_CREATE)
    private fun whatNext() {
        val intent = activity.intent ?: Intent()
        val event = when {
            TubeState.currentUri.toString().isEmpty() &&
                    intent.action == Intent.ACTION_MAIN ->
                if (savedInstanceState != null && intent.getBooleanExtra(EXTRA_INTERNAL, false))
                    TubeState.NOTHING
                else
                    TubeState.OPEN_YOUTUBE
            activity.canDrawOverlays                    -> TubeState.OPEN_TUBE

            else                                        -> TubeState.REQUEST_OVERDRAW
        }
        ReceiverBus.notify(event, intent)
    }

    fun closeBackTube() {
        ReceiverBus.notify(TubeState.STOP)
        ReceiverBus.notify(TubeState.CLOSE_APP)
    }

    fun onActivityResult(requestCode: Int) {
        if (requestCode == requestOverlay)
            ReceiverBus.notify(if (activity.canDrawOverlays)
                TubeState.OPEN_TUBE
            else
                TubeState.CLOSE_APP, activity.intent)
    }

    override fun onObjectsReceive(event: Int, vararg objects: Any) = when (event) {
        TubeState.OPEN_TUBE        -> {
            val intent = objects.first() as Intent
            var uri: Uri? = intent.data
            if (uri == null)
                attempt { uri = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)) }
            if (uri == null)
                uri = TubeState.currentUri

            if (uri == null || uri.toString().isEmpty())
                ReceiverBus.notify(TubeState.CLOSE_APP)
            else
                TubeService.start(activity, uri!!)
        }
        TubeState.CLOSE_APP        -> activity.finish()
        TubeState.OPEN_YOUTUBE     -> {
            val youtube = Intent(Intent.ACTION_MAIN)
                .setPackage("com.google.android.youtube")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (youtube.resolveActivity(activity.packageManager) == null)
                Toast.makeText(activity, R.string.no_youtube, Toast.LENGTH_LONG).show()
            else
                activity.startActivity(youtube)

            ReceiverBus.notify(TubeState.CLOSE_APP)
        }
        TubeState.REQUEST_OVERDRAW -> {
            if (VERSION.SDK_INT >= VERSION_CODES.M)
                activity.startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}")), requestOverlay)
            else
                throw IllegalStateException()
        }
        else                       -> whenDebug { throw IllegalStateException("Unknown event: $event") }
    }
}