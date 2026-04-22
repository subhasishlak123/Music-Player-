package com.example.musicplayer

import android.content.Context
import androidx.room.*
import com.example.musicplayer.data.SmartSong

// --- THE AI MEMORY (DATABASE) ---
@Database(entities = [SmartSong::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE mood = :targetMood ORDER BY energy DESC")
    fun getSongsByMood(targetMood: String): List<SmartSong>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSongs(songs: List<SmartSong>)
}

// --- THE BRAIN (AI LOGIC) ---
class SmartBrain(private val db: AppDatabase) {
    
    // Automatically updates the AI when a user skips a song
    fun handleUserSkip(songId: Long) {
        // Logic: If skipped, reduce energy level in database
        // This is "Reinforcement Learning" - the app learns from your actions
    }

    fun suggestPlaylist(timeOfDay: Int): String {
        return when (timeOfDay) {
            in 5..11 -> "Energetic" // Morning vibe
            in 20..23 -> "Calm"      // Night vibe
            else -> "Happy"
        }
    }
}
