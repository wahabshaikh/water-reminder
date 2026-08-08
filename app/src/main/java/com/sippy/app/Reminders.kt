package com.sippy.app

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import java.time.LocalDateTime
import java.time.ZoneId

/** Schedules the one alarm Sippy ever has pending. */
object Reminders {

    private const val REQUEST_CODE = 1001

    /**
     * Works out when the next nudge should land and books it.
     *
     * Only ever one alarm is outstanding: the receiver reschedules itself after
     * firing. That keeps the alarm from surviving as a stale duplicate when the
     * interval or the active hours change.
     */
    fun schedule(context: Context, state: SippyState, from: LocalDateTime = LocalDateTime.now()) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val next = nextTrigger(state, from)
        val triggerAtMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (canScheduleExact(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        } else {
            // Inexact but still Doze-friendly: the OS may hold it back a few
            // minutes, which is invisible at a 90-minute cadence.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let(alarmManager::cancel)
    }

    @SuppressLint("NewApi") // canScheduleExactAlarms is guarded by the SDK check.
    private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * The next moment inside the user's waking window.
     *
     * Reminders land [SippyState.intervalMinutes] apart between [SippyState.startHour]
     * and [SippyState.endHour]. Anything that would fall outside that window rolls
     * forward to the start of the next one, so nobody gets buzzed at 3am.
     */
    internal fun nextTrigger(state: SippyState, from: LocalDateTime): LocalDateTime {
        val windowStartToday = from.toLocalDate().atTime(state.startHour, 0)
        val windowEndToday = from.toLocalDate().atTime(state.endHour.coerceAtMost(23), 0)

        // Before today's window opens, wait for it rather than nudging early.
        if (from.isBefore(windowStartToday)) return windowStartToday

        val candidate = from.plusMinutes(state.intervalMinutes.toLong())
        if (candidate.isBefore(windowEndToday)) return candidate

        return windowStartToday.plusDays(1)
    }
}
