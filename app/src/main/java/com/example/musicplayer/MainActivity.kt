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
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class Mood { ALL, ENERGETIC, CHILL, DARK, BOLLYWOOD }

data class Song(val title: String, val uri: Uri, val mood: Mood)

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            
            // Determine correct permission based on Android Version
            val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    songList = fetchAndAnalyzeSongs(context)
                } else {
                    songList = emptyList() // User denied permission
                }
            }

            LaunchedEffect(Unit) {
                launcher.launch(permissionToRequest)
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
                when {
                    songList == null -> {
                        // This is what you see in the screenshot
                        Box(contentAlignment = Alignment.Center) {
                            Text("INITIALIZING SYSTEM...", color = Color(0xFF33FF00))
                        }
                    }
                    songList!!.isEmpty() -> {
                        Box(contentAlignment = Alignment.Center) {
                            Text("NO MUSIC FOUND ON DEVICE", color = Color.Red)
                        }
                    }
                    else -> {
                        MainPlayerUI(songList!!, player!!)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}

fun fetchAndAnalyzeSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
    
    // Sort by Title
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    try {
        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            
            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIdx)
                val id = cursor.getLong(idIdx)
                val contentUri = Uri.withAppendedPath(uri, id.toString())
                
                val mood = when {
                    title.contains("remix", true) || title.contains("bass", true) -> Mood.ENERGETIC
                    title.contains("lofi", true) || title.contains("slowed", true) -> Mood.CHILL
                    title.contains("hoon", true) || title.contains("gaana", true) -> Mood.BOLLYWOOD
                    else -> Mood.CHILL
                }
                list.add(Song(title, contentUri, mood))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

@Composable
fun MainPlayerUI(allSongs: List<Song>, player: ExoPlayer) {
    var selectedMood by remember { mutableStateOf(Mood.ALL) }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val filteredSongs = remember(selectedMood, allSongs) {
        if (selectedMood == Mood.ALL) allSongs else allSongs.filter { it.mood == selectedMood }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("PIXEL AI // STATUS: ONLINE", color = Color(0xFF33FF00), fontSize = 10.sp)
        
        // Mood Tabs
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            Mood.values().forEach { mood ->
                Surface(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { selectedMood = mood },
                    color = if (selectedMood == mood) Color(0xFF33FF00) else Color(0xFF1A1A1A),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        mood.name, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (selectedMood == mood) Color.Black else Color.White, 
                        fontSize = 10.sp
                    )
                }
            }
        }

        Visualizer(isPlaying)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredSongs) { song ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, if (currentSong == song) Color(0xFF33FF00) else Color.DarkGray)
                        .clickable {
                            currentSong = song
                            player.setMediaItem(MediaItem.fromUri(song.uri))
                            player.prepare()
                            player.play()
                            isPlaying = true
                        }
                        .padding(16.dp)
                ) {
                    Text(song.title, color = Color.White, fontSize = 14.sp, maxLines = 1)
                }
            }
        }

        currentSong?.let { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(song.title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
                Text(
                    if (isPlaying) "PAUSE" else "PLAY",
                    color = Color(0xFF33FF00),
                    modifier = Modifier.clickable {
                        if (player.isPlaying) player.pause() else player.play()
                        isPlaying = player.isPlaying
                    }
                )
            }
        }
    }
}

@Composable
fun Visualizer(playing: Boolean) {
    var heights by remember { mutableStateOf(List(10) { 0.1f }) }
    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                heights = List(10) { Random.nextFloat().coerceIn(0.2f, 1.0f) }
                delay(100)
            }
        } else {
            heights = List(10) { 0.1f }
        }
    }
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
        heights.forEach { h ->
            Box(Modifier.width(8.dp).fillMaxHeight(h).padding(horizontal = 1.dp).background(Color(0xFF00CCFF)))
        }
    }
}
