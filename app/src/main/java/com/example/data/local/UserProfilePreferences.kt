package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

class UserProfilePreferences(private val context: Context) {

    val userName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_USER_NAME] ?: ""
        }

    val favoriteTeamId: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_FAVORITE_TEAM_ID] ?: -1
        }

    val favoriteTeamName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_FAVORITE_TEAM_NAME] ?: ""
        }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name
        }
    }

    suspend fun saveFavoriteTeam(teamId: Int, teamName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FAVORITE_TEAM_ID] = teamId
            preferences[KEY_FAVORITE_TEAM_NAME] = teamName
        }
    }

    companion object {
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_FAVORITE_TEAM_ID = intPreferencesKey("favorite_team_id")
        private val KEY_FAVORITE_TEAM_NAME = stringPreferencesKey("favorite_team_name")
    }
}
