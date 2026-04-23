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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

// Anime Theme
val NeonCyan = Color(0xFF00FFF0)
val NeonPink = Color(0xFFFF007A)
val GlassBg = Color(255, 255, 255, 15)

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            
            // Safe Player Initialization
            DisposableEffect(Unit) {
                val player = ExoPlayer.Builder(context).build()
                exoPlayer = player
                onDispose {
                    player.release()
                    exoPlayer = null
                }
            }

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) songList = fetchSongs(context)
            }

            LaunchedEffect(Unit) {
                launcher.launch(
                    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO 
                    else Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF080808)) {
                    if (songList != null) {
                        AnimeMusicApp(songList!!, exoPlayer!!)
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeMusicApp(songs: List<Song>, player: ExoPlayer) {
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    
    // UI State variables
    var isPlaying by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(Player.REPEAT_MODE_OFF) }

    // Sync UI with Player engine
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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "PIXELFY",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPink)),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(Modifier.height(20.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(songs) { song ->
                    AnimeSongTile(song) {
                        currentSong = song
                        player.setMediaItem(MediaItem.fromUri(song.uri))
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        currentSong?.let { song ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                if (isFullScreen) {
                    FullScreenAnimePlayer(song, player, isPlaying, isShuffle, repeatMode) { isFullScreen = false }
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
fun FullScreenAnimePlayer(song: Song, player: ExoPlayer, isPlaying: Boolean, shuffle: Boolean, repeat: Int, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.95f))) {
        Column(Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onClose, Modifier.align(Alignment.Start)) {
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            
            Spacer(Modifier.height(40.dp))
            
            Surface(
                modifier = Modifier.size(320.dp).border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(40.dp)),
                shape = RoundedCornerShape(40.dp),
                color = GlassBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(120.dp), tint = NeonPink.copy(0.2f))
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(song.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("ANIME EDITION", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.weight(1f))

            // Control Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.shuffleModeEnabled = !shuffle }) {
                    Icon(Icons.Rounded.Shuffle, null, tint = if (shuffle) NeonCyan else Color.White)
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
                IconButton(onClick = { 
                    player.repeatMode = if (repeat == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF 
                }) {
                    Icon(if (repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if (repeat != Player.REPEAT_MODE_OFF) NeonCyan else Color.White)
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun MiniPlayerBar(song: Song, isPlaying: Boolean, player: ExoPlayer, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(8.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = NeonCyan
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("TAP TO EXPAND", color = Color.Black.copy(0.6f), fontSize = 10.sp)
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
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(GlassBg).border(1.dp, NeonPink.copy(0.1f), RoundedCornerShape(28.dp))) {
            Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center), tint = NeonPink.copy(0.1f))
        }
        Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
    }
}

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
    } catch (e: Exception) {}
    return list
}

data class Song(val title: String, val uri: Uri)
