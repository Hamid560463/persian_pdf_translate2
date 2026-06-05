package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TranslationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TranslationHistory): Long

    @Delete
    suspend fun deleteHistory(history: TranslationHistory)

    @Query("DELETE FROM translation_history")
    suspend fun clearAllHistory()
}
