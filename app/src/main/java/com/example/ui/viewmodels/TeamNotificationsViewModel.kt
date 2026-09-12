package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.BrasaFutApplication
import com.example.data.api.TeamDto
import com.example.data.local.NotificationPreferences
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TeamNotificationsUiState {
    object Idle : TeamNotificationsUiState()
    object Loading : TeamNotificationsUiState()
    data class Success(val teams: List<TeamDto>) : TeamNotificationsUiState()
    data class Error(val message: String) : TeamNotificationsUiState()
}

class TeamNotificationsViewModel(
    val notificationPreferences: NotificationPreferences
) : ViewModel() {
    private val repository = MatchRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<TeamNotificationsUiState>(TeamNotificationsUiState.Idle)
    val uiState: StateFlow<TeamNotificationsUiState> = _uiState.asStateFlow()

    @OptIn(FlowPreview::class)
    val debouncedSearch = _searchQuery
        .debounce(500)
        .onEach { query ->
            if (query.length >= 3) {
                performSearch(query)
            } else {
                _uiState.value = TeamNotificationsUiState.Idle
            }
        }
        .launchIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = TeamNotificationsUiState.Loading
            try {
                val results = repository.searchTeams(query)
                _uiState.value = TeamNotificationsUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = TeamNotificationsUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun toggleNotification(teamId: Int, enable: Boolean) {
        notificationPreferences.toggleTeamNotification(teamId, enable)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as BrasaFutApplication)
                TeamNotificationsViewModel(application.notificationPreferences)
            }
        }
    }
}
