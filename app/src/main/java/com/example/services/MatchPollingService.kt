package com.example.services

import android.content.Context
import com.example.data.api.MatchDto
import com.example.data.local.NotificationPreferences
import com.example.data.repository.FavoritesRepository
import com.example.data.repository.MatchRepository
import com.example.utils.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MatchPollingService(
    private val context: Context,
    private val favoritesRepository: FavoritesRepository,
    private val matchRepository: MatchRepository,
    private val notificationPreferences: NotificationPreferences
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationHelper = NotificationHelper(context)
    
    // Store previous states to detect changes
    private var previousMatches = mapOf<Int, MatchDto>()
    
    fun startPolling() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val liveMatches = matchRepository.getLiveMatches()
                    val favoriteTeams = favoritesRepository.allFavoriteTeams.first()
                    val favoriteTeamIds = favoriteTeams.map { it.id }.toSet()
                    val notifiedTeamIds = notificationPreferences.notifiedTeamIds.value
                    
                    val currentMatchesMap = liveMatches.associateBy { it.id }
                    
                    for (match in liveMatches) {
                        // Check if this match involves a favorite team OR notified team
                        if (match.homeTeam.id in favoriteTeamIds || match.awayTeam.id in favoriteTeamIds ||
                            match.homeTeam.id in notifiedTeamIds || match.awayTeam.id in notifiedTeamIds) {
                            val prevMatch = previousMatches[match.id]
                            
                            // Condition 1: Match just started
                            if (prevMatch == null && match.status != "FINISHED") {
                                // Or transitioned from NOT_STARTED to FIRST_HALF
                                notificationHelper.showNotification(
                                    title = "Partida Iniciada!",
                                    message = "${match.homeTeam.shortName} x ${match.awayTeam.shortName} começou!",
                                    notificationId = match.id * 10
                                )
                            }
                            
                            // Condition 2: Goal Update
                            if (prevMatch != null) {
                                val prevHomeScore = prevMatch.homeScore ?: 0
                                val prevAwayScore = prevMatch.awayScore ?: 0
                                val currHomeScore = match.homeScore ?: 0
                                val currAwayScore = match.awayScore ?: 0
                                
                                if (currHomeScore > prevHomeScore) {
                                    notificationHelper.showNotification(
                                        title = "GOL DO ${match.homeTeam.name.uppercase()}!",
                                        message = "${match.homeTeam.shortName} $currHomeScore - $currAwayScore ${match.awayTeam.shortName}",
                                        notificationId = match.id * 10 + 1
                                    )
                                } else if (currAwayScore > prevAwayScore) {
                                    notificationHelper.showNotification(
                                        title = "GOL DO ${match.awayTeam.name.uppercase()}!",
                                        message = "${match.homeTeam.shortName} $currHomeScore - $currAwayScore ${match.awayTeam.shortName}",
                                        notificationId = match.id * 10 + 2
                                    )
                                }
                            }
                        }
                    }
                    
                    previousMatches = currentMatchesMap
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Poll every 30 seconds
                delay(30_000)
            }
        }
    }
}
