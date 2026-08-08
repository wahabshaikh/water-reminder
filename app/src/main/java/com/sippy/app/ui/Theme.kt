package com.sippy.app.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Water reads the same in both themes; only the room around it changes.
val WaterLight = Color(0xFF7DD3FC)
val WaterMid = Color(0xFF38BDF8)
val WaterDeep = Color(0xFF0284C7)
val Blush = Color(0xFFFDA4AF)
val FaceInk = Color(0xFF0C4A6E)

private val LightColors = lightColorScheme(
    primary = WaterMid,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCF1FE),
    onPrimaryContainer = Color(0xFF06384F),
    secondary = Blush,
    background = Color(0xFFF2FAFF),
    onBackground = Color(0xFF0C4A6E),
    surface = Color.White,
    onSurface = Color(0xFF0C4A6E),
    surfaceVariant = Color(0xFFE3F3FD),
    onSurfaceVariant = Color(0xFF3E6E88),
    outline = Color(0xFFB6DCF0),
)

private val DarkColors = darkColorScheme(
    primary = WaterLight,
    onPrimary = Color(0xFF04283A),
    primaryContainer = Color(0xFF0B3A52),
    onPrimaryContainer = Color(0xFFDCF1FE),
    secondary = Blush,
    background = Color(0xFF07202E),
    onBackground = Color(0xFFDCF1FE),
    surface = Color(0xFF0C2C3E),
    onSurface = Color(0xFFDCF1FE),
    surfaceVariant = Color(0xFF12405A),
    onSurfaceVariant = Color(0xFFA5D6EF),
    outline = Color(0xFF1E5877),
)

private val SippyTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun SippyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (LocalContext.current as Activity).window
        SideEffect {
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colors, typography = SippyTypography, content = content)
}
