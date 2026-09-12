package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.MatchDto
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed class MatchesUiState {
    object Loading : MatchesUiState()
    data class Success(val matches: List<MatchDto>) : MatchesUiState()
    data class Error(val message: String) : MatchesUiState()
}

class MatchesViewModel : ViewModel() {
    private val repository = MatchRepository()

    private val _uiState = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allMatches: List<MatchDto> = emptyList()

    init {
        loadMatches()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterMatches()
    }

    private fun filterMatches() {
        val query = _searchQuery.value.trim().lowercase()
        if (query.isEmpty()) {
            _uiState.value = MatchesUiState.Success(allMatches)
        } else {
            val filtered = allMatches.filter { match ->
                match.homeTeam.name.lowercase().contains(query) ||
                match.homeTeam.shortName.lowercase().contains(query) ||
                match.awayTeam.name.lowercase().contains(query) ||
                match.awayTeam.shortName.lowercase().contains(query) ||
                (match.league?.lowercase()?.contains(query) == true)
            }
            _uiState.value = MatchesUiState.Success(filtered)
        }
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = MatchesUiState.Loading
            try {
                allMatches = repository.getMatches()
                filterMatches()
            } catch (e: Exception) {
                _uiState.value = MatchesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
