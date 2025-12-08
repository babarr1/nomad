package com.example.nomad

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_posts")
data class OfflinePostEntity(
    @PrimaryKey val observationId: Int,
    val imageUrl: String,
    val caption: String,
    val category: String,
    val username: String,
    val timestamp: String
)
