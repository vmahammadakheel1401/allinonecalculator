package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AppTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(this)
        val settingsManager = SettingsManager(this)

        setContent {
            val themePreference by settingsManager.themeFlow.collectAsState(initial = "System default")
            val isDarkTheme = when (themePreference) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            AppTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        database = database,
                        settingsManager = settingsManager
                    )
                }
            }
        }
    }
}
