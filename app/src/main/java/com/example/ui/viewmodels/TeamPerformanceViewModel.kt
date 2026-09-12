package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.MatchDto
import com.example.data.api.TeamDto
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PerformanceData(
    val matchIndex: Int,
    val goalsScoredCumulative: Int,
    val goalsConcededCumulative: Int,
    val opponentName: String
)

sealed class TeamPerformanceUiState {
    object Loading : TeamPerformanceUiState()
    data class Success(val team: TeamDto, val performance: List<PerformanceData>) : TeamPerformanceUiState()
    data class Error(val message: String) : TeamPerformanceUiState()
}

class TeamPerformanceViewModel : ViewModel() {
    private val repository = MatchRepository()

    private val _uiState = MutableStateFlow<TeamPerformanceUiState>(TeamPerformanceUiState.Loading)
    val uiState: StateFlow<TeamPerformanceUiState> = _uiState.asStateFlow()

    fun loadPerformance(teamId: Int) {
        viewModelScope.launch {
            _uiState.value = TeamPerformanceUiState.Loading
            try {
                // First get the team details. We can find the team from the standings.
                val standings = repository.getStandings()
                val team = standings.find { it.team.id == teamId }?.team
                
                if (team == null) {
                    _uiState.value = TeamPerformanceUiState.Error("Time não encontrado.")
                    return@launch
                }

                val allMatches = repository.getMatches()
                val teamMatches = allMatches.filter { 
                    (it.homeTeam.id == teamId || it.awayTeam.id == teamId) && it.status == "FINISHED" 
                }.sortedBy { it.kickoffTime } // Or by ID/round

                val performanceList = mutableListOf<PerformanceData>()
                var cumulativeScored = 0
                var cumulativeConceded = 0

                teamMatches.forEachIndexed { index, match ->
                    val isHome = match.homeTeam.id == teamId
                    val scored = if (isHome) match.homeScore ?: 0 else match.awayScore ?: 0
                    val conceded = if (isHome) match.awayScore ?: 0 else match.homeScore ?: 0
                    val opponent = if (isHome) match.awayTeam.shortName else match.homeTeam.shortName

                    cumulativeScored += scored
                    cumulativeConceded += conceded

                    performanceList.add(
                        PerformanceData(
                            matchIndex = index + 1,
                            goalsScoredCumulative = cumulativeScored,
                            goalsConcededCumulative = cumulativeConceded,
                            opponentName = opponent
                        )
                    )
                }

                _uiState.value = TeamPerformanceUiState.Success(team, performanceList)
            } catch (e: Exception) {
                _uiState.value = TeamPerformanceUiState.Error(e.message ?: "Erro ao carregar dados")
            }
        }
    }
}
