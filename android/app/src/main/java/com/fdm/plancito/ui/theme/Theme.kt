package com.fdm.plancito.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val PlancitoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Los componentes de Material toman estos colores; lo propio usa [Plancito.colors].
 * Sin colores dinámicos (Material You), que pisarían la paleta.
 */
private fun colorScheme(t: PlancitoColors) = (if (t.isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = t.primary,
    onPrimary = t.onPrimary,
    primaryContainer = t.primaryTint,
    onPrimaryContainer = t.primaryText,
    secondary = t.visited,
    onSecondary = Color.White,
    secondaryContainer = t.primaryTint,
    onSecondaryContainer = t.primaryText,
    background = t.bg,
    onBackground = t.text,
    surface = t.bg,
    onSurface = t.text,
    surfaceVariant = t.surfaceMuted,
    onSurfaceVariant = t.textMuted,
    surfaceTint = Color.Transparent,
    surfaceBright = t.surface,
    surfaceDim = t.bg,
    surfaceContainerLowest = t.surface,
    surfaceContainerLow = t.surface,
    surfaceContainer = t.surface,
    surfaceContainerHigh = t.surface,
    surfaceContainerHighest = t.surfaceMuted,
    inverseSurface = t.text,
    inverseOnSurface = t.bg,
    outline = t.border,
    outlineVariant = t.border,
    error = t.danger,
)

/** Sigue el modo claro/oscuro del teléfono. */
@Composable
fun PlancitoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val tokens = if (dark) DarkTokens else LightTokens

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalPlancitoColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme(tokens),
            typography = PlancitoTypography,
            shapes = PlancitoShapes,
            content = content,
        )
    }
}
