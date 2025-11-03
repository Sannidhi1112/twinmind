package com.voicerecorder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6DB6FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003258),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF00497D),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFCCE5FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFBAC9DB),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF243240),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3A4857),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD6E5F7)
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0061A4),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD1E4FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001D36),
    secondary = androidx.compose.ui.graphics.Color(0xFF535F70),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD7E3F7),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF101C2B)
)

@Composable
fun VoiceRecorderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
