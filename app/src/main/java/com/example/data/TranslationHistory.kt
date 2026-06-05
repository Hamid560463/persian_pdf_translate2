package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class TranslationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceName: String, // e.g. "Pasted Text" or "manual.pdf"
    val originalText: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)
