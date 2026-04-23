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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            var permissionGranted by remember { mutableStateOf(false) }
            
            // Safe initialization of Player
            DisposableEffect(Unit) {
                exoPlayer = ExoPlayer.Builder(context).build()
                onDispose {
                    exoPlayer?.release()
                    exoPlayer = null
                }
            }

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
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    when {
                        !permissionGranted -> {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Storage Permission Required", color = Color.White)
                            }
                        }
                        songList == null -> {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF1DB954))
                            }
                        }
                        else -> {
                            exoPlayer?.let { player ->
                                MusicScreen(songList!!, player)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicScreen(songs: List<Song>, player: ExoPlayer) {
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Synchronize UI state with Player state
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Your Library", 
                style = MaterialTheme.typography.headlineMedium, 
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(songs) { song ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentSong = song
                                val mediaItem = MediaItem.fromUri(song.uri)
                                player.setMediaItem(mediaItem)
                                player.prepare()
                                player.play()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = song.title,
                            color = if (currentSong == song) Color(0xFF1DB954) else Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.DarkGray))
                }
            }
            // Space for the bottom bar
            if (currentSong != null) Spacer(modifier = Modifier.height(80.dp))
        }

        // --- PLAYBACK CONTROL BAR ---
        currentSong?.let { song ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF282828)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.title, 
                            color = Color.White, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                        Text("Local Audio", color = Color.Gray, fontSize = 12.sp)
                    }
                    
                    IconButton(onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                    }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

data class Song(val title: String, val uri: Uri)

fun fetchSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DURATION)
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    
    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val tIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            
            while (cursor.moveToNext()) {
                if (cursor.getLong(dIdx) > 5000) { 
                    val songUri = Uri.withAppendedPath(uri, cursor.getLong(iIdx).toString())
                    list.add(Song(cursor.getString(tIdx), songUri))
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}
