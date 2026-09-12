package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.BrasaFutApplication
import com.example.data.local.FavoriteMatch
import com.example.data.local.FavoriteTeam
import com.example.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    val favoriteTeams: StateFlow<List<FavoriteTeam>> = favoritesRepository.allFavoriteTeams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteMatches: StateFlow<List<FavoriteMatch>> = favoritesRepository.allFavoriteMatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeTeam(teamId: Int) {
        viewModelScope.launch {
            // we can simulate the removal or pass the object. Actually repository takes toggle (we need object), or we can add delete by id. 
            // Wait, we can construct a dummy object to pass to toggle with isFavorite=false
            val dummyTeam = FavoriteTeam(id = teamId, name = "", shortName = "", logoUrl = null)
            favoritesRepository.toggleFavoriteTeam(dummyTeam, false)
        }
    }

    fun removeMatch(matchId: Int) {
        viewModelScope.launch {
            val dummyMatch = FavoriteMatch(id = matchId, homeTeamName = "", awayTeamName = "", kickoffTime = null, homeLogoUrl = null, awayLogoUrl = null)
            favoritesRepository.toggleFavoriteMatch(dummyMatch, false)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as BrasaFutApplication)
                FavoritesViewModel(application.favoritesRepository)
            }
        }
    }
}
