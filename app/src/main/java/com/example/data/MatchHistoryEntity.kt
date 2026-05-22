package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val numPlayers: Int,
    val boardShape: String,
    val playerNames: String,  // Comma-separated or JSON list
    val playerScores: String, // Comma-separated or JSON list
    val winnerName: String,
    val winnerScore: Int
)
