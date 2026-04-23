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
import androidx.compose.ui.graphics.Color
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
    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(Player.REPEAT_MODE_OFF) }

    // Handle system back button to exit full screen
    BackHandler(enabled = isFullScreen) {
        isFullScreen = false
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        // 1. Library View
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("PIXELFY", color = PixelGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

        // 2. Animated Full Screen Player
        AnimatedVisibility(
            visible = currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (isFullScreen) {
                FullScreenPlayer(
                    song = currentSong!!,
                    player = player,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onClose = { isFullScreen = false },
                    onShuffleToggle = {
                        isShuffle = !isShuffle
                        player.shuffleModeEnabled = isShuffle
                    },
                    onRepeatToggle = {
                        repeatMode = if (repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                        player.repeatMode = repeatMode
                    }
                )
            } else {
                // Mini Player (The Bar you circled)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    MiniPlayerBar(currentSong!!, player) { isFullScreen = true }
                }
            }
        }
    }
}

@Composable
fun FullScreenPlayer(
    song: Song, 
    player: ExoPlayer, 
    isShuffle: Boolean, 
    repeatMode: Int,
    onClose: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(DarkBg).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClose, Modifier.align(Alignment.Start)) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        
        // Large "Pixel" Album Art
        Box(Modifier.size(300.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
            Icon(Icons.Default.MusicNote, null, Modifier.size(100.dp).align(Alignment.Center), tint = Color.Gray)
        }

        Spacer(Modifier.height(32.dp))

        Text(song.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("Local Audio", color = PixelGreen, fontSize = 16.sp)

        Spacer(Modifier.weight(1f))

        // Playback Controls
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            // Shuffle Button
            IconButton(onClick = onShuffleToggle) {
                Icon(Icons.Default.Shuffle, null, tint = if (isShuffle) PixelGreen else Color.White)
            }
            
            IconButton(onClick = { player.seekToPrevious() }) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            
            IconButton(onClick = { if(player.isPlaying) player.pause() else player.play() }) {
                Icon(if(player.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, null, tint = PixelGreen, modifier = Modifier.size(80.dp))
            }

            IconButton(onClick = { player.seekToNext() }) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }

            // Auto-play (Repeat) Button
            IconButton(onClick = onRepeatToggle) {
                Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) PixelGreen else Color.White)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun MiniPlayerBar(song: Song, player: ExoPlayer, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(70.dp).background(PixelGreen).clickable { onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(45.dp).background(Color.Black))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("Playing Now", color = Color.Black.copy(0.7f), fontSize = 11.sp)
        }
        IconButton(onClick = { if(player.isPlaying) player.pause() else player.play() }) {
            Icon(if(player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black)
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

data class Song(val title: String, val uri: Uri)

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
