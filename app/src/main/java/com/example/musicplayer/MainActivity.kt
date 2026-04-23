package com.example.musicplayer

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

// Anime Theme Colors
val NeonPink = Color(0xFFFF2E63)
val NeonCyan = Color(0xFF08D9D6)
val GlassBg = Color(255, 255, 255, 15)

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            
            // Player & MediaSession (For Notifications)
            DisposableEffect(Unit) {
                val player = ExoPlayer.Builder(context).build()
                exoPlayer = player
                mediaSession = MediaSession.Builder(context, player).build()
                
                onDispose {
                    mediaSession?.release()
                    exoPlayer?.release()
                }
            }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) songList = fetchSongs(context)
            }

            LaunchedEffect(Unit) {
                launcher.launch(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (songList != null) {
                AnimeMusicPlayer(songList!!, exoPlayer!!)
            }
        }
    }
}

@Composable
fun AnimeMusicPlayer(songs: List<Song>, player: ExoPlayer) {
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(Player.REPEAT_MODE_OFF) }

    // Sync State with Player
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { isShuffle = enabled }
            override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    BackHandler(enabled = isFullScreen) { isFullScreen = false }

    Box(Modifier.fillMaxSize().background(Color(0xFF121212))) {
        // Main Library
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("PIXELFY", color = NeonCyan, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(songs) { song ->
                    // Corrected: Only the tile is clickable
                    AnimeSongTile(song) {
                        currentSong = song
                        player.setMediaItem(MediaItem.fromUri(song.uri))
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        // Full Screen Glass Overlay
        currentSong?.let { song ->
            AnimatedVisibility(visible = true, enter = slideInVertically { it }) {
                if (isFullScreen) {
                    FullScreenAnimePlayer(song, player, isPlaying, isShuffle, repeatMode) { isFullScreen = false }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        AnimeMiniBar(song, isPlaying, player) { isFullScreen = true }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenAnimePlayer(song: Song, player: ExoPlayer, isPlaying: Boolean, isShuffle: Boolean, repeatMode: Int, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.9f))) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onClose, Modifier.align(Alignment.Start)) {
                Icon(Icons.Rounded.ExpandMore, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Glass Morphism Art
            Surface(
                modifier = Modifier.size(300.dp).border(2.dp, NeonCyan, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = GlassBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(100.dp), tint = NeonCyan.copy(0.3f))
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(song.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("ANIME EDITION", color = NeonPink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.weight(1f))

            // Logic-Fixed Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.shuffleModeEnabled = !isShuffle }) {
                    Icon(Icons.Rounded.Shuffle, null, tint = if (isShuffle) NeonCyan else Color.White)
                }
                IconButton(onClick = { player.seekToPrevious() }) {
                    Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                    Icon(if (isPlaying) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled, null, tint = NeonCyan, modifier = Modifier.size(88.dp))
                }
                IconButton(onClick = { player.seekToNext() }) {
                    Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = { player.repeatMode = if (repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }) {
                    Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) NeonCyan else Color.White)
                }
            }
            Spacer(Modifier.height(50.dp))
        }
    }
}

@Composable
fun AnimeMiniBar(song: Song, isPlaying: Boolean, player: ExoPlayer, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(8.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = NeonCyan
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.Black, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text("CLICK FOR ANIME PLAYER", color = Color.Black.copy(0.6f), fontSize = 10.sp)
            }
            IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black)
            }
        }
    }
}

@Composable
fun AnimeSongTile(song: Song, onClick: () -> Unit) {
    Column(Modifier.width(160.dp).clickable { onClick() }) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(GlassBg).border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(24.dp))) {
            Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center), tint = NeonPink.copy(0.2f))
        }
        Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 4.dp), fontWeight = FontWeight.Medium)
    }
}

fun fetchSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
    context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { cursor ->
        val tIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val iIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        while (cursor.moveToNext()) {
            list.add(Song(cursor.getString(tIdx), Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(iIdx).toString())))
        }
    }
    return list
}

data class Song(val title: String, val uri: Uri)
