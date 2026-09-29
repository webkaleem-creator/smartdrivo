package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.db.RideHistoryRepository
import com.example.data.db.entity.RideHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

class RideHistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: RideHistoryRepository = RideHistoryRepository.getInstance(application)
    // RECOVER_TODAY_HISTORY_V1
    init {
        viewModelScope.launch {
            val today =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.ENGLISH
                ).apply {
                    timeZone =
                        TimeZone.getTimeZone("Asia/Kolkata")
                }.format(Date())

            // Recover today's legacy saved history if Room was
            // accidentally purged by the previous implementation.
            val legacyToday =
                PreferencesManager(
                    getApplication<Application>()
                ).orderHistory.value.filter { item ->
                    item.dateStr == today
                }

            if (legacyToday.isNotEmpty()) {
                repository.restoreHistoryItems(
                    legacyToday
                )
            }

            // Now delete ONLY records belonging to older dates.
            repository.purgePreviousDaysSafe()
        }
    }
val history: StateFlow<List<RideHistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val latestRide: StateFlow<RideHistoryEntity?> = repository.latestRide
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val acceptedCount: StateFlow<Int> = repository.acceptedCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
