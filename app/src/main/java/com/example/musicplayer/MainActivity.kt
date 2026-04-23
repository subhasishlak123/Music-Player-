package com.example.musicplayer

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

val PixelGreen = Color(0xFF1DB954)
val DarkBg = Color(0xFF0A0E0F)

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            var hasPermission by remember { mutableStateOf(false) }
            
            // Safety: Initialize Player only once
            DisposableEffect(Unit) {
                try {
                    exoPlayer = ExoPlayer.Builder(context).build()
                } catch (e: Exception) {
                    Log.e("MUSIC_APP", "Player Init Failed: ${e.message}")
                }
                onDispose { 
                    exoPlayer?.release()
                    exoPlayer = null
                }
            }

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasPermission = isGranted
                if (isGranted) { songList = fetchSongs(context) }
            }

            LaunchedEffect(Unit) {
                val permission = if (Build.VERSION.SDK_INT >= 33) 
                    Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                launcher.launch(permission)
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBg) {
                    when {
                        !hasPermission -> PermissionDeniedScreen { launcher.launch(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE) }
                        songList == null -> LoadingScreen()
                        else -> PixelfyApp(songList!!, exoPlayer)
                    }
                }
            }
        }
    }
}

@Composable
fun PixelfyApp(songs: List<Song>, player: ExoPlayer?) {
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    // Sync UI with Player State safely
    player?.let { p ->
        DisposableEffect(p) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            }
            p.addListener(listener)
            onDispose { p.removeListener(listener) }
        }
    }

    BackHandler(enabled = isFullScreen) { isFullScreen = false }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("PIXELFY", color = PixelGreen, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(songs) { song ->
                    SongTile(song) {
                        currentSong = song
                        player?.let {
                            it.setMediaItem(MediaItem.fromUri(song.uri))
                            it.prepare()
                            it.play()
                        }
                    }
                }
            }
        }

        currentSong?.let { song ->
            AnimatedVisibility(visible = true, enter = slideInVertically { it }) {
                if (isFullScreen) {
                    FullScreenPlayer(song, player, isPlaying) { isFullScreen = false }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        MiniPlayerBar(song, isPlaying, player) { isFullScreen = true }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenPlayer(song: Song, player: ExoPlayer?, isPlaying: Boolean, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(DarkBg).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClose, Modifier.align(Alignment.Start)) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(300.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A))) {
            Icon(Icons.Default.MusicNote, null, Modifier.size(100.dp).align(Alignment.Center), tint = Color.DarkGray)
        }
        Spacer(Modifier.height(40.dp))
        Text(song.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("LOCAL AUDIO", color = PixelGreen, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player?.seekToPrevious() }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            IconButton(onClick = { if (player?.isPlaying == true) player.pause() else player?.play() }) {
                Icon(if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, null, tint = PixelGreen, modifier = Modifier.size(80.dp))
            }
            IconButton(onClick = { player?.seekToNext() }) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun MiniPlayerBar(song: Song, isPlaying: Boolean, player: ExoPlayer?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(75.dp).background(PixelGreen).clickable { onClick() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).background(Color.Black))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("OPEN PLAYER", color = Color.Black.copy(0.7f), fontSize = 10.sp)
        }
        IconButton(onClick = { if (player?.isPlaying == true) player.pause() else player?.play() }) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black)
        }
    }
}

@Composable
fun SongTile(song: Song, onClick: () -> Unit) {
    Column(Modifier.clickable { onClick() }) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A))) {
            Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center), tint = Color.DarkGray)
        }
        Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun LoadingScreen() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PixelGreen) }

@Composable
fun PermissionDeniedScreen(onRetry: () -> Unit) = Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Text("Storage Permission Denied", color = Color.White)
    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PixelGreen)) { Text("Allow Access", color = Color.Black) }
}

data class Song(val title: String, val uri: Uri)

fun fetchSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
    try {
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val tIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(iIdx).toString())
                list.add(Song(cursor.getString(tIdx) ?: "Unknown", uri))
            }
        }
    } catch (e: Exception) { Log.e("MUSIC_APP", "Fetch Error: ${e.message}") }
    return list
}
