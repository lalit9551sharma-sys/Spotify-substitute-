package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.ui.MusicViewModel
import com.example.ui.MusicViewModelFactory
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and Repository Data node
        val database = MusicDatabase.getDatabase(applicationContext)
        val repository = MusicRepository(database.musicDao())

        // 2. Create controller ViewModel via custom Provider Factory
        val viewModelFactory = MusicViewModelFactory(repository, applicationContext)
        val musicViewModel = ViewModelProvider(this, viewModelFactory)[MusicViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(viewModel = musicViewModel)
                }
            }
        }
    }
}
