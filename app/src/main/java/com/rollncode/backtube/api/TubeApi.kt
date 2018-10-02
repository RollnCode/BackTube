package com.rollncode.backtube.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.rollncode.backtube.BuildConfig
import com.rollncode.backtube.logic.attempt
import com.rollncode.utility.ICoroutines
import java.net.HttpURLConnection
import java.net.URL

@Suppress("RedundantSuspendModifier")
object TubeApi {

    private const val DEFAULT_KEY = BuildConfig.YOUTUBE_APIKEY
    private const val DEFAULT_PART = "snippet,contentDetails"
    private const val DEFAULT_ATTEMPTS = 3
    private const val DEFAULT_MAX = 50L

    private val playlistCache = mutableMapOf<String, TubePlaylist?>()
    private val videoCache = mutableMapOf<String, TubeVideo?>()

    private val youtube by lazy {
        YouTube.Builder(NetHttpTransport(), JacksonFactory(), null)
            .setApplicationName(BuildConfig.APPLICATION_ID)
            .build()
    }

    suspend fun requestBitmap(url: String): Bitmap =
            (URL(url).openConnection() as HttpURLConnection)
                .apply { doInput = true }
                .run { inputStream }
                .run { BitmapFactory.decodeStream(this) }

    suspend fun requestVideo(id: String,
                             onResult: (playlist: TubeVideo) -> Unit,
                             onError: (Exception) -> Unit) =
            attempt(DEFAULT_ATTEMPTS, {
                var video = videoCache[id]
                if (video == null) {
                    video = retrieveVideo(id)
                    videoCache[id] = video
                }
                onResult(video)
            }, onError)

    suspend fun requestPlaylist(id: String,
                                onResult: (playlist: TubePlaylist) -> Unit,
                                onError: (Exception) -> Unit) =
            attempt(DEFAULT_ATTEMPTS, {
                var playlist = playlistCache[id]
                if (playlist == null) {
                    val playlistAsync = ICoroutines.async {
                        retrievePlaylist(id)
                    }
                    val videosAsync = ICoroutines.async {
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

                    playlistCache[id] = playlist
                }
                onResult(playlist)
            }, onError)

    private suspend fun retrieveVideo(id: String)
            : TubeVideo = youtube
        .videos()
        .list(DEFAULT_PART)
        .apply {
            this.id = id
            key = DEFAULT_KEY
        }
        .execute()
        .run { TubeVideo(items.first()) }

    private suspend fun retrievePlaylist(id: String)
            : TubePlaylist = youtube
        .playlists()
        .list(DEFAULT_PART)
        .apply {
            this.id = id
            key = DEFAULT_KEY
            maxResults = DEFAULT_MAX
        }
        .execute()
        .run { TubePlaylist(items.first()) }

    private suspend fun retrievePlaylistVideos(id: String, nextPage: String?)
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
        .run { nextPageToken to items.map { TubeVideo(it) } }
}