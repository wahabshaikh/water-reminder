package com.sippy.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sippy.app.ui.HomeScreen
import com.sippy.app.ui.OnboardingScreen
import com.sippy.app.ui.SettingsSheet
import com.sippy.app.ui.SippyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Name carried in by a personalised invite link, if there was one. */
    private var invitedName by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        invitedName = nameFromInvite(intent)

        setContent {
            SippyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SippyApp(invitedName)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nameFromInvite(intent).takeIf { it.isNotBlank() }?.let { invitedName = it }
    }

    /**
     * Reads `sippy://hi?name=Priya`.
     *
     * The name is only ever a *suggestion*: it pre-fills onboarding, and the
     * screen below ignores it once someone has already set themselves up, so a
     * stray link cannot rename an existing user behind their back.
     */
    private fun nameFromInvite(intent: Intent?): String {
        val data = intent?.data ?: return ""
        if (data.scheme != "sippy") return ""
        return data.getQueryParameter("name").orEmpty().trim().take(24)
    }

    @androidx.compose.runtime.Composable
    private fun SippyApp(invitedName: String) {
        val state by Prefs.flow(this).collectAsStateWithLifecycle(initialValue = null)
        var showSettings by remember { mutableStateOf(false) }

        val notificationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Declined is survivable: the app still counts glasses. */ }

        val current = state
        if (current == null) {
            // First frame before DataStore answers — hold the background colour
            // rather than flashing onboarding at someone who is already set up.
            Box(Modifier.fillMaxSize())
            return
        }

        // Keep exactly one alarm booked whenever the schedule could have changed.
        LaunchedEffect(
            current.onboarded,
            current.intervalMinutes,
            current.startHour,
            current.endHour,
        ) {
            if (current.onboarded) Reminders.schedule(this@MainActivity, current)
        }

        LaunchedEffect(current.onboarded) {
            if (current.onboarded &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !ReminderReceiver.hasNotificationPermission(this@MainActivity)
            ) {
                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (!current.onboarded) {
            OnboardingScreen(
                suggestedName = invitedName,
                onDone = { name ->
                    lifecycleScope.launch {
                        Prefs.setName(this@MainActivity, name)
                        Reminders.schedule(this@MainActivity, Prefs.get(this@MainActivity))
                    }
                },
            )
            return
        }

        HomeScreen(
            state = current,
            onDrink = {
                lifecycleScope.launch {
                    val next = Prefs.addGlass(this@MainActivity)
                    Reminders.schedule(this@MainActivity, next)
                }
            },
            onUndo = { lifecycleScope.launch { Prefs.undoGlass(this@MainActivity) } },
            onOpenSettings = { showSettings = true },
        )

        if (showSettings) {
            SettingsSheet(
                state = current,
                onDismiss = { showSettings = false },
                onNameChange = { name ->
                    lifecycleScope.launch { Prefs.setName(this@MainActivity, name) }
                },
                onScheduleChange = { goal, interval, start, end ->
                    lifecycleScope.launch {
                        Prefs.setSchedule(this@MainActivity, goal, interval, start, end)
                        Reminders.schedule(this@MainActivity, Prefs.get(this@MainActivity))
                    }
                },
            )
        }
    }
}
