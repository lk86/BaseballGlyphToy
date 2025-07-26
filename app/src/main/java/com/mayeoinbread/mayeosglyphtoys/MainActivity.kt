package com.mayeoinbread.mayeosglyphtoys

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mayeoinbread.mayeosglyphtoys.DataUtils.fetchAndFlattenJson
import com.mayeoinbread.mayeosglyphtoys.ui.theme.MayeosGlyphToysTheme
import androidx.core.content.edit

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
                    prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                )
            }
        }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NowPlayingListenerService::class.java)
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat?.contains(cn.flattenToString()) == true
}

@Composable
fun GlyphSettingsScreen(prefs: SharedPreferences) {

    val isDarkTheme = isSystemInDarkTheme()

    var apiUrl by remember { mutableStateOf(prefs.getString("api_url", "") ?: "") }
    var flattenedFields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val selectedKeys = remember { mutableStateMapOf<String, Boolean>() }

    MaterialTheme (
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        Surface (
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp, 64.dp)) {
                Text("Enter API Endpoint", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it},
                    label = { Text("API URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        prefs.edit { putString("api_url", apiUrl) }
                        fetchAndFlattenJson(apiUrl) { result ->
                            flattenedFields = result
                            result.keys.forEach { key ->
                                selectedKeys[key] = prefs.getStringSet("selected_fields", emptySet())?.contains(key) == true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Fetch")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (flattenedFields.isNotEmpty()) {
                    Text("Select Fields to Display", style = MaterialTheme.typography.titleMedium)
                    LazyColumn {
                        items(flattenedFields.keys.toList()) { key ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = selectedKeys[key] ?: false,
                                    onCheckedChange = { changedVal ->
                                        selectedKeys[key] = changedVal
                                        val updated = selectedKeys.filterValues { it }.keys
                                        prefs.edit {
                                            putStringSet(
                                                "selected_fields",
                                                updated.toSet()
                                            )
                                        }
                                    }
                                )
                                Text(key, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
