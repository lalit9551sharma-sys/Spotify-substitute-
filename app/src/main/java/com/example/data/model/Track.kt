package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val url: String,
    val coverUrl: String,
    val category: String,
    val lyrics: String = "No lyrics available for this track.",
    val isLiked: Boolean = false
)
