package com.example.musicplayer

import android.os.Bundle
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

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
            Text("SYSTEM: READY // AI: ONLINE", color = RetroGreen, fontSize = 10.sp)
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
            Text("PIXEL_BEATS.MP3", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("MODE: HI-QUALITY // STATUS: PLAYING", color = RetroGreen, fontSize = 12.sp)
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

        // --- AI STATUS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF1A1A1A))
                .border(1.dp, Color.Gray)
                .padding(8.dp)
        ) {
            Text("> AI: Calibrating pixel response times...", 
                 color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun PixelButton(text: String, isMain: Boolean = false) {
    Box(
        modifier = Modifier
            // Using the full path here fixes the "Unresolved Reference" for good
            .border(3.dp, if (isMain) RetroGreen else Color.White, androidx.compose.ui.graphics.RectangleShape)
            .background(if (isMain) Color(0xFF113311) else BgBlack)
            .clickable { /* Audio Logic */ }
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
