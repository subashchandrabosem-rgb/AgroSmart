package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MintAccent,
    secondary = PaleLeaf,
    tertiary = ClayOrange,
    background = ShadowSlate,
    surface = DarkCardBg,
    onPrimary = DarkForestGreen,
    onSecondary = DarkForestGreen,
    onBackground = LightSageText,
    onSurface = LightSageText
)

private val LightColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    secondary = HighDensitySecondary,
    tertiary = HighDensitySage,
    background = HighDensityBg,
    surface = HighDensityCardWhite,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = HighDensityText,
    onBackground = HighDensityText,
    onSurface = HighDensityText,
    outline = HighDensityBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false to strictly enforce our custom agriculture theme, keeping UI highly distinctive
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
