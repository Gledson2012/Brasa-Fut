package com.example.data.repository

import com.example.data.local.FavoriteMatch
import com.example.data.local.FavoriteTeam
import com.example.data.local.FavoritesDao
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val favoritesDao: FavoritesDao) {

    // Teams
    val allFavoriteTeams: Flow<List<FavoriteTeam>> = favoritesDao.getAllFavoriteTeams()

    fun isTeamFavorite(teamId: Int): Flow<Boolean> = favoritesDao.isTeamFavorite(teamId)

    suspend fun toggleFavoriteTeam(team: FavoriteTeam, isFavorite: Boolean) {
        if (isFavorite) {
            favoritesDao.insertFavoriteTeam(team)
        } else {
            favoritesDao.deleteFavoriteTeam(team.id)
        }
    }

    // Matches
    val allFavoriteMatches: Flow<List<FavoriteMatch>> = favoritesDao.getAllFavoriteMatches()

    fun isMatchFavorite(matchId: Int): Flow<Boolean> = favoritesDao.isMatchFavorite(matchId)

    suspend fun toggleFavoriteMatch(match: FavoriteMatch, isFavorite: Boolean) {
        if (isFavorite) {
            favoritesDao.insertFavoriteMatch(match)
        } else {
            favoritesDao.deleteFavoriteMatch(match.id)
        }
    }
}
