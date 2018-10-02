package com.rollncode.backtube.logic

import android.net.Uri
import android.support.annotation.StringDef

class TubeUri(uri: Uri) {

    val original = uri.checkHierarchical()
    var id: String = ""
        private set
    @TubeType
    var type: String = TUBE_IGNORE
        private set

    init {
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

    override fun toString() = "[$type]$original{$id}"
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
