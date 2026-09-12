package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.MatchDto
import com.example.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

sealed class CalendarUiState {
    object Loading : CalendarUiState()
    data class Success(val groupedMatches: Map<String, List<MatchDto>>) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

class CalendarViewModel : ViewModel() {
    private val repository = MatchRepository()

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()

    private var allMatches: List<MatchDto> = emptyList()

    init {
        loadMatches()
    }

    fun nextMonth() {
        val next = _currentMonth.value.clone() as Calendar
        next.add(Calendar.MONTH, 1)
        _currentMonth.value = next
        updateMatchesForCurrentMonth()
    }

    fun previousMonth() {
        val prev = _currentMonth.value.clone() as Calendar
        prev.add(Calendar.MONTH, -1)
        _currentMonth.value = prev
        updateMatchesForCurrentMonth()
    }

    private fun updateMatchesForCurrentMonth() {
        val calendar = _currentMonth.value
        val targetMonth = calendar.get(Calendar.MONTH)
        val targetYear = calendar.get(Calendar.YEAR)

        val filtered = allMatches.filter { match ->
            if (match.kickoffTime == null) return@filter false
            try {
                val p = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                p.timeZone = TimeZone.getTimeZone("UTC")
                val d = p.parse(match.kickoffTime)
                if (d != null) {
                    val cal = Calendar.getInstance()
                    cal.time = d
                    cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
                } else false
            } catch (e: Exception) {
                false
            }
        }

        val grouped = filtered.groupBy { match ->
            try {
                val p = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                p.timeZone = TimeZone.getTimeZone("UTC")
                val d = p.parse(match.kickoffTime!!)
                val f = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                f.format(d!!)
            } catch (e: Exception) {
                "Desconhecido"
            }
        }.toSortedMap()

        _uiState.value = CalendarUiState.Success(grouped)
    }

    fun getMonthName(): String {
        val f = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
        return f.format(_currentMonth.value.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = CalendarUiState.Loading
            try {
                allMatches = repository.getMatches()
                updateMatchesForCurrentMonth()
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
