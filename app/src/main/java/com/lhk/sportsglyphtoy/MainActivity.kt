package com.lhk.sportsglyphtoy

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lhk.sportsglyphtoy.ui.theme.SportsGlyphTheme


class MainActivity : ComponentActivity() {

    private val prefsName = "sports_glyph_prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            SportsGlyphTheme {
                GlyphSettingsScreen(
                    prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
                )
            }
        }
    }
}

@Composable
fun GlyphSettingsScreen(prefs: SharedPreferences) {

    val isDarkTheme = isSystemInDarkTheme()

    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "sb11te7geey36bayvx7540d") ?: "") }
    var apiTeam by remember { mutableStateOf(prefs.getString("api_team", "New York Mets") ?: "") }
    var apiSport by remember { mutableStateOf(prefs.getString("api_sport", "mlb") ?: "") }

    MaterialTheme (
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        Surface (
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp, 64.dp)) {
                Text("Choose your Team", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it},
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiTeam,
                    onValueChange = { apiTeam = it},
                    label = { Text("Team") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiSport,
                    onValueChange = { apiSport = it},
                    label = { Text("Sport") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
