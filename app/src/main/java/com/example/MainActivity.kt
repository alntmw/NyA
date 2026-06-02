package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PerformanceRepository
import com.example.ui.StopwatchScreen
import com.example.ui.StopwatchViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room Database and Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = PerformanceRepository(database.performanceLogDao)
    val viewModelFactory = StopwatchViewModel.Factory(application, repository)

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          // Instantiate StopwatchViewModel using our Repository Factory
          val viewModel: StopwatchViewModel = viewModel(factory = viewModelFactory)
          StopwatchScreen(viewModel = viewModel)
        }
      }
    }
  }
}
