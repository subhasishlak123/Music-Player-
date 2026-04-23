package com.example.musicplayer

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

// Neon Green Color from your image
val PixelGreen = Color(0xFF1DB954)
val DarkBackground = Color(0xFF0A0E0F)
val SurfaceColor = Color(0xFF15191A)

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

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                    if (songList != null) {
                        PixelfyUI(songList!!, exoPlayer!!)
                    }
                }
            }
        }
    }
}

@Composable
fun PixelfyUI(songs: List<Song>, player: ExoPlayer) {
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            // --- SIDEBAR ---
            Column(
                Modifier.width(200.dp).fillMaxHeight().background(DarkBackground).padding(16.dp)
            ) {
                Text("PIXELFY", color = PixelGreen, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(32.dp))
                SidebarItem(Icons.Default.Home, "HOME", true)
                SidebarItem(Icons.Default.Search, "SEARCH", false)
                SidebarItem(Icons.Default.LibraryMusic, "LIBRARY", false)
                SidebarItem(Icons.Default.PlaylistPlay, "PLAYLISTS", false)
            }

            // --- MAIN CONTENT ---
            Column(Modifier.fillMaxSize().background(SurfaceColor).padding(24.dp)) {
                Text("YOUR DAILY PIXELS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(songs) { song ->
                        SongCard(song) {
                            currentSong = song
                            player.setMediaItem(MediaItem.fromUri(song.uri))
                            player.prepare()
                            player.play()
                            isPlaying = true
                        }
                    }
                }
            }
        }

        // --- BOTTOM PLAYER BAR ---
        currentSong?.let { song ->
            BottomPlayer(song, isPlaying, player) { isPlaying = !isPlaying }
        }
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable { onClick() }) {
        Box(Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray)) {
            Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center), tint = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
        Text("Local Audio", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun SidebarItem(icon: ImageVector, label: String, active: Boolean) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (active) PixelGreen else Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (active) PixelGreen else Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BoxScope.BottomPlayer(song: Song, isPlaying: Boolean, player: ExoPlayer, onToggle: () -> Unit) {
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(PixelGreen).padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(Modifier.size(40.dp).background(Color.Black)) // Placeholder for Art
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Playing Now", color = Color.Black.copy(0.7f), fontSize = 10.sp)
            }
            IconButton(onClick = { player.seekToPrevious() }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.Black) }
            IconButton(onClick = { 
                if (player.isPlaying) player.pause() else player.play()
                onToggle()
            }) { 
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(48.dp)) 
            }
            IconButton(onClick = { player.seekToNext() }) { Icon(Icons.Default.SkipNext, null, tint = Color.Black) }
        }
        // Custom Seekbar look
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color.Black.copy(0.2f))) {
            Box(Modifier.fillMaxWidth(0.4f).fillMaxHeight().background(Color.Black))
        }
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
