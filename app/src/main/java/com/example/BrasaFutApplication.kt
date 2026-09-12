package com.example

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.repository.FavoritesRepository
import com.example.data.repository.MatchRepository
import com.example.services.MatchPollingService
import com.example.workers.ScheduleRemindersWorker
import java.util.concurrent.TimeUnit

class BrasaFutApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val favoritesRepository by lazy { FavoritesRepository(database.favoritesDao()) }
    lateinit var themePreferences: com.example.data.local.ThemePreferences
        private set
    lateinit var notificationPreferences: com.example.data.local.NotificationPreferences
        private set
    lateinit var userProfilePreferences: com.example.data.local.UserProfilePreferences
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Configura e inicializa a coleta do Firebase Crashlytics
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        themePreferences = com.example.data.local.ThemePreferences(this)
        userProfilePreferences = com.example.data.local.UserProfilePreferences(this)
        notificationPreferences = com.example.data.local.NotificationPreferences(this)
        val matchRepository = MatchRepository()
        val pollingService = MatchPollingService(this, favoritesRepository, matchRepository, notificationPreferences)
        pollingService.startPolling()
        
        setupWorkers()
    }

    private fun setupWorkers() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ScheduleRemindersWorker>(
            6, TimeUnit.HOURS
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ScheduleRemindersWork",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}
