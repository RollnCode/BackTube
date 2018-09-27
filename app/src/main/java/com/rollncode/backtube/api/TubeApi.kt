package com.rollncode.backtube.api

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.rollncode.backtube.BuildConfig
import com.rollncode.backtube.logic.attempt
import kotlinx.coroutines.experimental.Dispatchers.Default
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async

object TubeApi {

    private const val DEFAULT_KEY = BuildConfig.YOUTUBE_APIKEY
    private const val DEFAULT_PART = "snippet,contentDetails"
    private const val DEFAULT_ATTEMPTS = 5
    private const val DEFAULT_MAX = 50L

    private val playlists = mutableMapOf<String, TubePlaylist?>()
    private val videos = mutableMapOf<String, TubeVideo?>()

    private val youtube by lazy {
        YouTube.Builder(NetHttpTransport(), JacksonFactory(), null)
            .setApplicationName("com.rollncode.backtube")
            .build()
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun requestVideo(id: String,
                             onResult: (playlist: TubeVideo) -> Unit,
                             onError: () -> Unit
                            ) = attempt(DEFAULT_ATTEMPTS,
            {
                var video = videos[id]
                if (video == null) youtube
                    .videos()
                    .list(DEFAULT_PART)
                    .apply {
                        this.id = id
                        key = DEFAULT_KEY
                    }
                    .execute()
                    .run {
                        video = TubeVideo(items.first())
                        videos[id] = video
                    }
                video?.run { onResult(this) }
            },
            {
                onError()
            })

    suspend fun requestPlaylist(id: String,
                                onResult: (playlist: TubePlaylist) -> Unit,
                                onError: () -> Unit
                               ) = attempt(DEFAULT_ATTEMPTS,
            {
                var playlist = playlists[id]
                if (playlist == null) {
                    val playlistAsync = GlobalScope.async(Default) {
                        retrievePlaylist(id)
                    }
                    val videosAsync = GlobalScope.async(Default) {
                        var nextPage: String? = null
                        val videos = mutableListOf<TubeVideo>()
                        do {
                            val pair = retrievePlaylistVideos(id, nextPage)

                            nextPage = pair.first
                            videos.addAll(pair.second)

                        } while (nextPage != null)

                        videos
                    }
                    playlist = playlistAsync.await()
                    playlist.videos = videosAsync.await()

                    playlists[id] = playlist
                }
                playlist.run { onResult(this) }
            },
            {
                onError()
            })

    @Suppress("RedundantSuspendModifier")
    private suspend fun retrievePlaylist(id: String): TubePlaylist = youtube
        .playlists()
        .list(DEFAULT_PART)
        .apply {
            this.id = id
            key = DEFAULT_KEY
            maxResults = DEFAULT_MAX
        }
        .execute()
        .run { TubePlaylist(items.first()) }

    @Suppress("RedundantSuspendModifier")
    private suspend fun retrievePlaylistVideos(id: String, nextPage: String? = null)
            : Pair<String?, List<TubeVideo>> = youtube
        .playlistItems()
        .list(DEFAULT_PART)
        .apply {
            playlistId = id
            key = DEFAULT_KEY
            maxResults = DEFAULT_MAX
            if (nextPage != null)
                pageToken = nextPage
        }
        .execute()
        .run {
            nextPageToken to items.map { TubeVideo(it) }
        }
}