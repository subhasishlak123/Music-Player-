package com.example.musicplayer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PixelPlayerScreen(songTitle: String, mood: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)) // Deep Black
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pixel Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(4.dp, Color.White)
                .padding(8.dp)
        ) {
            Text("AI PIXEL PLAYER v1.0", color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(50.dp))

        // "Album Art" Pixel Box
        Box(
            modifier = Modifier
                .size(200.dp)
                .border(8.dp, Color(0xFF00FF41)) // Matrix Green
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("♪", color = Color(0xFF00FF41), fontSize = 80.sp)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // AI Info Display
        Text(songTitle.uppercase(), color = Color.White, fontSize = 20.sp)
        Text("AI MOOD: $mood", color = Color(0xFF00FF41), fontSize = 14.sp)

        Spacer(modifier = Modifier.weight(1f))

        // Pixel Buttons
        Row {
            PixelButton("PREV")
            Spacer(modifier = Modifier.width(10.dp))
            PixelButton("PLAY")
            Spacer(modifier = Modifier.width(10.dp))
            PixelButton("NEXT")
        }
    }
}

@Composable
fun PixelButton(label: String) {
    Box(
        modifier = Modifier
            .border(3.dp, Color.White)
            .background(Color.DarkGray)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clickable { /* Action */ }
    ) {
        Text(label, color = Color.White)
    }
}
