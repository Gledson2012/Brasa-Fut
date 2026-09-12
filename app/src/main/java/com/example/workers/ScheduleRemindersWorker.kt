package com.example.workers

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.*
import com.example.data.local.AppDatabase
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ScheduleRemindersWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val favoritesDao = database.favoritesDao()
            val matchRepository = MatchRepository()

            val favoriteTeams = favoritesDao.getAllFavoriteTeams().first()
            if (favoriteTeams.isEmpty()) {
                return Result.success()
            }

            val favoriteTeamIds = favoriteTeams.map { it.id }.toSet()
            val allMatches = matchRepository.getMatches()

            val workManager = WorkManager.getInstance(applicationContext)

            val now = Instant.now()

            allMatches.forEach { match ->
                if (match.status == "NOT_STARTED" && match.kickoffTime != null) {
                    if (match.homeTeam.id in favoriteTeamIds || match.awayTeam.id in favoriteTeamIds) {
                        
                        val kickoffInstant = try {
                            Instant.parse(match.kickoffTime)
                        } catch (e: Exception) {
                            null
                        }

                        if (kickoffInstant != null) {
                            val triggerTime = kickoffInstant.minusSeconds(15 * 60) // 15 minutes before
                            
                            // If the trigger time is in the future
                            if (triggerTime.isAfter(now)) {
                                val delayInMillis = triggerTime.toEpochMilli() - now.toEpochMilli()

                                val data = workDataOf(
                                    MatchReminderWorker.KEY_MATCH_ID to match.id,
                                    MatchReminderWorker.KEY_HOME_TEAM to match.homeTeam.shortName,
                                    MatchReminderWorker.KEY_AWAY_TEAM to match.awayTeam.shortName
                                )

                                val reminderWorkRequest = OneTimeWorkRequestBuilder<MatchReminderWorker>()
                                    .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                                    .setInputData(data)
                                    .addTag("reminder_\${match.id}")
                                    .build()

                                // Enqueue unique work to avoid duplicates
                                workManager.enqueueUniqueWork(
                                    "reminder_\${match.id}",
                                    ExistingWorkPolicy.REPLACE,
                                    reminderWorkRequest
                                )
                            }
                        }
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
