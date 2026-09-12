package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.BrasaFutApplication
import com.example.data.api.TeamDto
import com.example.data.local.FavoriteTeam
import com.example.data.repository.FavoritesRepository
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.workers.ScheduleRemindersWorker
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val teams: List<TeamDto>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val application: BrasaFutApplication
) : ViewModel() {
    private val repository = MatchRepository()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val favoriteTeams: StateFlow<List<FavoriteTeam>> = favoritesRepository.allFavoriteTeams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _uiState.value = SearchUiState.Loading
            try {
                val results = repository.searchTeams(query)
                _uiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun toggleFavoriteTeam(team: TeamDto, isFavorite: Boolean) {
        viewModelScope.launch {
            val favoriteTeam = FavoriteTeam(
                id = team.id,
                name = team.name,
                shortName = team.shortName,
                logoUrl = team.logoUrl
            )
            favoritesRepository.toggleFavoriteTeam(favoriteTeam, !isFavorite)
            
            // Trigger a quick schedule check after favoriting a new team
            if (!isFavorite) {
                val oneTimeWork = OneTimeWorkRequestBuilder<ScheduleRemindersWorker>().build()
                WorkManager.getInstance(application).enqueue(oneTimeWork)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as BrasaFutApplication)
                SearchViewModel(application.favoritesRepository, application)
            }
        }
    }
}
