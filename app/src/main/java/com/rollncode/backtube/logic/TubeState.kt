package com.rollncode.backtube.logic

import android.net.Uri
import com.rollncode.backtube.R

object TubeState {

    var currentUri: Uri = Uri.EMPTY
    var windowShowed = false

    const val PLAY = R.id.code_play
    const val PAUSE = R.id.code_pause
    const val PREVIOUS = R.id.code_previous
    const val NEXT = R.id.code_next

    const val WINDOW_SHOW = R.id.code_show
    const val WINDOW_HIDE = R.id.code_hide

    const val SERVICE_START = R.id.code_service_start
    const val SERVICE_STOP = R.id.code_service_stop
    const val RELEASE = R.id.code_release

    const val NOTHING = R.id.code_nothing
    const val OPEN_TUBE = R.id.code_tube
    const val CLOSE_APP = R.id.code_close
    const val OPEN_YOUTUBE = R.id.code_youtube
    const val REQUEST_OVERDRAW = R.id.code_overdraw

    const val ACTION_PLAY = "com.rollncode.backtube.ACTION_0"
    const val ACTION_PAUSE = "com.rollncode.backtube.ACTION_1"
    const val ACTION_CLOSE = "com.rollncode.backtube.ACTION_2"
    const val ACTION_TOGGLE = "com.rollncode.backtube.ACTION_3"
    const val ACTION_PREVIOUS = "com.rollncode.backtube.ACTION_4"
    const val ACTION_NEXT = "com.rollncode.backtube.ACTION_5"
}