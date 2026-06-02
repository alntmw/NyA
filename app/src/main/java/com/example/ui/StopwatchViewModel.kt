package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PerformanceLog
import com.example.data.PerformanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StopwatchViewModel(
    application: Application,
    private val repository: PerformanceRepository
) : AndroidViewModel(application) {

    private val _elapsedTimeFlow = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTimeFlow.asStateFlow()

    private val _isRunningFlow = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

    private val _noteText = MutableStateFlow("")
    val noteText: StateFlow<String> = _noteText.asStateFlow()

    val performanceLogs: StateFlow<List<PerformanceLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var tickJob: Job? = null
    
    // Core Stopwatch accumulation properties
    private var accumulatedTimeMillis = 0L
    private var startSystemTime = 0L

    // For interval logging
    private var sessionStartTime = 0L      // Epoch millis when stopwatch is first started
    private var lastSubmissionTime = 0L    // Epoch millis of the last recorded submit key

    fun onNoteChange(text: String) {
        _noteText.value = text
    }

    fun startStopwatch() {
        if (_isRunningFlow.value) return
        _isRunningFlow.value = true
        
        val now = System.currentTimeMillis()
        if (sessionStartTime == 0L) {
            sessionStartTime = now
        }
        startSystemTime = now
        
        tickJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val sessionElapsed = accumulatedTimeMillis + (currentTime - startSystemTime)
                _elapsedTimeFlow.value = sessionElapsed
                delay(16) // ~60 FPS update rate
            }
        }
    }

    fun pauseStopwatch() {
        if (!_isRunningFlow.value) return
        _isRunningFlow.value = false
        
        accumulatedTimeMillis += System.currentTimeMillis() - startSystemTime
        _elapsedTimeFlow.value = accumulatedTimeMillis
        tickJob?.cancel()
    }

    fun resetStopwatch() {
        _isRunningFlow.value = false
        tickJob?.cancel()
        accumulatedTimeMillis = 0L
        startSystemTime = 0L
        sessionStartTime = 0L
        lastSubmissionTime = 0L
        _elapsedTimeFlow.value = 0L
    }

    fun submitLog() {
        val now = System.currentTimeMillis()
        
        // 1. Calculate precise visual duration of the stopwatch at the moment of submission
        val segmentTimeTaken = if (_isRunningFlow.value) {
            accumulatedTimeMillis + (now - startSystemTime)
        } else {
            accumulatedTimeMillis
        }
        
        // 2. Real world interval is duration between now and the previous submission.
        // If it's the first submission, it's duration since start of active stopwatch.
        // If stopwatch hasn't even run but they hit submit (0ms), we just use segmentTimeTaken.
        val interval = if (lastSubmissionTime > 0L) {
            now - lastSubmissionTime
        } else {
            if (sessionStartTime > 0L) {
                now - sessionStartTime
            } else {
                segmentTimeTaken
            }
        }
        
        // Keep track of this submit time for the next round
        lastSubmissionTime = now
        
        // 3. Reset segment timer
        if (_isRunningFlow.value) {
            accumulatedTimeMillis = 0L
            startSystemTime = now // resumes seamlessly from now
            // tickJob continues running smoothly
        } else {
            accumulatedTimeMillis = 0L
            _elapsedTimeFlow.value = 0L
        }
        
        // Capture note and clear it for the next entry
        val currentNote = _noteText.value
        _noteText.value = ""

        // 4. Save entry to Room storage
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog(
                PerformanceLog(
                    timestamp = now,
                    timeTakenMillis = segmentTimeTaken,
                    intervalMillis = interval,
                    label = currentNote
                )
            )
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLogById(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }

    companion object {
        fun formatDuration(ms: Long): String {
            val minutes = (ms % 3600000) / 60000
            val seconds = (ms % 60000) / 1000
            val hundredths = (ms % 1000) / 10
            return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
        }

        fun formatDetailTime(ms: Long): String {
            val seconds = ms / 1000.0
            return String.format("%.2fs", seconds)
        }
    }

    class Factory(
        private val application: Application,
        private val repository: PerformanceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StopwatchViewModel::class.java)) {
                return StopwatchViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
