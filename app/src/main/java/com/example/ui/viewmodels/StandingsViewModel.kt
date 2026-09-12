package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.StandingDto
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StandingsUiState {
    object Loading : StandingsUiState()
    data class Success(val standings: List<StandingDto>) : StandingsUiState()
    data class Error(val message: String) : StandingsUiState()
}

class StandingsViewModel : ViewModel() {
    private val repository = MatchRepository()

    private val _uiState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()

    init {
        loadStandings()
    }

    private fun loadStandings() {
        viewModelScope.launch {
            _uiState.value = StandingsUiState.Loading
            try {
                // Fetch standings for default season (seasonId = 1)
                val standings = repository.getStandings(1)
                _uiState.value = StandingsUiState.Success(standings)
            } catch (e: Exception) {
                _uiState.value = StandingsUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
