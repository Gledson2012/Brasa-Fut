package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.BrasaFutApplication
import com.example.data.api.MatchDto
import com.example.data.local.FavoriteMatch
import com.example.data.repository.FavoritesRepository
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class MatchDetailsUiState {
    object Loading : MatchDetailsUiState()
    data class Success(
        val match: MatchDto,
        val statistics: List<MatchStatistic>,
        val homeLineup: List<PlayerMock>,
        val awayLineup: List<PlayerMock>,
        val headToHead: List<MatchDto> = emptyList(),
        val events: List<com.example.data.api.MatchEventDto> = emptyList()
    ) : MatchDetailsUiState()
    data class Error(val message: String) : MatchDetailsUiState()
}

data class MatchStatistic(
    val label: String,
    val homeValue: Int,
    val awayValue: Int,
    val isPercentage: Boolean = false
)

data class PlayerMock(
    val name: String,
    val number: Int,
    val position: String
)

class MatchDetailsViewModel(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    private val repository = MatchRepository()

    private val _uiState = MutableStateFlow<MatchDetailsUiState>(MatchDetailsUiState.Loading)
    val uiState: StateFlow<MatchDetailsUiState> = _uiState.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private var currentMatchId: Int? = null
    private var favoriteJob: kotlinx.coroutines.Job? = null

    fun loadMatchDetails(matchId: Int) {
        currentMatchId = matchId
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            favoritesRepository.isMatchFavorite(matchId).collect { isFav ->
                _isFavorite.value = isFav
            }
        }

        viewModelScope.launch {
            _uiState.value = MatchDetailsUiState.Loading
            try {
                val match = repository.getMatch(matchId)
                
                // MOCK DATA since API doesn't provide statistics or lineups yet
                val stats = listOf(
                    MatchStatistic("Posse de Bola", (40..60).random(), (40..60).random(), true),
                    MatchStatistic("Finalizações", (5..20).random(), (5..20).random()),
                    MatchStatistic("Chutes ao Gol", (2..10).random(), (2..10).random()),
                    MatchStatistic("Faltas", (8..25).random(), (8..25).random()),
                    MatchStatistic("Escanteios", (2..12).random(), (2..12).random()),
                    MatchStatistic("Cartões Amarelos", (0..5).random(), (0..5).random()),
                    MatchStatistic("Cartões Vermelhos", (0..1).random(), (0..1).random())
                )
                
                val positions = listOf("Goleiro", "Zagueiro", "Zagueiro", "Lateral", "Lateral", "Volante", "Meia", "Meia", "Ponta", "Ponta", "Atacante")
                val homeLineup = positions.mapIndexed { index, pos -> 
                    PlayerMock("Jogador ${match.homeTeam.shortName} ${index + 1}", index + 1, pos)
                }
                val awayLineup = positions.mapIndexed { index, pos -> 
                    PlayerMock("Jogador ${match.awayTeam.shortName} ${index + 1}", index + 1, pos)
                }

                var h2h = emptyList<MatchDto>()
                var events = emptyList<com.example.data.api.MatchEventDto>()
                try {
                    events = repository.getMatchEvents(match.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    h2h = repository.getHeadToHead(match.homeTeam.id, match.awayTeam.id)
                } catch (e: Exception) {
                    // Ignore h2h failure for now to avoid breaking the screen
                }

                _uiState.value = MatchDetailsUiState.Success(match, stats, homeLineup, awayLineup, h2h, events)
            } catch (e: Exception) {
                _uiState.value = MatchDetailsUiState.Error(e.message ?: "Erro ao carregar detalhes")
            }
        }
    }

    fun toggleFavorite(context: android.content.Context) {
        val state = uiState.value
        if (state is MatchDetailsUiState.Success) {
            val match = state.match
            val favoriteMatch = FavoriteMatch(
                id = match.id,
                homeTeamName = match.homeTeam.shortName,
                awayTeamName = match.awayTeam.shortName,
                kickoffTime = match.kickoffTime,
                homeLogoUrl = match.homeTeam.logoUrl,
                awayLogoUrl = match.awayTeam.logoUrl
            )
            
            val isCurrentlyFavorite = isFavorite.value
            
            viewModelScope.launch {
                favoritesRepository.toggleFavoriteMatch(favoriteMatch, !isCurrentlyFavorite)
                
                val scheduler = com.example.services.MatchAlarmScheduler(context)
                if (!isCurrentlyFavorite) { // It is now favorited
                    scheduler.scheduleMatchReminder(favoriteMatch)
                } else { // It is unfavorited
                    scheduler.cancelMatchReminder(match.id)
                }
            }
        }
    }

    fun scheduleReminder(applicationContext: android.content.Context) {
        val state = uiState.value
        if (state is MatchDetailsUiState.Success) {
            val match = state.match
            if (match.kickoffTime == null) return
            
            try {
                val kickoffInstant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.time.Instant.parse(match.kickoffTime)
                } else return
                
                val triggerTime = kickoffInstant.minusSeconds(30 * 60)
                val now = java.time.Instant.now()
                
                if (triggerTime.isAfter(now)) {
                    val delayInMillis = triggerTime.toEpochMilli() - now.toEpochMilli()
                    val data = androidx.work.workDataOf(
                        com.example.workers.MatchReminderWorker.KEY_MATCH_ID to match.id,
                        com.example.workers.MatchReminderWorker.KEY_HOME_TEAM to match.homeTeam.shortName,
                        com.example.workers.MatchReminderWorker.KEY_AWAY_TEAM to match.awayTeam.shortName,
                        com.example.workers.MatchReminderWorker.KEY_MINUTES_BEFORE to 30
                    )
                    val reminderWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.workers.MatchReminderWorker>()
                        .setInitialDelay(delayInMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .addTag("user_reminder_${match.id}")
                        .build()
                    androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                        "user_reminder_${match.id}",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        reminderWorkRequest
                    )
                    android.widget.Toast.makeText(applicationContext, "Lembrete configurado para 30 minutos antes!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(applicationContext, "A partida começa em menos de 30 minutos!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as BrasaFutApplication)
                MatchDetailsViewModel(application.favoritesRepository)
            }
        }
    }
}
