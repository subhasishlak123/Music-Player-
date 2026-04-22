package com.example.musicplayer.logic

import com.example.musicplayer.data.SmartSong
import java.util.*

class PlaylistGenerator {
    fun generateSmartList(allSongs: List<SmartSong>, currentMood: String): List<SmartSong> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        val timeContext = when (hour) {
            in 6..11 -> "Morning"
            in 18..23 -> "Night"
            else -> "Any"
        }

        return allSongs.filter { song ->
            song.mood == currentMood || song.timeOfDayPreference == timeContext
        }.sortedByDescending { it.energy }
    }
}
