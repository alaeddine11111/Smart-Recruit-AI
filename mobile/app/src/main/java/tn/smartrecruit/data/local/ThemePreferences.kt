package tn.smartrecruit.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smartrecruit_theme"
)

/** Persists the dark-mode choice made from the top-bar toggle. */
class ThemePreferences(context: Context) {
    private val dataStore = context.applicationContext.themeDataStore

    val darkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    private companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
}
