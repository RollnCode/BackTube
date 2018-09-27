package com.rollncode.backtube.logic

import android.net.Uri
import com.rollncode.backtube.R.id

object TubeState {

    var currentUri: Uri = Uri.EMPTY
    var windowShowed = false

    const val PLAY = id.code_play
    const val PAUSE = id.code_pause
    const val STOP = id.code_stop
    const val WINDOW_SHOW = id.code_show
    const val WINDOW_HIDE = id.code_hide

    const val NOTHING = id.code_nothing
    const val OPEN_TUBE = id.code_tube
    const val CLOSE_APP = id.code_close
    const val OPEN_YOUTUBE = id.code_youtube
    const val REQUEST_OVERDRAW = id.code_overdraw

    const val ACTION_PLAY = "com.rollncode.backtube.ACTION_0"
    const val ACTION_PAUSE = "com.rollncode.backtube.ACTION_1"
    const val ACTION_CLOSE = "com.rollncode.backtube.ACTION_2"
    const val ACTION_TOGGLE = "com.rollncode.backtube.ACTION_3"
}