package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<TranslationHistory>> = historyDao.getAllHistory()

    suspend fun insert(history: TranslationHistory): Long {
        return historyDao.insertHistory(history)
    }

    suspend fun delete(history: TranslationHistory) {
        historyDao.deleteHistory(history)
    }

    suspend fun clearAll() {
        historyDao.clearAllHistory()
    }
}
