package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_cache")
data class CacheEntity(
    @PrimaryKey val key: String,
    val jsonPayload: String,
    val timestamp: Long
)
