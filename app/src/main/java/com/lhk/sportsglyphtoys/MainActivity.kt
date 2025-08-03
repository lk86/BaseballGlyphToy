package com.lhk.sportsglyphtoys

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.lhk.sportsglyphtoys.DataUtils.fetchTeams
import com.lhk.sportsglyphtoys.ui.theme.SportsGlyphTheme


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

    var mlbTeam by remember { mutableStateOf(prefs.getString("mlb_team", "") ?: "") }
    var nflTeam by remember { mutableStateOf(prefs.getString("nfl_team", "") ?: "") }

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
                    value = mlbTeam,
                    onValueChange = { mlbTeam = it},
                    label = { Text("MLB Team") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nflTeam,
                    onValueChange = { nflTeam = it},
                    label = { Text("NFL Team") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        prefs.edit { putString("mlb_team", mlbTeam) }
                        prefs.edit { putString("nfl_team", nflTeam) }

                        fetchTeams("https://statsapi.mlb.com/api/v1/teams/") { result ->
                            prefs.edit { putString("mlb_team_id", result[mlbTeam]) }
                            prefs.edit { putString("nfl_team_id", result[nflTeam]) }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Fetch Team IDs")
                }
            }
        }
    }
}
