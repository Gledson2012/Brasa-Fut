package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.data.api.TeamDto
import com.example.data.local.UserProfilePreferences
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class UserProfileUiState {
    object Idle : UserProfileUiState()
    object Loading : UserProfileUiState()
    data class Success(val teams: List<TeamDto>) : UserProfileUiState()
    data class Error(val message: String) : UserProfileUiState()
}

class UserProfileViewModel(
    private val userProfilePreferences: UserProfilePreferences
) : ViewModel() {

    private val repository = MatchRepository()

    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Idle)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _favoriteTeamId = MutableStateFlow(-1)
    val favoriteTeamId: StateFlow<Int> = _favoriteTeamId.asStateFlow()

    private val _favoriteTeamName = MutableStateFlow("")
    val favoriteTeamName: StateFlow<String> = _favoriteTeamName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            _userName.value = userProfilePreferences.userName.first()
            _favoriteTeamId.value = userProfilePreferences.favoriteTeamId.first()
            _favoriteTeamName.value = userProfilePreferences.favoriteTeamName.first()
        }
    }

    fun updateUserName(name: String) {
        _userName.value = name
        viewModelScope.launch {
            userProfilePreferences.saveUserName(name)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchTeams()
    }

    fun selectFavoriteTeam(team: TeamDto) {
        _favoriteTeamId.value = team.id
        _favoriteTeamName.value = team.name
        viewModelScope.launch {
            userProfilePreferences.saveFavoriteTeam(team.id, team.name)
        }
    }

    private fun searchTeams() {
        if (_searchQuery.value.length < 3) {
            _uiState.value = UserProfileUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = UserProfileUiState.Loading
            try {
                val teams = repository.searchTeams(_searchQuery.value)
                _uiState.value = UserProfileUiState.Success(teams)
            } catch (e: Exception) {
                _uiState.value = UserProfileUiState.Error(e.message ?: "Erro ao buscar times")
            }
        }
    }

    class Factory(private val userProfilePreferences: UserProfilePreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
                return UserProfileViewModel(userProfilePreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
