package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.modeDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "nececity_mode_prefs"
)

class AppModePreferences(private val context: Context) {

  private val offlineModeKey = booleanPreferencesKey("offline_mode_enabled")

  val isOfflineModeFlow: Flow<Boolean> = context.modeDataStore.data
    .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
    .map { prefs -> prefs[offlineModeKey] == true }

  suspend fun setOfflineMode(enabled: Boolean) {
    withContext(Dispatchers.IO) {
      context.modeDataStore.edit { prefs ->
        prefs[offlineModeKey] = enabled
      }
    }
  }
}
