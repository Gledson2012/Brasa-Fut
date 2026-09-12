package com.example.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.FavoriteMatch
import java.time.Instant

class MatchAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleMatchReminder(match: FavoriteMatch) {
        if (match.kickoffTime == null) return
        
        try {
            val kickoffInstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant.parse(match.kickoffTime)
            } else return
            
            val triggerTime = kickoffInstant.minusSeconds(30 * 60)
            val now = Instant.now()
            
            // Only schedule if it's in the future
            if (triggerTime.isAfter(now)) {
                val intent = Intent(context, MatchAlarmReceiver::class.java).apply {
                    putExtra(MatchAlarmReceiver.EXTRA_MATCH_ID, match.id)
                    putExtra(MatchAlarmReceiver.EXTRA_HOME_TEAM, match.homeTeamName)
                    putExtra(MatchAlarmReceiver.EXTRA_AWAY_TEAM, match.awayTeamName)
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    match.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val triggerMillis = triggerTime.toEpochMilli()
                
                // Need permission for exact alarms on S+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                        Log.d("MatchAlarmScheduler", "Exact alarm scheduled for match ${match.id}")
                    } else {
                        // Fallback to inexact
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                        Log.d("MatchAlarmScheduler", "Inexact alarm scheduled for match ${match.id}")
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    Log.d("MatchAlarmScheduler", "Exact alarm scheduled for match ${match.id} (Pre-S)")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelMatchReminder(matchId: Int) {
        val intent = Intent(context, MatchAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            matchId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("MatchAlarmScheduler", "Alarm cancelled for match $matchId")
    }
}
