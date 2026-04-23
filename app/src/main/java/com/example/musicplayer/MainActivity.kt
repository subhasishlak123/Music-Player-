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
import androidx.media3.exoplayer.ExoPlayer

enum class Mood { ALL, ENERGETIC, CHILL, DARK, BOLLYWOOD }
data class Song(val title: String, val uri: Uri, val mood: Mood)

class MainActivity : ComponentActivity() {
    // Moved to nullable to prevent crash during initialization
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            var songList by remember { mutableStateOf<List<Song>?>(null) }
            
            // Create player safely
            DisposableEffect(Unit) {
                player = ExoPlayer.Builder(context).build()
                onDispose {
                    player?.release()
                    player = null
                }
            }

            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { 
                if (it) songList = fetchAndAnalyzeSongs(context) else songList = emptyList()
            }

            LaunchedEffect(Unit) { launcher.launch(permission) }

            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (songList == null) {
                        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1DB954)) }
                    } else {
                        player?.let { safePlayer ->
                            ModernPlayerUI(songList!!, safePlayer)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernPlayerUI(allSongs: List<Song>, player: ExoPlayer) {
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf(Mood.ALL) }

    val displayList = remember(selectedMood, isShuffle, allSongs) {
        val filtered = if (selectedMood == Mood.ALL) allSongs else allSongs.filter { it.mood == selectedMood }
        if (isShuffle) filtered.shuffled() else filtered
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF121212)))
    )) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(48.dp))
            Text("Your Library", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                Mood.values().forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood },
                        label = { Text(mood.name) },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1DB954),
                            containerColor = Color(0xFF282828),
                            labelColor = Color.White
                        ),
                        border = null
                    )
                }
            }

            IconButton(onClick = { isShuffle = !isShuffle }) {
                Icon(Icons.Default.Shuffle, null, tint = if (isShuffle) Color(0xFF1DB954) else Color.White)
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(displayList, key = { it.uri.toString() }) { song ->
                    SongRow(song, isCurrent = currentSong == song) {
                        currentSong = song
                        player.setMediaItem(MediaItem.fromUri(song.uri))
                        player.prepare()
                        player.play()
                        isPlaying = true
                    }
                }
            }
        }

        currentSong?.let { song ->
            BottomPlayerBar(song, isPlaying) {
                if (player.isPlaying) player.pause() else player.play()
                isPlaying = !isPlaying
            }
        }
    }
}

@Composable
fun SongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF282828))) {
            Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center), tint = if(isCurrent) Color(0xFF1DB954) else Color.Gray)
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(song.title, color = if(isCurrent) Color(0xFF1DB954) else Color.White, fontSize = 15.sp, maxLines = 1)
            Text(song.mood.name, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun BoxScope.BottomPlayerBar(song: Song, isPlaying: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().height(64.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(song.title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onToggle) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
            }
        }
    }
}

fun fetchAndAnalyzeSongs(context: Context): List<Song> {
    val list = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID)
    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIdx)
                val contentUri = Uri.withAppendedPath(uri, cursor.getLong(idIdx).toString())
                val mood = if (title.contains("remix", true)) Mood.ENERGETIC else Mood.CHILL
                list.add(Song(title, contentUri, mood))
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}
