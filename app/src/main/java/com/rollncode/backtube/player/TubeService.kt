package com.rollncode.backtube.player

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder

class TubeService : Service() {

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, TubeService::class.java)

            if (VERSION.SDK_INT >= VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TubePlayer.attachContext(this)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        TubePlayer.onServiceDestroy()
    }
}