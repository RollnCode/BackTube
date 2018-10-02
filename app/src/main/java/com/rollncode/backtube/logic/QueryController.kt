package com.rollncode.backtube.logic

import com.rollncode.backtube.api.TubePlaylist
import com.rollncode.backtube.api.TubeVideo
import com.rollncode.utility.receiver.ReceiverBus

interface IQueryController {

    companion object {
        val EMPTY = object : IQueryController {
            override val query: List<TubeVideo> = emptyList()
            override var currentIndex: Int = -1
            override fun current() = throw NoSuchMethodException()
            override fun toNext() = throw NoSuchMethodException()
            override fun toPrevious() = throw NoSuchMethodException()
        }
    }

    val query: List<TubeVideo>
    var currentIndex: Int

    fun current(): TubeVideo {
        ReceiverBus.notify(TubeState.LIST_HIGHLIGHT_ITEM, currentIndex)
        return query[currentIndex]
    }

    fun setCurrent(index: Int): Boolean {
        val success = index in (0 until query.size)
        if (success)
            currentIndex = index

        return success
    }

    fun toNext(): Boolean {
        val success = currentIndex + 1 < query.size
        if (success)
            currentIndex++

        return success
    }

    fun toPrevious(): Boolean {
        val success = currentIndex > 0
        if (success)
            currentIndex--

        return success
    }
}

fun TubePlaylist.createQueryController() = object : IQueryController {
    override val query: List<TubeVideo> = videos
    override var currentIndex: Int = 0
}

fun TubeVideo.createQueryController() = object : IQueryController {
    override val query: List<TubeVideo> = listOf(this@createQueryController)
    override var currentIndex: Int = 0
}