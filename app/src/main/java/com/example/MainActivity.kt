package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
    setContent {
      MyApplicationTheme {
        val translationViewModel: TranslationViewModel = viewModel()
        val historyViewModel: HistoryViewModel = viewModel()
        
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding)) {
            MainLayout(
              translationViewModel = translationViewModel,
              historyViewModel = historyViewModel
            )
          }
        }
      }
    }
  }
}
