package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ghestYarPreferences by preferencesDataStore(name = "ghestyar_prefs")

class ThemePreferencesRepository(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("is_dark_theme")

    val isDarkTheme: Flow<Boolean> = context.ghestYarPreferences.data.map { prefs ->
        prefs[darkThemeKey] ?: false
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.ghestYarPreferences.edit { prefs ->
            prefs[darkThemeKey] = enabled
        }
    }
}
