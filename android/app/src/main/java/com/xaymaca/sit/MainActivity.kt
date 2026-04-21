package com.xaymaca.sit

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xaymaca.sit.ui.nav.NavGraph
import com.xaymaca.sit.ui.theme.SITTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate — the system-level splash draws from
        // the moment the launcher icon is tapped, eliminating the blank-window flash.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = applicationContext
            val prefs = remember { context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE) }
            
            var themeMode by remember { mutableIntStateOf(prefs.getInt(SITApp.KEY_THEME_MODE, 0)) }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    if (key == SITApp.KEY_THEME_MODE) {
                        themeMode = p.getInt(SITApp.KEY_THEME_MODE, 0)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }
            
            val useDarkTheme = when (themeMode) {
                1 -> false // Light
                2 -> true  // Dark
                else -> isSystemInDarkTheme() // System (0)
            }

            SITTheme(darkTheme = useDarkTheme) {
                NavGraph()
            }
        }
    }
}
