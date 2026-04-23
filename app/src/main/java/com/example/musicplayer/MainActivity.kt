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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            var permissionGranted by remember { mutableStateOf(false) }
            
            // Safe initialization
            DisposableEffect(Unit) {
                exoPlayer = ExoPlayer.Builder(context).build()
                onDispose {
                    exoPlayer?.release()
                    exoPlayer = null
                }
            }

            // Permission Launcher
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                permissionGranted = isGranted
                if (isGranted) {
                    songList = fetchSongs(context)
                }
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
                                CircularProgressIndicator(color = Color.Green)
                            }
                        }
                        songList!!.isEmpty() -> {
                            Box(contentAlignment = Alignment.Center) {
                                Text("No Music Found on Device", color = Color.Gray)
                            }
                        }
                        else -> {
                            // Only show this if we actually have songs
                            MusicListScreen(songList!!)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicListScreen(songs: List<Song>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(songs) { song ->
            Text(
                text = song.title,
                color = Color.White,
                modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()
            )
            HorizontalDivider(color = Color.DarkGray)
        }
    }
}

data class Song(val title: String, val uri: Uri)

fun fetchSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    
    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val tIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val songUri = Uri.withAppendedPath(uri, cursor.getLong(iIdx).toString())
                list.add(Song(cursor.getString(tIdx), songUri))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
