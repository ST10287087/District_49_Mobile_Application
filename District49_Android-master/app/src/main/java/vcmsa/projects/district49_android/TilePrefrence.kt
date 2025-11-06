package vcmsa.projects.district49_android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tiles_prefs")

object TilePreferences {
    private val TILE_DATA_KEY = stringPreferencesKey("tile_data")

    suspend fun saveTiles(context: Context, json: String) {
        context.dataStore.edit { preferences ->
            preferences[TILE_DATA_KEY] = json
        }
    }

    fun getTiles(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TILE_DATA_KEY]
        }
    }
}