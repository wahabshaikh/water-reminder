package com.sippy.app

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "sippy")

/**
 * Everything Sippy knows, which is not much on purpose: who you are, how often
 * you want a nudge, and how many glasses you have had today.
 */
data class SippyState(
    val name: String = "",
    val onboarded: Boolean = false,
    val goalGlasses: Int = 8,
    val intervalMinutes: Int = 90,
    val startHour: Int = 9,
    val endHour: Int = 21,
    val glassesToday: Int = 0,
    val streak: Int = 0,
) {
    val progress: Float
        get() = if (goalGlasses <= 0) 0f else (glassesToday.toFloat() / goalGlasses).coerceIn(0f, 1f)

    val goalReached: Boolean
        get() = glassesToday >= goalGlasses
}

object Prefs {
    private val NAME = stringPreferencesKey("name")
    private val ONBOARDED = booleanPreferencesKey("onboarded")
    private val GOAL = intPreferencesKey("goal_glasses")
    private val INTERVAL = intPreferencesKey("interval_minutes")
    private val START_HOUR = intPreferencesKey("start_hour")
    private val END_HOUR = intPreferencesKey("end_hour")
    private val GLASSES = intPreferencesKey("glasses_today")
    private val COUNT_DAY = stringPreferencesKey("count_day")
    private val STREAK = intPreferencesKey("streak")
    private val STREAK_DAY = stringPreferencesKey("streak_day")

    fun flow(context: Context): Flow<SippyState> =
        context.dataStore.data.map { it.toState(LocalDate.now()) }

    suspend fun get(context: Context): SippyState =
        context.dataStore.data.first().toState(LocalDate.now())

    /**
     * The stored glass count belongs to a specific day. Rather than running a
     * midnight job, we compare the stored day on every read and treat a stale
     * count as zero — the write catches up the next time anyone drinks.
     */
    private fun Preferences.toState(today: LocalDate): SippyState {
        val storedDay = this[COUNT_DAY]
        val glasses = if (storedDay == today.toString()) this[GLASSES] ?: 0 else 0

        // A streak survives if the goal was last met today or yesterday.
        val streakDay = this[STREAK_DAY]?.let(LocalDate::parse)
        val streak = when (streakDay) {
            today, today.minusDays(1) -> this[STREAK] ?: 0
            else -> 0
        }

        return SippyState(
            name = this[NAME] ?: "",
            onboarded = this[ONBOARDED] ?: false,
            goalGlasses = this[GOAL] ?: 8,
            intervalMinutes = this[INTERVAL] ?: 90,
            startHour = this[START_HOUR] ?: 9,
            endHour = this[END_HOUR] ?: 21,
            glassesToday = glasses,
            streak = streak,
        )
    }

    suspend fun setName(context: Context, name: String) {
        context.dataStore.edit {
            it[NAME] = name.trim().take(24)
            it[ONBOARDED] = true
        }
    }

    suspend fun setSchedule(
        context: Context,
        goalGlasses: Int,
        intervalMinutes: Int,
        startHour: Int,
        endHour: Int,
    ) {
        context.dataStore.edit {
            it[GOAL] = goalGlasses
            it[INTERVAL] = intervalMinutes
            it[START_HOUR] = startHour
            it[END_HOUR] = endHour
        }
    }

    /** Logs one glass and returns the state that results. */
    suspend fun addGlass(context: Context): SippyState {
        val today = LocalDate.now()
        context.dataStore.edit { prefs ->
            val current = if (prefs[COUNT_DAY] == today.toString()) prefs[GLASSES] ?: 0 else 0
            val next = current + 1
            prefs[GLASSES] = next
            prefs[COUNT_DAY] = today.toString()

            val goal = prefs[GOAL] ?: 8
            // Bump the streak exactly once, on the glass that crosses the goal.
            if (current < goal && next >= goal && prefs[STREAK_DAY] != today.toString()) {
                val previous = prefs[STREAK_DAY]?.let(LocalDate::parse)
                prefs[STREAK] = if (previous == today.minusDays(1)) (prefs[STREAK] ?: 0) + 1 else 1
                prefs[STREAK_DAY] = today.toString()
            }
        }
        return get(context)
    }

    suspend fun undoGlass(context: Context) {
        val today = LocalDate.now()
        context.dataStore.edit { prefs ->
            val current = if (prefs[COUNT_DAY] == today.toString()) prefs[GLASSES] ?: 0 else 0
            prefs[GLASSES] = (current - 1).coerceAtLeast(0)
            prefs[COUNT_DAY] = today.toString()
        }
    }
}
