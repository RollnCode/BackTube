package com.rollncode.backtube

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.experimental.async

object TubeHelper {

    private const val DEFAULT_KEY = BuildConfig.YOUTUBE_APIKEY
    private const val DEFAULT_PART = "snippet,contentDetails"
    private const val DEFAULT_ATTEMPTS = 5
    private const val DEFAULT_MAX = 50L

    private val youtube by lazy {
        YouTube.Builder(NetHttpTransport(), JacksonFactory(), null)
            .setApplicationName("com.rollncode.backtube")
            .build()
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun requestVideo(id: String, onResult: (playlist: TubeVideo) -> Unit) {
        attempt(DEFAULT_ATTEMPTS) {
            youtube
                .videos()
                .list(DEFAULT_PART)
                .apply {
                    this.id = id
                    key = DEFAULT_KEY
                }
                .execute()
                .run {
                    val it = items.first()
                    onResult(TubeVideo(it.id, it.snippet.title))
                }
        }
    }

    suspend fun requestPlaylist(id: String, onResult: (playlist: TubePlaylist) -> Unit) {
        attempt(DEFAULT_ATTEMPTS) {
            val playlistAsync = async {
                retrievePlaylist(id)
            }
            val videosAsync = async {
                var nextPage: String? = null
                val videos = mutableListOf<TubeVideo>()
                do {
                    val pair = retrievePlaylistVideos(id, nextPage)

                    nextPage = pair.first
                    videos.addAll(pair.second)

                } while (nextPage != null)

                videos
            }
            val playlist = playlistAsync.await()
            playlist.videos = videosAsync.await()

            onResult.invoke(playlist)
        }
    }

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
        .run {
            val it = items.first()
            TubePlaylist(
                    it.snippet.channelId,
                    it.snippet.title,
                    it.snippet.channelTitle
                        )
        }

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
            nextPageToken to items.map { TubeVideo(it.contentDetails.videoId, it.snippet.title) }
        }
}

data class TubePlaylist(
        val id: String,
        val title: String,
        val channelTitle: String,
        var videos: List<TubeVideo> = emptyList()
                       )

data class TubeVideo(
        val id: String,
        val title: String
                    )