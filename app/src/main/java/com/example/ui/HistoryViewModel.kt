package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryRepository
import com.example.data.TranslationHistory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HistoryRepository

    val allHistory: StateFlow<List<TranslationHistory>>

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.historyDao()
        repository = HistoryRepository(dao)
        allHistory = repository.allItemsState()
    }

    private fun HistoryRepository.allItemsState(): StateFlow<List<TranslationHistory>> {
        return allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addHistory(sourceName: String, originalText: String, translatedText: String) {
        viewModelScope.launch {
            repository.insert(
                TranslationHistory(
                    sourceName = sourceName,
                    originalText = originalText,
                    translatedText = translatedText
                )
            )
        }
    }

    fun deleteHistory(history: TranslationHistory) {
        viewModelScope.launch {
            repository.delete(history)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
