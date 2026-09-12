package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.MatchDto
import com.example.data.api.TeamDto
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlayerMockGlobal(val name: String, val position: String, val teamName: String)

sealed class GlobalSearchUiState {
    object Idle : GlobalSearchUiState()
    object Loading : GlobalSearchUiState()
    data class Success(
        val matches: List<MatchDto>,
        val teams: List<TeamDto>,
        val players: List<PlayerMockGlobal>
    ) : GlobalSearchUiState()
    data class Error(val message: String) : GlobalSearchUiState()
}

class GlobalSearchViewModel : ViewModel() {
    private val repository = MatchRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _uiState = MutableStateFlow<GlobalSearchUiState>(GlobalSearchUiState.Idle)
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    private val mockPlayers = listOf(
        PlayerMockGlobal("Neymar Jr", "Atacante", "Al Hilal"),
        PlayerMockGlobal("Vinícius Júnior", "Atacante", "Real Madrid"),
        PlayerMockGlobal("Rodrygo", "Ponta", "Real Madrid"),
        PlayerMockGlobal("Alisson", "Goleiro", "Liverpool"),
        PlayerMockGlobal("Marquinhos", "Zagueiro", "PSG"),
        PlayerMockGlobal("Casemiro", "Volante", "Manchester United"),
        PlayerMockGlobal("Lucas Paquetá", "Meia", "West Ham"),
        PlayerMockGlobal("Raphinha", "Ponta", "Barcelona"),
        PlayerMockGlobal("Gabriel Jesus", "Atacante", "Arsenal"),
        PlayerMockGlobal("Ederson", "Goleiro", "Manchester City"),
        PlayerMockGlobal("Cássio", "Goleiro", "Corinthians"),
        PlayerMockGlobal("Yuri Alberto", "Atacante", "Corinthians"),
        PlayerMockGlobal("Pedro", "Atacante", "Flamengo"),
        PlayerMockGlobal("Endrick", "Atacante", "Real Madrid")
    )

    @OptIn(FlowPreview::class)
    val debouncedSearch = _searchQuery
        .debounce(500)
        .onEach { query ->
            if (query.length >= 2) {
                performSearch(query)
            } else {
                _uiState.value = GlobalSearchUiState.Idle
            }
        }
        .launchIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSearchActiveChanged(isActive: Boolean) {
        _isSearchActive.value = isActive
        if (!isActive) {
            _searchQuery.value = ""
            _uiState.value = GlobalSearchUiState.Idle
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = GlobalSearchUiState.Loading
            try {
                // Search Teams
                val teamsDeferred = async {
                    try { repository.searchTeams(query) } catch (e: Exception) { emptyList() }
                }

                // Search Matches (fetch all and filter locally for simplicity)
                val matchesDeferred = async {
                    try { 
                        repository.getMatches().filter { 
                            it.homeTeam.name.contains(query, ignoreCase = true) ||
                            it.awayTeam.name.contains(query, ignoreCase = true) ||
                            it.homeTeam.shortName.contains(query, ignoreCase = true) ||
                            it.awayTeam.shortName.contains(query, ignoreCase = true) ||
                            it.league?.contains(query, ignoreCase = true) == true
                        } 
                    } catch (e: Exception) { emptyList() }
                }

                val teams = teamsDeferred.await()
                val matches = matchesDeferred.await()
                val players = mockPlayers.filter { it.name.contains(query, ignoreCase = true) }

                _uiState.value = GlobalSearchUiState.Success(matches, teams, players)
            } catch (e: Exception) {
                _uiState.value = GlobalSearchUiState.Error(e.message ?: "Erro ao buscar")
            }
        }
    }
}
