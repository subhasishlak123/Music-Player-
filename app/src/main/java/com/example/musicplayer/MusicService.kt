package com.example.musicplayer

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    // This is the High Quality Audio Engine (ExoPlayer)
    override fun onCreate() {
        super.onCreate()
        
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    // This tells the Android System which session to control
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // Clean up when the app is closed to prevent memory leaks
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
