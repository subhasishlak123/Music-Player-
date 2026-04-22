package com.example.musicplayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var hasPermission by remember { mutableStateOf(checkPermission(context)) }
            var songList by remember { mutableStateOf(listOf<String>()) }

            // Permission Launcher
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted -> hasPermission = isGranted }

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    songList = fetchSongs(context)
                } else {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                    launcher.launch(permission)
                }
            }

            PixelPlayerTheme {
                if (hasPermission) {
                    PlayerScreen(songList)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("PERMISSION REQUIRED TO PLAY MUSIC", color = Color.Red)
                    }
                }
            }
        }
    }
}

// Function to scan your phone for audio files
fun fetchSongs(context: Context): List<String> {
    val list = mutableListOf<String>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Media.TITLE)
    
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        while (cursor.moveToNext()) {
            list.add(cursor.getString(titleIndex))
        }
    }
    return if (list.isEmpty()) listOf("No Music Found") else list
}

fun checkPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun PlayerScreen(songs: List<String>) {
    var currentSongIndex by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("SYSTEM: ONLINE", color = Color(0xFF33FF00), fontSize = 12.sp)

        // Song Display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = songs.getOrElse(currentSongIndex) { "SEARCHING..." },
                color = Color.White,
                fontSize = 22.sp
            )
            Text("TRACK ${currentSongIndex + 1} / ${songs.size}", color = Color.Gray)
        }

        // Controls
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { if (currentSongIndex > 0) currentSongIndex-- }) { Text("PREV") }
            Spacer(Modifier.width(10.dp))
            Button(onClick = { /* Add ExoPlayer Start Logic */ }) { Text("PLAY") }
            Spacer(Modifier.width(10.dp))
            Button(onClick = { if (currentSongIndex < songs.size - 1) currentSongIndex++ }) { Text("NEXT") }
        }
    }
}

@Composable
fun PixelPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
