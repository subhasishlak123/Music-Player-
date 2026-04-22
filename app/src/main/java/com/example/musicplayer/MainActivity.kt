package com.example.musicplayer

import android.Manifest
import android.content.Context
import android.net.Uri
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

// --- AI CORE: MOOD DEFINITIONS ---
enum class Mood { ALL, ENERGETIC, CHILL, DARK, BOLLYWOOD }

data class Song(
    val title: String,
    val uri: Uri,
    val mood: Mood
)

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf(listOf<Song>()) }
            var hasPermission by remember { mutableStateOf(false) }

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { hasPermission = it }

            LaunchedEffect(Unit) {
                launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    songList = fetchAndAnalyzeSongs(context)
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
                if (songList.isNotEmpty()) {
                    MainPlayerUI(songList, player!!)
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF33FF00))
                            Spacer(Modifier.height(10.dp))
                            Text("SCANNING DATABASE...", color = Color(0xFF33FF00), fontSize = 10.sp)
                        }
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

// --- AI KEYWORD SCANNER ---
fun fetchAndAnalyzeSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
    
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        
        while (cursor.moveToNext()) {
            val title = cursor.getString(titleIdx)
            val contentUri = Uri.withAppendedPath(uri, cursor.getLong(idIdx).toString())
            
            val mood = when {
                title.contains("remix", true) || title.contains("bass", true) -> Mood.ENERGETIC
                title.contains("lofi", true) || title.contains("slowed", true) -> Mood.CHILL
                title.contains("sad", true) || title.contains("dark", true) -> Mood.DARK
                title.contains("ki", true) || title.contains("hoon", true) || title.contains("gaana", true) -> Mood.BOLLYWOOD
                else -> Mood.CHILL
            }
            list.add(Song(title, contentUri, mood))
        }
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
        Text("PIXEL AI ENGINE // V2.0", color = Color(0xFF33FF00), fontSize = 10.sp)
        
        // --- MOOD SELECTOR ---
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            Mood.values().forEach { mood ->
                Button(
                    onClick = { selectedMood = mood },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMood == mood) Color(0xFF33FF00) else Color(0xFF1A1A1A)
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(mood.name, color = if (selectedMood == mood) Color.Black else Color.White, fontSize = 10.sp)
                }
            }
        }

        // --- VISUALIZER ---
        Visualizer(isPlaying)

        // --- SONG LIST ---
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredSongs) { song ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        currentSong = song
                        val mediaItem = MediaItem.fromUri(song.uri)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                        isPlaying = true
                    },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                ) {
                    Text(song.title, color = Color.White, modifier = Modifier.padding(16.dp), fontSize = 14.sp, maxLines = 1)
                }
            }
        }

        // --- NOW PLAYING BAR ---
        currentSong?.let { song ->
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(song.title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
                Button(onClick = { 
                    if (player.isPlaying) player.pause() else player.play()
                    isPlaying = player.isPlaying
                }) {
                    Text(if (isPlaying) "PAUSE" else "PLAY")
                }
            }
        }
    }
}

@Composable
fun Visualizer(playing: Boolean) {
    var heights by remember { mutableStateOf(List(15) { 0.1f }) }

    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                heights = List(15) { Random.nextFloat().coerceIn(0.2f, 1.0f) }
                delay(100)
            }
        } else {
            heights = List(15) { 0.1f }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(vertical = 10.dp), 
        verticalAlignment = Alignment.Bottom, 
        horizontalArrangement = Arrangement.Center
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(h)
                    .padding(horizontal = 1.dp)
                    .background(if (playing) Color(0xFF00CCFF) else Color.DarkGray)
            )
        }
    }
}
