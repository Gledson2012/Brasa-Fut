package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    // Teams
    @Query("SELECT * FROM favorite_teams")
    fun getAllFavoriteTeams(): Flow<List<FavoriteTeam>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_teams WHERE id = :teamId)")
    fun isTeamFavorite(teamId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTeam(team: FavoriteTeam)

    @Query("DELETE FROM favorite_teams WHERE id = :teamId")
    suspend fun deleteFavoriteTeam(teamId: Int)

    // Matches
    @Query("SELECT * FROM favorite_matches")
    fun getAllFavoriteMatches(): Flow<List<FavoriteMatch>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_matches WHERE id = :matchId)")
    fun isMatchFavorite(matchId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteMatch(match: FavoriteMatch)

    @Query("DELETE FROM favorite_matches WHERE id = :matchId")
    suspend fun deleteFavoriteMatch(matchId: Int)
}
