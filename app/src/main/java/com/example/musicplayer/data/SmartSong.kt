package com.example.musicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SmartSong(
    @PrimaryKey val id: Long,
    val title: String,
    val path: String,
    val energy: Float, // 0.0 (Chill) to 1.0 (Workout)
    val mood: String,   // "Happy", "Sad", "Calm"
    val timeOfDayPreference: String // "Morning", "Night", "Any"
)
