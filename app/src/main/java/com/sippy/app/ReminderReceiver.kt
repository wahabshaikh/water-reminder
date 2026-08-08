package com.sippy.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires the nudge, and handles the "I drank it" button on the nudge itself so a
 * glass can be logged without ever opening the app.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        // goAsync keeps the process alive across the DataStore read/write, which
        // onReceive's own window is too short for.
        val pending = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_DRANK -> {
                        Prefs.addGlass(appContext)
                        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
                    }

                    else -> {
                        val state = Prefs.get(appContext)
                        // Nothing to nag about once the goal is met.
                        if (!state.goalReached) notify(appContext, state)
                    }
                }
                Reminders.schedule(appContext, Prefs.get(appContext))
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, state: SippyState) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val drank = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_DRANK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_droplet)
            .setContentTitle(Messages.title(state.name))
            .setContentText(Messages.body())
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(R.drawable.ic_droplet, context.getString(R.string.action_drank), drank)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_description)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_REMIND = "com.sippy.app.REMIND"
        const val ACTION_DRANK = "com.sippy.app.DRANK"
        const val CHANNEL_ID = "sippy_reminders"
        const val NOTIFICATION_ID = 42

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }
}
