package com.rollncode.backtube.player

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import com.rollncode.backtube.logic.TubeUri

class TubeService : Service() {

    companion object {

        private const val ACTION_START = "com.rollncode.backtube.player.ACTION_START"

        fun start(context: Context, uri: Uri? = null) {
            val intent = Intent(context, TubeService::class.java)
            if (uri != null)
                intent.setAction(ACTION_START).data = uri

            if (VERSION.SDK_INT >= VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TubePlayer.onServiceCreated(this)

        if (intent != null && intent.action == ACTION_START)
            TubePlayer.onTubeUri(TubeUri(intent))

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        TubePlayer.onServiceDestroy()
    }
}