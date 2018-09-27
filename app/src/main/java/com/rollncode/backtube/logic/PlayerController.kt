package com.rollncode.backtube.logic

import android.net.Uri
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants.PlaybackQuality
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants.PlaybackRate
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants.PlayerError
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants.PlayerState
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerInitListener
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerListener
import com.rollncode.backtube.api.TubePlaylist
import com.rollncode.backtube.api.TubeVideo

class PlayerController(val listener: OnPlayerControllerListener) :
        YouTubePlayerInitListener,
        YouTubePlayerListener {

    private var player: YouTubePlayer? = null

    private var playerReady = false
    private var shouldPlay = false
    private var loadVideo = false

    private var video: TubeVideo? = null
    private var playlist: TubePlaylist? = null

    private var query = IQueryController.EMPTY

    fun play(video: TubeVideo) {
        if (this.video == video) return
        this.video = video
        playlist = null

        query = video.createQueryController()
        loadVideo = true
        play()
    }

    fun play(playlist: TubePlaylist) {
        if (this.playlist == playlist) return
        this.playlist = playlist
        video = null

        query = playlist.createQueryController()
        loadVideo = true
        play()
    }

    fun play() {
        if (query == IQueryController.EMPTY || player == null || !playerReady) {
            shouldPlay = true

        } else if (loadVideo) {
            loadVideo = false

            val video = query.current()
            player?.loadVideo(video.id, 0F)

            listener.onNewVideo(video, playlist)

        } else {
            player?.play()
        }
    }

    fun pause() {
        player?.pause()
    }

    fun release() {
        toLog("PlayerController.release")

        video = null
        playlist = null
        TubeState.currentUri = Uri.EMPTY

        playerReady = false
        shouldPlay = false
        loadVideo = false

        attempt {
            player?.removeListener(this)
            player?.pause()
            player = null
        }
    }

    override fun onInitSuccess(player: YouTubePlayer) {
        toLog("PlayerController.onInitSuccess")

        playerReady = false
        this.player = player
        player.addListener(this)
    }

    override fun onReady() {
        toLog("PlayerController.onReady")

        playerReady = true
        if (shouldPlay) {
            shouldPlay = false
            play()
        }
    }

    override fun onStateChange(state: PlayerState) {
        when (state) {
            PlayerState.PLAYING -> listener.onPlay(query.current(), playlist)
            PlayerState.PAUSED  -> listener.onPause(query.current(), playlist)
            PlayerState.ENDED   -> {
                listener.onPause(query.current(), playlist)

                if (query.toNext()) {
                    loadVideo = true
                    play()
                }
            }
            else                -> toLog("PlayerController.onStateChange: $state")
        }
    }

    override fun onPlaybackQualityChange(playbackQuality: PlaybackQuality) = Unit
    override fun onPlaybackRateChange(playbackRate: PlaybackRate) = Unit
    override fun onVideoLoadedFraction(loadedFraction: Float) = Unit
    override fun onVideoDuration(duration: Float) = Unit
    override fun onCurrentSecond(second: Float) = Unit
    override fun onError(error: PlayerError) = Unit
    override fun onVideoId(videoId: String) = Unit
    override fun onApiChange() = Unit
}

interface OnPlayerControllerListener {
    fun onNewVideo(video: TubeVideo, playlist: TubePlaylist?)
    fun onPlay(video: TubeVideo, playlist: TubePlaylist?)
    fun onPause(video: TubeVideo, playlist: TubePlaylist?)
}