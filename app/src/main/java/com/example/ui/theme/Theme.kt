package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.TemaApp

data class JakeroCustomPalette(
    val pageBackground: Color,
    val surfaceBackground: Color,
    val cardBackground: Color,
    val darkBlockBackground: Color,
    val textColor: Color,
    val textSecondaryColor: Color,
    val borderColor: Color,
    val brandLime: Color = JakeroLime,
    val brandLimeText: Color = JakeroLimeDarkText,
    val brandTeal: Color = JakeroTeal,
    val brandBrown: Color = JakeroBrown,
    val alertRedBg: Color = AlertRedDarkBg,
    val alertRedText: Color = AlertRedText,
    val alertBlueBg: Color = AlertBlueDarkBg,
    val alertBlueText: Color = AlertBlueText
)

val LocalJakeroPalette = staticCompositionLocalOf {
    JakeroCustomPalette(
        pageBackground = LightBackground,
        surfaceBackground = LightSurface,
        cardBackground = LightCard,
        darkBlockBackground = JakeroTinta,
        textColor = LightText,
        textSecondaryColor = LightSecondaryText,
        borderColor = LightBorder
    )
}

private val LightJakeroPalette = JakeroCustomPalette(
    pageBackground = LightBackground,
    surfaceBackground = LightSurface,
    cardBackground = LightCard,
    darkBlockBackground = JakeroTinta,
    textColor = LightText,
    textSecondaryColor = LightSecondaryText,
    borderColor = LightBorder
)

private val DarkJakeroPalette = JakeroCustomPalette(
    pageBackground = DarkBackground,
    surfaceBackground = DarkSurface,
    cardBackground = DarkCard,
    darkBlockBackground = DarkBlock,
    textColor = DarkText,
    textSecondaryColor = DarkSecondaryText,
    borderColor = DarkBorder,
    alertRedBg = Color(0xFF3A1A10),
    alertRedText = Color(0xFFFF8A75),
    alertBlueBg = Color(0xFF10333A),
    alertBlueText = Color(0xFF76D8E8)
)

private val JakeroThemePalette = JakeroCustomPalette(
    pageBackground = JakeroThemeBackground,
    surfaceBackground = JakeroThemeSurface,
    cardBackground = JakeroThemeCard,
    darkBlockBackground = JakeroThemeBlock,
    textColor = JakeroThemeText,
    textSecondaryColor = JakeroThemeSecondaryText,
    borderColor = JakeroThemeBorder
)

private val LightM3ColorScheme = lightColorScheme(
    primary = JakeroLime,
    onPrimary = JakeroLimeDarkText,
    secondary = JakeroTeal,
    onSecondary = Color.White,
    tertiary = JakeroBrown,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onBackground = LightText,
    onSurface = LightText,
    outline = LightBorder
)

private val DarkM3ColorScheme = darkColorScheme(
    primary = JakeroLime,
    onPrimary = JakeroLimeDarkText,
    secondary = JakeroTeal,
    onSecondary = Color.White,
    tertiary = JakeroBrown,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = DarkText,
    onSurface = DarkText,
    outline = DarkBorder
)

private val JakeroM3ColorScheme = lightColorScheme(
    primary = JakeroLime,
    onPrimary = JakeroLimeDarkText,
    secondary = JakeroThemeBlock,
    onSecondary = Color.White,
    tertiary = JakeroBrown,
    background = JakeroThemeBackground,
    surface = JakeroThemeSurface,
    surfaceVariant = JakeroThemeCard,
    onBackground = JakeroThemeText,
    onSurface = JakeroThemeText,
    outline = JakeroThemeBorder
)

object JakeroTheme {
    val palette: JakeroCustomPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalJakeroPalette.current
}

@Composable
fun JakeroTudoTheme(
    tema: TemaApp = TemaApp.CLARO,
    content: @Composable () -> Unit
) {
    val (m3ColorScheme, customPalette) = when (tema) {
        TemaApp.CLARO -> LightM3ColorScheme to LightJakeroPalette
        TemaApp.ESCURO -> DarkM3ColorScheme to DarkJakeroPalette
        TemaApp.JAKERO -> JakeroM3ColorScheme to JakeroThemePalette
    }

    CompositionLocalProvider(LocalJakeroPalette provides customPalette) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = Typography,
            content = content
        )
    }
}
