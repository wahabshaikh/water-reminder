package com.sippy.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/**
 * The scheduling window is the one piece of Sippy with real edge cases: it has
 * to keep nudges inside waking hours no matter when the alarm happens to fire.
 */
class RemindersTest {

    private val state = SippyState(
        intervalMinutes = 90,
        startHour = 9,
        endHour = 21,
    )

    private fun next(at: String) = Reminders.nextTrigger(state, LocalDateTime.parse(at))

    @Test
    fun `mid-window reminders land one interval later`() {
        assertEquals(LocalDateTime.parse("2026-08-08T11:30"), next("2026-08-08T10:00"))
    }

    @Test
    fun `early morning waits for the window to open instead of nudging`() {
        assertEquals(LocalDateTime.parse("2026-08-08T09:00"), next("2026-08-08T06:15"))
    }

    @Test
    fun `an interval that would spill past bedtime rolls to tomorrow morning`() {
        // 20:00 + 90m = 21:30, past the 21:00 cut-off.
        assertEquals(LocalDateTime.parse("2026-08-09T09:00"), next("2026-08-08T20:00"))
    }

    @Test
    fun `late night rolls to tomorrow morning`() {
        assertEquals(LocalDateTime.parse("2026-08-09T09:00"), next("2026-08-08T23:40"))
    }

    @Test
    fun `the last slot before the cut-off is still used`() {
        assertEquals(LocalDateTime.parse("2026-08-08T20:55"), next("2026-08-08T19:25"))
    }

    @Test
    fun `a short interval still respects the window`() {
        val fast = state.copy(intervalMinutes = 30)
        assertEquals(
            LocalDateTime.parse("2026-08-09T09:00"),
            Reminders.nextTrigger(fast, LocalDateTime.parse("2026-08-08T20:45")),
        )
    }
}

/**
 * Guards the daily-reset and goal arithmetic that [SippyState] exposes to the UI.
 */
class SippyStateTest {

    @Test
    fun `progress is clamped when the goal is overshot`() {
        assertEquals(1f, SippyState(goalGlasses = 8, glassesToday = 12).progress, 0.001f)
    }

    @Test
    fun `progress is proportional below the goal`() {
        assertEquals(0.5f, SippyState(goalGlasses = 8, glassesToday = 4).progress, 0.001f)
    }

    @Test
    fun `a zero goal cannot divide by zero`() {
        assertEquals(0f, SippyState(goalGlasses = 0, glassesToday = 3).progress, 0.001f)
    }
}
