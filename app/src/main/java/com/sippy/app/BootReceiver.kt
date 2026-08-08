package com.sippy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Alarms do not survive a reboot, an app update, or a clock change, so we book a
 * fresh one whenever the system tells us any of those happened.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val state = Prefs.get(appContext)
                if (state.onboarded) Reminders.schedule(appContext, state)
            } finally {
                pending.finish()
            }
        }
    }
}
