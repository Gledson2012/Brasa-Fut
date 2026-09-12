package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_teams")
data class FavoriteTeam(
    @PrimaryKey val id: Int,
    val name: String,
    val shortName: String,
    val logoUrl: String?
)

@Entity(tableName = "favorite_matches")
data class FavoriteMatch(
    @PrimaryKey val id: Int,
    val homeTeamName: String,
    val awayTeamName: String,
    val kickoffTime: String?,
    val homeLogoUrl: String?,
    val awayLogoUrl: String?
)
