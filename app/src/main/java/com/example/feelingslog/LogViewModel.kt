package com.example.feelingslog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(private val repository: LogRepository) : ViewModel() {
    
    // Subscribe to all logs from Room
    val logs: StateFlow<List<FeelingLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        seedDataIfEmpty()
    }

    private fun seedDataIfEmpty() {
        viewModelScope.launch {
            val currentLogs = repository.allLogs.first()
            if (currentLogs.isEmpty()) {
                val samples = listOf(
                    FeelingLog(title = "Morning Walk", blocks = listOf(LogContentBlock.Text("The air was so fresh today.")), tags = listOf("happy", "calm")),
                    FeelingLog(title = "Coffee with Friend", blocks = listOf(LogContentBlock.Text("Caught up with Sarah after a long time.")), tags = listOf("happy")),
                    FeelingLog(title = "Rainy Afternoon", blocks = listOf(LogContentBlock.Text("Reading a book while it pours outside.")), tags = listOf("calm")),
                    FeelingLog(title = "Gym Session", blocks = listOf(LogContentBlock.Text("New PR today! Feeling strong.")), tags = listOf("excited", "happy"))
                )
                samples.forEach { repository.saveLog(it) }
            }
        }
    }

    suspend fun getLog(id: String): FeelingLog? {
        return repository.getLog(id)
    }

    fun saveLog(log: FeelingLog) {
        viewModelScope.launch {
            repository.saveLog(log)
        }
    }

    fun addLog(title: String, content: String) {
        val newLog = FeelingLog(
            title = title,
            blocks = listOf(LogContentBlock.Text(content))
        )
        saveLog(newLog)
    }

    fun updateLog(log: FeelingLog, title: String, content: String) {
        val updatedBlocks = log.blocks.toMutableList()
        val textIndex = updatedBlocks.indexOfFirst { it is LogContentBlock.Text }
        
        if (textIndex != -1) {
            updatedBlocks[textIndex] = LogContentBlock.Text(content)
        } else {
            updatedBlocks.add(0, LogContentBlock.Text(content))
        }

        saveLog(log.copy(title = title, blocks = updatedBlocks))
    }

    fun deleteLog(log: FeelingLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }
}
