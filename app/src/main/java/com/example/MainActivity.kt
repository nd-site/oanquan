package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GameDatabase
import com.example.data.MatchHistoryRepository
import com.example.model.GamePlayState
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.OAnQuanViewModel
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize database and repository inside active Context scope
                val context = LocalContext.current.applicationContext
                val database = remember { GameDatabase.getDatabase(context) }
                val repository = remember { MatchHistoryRepository(database.matchHistoryDao()) }

                // Constructor injection ViewModel Provider
                val viewModel: OAnQuanViewModel = viewModel(
                    factory = remember {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return OAnQuanViewModel(repository) as T
                            }
                        }
                    }
                )

                MainScaffold(viewModel)
            }
        }
    }
}

@Composable
fun MainScaffold(viewModel: OAnQuanViewModel) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Play, 1 = Stats
    val gameplayState by viewModel.gameState.collectAsState()
    val context = LocalContext.current

    // Trigger visual toast or audio haptic reminders based on active game flow events
    LaunchedEffect(key1 = true) {
        viewModel.gameEvents.collectLatest { event ->
            when (event) {
                "pickup" -> {} // Can play physical haptics/sound elements
                "capture" -> Toast.makeText(context, "Nice Capture!", Toast.LENGTH_SHORT).show()
                "game_over" -> Toast.makeText(context, "Match finished! Scores computed.", Toast.LENGTH_LONG).show()
                "feed" -> Toast.makeText(context, "Side empty! Fed 5 stones.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Play") },
                    label = { Text("Game Play") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Stats") },
                    label = { Text("Statistics") }
                )
            }
        }
    ) { innerPadding ->
        when (activeTab) {
            0 -> {
                if (gameplayState == GamePlayState.CONFIG) {
                    ConfigScreen(
                        viewModel = viewModel,
                        onStartGame = {
                            // Instant redirection to central layout
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    GameScreen(
                        viewModel = viewModel,
                        onBackToSetup = {
                            viewModel.resetToConfig()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
            1 -> {
                StatsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
