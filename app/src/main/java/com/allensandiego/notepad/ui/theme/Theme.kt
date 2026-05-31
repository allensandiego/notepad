package com.allensandiego.notepad.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

fun getThemePreference(context: Context): ThemePreference {
    val prefs = context.getSharedPreferences("notepad_prefs", Context.MODE_PRIVATE)
    val name = prefs.getString("theme_preference", ThemePreference.SYSTEM.name) ?: ThemePreference.SYSTEM.name
    return try {
        ThemePreference.valueOf(name)
    } catch (e: Exception) {
        ThemePreference.SYSTEM
    }
}

fun setThemePreference(context: Context, preference: ThemePreference) {
    val prefs = context.getSharedPreferences("notepad_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("theme_preference", preference.name).apply()
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEmerald,
    onPrimary = TextLight,
    primaryContainer = PrimaryDeepTeal,
    onPrimaryContainer = TextLight,
    secondary = AccentMint,
    onSecondary = SecondaryNavy,
    background = SecondaryNavy,
    onBackground = TextLight,
    surface = SurfaceDark,
    onSurface = TextLight,
    onSurfaceVariant = TextMuted,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryEmeraldLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = PrimaryEmerald,
    onSecondary = Color.White,
    background = BackgroundLightColor,
    onBackground = TextDark,
    surface = SurfaceLightColor,
    onSurface = TextDark,
    onSurfaceVariant = TextMutedLight,
    outline = OutlineLight
)

@Composable
fun NotepadTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}