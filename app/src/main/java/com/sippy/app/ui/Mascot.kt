package com.sippy.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin

/**
 * Sippy itself: a droplet that fills up as the day goes on, bobs gently, blinks,
 * and grins once the goal is met.
 *
 * @param progress 0f..1f — how full the droplet's water is.
 * @param celebrating draws the happy squint-eyed face.
 */
@Composable
fun Mascot(
    progress: Float,
    celebrating: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "sippy")

    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )

    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )

    // Drives a blink: the face reads this ramp and squints only near its very
    // end, so the eyes shut briefly once per cycle rather than half the time.
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blink",
    )

    // Water rises smoothly rather than snapping when a glass is logged.
    val level by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "level",
    )

    Canvas(modifier = modifier) {
        translate(top = bob * size.height * 0.012f) {
            drawSippy(level, wavePhase, blink, celebrating)
        }
    }
}

private fun DrawScope.drawSippy(
    level: Float,
    wavePhase: Float,
    blinkProgress: Float,
    celebrating: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val radius = w * 0.40f
    val cy = h - radius - h * 0.04f

    val body = dropletPath(cx, cy, radius, topY = h * 0.03f)

    // Glass shell, so an empty Sippy still reads as a droplet.
    drawPath(body, Color.White.copy(alpha = 0.55f))

    clipPath(body) {
        // Waterline measured from the bottom of the droplet up to its tip.
        val bottom = cy + radius
        val top = h * 0.03f
        val waterY = bottom - (bottom - top) * level
        val amplitude = h * 0.018f

        drawPath(
            path = wavePath(waterY, amplitude, wavePhase, bottom),
            brush = Brush.verticalGradient(
                colors = listOf(WaterLight, WaterMid, WaterDeep),
                startY = waterY,
                endY = bottom,
            ),
        )

        // A second, slower wave gives the surface some depth.
        drawPath(
            path = wavePath(waterY + amplitude * 0.7f, amplitude * 0.6f, -wavePhase * 0.7f, bottom),
            color = WaterDeep.copy(alpha = 0.25f),
        )

        drawBubbles(cx, waterY, bottom, radius, wavePhase)
    }

    // Outline last so it sits on top of the water.
    drawPath(body, WaterDeep.copy(alpha = 0.35f), style = Stroke(width = w * 0.018f))

    drawFace(cx, cy, radius, blinkProgress, celebrating)
}

/** A classic teardrop: pointed at the top, circular at the bottom. */
private fun dropletPath(cx: Float, cy: Float, radius: Float, topY: Float): Path = Path().apply {
    moveTo(cx, topY)
    cubicTo(
        cx + radius * 0.28f, topY + (cy - topY) * 0.34f,
        cx + radius, cy - radius * 0.72f,
        cx + radius, cy,
    )
    arcTo(
        rect = Rect(Offset(cx - radius, cy - radius), Size(radius * 2, radius * 2)),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 180f,
        forceMoveTo = false,
    )
    cubicTo(
        cx - radius, cy - radius * 0.72f,
        cx - radius * 0.28f, topY + (cy - topY) * 0.34f,
        cx, topY,
    )
    close()
}

private fun DrawScope.wavePath(
    waterY: Float,
    amplitude: Float,
    phase: Float,
    bottom: Float,
): Path = Path().apply {
    val steps = 24
    moveTo(0f, waterY)
    for (i in 0..steps) {
        val x = size.width * i / steps
        val y = waterY + sin(phase + i / steps.toFloat() * 2f * Math.PI.toFloat() * 1.5f) * amplitude
        lineTo(x, y)
    }
    lineTo(size.width, bottom + amplitude * 4)
    lineTo(0f, bottom + amplitude * 4)
    close()
}

private fun DrawScope.drawBubbles(
    cx: Float,
    waterY: Float,
    bottom: Float,
    radius: Float,
    phase: Float,
) {
    if (bottom - waterY < radius * 0.3f) return // too little water to hold bubbles

    val bubbles = listOf(
        Triple(-0.42f, 0.15f, 0.055f),
        Triple(0.30f, 0.55f, 0.040f),
        Triple(-0.10f, 0.80f, 0.032f),
    )

    bubbles.forEachIndexed { index, (dx, seed, scale) ->
        // Each bubble drifts upward on its own offset loop, wrapping at the top.
        val travel = ((phase / (2 * Math.PI.toFloat())) + seed) % 1f
        val y = bottom - (bottom - waterY) * travel
        drawCircle(
            color = Color.White.copy(alpha = 0.35f * (1f - travel)),
            radius = radius * scale * (1f + index * 0.1f),
            center = Offset(cx + radius * dx, y),
        )
    }
}

private fun DrawScope.drawFace(
    cx: Float,
    cy: Float,
    radius: Float,
    blinkProgress: Float,
    celebrating: Boolean,
) {
    val eyeOffsetX = radius * 0.38f
    val eyeY = cy - radius * 0.08f
    val eyeRadius = radius * 0.10f
    val strokeWidth = radius * 0.075f

    // Blink occupies only the last sliver of the cycle.
    val blinking = blinkProgress > 0.94f
    val squinting = celebrating || blinking

    listOf(-1f, 1f).forEach { side ->
        val ex = cx + side * eyeOffsetX
        if (squinting) {
            // ^ ^ — a happy arc instead of a round eye.
            drawPath(
                path = Path().apply {
                    moveTo(ex - eyeRadius, eyeY + eyeRadius * 0.5f)
                    quadraticTo(ex, eyeY - eyeRadius * 0.9f, ex + eyeRadius, eyeY + eyeRadius * 0.5f)
                },
                color = FaceInk,
                style = Stroke(width = strokeWidth),
            )
        } else {
            drawCircle(FaceInk, radius = eyeRadius, center = Offset(ex, eyeY))
            drawCircle(
                Color.White.copy(alpha = 0.9f),
                radius = eyeRadius * 0.34f,
                center = Offset(ex + eyeRadius * 0.3f, eyeY - eyeRadius * 0.34f),
            )
        }
    }

    // Blush
    listOf(-1f, 1f).forEach { side ->
        drawCircle(
            color = Blush.copy(alpha = 0.55f),
            radius = radius * 0.13f,
            center = Offset(cx + side * radius * 0.60f, eyeY + radius * 0.26f),
        )
    }

    // Mouth: a wider smile when celebrating.
    val mouthWidth = radius * (if (celebrating) 0.30f else 0.22f)
    val mouthDepth = radius * (if (celebrating) 0.30f else 0.18f)
    val mouthY = eyeY + radius * 0.30f

    drawPath(
        path = Path().apply {
            moveTo(cx - mouthWidth, mouthY)
            quadraticTo(cx, mouthY + mouthDepth, cx + mouthWidth, mouthY)
        },
        color = FaceInk,
        style = Stroke(width = strokeWidth),
    )
}
