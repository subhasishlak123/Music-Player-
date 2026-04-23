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

val PixelGreen = Color(0xFF1DB954)
val GlassWhite = Color(255, 255, 255, 20) // Transparency for Glass effect

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            
            DisposableEffect(Unit) {
                exoPlayer = ExoPlayer.Builder(context).build()
                onDispose { exoPlayer?.release() }
            }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) songList = fetchSongs(context)
            }

            LaunchedEffect(Unit) {
                launcher.launch(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (songList != null) {
                PixelfyApp(songList!!, exoPlayer!!)
            }
        }
    }
}

@Composable
fun PixelfyApp(songs: List<Song>, player: ExoPlayer) {
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(Player.REPEAT_MODE_OFF) }

    // State Sync
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

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // --- BACKGROUND SONG LIST ---
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("PIXELFY", color = PixelGreen, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), 
                horizontalArrangement = Arrangement.spacedBy(16.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(songs) { song ->
                    SongTile(song) {
                        currentSong = song
                        player.setMediaItem(MediaItem.fromUri(song.uri))
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        // --- LIQUID GLASS OVERLAY ---
        currentSong?.let { song ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                if (isFullScreen) {
                    FullScreenGlassPlayer(
                        song = song, 
                        player = player, 
                        isPlaying = isPlaying, 
                        isShuffle = isShuffle, 
                        repeatMode = repeatMode, 
                        onClose = { isFullScreen = false }
                    )
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
fun FullScreenGlassPlayer(
    song: Song, player: ExoPlayer, isPlaying: Boolean, 
    isShuffle: Boolean, repeatMode: Int, onClose: () -> Unit
) {
    // Liquid Glass Container
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))) {
        Column(
            Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onClose, Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            
            Spacer(Modifier.height(40.dp))
            
            // Glass Art Card
            Surface(
                modifier = Modifier.size(320.dp),
                shape = RoundedCornerShape(24.dp),
                color = GlassWhite,
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(100.dp), tint = PixelGreen.copy(0.5f))
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(song.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("LIQUID GLASS AUDIO", color = PixelGreen, fontSize = 12.sp, fontWeight = FontWeight.Light)

            Spacer(Modifier.weight(1f))

            // Control Row
            Row(
                Modifier.fillMaxWidth().padding(bottom = 40.dp), 
                horizontalArrangement = Arrangement.SpaceEvenly, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.shuffleModeEnabled = !isShuffle }) {
                    Icon(Icons.Default.Shuffle, null, tint = if (isShuffle) PixelGreen else Color.White)
                }
                IconButton(onClick = { player.seekToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                    Icon(if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, 
                         null, tint = PixelGreen, modifier = Modifier.size(85.dp))
                }
                IconButton(onClick = { player.seekToNext() }) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { 
                    player.repeatMode = if (repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF 
                }) {
                    Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, 
                         null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) PixelGreen else Color.White)
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(song: Song, isPlaying: Boolean, player: ExoPlayer, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(8.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = PixelGreen,
        shadowElevation = 8.dp
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("LIQUID UI", color = Color.Black.copy(0.6f), fontSize = 10.sp)
            }
            IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black)
            }
        }
    }
}

@Composable
fun SongTile(song: Song, onClick: () -> Unit) {
    // Narrow click target to prevent "blank side" clicks
    Column(
        Modifier.width(160.dp).clickable { onClick() }
    ) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(GlassWhite)) {
            Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center), tint = Color.DarkGray)
        }
        Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
    }
}

fun fetchSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
    context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { cursor ->
        val tIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val iIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        while (cursor.moveToNext()) {
            val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(iIdx).toString())
            list.add(Song(cursor.getString(tIdx), uri))
        }
    }
    return list
}

data class Song(val title: String, val uri: Uri)
