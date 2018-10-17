package com.rollncode.backtube.logic

import android.net.Uri
import android.support.annotation.StringDef

class TubeUri(uri: Uri) {

    val original = uri.checkHierarchical()
    var playlistId: String = ""
        private set
    var videoId: String = ""
        private set
    var timeReference = 0
    @TubeType
    var type: String = TUBE_IGNORE
        private set

    init {
        val authority = original.authority ?: ""

        playlistId = original.getQueryParameter("list") ?: ""
        timeReference = (original.getQueryParameter("t") ?: "0").toInt()

        videoId = original.getQueryParameter("v") ?: ""
        val u = original.getQueryParameter("u") ?: ""
        if (u.isNotEmpty())
            videoId = Uri.parse(u).getQueryParameter("v") ?: ""

        if (videoId.isEmpty() &&
                (playlistId.isEmpty() && authority.contains("youtube.com") ||
                        authority.contains("youtu.be"))) {
            videoId = original.lastPathSegment ?: ""
            if (videoId.length < 11)
                videoId = ""
        }

        if (playlistId.isEmpty()) {
            if (videoId.isNotEmpty())
                type = TUBE_VIDEO

        } else {
            type = TUBE_PLAYLIST
        }

        if (type == TUBE_IGNORE) {
            playlistId = ""
            videoId = ""
            timeReference = 0
        }
    }

    override fun toString() = when (type) {
        TUBE_VIDEO    -> "[$TUBE_VIDEO]$original{$videoId}:$timeReference"
        TUBE_PLAYLIST -> "[$TUBE_PLAYLIST]$original{$playlistId/$videoId}:$timeReference"
        TUBE_IGNORE   -> "[$TUBE_IGNORE]$original"

        else          -> throw IllegalStateException("Unknown type: $type")
    }
}

private fun Uri.checkHierarchical(): Uri {
    if (isHierarchical)
        return this

    val uriString = toString()
    return Uri.parse(uriString.substring(uriString.indexOf(": ") + 2))
}

@StringDef(TUBE_VIDEO, TUBE_PLAYLIST, TUBE_IGNORE)
annotation class TubeType

const val TUBE_VIDEO = "video"
const val TUBE_PLAYLIST = "playlist"
const val TUBE_IGNORE = "ignore"
