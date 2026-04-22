package com.example.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RectangleShape // Added this missing import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random // Added for the visualizer math

// Retro Palette
val RetroGreen = Color(0xFF33FF00)
val RetroBlue = Color(0xFF00CCFF)
val BgBlack = Color(0xFF0A0A0A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelPlayerTheme {
                PlayerScreen()
            }
        }
    }
}

@Composable
fun PlayerScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- TOP BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, RetroGreen)
                .padding(8.dp)
        ) {
            Text("SYSTEM: READY // AI: SCANNING", color = RetroGreen, fontSize = 10.sp)
        }

        // --- VISUALIZER AREA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(4.dp, Color.White),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.height(100.dp)) {
                repeat(8) { 
                    // Fixed random logic
                    val barHeight = remember { Random.nextFloat().coerceIn(0.3f, 0.9f) }
                    Box(modifier = Modifier
                        .width(15.dp)
                        .fillMaxHeight(fraction = barHeight)
                        .padding(horizontal = 2.dp)
                        .background(RetroBlue))
                }
            }
        }

        // --- SONG INFO ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CURRENT_TRACK.MP3", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("MOOD: ENERGETIC // 128 BPM", color = RetroGreen, fontSize = 12.sp)
        }

        // --- CONTROLS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PixelButton("PREV")
            PixelButton("PLAY", isMain = true)
            PixelButton("NEXT")
        }

        // --- AI LOG ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xFF1A1A1A))
                .border(1.dp, Color.Gray)
                .padding(8.dp)
        ) {
            Text("> AI Suggestion: Night detected. Switching to Chill list...", 
                 color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun PixelButton(text: String, isMain: Boolean = false) {
    Box(
        modifier = Modifier
            .border(3.dp, if (isMain) RetroGreen else Color.White, RectangleShape)
            .background(if (isMain) Color(0xFF113311) else BgBlack)
            .clickable { /* Click Logic */ }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isMain) RetroGreen else Color.White, fontSize = 14.sp)
    }
}

@Composable
fun PixelPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
