package com.rollncode.backtube.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.rollncode.backtube.R
import com.rollncode.backtube.R.id
import com.rollncode.backtube.logic.TubeState
import com.rollncode.backtube.logic.attempt
import com.rollncode.backtube.logic.canDrawOverlays
import com.rollncode.backtube.logic.whenDebug
import com.rollncode.backtube.player.TubeService
import com.rollncode.utility.receiver.ObjectsReceiver
import com.rollncode.utility.receiver.ReceiverBus

class TubeActivity : AppCompatActivity(), ObjectsReceiver {

    companion object {

        private const val EXTRA_INTERNAL = "EXTRA_0"

        fun newInstance(context: Context): Intent =
                Intent(context, TubeActivity::class.java)
                    .putExtra(EXTRA_INTERNAL, true)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private val events by lazy {
        intArrayOf(TubeState.OPEN_YOUTUBE, TubeState.REQUEST_OVERDRAW, TubeState.OPEN_TUBE, TubeState.CLOSE_APP)
    }
    private val requestOverlay = 0xA

    @SuppressLint("InlinedApi")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        val intent = super.getIntent() ?: Intent()
        val event = when {
            TubeState.currentUri.toString().isEmpty() &&
                    intent.action == Intent.ACTION_MAIN ->
                if (b != null && intent.getBooleanExtra(EXTRA_INTERNAL, false))
                    TubeState.NOTHING
                else
                    TubeState.OPEN_YOUTUBE
            canDrawOverlays                             -> TubeState.OPEN_TUBE

            else                                        -> TubeState.REQUEST_OVERDRAW
        }
        ReceiverBus.subscribe(this, *events)
        ReceiverBus.notify(event)
    }

    override fun onResume() {
        super.onResume()
        ReceiverBus.notify(TubeState.WINDOW_SHOW)
    }

    override fun onPause() {
        super.onPause()
        ReceiverBus.notify(TubeState.WINDOW_HIDE)
    }

    override fun onDestroy() {
        super.onDestroy()
        ReceiverBus.unSubscribe(this, *events)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_close, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        id.menu_close -> {
            ReceiverBus.notify(TubeState.STOP)
            ReceiverBus.notify(TubeState.CLOSE_APP)
            true
        }
        else          -> super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == requestOverlay)
            ReceiverBus.notify(if (canDrawOverlays) TubeState.OPEN_TUBE else TubeState.CLOSE_APP)
        else
            super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onObjectsReceive(event: Int, vararg objects: Any) = when (event) {
        TubeState.OPEN_TUBE        -> {
            val intent = super.getIntent() ?: Intent()
            var uri: Uri? = intent.data
            if (uri == null)
                attempt { uri = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)) }
            if (uri == null)
                uri = TubeState.currentUri

            if (uri == null || uri.toString().isEmpty())
                ReceiverBus.notify(TubeState.CLOSE_APP)
            else
                TubeService.start(this, uri!!)
        }
        TubeState.CLOSE_APP        -> {
            super.finish()
        }
        TubeState.OPEN_YOUTUBE     -> {
            val youtube = Intent(Intent.ACTION_MAIN)
                .setPackage("com.google.android.youtube")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (youtube.resolveActivity(packageManager) == null)
                Toast.makeText(this, R.string.no_youtube, Toast.LENGTH_LONG).show()
            else
                super.startActivity(youtube)

            ReceiverBus.notify(TubeState.CLOSE_APP)
        }
        TubeState.REQUEST_OVERDRAW -> {
            if (VERSION.SDK_INT >= VERSION_CODES.M)
                super.startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), requestOverlay)
            else
                throw IllegalStateException()
        }
        else                       -> whenDebug { throw IllegalStateException("Unknown event: $event") }
    }
}