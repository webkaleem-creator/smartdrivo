package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.RideHistoryRepository
import com.example.data.db.entity.RideHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RideHistoryViewModel(
    application: Application,
    private val repository: RideHistoryRepository = RideHistoryRepository.getInstance(application)
) : AndroidViewModel(application) {

    val history: StateFlow<List<RideHistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val latestRide: StateFlow<RideHistoryEntity?> = repository.latestRide
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val acceptedCount: StateFlow<Int> = repository.acceptedCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
