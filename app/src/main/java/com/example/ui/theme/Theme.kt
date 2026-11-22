package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private fun colorSchemeForAccent(accentKey: String) = when (accentKey) {
    "GOLD_CHAMPION" -> darkColorScheme(
        primary = BlazeGold,
        onPrimary = ObsidianBg,
        primaryContainer = Color(0xFF3B2C08),
        onPrimaryContainer = BlazeGoldBright,
        secondary = BlazeOrange,
        onSecondary = TextPrimary,
        secondaryContainer = ObsidianSurfaceVariant,
        onSecondaryContainer = BlazeGoldBright,
        tertiary = GlooCyan,
        onTertiary = ObsidianBg,
        background = ObsidianBg,
        onBackground = TextPrimary,
        surface = ObsidianSurface,
        onSurface = TextPrimary,
        surfaceVariant = ObsidianSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = OutlineSubtle,
        error = DangerCrimson,
        onError = TextPrimary
    )
    "CYAN_CYBER" -> darkColorScheme(
        primary = GlooCyan,
        onPrimary = ObsidianBg,
        primaryContainer = Color(0xFF0A323D),
        onPrimaryContainer = Color(0xFF99F6FF),
        secondary = BlazeGold,
        onSecondary = ObsidianBg,
        secondaryContainer = ObsidianSurfaceVariant,
        onSecondaryContainer = BlazeGoldBright,
        tertiary = BlazeOrange,
        onTertiary = TextPrimary,
        background = ObsidianBg,
        onBackground = TextPrimary,
        surface = ObsidianSurface,
        onSurface = TextPrimary,
        surfaceVariant = ObsidianSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = OutlineSubtle,
        error = DangerCrimson,
        onError = TextPrimary
    )
    "CRIMSON_FURY" -> darkColorScheme(
        primary = DangerCrimson,
        onPrimary = TextPrimary,
        primaryContainer = Color(0xFF3F1215),
        onPrimaryContainer = Color(0xFFFFB4AB),
        secondary = BlazeGold,
        onSecondary = ObsidianBg,
        secondaryContainer = ObsidianSurfaceVariant,
        onSecondaryContainer = BlazeGoldBright,
        tertiary = GlooCyan,
        onTertiary = ObsidianBg,
        background = ObsidianBg,
        onBackground = TextPrimary,
        surface = ObsidianSurface,
        onSurface = TextPrimary,
        surfaceVariant = ObsidianSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = OutlineSubtle,
        error = DangerCrimson,
        onError = TextPrimary
    )
    "VIOLET_ELITE" -> darkColorScheme(
        primary = ChronoViolet,
        onPrimary = TextPrimary,
        primaryContainer = Color(0xFF2E1749),
        onPrimaryContainer = Color(0xFFE9D5FF),
        secondary = BlazeGold,
        onSecondary = ObsidianBg,
        secondaryContainer = ObsidianSurfaceVariant,
        onSecondaryContainer = BlazeGoldBright,
        tertiary = GlooCyan,
        onTertiary = ObsidianBg,
        background = ObsidianBg,
        onBackground = TextPrimary,
        surface = ObsidianSurface,
        onSurface = TextPrimary,
        surfaceVariant = ObsidianSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = OutlineSubtle,
        error = DangerCrimson,
        onError = TextPrimary
    )
    else -> darkColorScheme(
        primary = BlazeOrange,
        onPrimary = TextPrimary,
        primaryContainer = Color(0xFF3E1C12),
        onPrimaryContainer = BlazeGoldBright,
        secondary = BlazeGold,
        onSecondary = ObsidianBg,
        secondaryContainer = ObsidianSurfaceVariant,
        onSecondaryContainer = BlazeGoldBright,
        tertiary = GlooCyan,
        onTertiary = ObsidianBg,
        background = ObsidianBg,
        onBackground = TextPrimary,
        surface = ObsidianSurface,
        onSurface = TextPrimary,
        surfaceVariant = ObsidianSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = OutlineSubtle,
        error = DangerCrimson,
        onError = TextPrimary
    )
}

val BlazeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BlazeZoneTheme(
    accentTheme: String = "BLAZE_ORANGE",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemeForAccent(accentTheme),
        typography = AppTypography,
        shapes = BlazeShapes,
        content = content
    )
}
