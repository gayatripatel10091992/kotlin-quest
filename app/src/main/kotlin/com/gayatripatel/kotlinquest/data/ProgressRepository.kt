package com.gayatripatel.kotlinquest.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gayatripatel.kotlinquest.core.Progress
import com.gayatripatel.kotlinquest.core.ProgressCodec
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface Clock {
    fun now(): Long
    fun today(): LocalDate
}

object SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now()
}

private val Context.questStore: DataStore<Preferences> by preferencesDataStore(name = "quest")

/**
 * Single source of truth for the learner's progress. The whole [Progress] is one encoded string, so every
 * update is a single atomic DataStore transaction: XP, streak and review deck can never get out of step.
 */
class ProgressRepository(context: Context, scope: CoroutineScope) {
    private val store = context.questStore
    private val key = stringPreferencesKey("progress")

    private val data: Flow<Progress> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { ProgressCodec.decode(it[key]) }

    val progress: StateFlow<Progress> = data.stateIn(scope, SharingStarted.Eagerly, Progress())

    /** Reads from disk, for callers that must not act on the empty placeholder (e.g. starting a lesson). */
    suspend fun current(): Progress = data.first()

    suspend fun update(transform: (Progress) -> Progress) {
        store.edit { prefs -> prefs[key] = ProgressCodec.encode(transform(ProgressCodec.decode(prefs[key]))) }
    }
}
