package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.utils.NotificationHelper

class MatchReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val matchId = inputData.getInt(KEY_MATCH_ID, -1)
        val homeTeam = inputData.getString(KEY_HOME_TEAM) ?: "Time Mandante"
        val awayTeam = inputData.getString(KEY_AWAY_TEAM) ?: "Time Visitante"
        val minutesBefore = inputData.getInt(KEY_MINUTES_BEFORE, 15)

        if (matchId != -1) {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.showNotification(
                title = "Partida em Breve!",
                message = "A partida entre $homeTeam x $awayTeam começa em $minutesBefore minutos!",
                notificationId = matchId * 100 // unique ID
            )
            return Result.success()
        }
        return Result.failure()
    }

    companion object {
        const val KEY_MATCH_ID = "match_id"
        const val KEY_HOME_TEAM = "home_team"
        const val KEY_AWAY_TEAM = "away_team"
        const val KEY_MINUTES_BEFORE = "minutes_before"
    }
}
