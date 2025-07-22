package com.mayeoinbread.mayeosglyphtoys

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mayeoinbread.mayeosglyphtoys.ui.theme.MayeosGlyphToysTheme

class MainActivity : ComponentActivity() {

    private val prefsName = "rest_glyph_prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        if (!isNotificationServiceEnabled(this)) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        setContent {
            MayeosGlyphToysTheme {
                GlyphSettingsScreen(
                    context = this,
                    prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                )
            }
        }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NowPlayingGlyphService::class.java)
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat?.contains(cn.flattenToString()) == true
}

@Composable
fun GlyphSettingsScreen(context: Context, prefs: SharedPreferences) {

    val isDarkTheme = isSystemInDarkTheme()

    var apiUrl by remember { mutableStateOf(prefs.getString("api_url", "") ?: "") }

    MaterialTheme (
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        Surface (
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it},
                    label = { Text("API URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        prefs.edit()
                            .putString("api_url", apiUrl)
                            .apply()
                        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    }
}