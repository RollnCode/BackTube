package com.rollncode.backtube.logic

import android.content.Intent
import android.net.Uri
import android.support.annotation.StringDef

class TubeUri(intent: Intent) {

    val original: Uri
    var id: String = ""
        private set
    @TubeType
    var type: String = TUBE_IGNORE
        private set

    init {
        original = retrieveUri(intent)
        var parameter = original.getQueryParameter("v")
        if (parameter.isNullOrEmpty())
            parameter = original.lastPathSegment

        if (parameter?.contains("list") == true) {
            parameter = original.getQueryParameter("list")
            if (parameter?.isNotEmpty() == true) {
                id = parameter
                type = TUBE_PLAYLIST
            }

        } else if (parameter?.isNotEmpty() == true) {
            id = parameter
            type = TUBE_VIDEO
        }
    }

    private fun retrieveUri(intent: Intent): Uri {
        var uri = intent.data ?: throw java.lang.IllegalStateException()
        if (!uri.isHierarchical) {
            val uriString = uri.toString()
            uri = Uri.parse(uriString.substring(uriString.indexOf(": ") + 2))
        }
        return uri
    }

    override fun toString() = "[$type]$original{$id}"
}

@StringDef(TUBE_VIDEO, TUBE_PLAYLIST, TUBE_IGNORE)
annotation class TubeType

const val TUBE_VIDEO = "video"
const val TUBE_PLAYLIST = "playlist"
const val TUBE_IGNORE = "ignore"
