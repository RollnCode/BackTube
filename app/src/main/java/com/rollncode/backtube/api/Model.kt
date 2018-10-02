package com.rollncode.backtube.api

import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.Video

data class TubePlaylist(
        val id: String,
        val title: String,
        val channelTitle: String,
        var videos: List<TubeVideo> = emptyList()) {

    constructor(playlist: Playlist) : this(
            playlist.snippet.channelId,
            playlist.snippet.title,
            playlist.snippet.channelTitle)
}

data class TubeVideo(
        val id: String,
        val title: String,
        val channelTitle: String,
        val thumbnail: String) {

    constructor(video: Video) : this(
            video.id,
            video.snippet.title,
            video.snippet.channelTitle,
            video.snippet.thumbnails.medium.url)

    constructor(video: PlaylistItem) : this(
            video.contentDetails.videoId,
            video.snippet.title,
            "",
            video.snippet.thumbnails.medium.url)
}