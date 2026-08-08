package com.sippy.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sippy.app.Messages
import com.sippy.app.R
import com.sippy.app.SippyState

@Composable
fun HomeScreen(
    state: SippyState,
    onDrink: () -> Unit,
    onUndo: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.primaryContainer.copy(alpha = 0.45f))
                )
            )
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = colors.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (state.name.isBlank()) "hi there" else "hi, ${state.name}",
                style = MaterialTheme.typography.displaySmall,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (state.goalReached) {
                    Messages.celebration(state.name)
                } else {
                    Messages.greeting(state.name, state.glassesToday, state.goalGlasses)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            Mascot(
                progress = state.progress,
                celebrating = state.goalReached,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(0.82f),
            )

            Spacer(Modifier.height(20.dp))

            GlassDots(
                filled = state.glassesToday,
                total = state.goalGlasses,
            )

            Spacer(Modifier.height(28.dp))

            SquishyButton(onClick = onDrink)

            Spacer(Modifier.height(4.dp))

            // Undo only matters once there is something to undo.
            if (state.glassesToday > 0) {
                TextButton(onClick = onUndo) {
                    Text(
                        stringResource(R.string.undo),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Spacer(Modifier.height(40.dp))
            }

            if (state.streak > 1) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.surfaceVariant,
                ) {
                    Text(
                        text = "🔥 ${state.streak} day streak",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One dot per glass in the daily goal, filling left to right. */
@Composable
private fun GlassDots(filled: Int, total: Int) {
    val colors = MaterialTheme.colorScheme

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Long goals would overflow a single row, so cap the dots and count the rest.
        val shown = total.coerceAtMost(10)
        repeat(shown) { index ->
            val isFilled = index < filled
            val scale by animateFloatAsState(
                targetValue = if (isFilled) 1f else 0.72f,
                animationSpec = spring(),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .scale(scale)
                    .background(
                        color = if (isFilled) WaterMid else colors.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50),
                    )
            )
        }
        if (total > shown) {
            Text(
                text = "+${total - shown}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** The one button that matters. Squishes when pressed, because it should. */
@Composable
private fun SquishyButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 900f),
        label = "squish",
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WaterMid,
            contentColor = Color.White,
        ),
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .height(62.dp),
    ) {
        Text(
            text = stringResource(R.string.drank_a_glass),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
