package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.ThemeMode
import com.example.ui.glass.GlassTokens
import com.example.ui.glass.LocalGlassTokens
import com.example.util.ProvideLocalizedResources
import java.time.LocalTime

@Composable
fun TaskalTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val currentHour = remember { LocalTime.now().hour }
    val ambience = remember(currentHour) { DayPeriodAmbience.current(currentHour) }

    val isDark = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> systemDark
        ThemeMode.TIME_AWARE -> currentHour < 7 || currentHour >= 18
    }

    val isAmoled = settings.themeMode == ThemeMode.AMOLED
    val palette = CuratedAccentPalettes.getOrElse(settings.accentPaletteIndex) { CuratedAccentPalettes.first() }
    val basePrimary = if (settings.useCustomAccent) parseColorHex(settings.customAccentHex, palette.primary) else palette.primary

    // Time-aware ambience subtly warms at golden hour and cools at night
    val ambientShiftColor = when (ambience) {
        DayPeriodAmbience.DAWN -> Color(0xFFF472B6)
        DayPeriodAmbience.DAY -> basePrimary
        DayPeriodAmbience.GOLDEN_HOUR -> Color(0xFFFB923C)
        DayPeriodAmbience.NIGHT -> Color(0xFF60A5FA)
    }
    val effectivePrimary = if (settings.timeAwareAmbience) {
        lerp(basePrimary, ambientShiftColor, 0.16f)
    } else {
        basePrimary
    }

    val colorScheme = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (isDark) {
        val bg = if (isAmoled) Color(0xFF000000) else Color(0xFF080C16)
        val surface = if (isAmoled) Color(0xFF05070D) else Color(0xFF101728)
        darkColorScheme(
            primary = effectivePrimary,
            onPrimary = Color(0xFF04101E),
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = bg,
            onBackground = Color(0xFFF1F5F9),
            surface = surface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFFCBD5E1)
        )
    } else {
        lightColorScheme(
            primary = lerp(effectivePrimary, Color(0xFF0F172A), 0.22f),
            onPrimary = Color.White,
            secondary = lerp(palette.secondary, Color(0xFF0F172A), 0.18f),
            tertiary = palette.tertiary,
            background = Color(0xFFF3F7FC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF334155)
        )
    }

    val perfActive = remember(settings, context) { settings.isLowRamOrPerformance(context) }

    val glassTokens = remember(settings, isDark, isAmoled, effectivePrimary, palette, ambience, perfActive) {
        GlassTokens(
            isDark = isDark,
            isAmoled = isAmoled,
            accentColor = effectivePrimary,
            secondaryAccent = palette.secondary,
            tertiaryAccent = palette.tertiary,
            ambience = ambience,
            blurStrength = if (perfActive) 0f else settings.glassBlurStrength,
            tintOpacity = settings.glassTintOpacity,
            saturation = settings.glassSaturation,
            edgeGlow = settings.glassEdgeGlow,
            specularSheen = if (perfActive) 0f else settings.glassSpecularSheen,
            tiltEnabled = settings.glassTiltSensor && !perfActive,
            noiseIntensity = if (perfActive) 0f else settings.glassNoise,
            thickness = settings.glassThickness,
            cornerRadiusDp = settings.cornerRadiusDp,
            densityScale = settings.density.scale,
            animationSpeed = if (perfActive && settings.animationSpeed > 0.7f) 0.7f else settings.animationSpeed,
            hapticsEnabled = settings.hapticsEnabled,
            performanceModeActive = perfActive
        )
    }

    val typography = remember(settings.fontStyle, settings.textScale) {
        buildTaskalTypography(settings.fontStyle, settings.textScale)
    }

    val r = settings.cornerRadiusDp.coerceIn(4, 36)
    val shapes = remember(r) {
        Shapes(
            extraSmall = RoundedCornerShape((r * 0.4f).dp),
            small = RoundedCornerShape((r * 0.65f).dp),
            medium = RoundedCornerShape(r.dp),
            large = RoundedCornerShape((r * 1.25f).dp),
            extraLarge = RoundedCornerShape((r * 1.5f).dp)
        )
    }

    ProvideLocalizedResources(language = settings.language) {
        CompositionLocalProvider(LocalGlassTokens provides glassTokens) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                shapes = shapes,
                content = content
            )
        }
    }
}
