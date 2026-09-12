package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.utils.NotificationHelper
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first


@kotlinx.coroutines.DelicateCoroutinesApi
class MatchAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("MatchAlarmReceiver", "Boot completed. Rescheduling match alarms...")
            kotlinx.coroutines.GlobalScope.launch {
                val db = com.example.data.local.AppDatabase.getDatabase(context)
                val matches = db.favoritesDao().getAllFavoriteMatches().first()
                val scheduler = MatchAlarmScheduler(context)
                matches.forEach { match ->
                    scheduler.scheduleMatchReminder(match)
                }
            }
            return
        }
        
        val matchId = intent.getIntExtra(EXTRA_MATCH_ID, -1)
        val homeTeam = intent.getStringExtra(EXTRA_HOME_TEAM) ?: "Mandante"
        val awayTeam = intent.getStringExtra(EXTRA_AWAY_TEAM) ?: "Visitante"
        
        if (matchId != -1) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showNotification(
                title = "Partida Favorita em Breve!",
                message = "A partida entre $homeTeam x $awayTeam começa em 30 minutos!",
                notificationId = matchId * 1000 // unique ID to avoid clashing with manual reminder
            )
        }
    }
    
    companion object {
        const val EXTRA_MATCH_ID = "extra_match_id"
        const val EXTRA_HOME_TEAM = "extra_home_team"
        const val EXTRA_AWAY_TEAM = "extra_away_team"
    }
}
