package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    
    private val _notifiedTeamIds = MutableStateFlow<Set<Int>>(
        prefs.getStringSet(KEY_NOTIFIED_TEAMS, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    )
    val notifiedTeamIds: StateFlow<Set<Int>> = _notifiedTeamIds.asStateFlow()

    fun toggleTeamNotification(teamId: Int, enable: Boolean) {
        val current = _notifiedTeamIds.value.toMutableSet()
        val topicName = "team_${teamId}"
        
        if (enable) {
            current.add(teamId)
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(topicName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            current.remove(teamId)
            try {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topicName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        prefs.edit().putStringSet(KEY_NOTIFIED_TEAMS, current.map { it.toString() }.toSet()).apply()
        _notifiedTeamIds.value = current
    }

    companion object {
        private const val KEY_NOTIFIED_TEAMS = "notified_teams"
    }
}
