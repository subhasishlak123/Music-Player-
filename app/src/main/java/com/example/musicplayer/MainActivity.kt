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

// Cyber Anime Theme
val NeonCyan = Color(0xFF00FFF0)
val NeonPink = Color(0xFFFF007A)
val GlassBg = Color(255, 255, 255, 12)

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            var permissionGranted by remember { mutableStateOf(false) }
            
            // 1. Safe Player Setup
            DisposableEffect(Unit) {
                val player = ExoPlayer.Builder(context).build()
                exoPlayer = player
                onDispose { player.release() }
            }

            // 2. Permission Handling
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                permissionGranted = isGranted
                if (isGranted) { songList = fetchSongs(context) }
            }

            LaunchedEffect(Unit) {
                val permission = if (Build.VERSION.SDK_INT >= 33) 
                    Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                launcher.launch(permission)
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF050505)) {
                    when {
                        !permissionGranted -> PermissionDeniedUI { 
                            launcher.launch(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE) 
                        }
                        songList == null -> LoadingUI()
                        else -> AnimeMusicInterface(songList!!, exoPlayer!!)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeMusicInterface(songs: List<Song>, player: ExoPlayer) {
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    
    // UI State Sync
    var isPlaying by remember { mutableStateOf(false) }
    var shuffleOn by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(Player.REPEAT_MODE_OFF) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onShuffleModeEnabledChanged(s: Boolean) { shuffleOn = s }
            override fun onRepeatModeChanged(m: Int) { repeatMode = m }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    BackHandler(enabled = isFullScreen) { isFullScreen = false }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "PIXELFY",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPink)),
                    fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            AnimatedVisibility(visible = true, enter = slideInVertically { it }) {
                if (isFullScreen) {
                    FullScreenGlassPlayer(song, player, isPlaying, shuffleOn, repeatMode) { isFullScreen = false }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        NeonMiniBar(song, isPlaying, player) { isFullScreen = true }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenGlassPlayer(song: Song, player: ExoPlayer, isPlaying: Boolean, shuffle: Boolean, repeat: Int, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.92f))) {
        Column(Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onClose, Modifier.align(Alignment.Start)) {
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(40.dp))
            Surface(
                modifier = Modifier.size(310.dp).border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp), color = GlassBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(110.dp), tint = NeonPink.copy(0.2f))
                }
            }
            Spacer(Modifier.height(40.dp))
            Text(song.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("CYBER GLASS AUDIO", color = NeonCyan, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            
            // Logic Fixed Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.shuffleModeEnabled = !shuffle }) {
                    Icon(Icons.Rounded.Shuffle, null, tint = if (shuffle) NeonCyan else Color.White)
                }
                IconButton(onClick = { player.seekToPrevious() }) {
                    Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(45.dp))
                }
                IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                    Icon(if (isPlaying) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled, null, tint = NeonCyan, modifier = Modifier.size(85.dp))
                }
                IconButton(onClick = { player.seekToNext() }) {
                    Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(45.dp))
                }
                IconButton(onClick = { player.repeatMode = if (repeat == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }) {
                    Icon(if (repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if (repeat != Player.REPEAT_MODE_OFF) NeonCyan else Color.White)
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun NeonMiniBar(song: Song, isPlaying: Boolean, player: ExoPlayer, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(8.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp), color = NeonCyan
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black))
            Spacer(Modifier.width(12.dp))
            Text(song.title, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black)
            }
        }
    }
}

@Composable
fun AnimeSongTile(song: Song, onClick: () -> Unit) {
    Column(Modifier.width(160.dp).clickable { onClick() }) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(GlassBg).border(1.dp, NeonPink.copy(0.1f), RoundedCornerShape(24.dp))) {
            Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center), tint = NeonPink.copy(0.1f))
        }
        Text(song.title, color = Color.White, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun LoadingUI() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }

@Composable
fun PermissionDeniedUI(onRetry: () -> Unit) = Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Storage Permission Needed", color = Color.White)
    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) { Text("Grant Access", color = Color.Black) }
}

fun fetchSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    try {
        val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol).toString())
                list.add(Song(cursor.getString(titleCol) ?: "Unknown", uri))
            }
        }
    } catch (e: Exception) { Log.e("CRASH_FIX", "Error: ${e.message}") }
    return list
}

data class Song(val title: String, val uri: Uri)
