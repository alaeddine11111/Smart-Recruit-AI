package tn.smartrecruit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tn.smartrecruit.ui.navigation.AppNavGraph
import tn.smartrecruit.ui.theme.SmartRecruitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreferences = (applicationContext as SmartRecruitApp).container.themePreferences
            val darkMode by themePreferences.darkMode.collectAsStateWithLifecycle(initialValue = false)
            val scope = rememberCoroutineScope()
            val activityWindow = this@MainActivity.window

            DisposableEffect(darkMode) {
                val controller = WindowCompat.getInsetsController(activityWindow, activityWindow.decorView)
                controller?.isAppearanceLightStatusBars = !darkMode
                controller?.isAppearanceLightNavigationBars = !darkMode
                onDispose { }
            }

            SmartRecruitTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        darkMode = darkMode,
                        onToggleDarkMode = {
                            scope.launch { themePreferences.setDarkMode(!darkMode) }
                        }
                    )
                }
            }
        }
    }
}
