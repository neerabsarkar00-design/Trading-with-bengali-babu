package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = ObsidianBlack,
    primaryContainer = GoldContainerDark,
    onPrimaryContainer = BrightGold,
    secondary = SlateNavy,
    onSecondary = WhiteText,
    secondaryContainer = MidnightNavy,
    onSecondaryContainer = BrightGold,
    tertiary = ProfitEmerald,
    onTertiary = ObsidianBlack,
    tertiaryContainer = ProfitEmeraldContainerDark,
    onTertiaryContainer = ProfitEmerald,
    background = ObsidianBlack,
    onBackground = WhiteText,
    surface = DeepNavy,
    onSurface = WhiteText,
    surfaceVariant = SlateNavy,
    onSurfaceVariant = MutedSteelText,
    outline = SteelNavyBorder,
    outlineVariant = MidnightNavy,
    error = LossCrimson,
    onError = PureWhite,
    errorContainer = LossCrimsonContainerDark,
    onErrorContainer = LossCrimson
)

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = PureWhite,
    primaryContainer = GoldContainerLight,
    onPrimaryContainer = DeepNavy,
    secondary = WarmGoldMuted,
    onSecondary = PureWhite,
    secondaryContainer = SoftMist,
    onSecondaryContainer = InkPrimary,
    tertiary = ProfitEmeraldDark,
    onTertiary = PureWhite,
    tertiaryContainer = ProfitEmeraldContainerLight,
    onTertiaryContainer = ProfitEmeraldDark,
    background = IvorySurface,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = SoftMist,
    onSurfaceVariant = InkSecondary,
    outline = LightBorder,
    outlineVariant = SoftMist,
    error = LossCrimsonDark,
    onError = PureWhite,
    errorContainer = LossCrimsonContainerLight,
    onErrorContainer = LossCrimsonDark
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BangaliBabuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
