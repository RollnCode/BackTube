package com.rollncode.backtube.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rollncode.backtube.logic.TubeState
import com.rollncode.backtube.logic.startActivity
import com.rollncode.backtube.logic.whenDebug
import com.rollncode.backtube.screen.TubeActivity
import com.rollncode.utility.receiver.ReceiverBus

class TubeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = when (intent.action) {
            TubeState.ACTION_PLAY   -> TubeState.PLAY
            TubeState.ACTION_PAUSE  -> TubeState.PAUSE
            TubeState.ACTION_CLOSE  -> TubeState.STOP
            TubeState.ACTION_TOGGLE ->
                if (TubeState.windowShowed)
                    TubeState.CLOSE_APP
                else
                    return TubeActivity.newInstance(context).startActivity(context)

            else                    -> return whenDebug { throw IllegalStateException("Unknown intent action: ${intent.action}") }
        }
        ReceiverBus.notify(event)
    }
}