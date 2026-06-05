package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HistoryViewModel
import com.example.ui.MainLayout
import com.example.ui.TranslationViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    setContent {
      val systemInDark = isSystemInDarkTheme()
      // theme preference: "system", "light", "dark"
      var themePreference by remember { 
        mutableStateOf(sharedPrefs.getString("theme_mode", "system") ?: "system") 
      }
      
      val useDarkTheme = when (themePreference) {
        "light" -> false
        "dark" -> true
        else -> systemInDark
      }
      
      MyApplicationTheme(darkTheme = useDarkTheme) {
        val translationViewModel: TranslationViewModel = viewModel()
        val historyViewModel: HistoryViewModel = viewModel()
        
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding)) {
            MainLayout(
              translationViewModel = translationViewModel,
              historyViewModel = historyViewModel,
              currentThemeMode = themePreference,
              onThemeChange = { newMode ->
                themePreference = newMode
                sharedPrefs.edit().putString("theme_mode", newMode).apply()
              }
            )
          }
        }
      }
    }
  }
}
