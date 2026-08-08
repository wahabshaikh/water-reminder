package com.sippy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sippy.app.R
import com.sippy.app.SippyState
import kotlin.math.roundToInt

/**
 * Everything tweakable, in one sheet. Changes apply as they are made — there is
 * no save button to forget to press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    state: SippyState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onScheduleChange: (goal: Int, interval: Int, start: Int, end: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(state.name) }
    var goal by remember { mutableFloatStateOf(state.goalGlasses.toFloat()) }
    var interval by remember { mutableFloatStateOf(state.intervalMinutes.toFloat()) }
    var start by remember { mutableFloatStateOf(state.startHour.toFloat()) }
    var end by remember { mutableFloatStateOf(state.endHour.toFloat()) }

    fun pushSchedule() = onScheduleChange(
        goal.roundToInt(),
        interval.roundToInt(),
        start.roundToInt(),
        end.roundToInt(),
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 24) {
                        name = it
                        onNameChange(it)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                label = { Text(stringResource(R.string.settings_name)) },
                modifier = Modifier.fillMaxWidth(),
            )

            SettingSlider(
                label = stringResource(R.string.settings_goal),
                value = "${goal.roundToInt()} glasses",
                sliderValue = goal,
                range = 4f..14f,
                steps = 9,
                onValueChange = { goal = it },
                onValueChangeFinished = ::pushSchedule,
            )

            SettingSlider(
                label = stringResource(R.string.settings_interval),
                value = formatInterval(interval.roundToInt()),
                sliderValue = interval,
                range = 30f..180f,
                steps = 9,
                onValueChange = { interval = it },
                onValueChangeFinished = ::pushSchedule,
            )

            SettingSlider(
                label = stringResource(R.string.settings_start),
                value = formatHour(start.roundToInt()),
                sliderValue = start,
                range = 4f..12f,
                steps = 7,
                onValueChange = { start = it },
                onValueChangeFinished = ::pushSchedule,
            )

            SettingSlider(
                label = stringResource(R.string.settings_end),
                value = formatHour(end.roundToInt()),
                sliderValue = end,
                // Kept above the start hour so the window can never invert.
                range = 14f..23f,
                steps = 8,
                onValueChange = { end = it },
                onValueChangeFinished = ::pushSchedule,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_footnote),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: String,
    sliderValue: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Spacer(Modifier.height(18.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    Slider(
        value = sliderValue,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = range,
        steps = steps,
    )
}

private fun formatInterval(minutes: Int): String = when {
    minutes < 60 -> "every $minutes min"
    minutes % 60 == 0 -> "every ${minutes / 60}h"
    else -> "every ${minutes / 60}h ${minutes % 60}m"
}

private fun formatHour(hour: Int): String = when {
    hour == 0 -> "12 am"
    hour < 12 -> "$hour am"
    hour == 12 -> "12 pm"
    else -> "${hour - 12} pm"
}
