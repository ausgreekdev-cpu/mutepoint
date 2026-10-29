package com.mutepoint.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = CalmIndigo,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CalmIndigoLight,
    secondary = SoftTeal,
    secondaryContainer = SoftTealLight,
    error = AlertCoral,
)

private val DarkColors = darkColorScheme(
    primary = CalmIndigoLight,
    onPrimary = CalmIndigoDark,
    primaryContainer = CalmIndigoDark,
    secondary = SoftTealLight,
    secondaryContainer = SoftTeal,
    error = AlertCoral,
    surface = NightSurface,
    background = NightBackground,
)

@Composable
fun MutePointTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MutePointTypography,
        content = content,
    )
}
