package tn.smartrecruit.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

/** Keeps the JWT, the role and the name on the phone (DataStore). */
class TokenStore(private val context: Context) {

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val ROLE = stringPreferencesKey("role")
        val NAME = stringPreferencesKey("name")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN] }
    val role: Flow<String?> = context.dataStore.data.map { it[ROLE] }
    val name: Flow<String?> = context.dataStore.data.map { it[NAME] }

    suspend fun save(token: String, role: String, name: String) {
        context.dataStore.edit {
            it[TOKEN] = token
            it[ROLE] = role
            it[NAME] = name
        }
    }

    /** Used after a profile edit so the greeting shows the new name. */
    suspend fun saveName(name: String) {
        context.dataStore.edit { it[NAME] = name }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
