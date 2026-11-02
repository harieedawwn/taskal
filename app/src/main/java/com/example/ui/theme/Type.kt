package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.FontStyleOption

val JakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildTaskalTypography(fontOption: FontStyleOption, scale: Float): Typography {
    val s = scale.coerceIn(0.85f, 1.45f)
    val displayFamily = when (fontOption) {
        FontStyleOption.MODERN -> JakartaFontFamily
        FontStyleOption.EDITORIAL -> PlayfairFontFamily
        FontStyleOption.MONO -> JetBrainsMonoFamily
        FontStyleOption.SYSTEM -> FontFamily.SansSerif
    }
    val bodyFamily = when (fontOption) {
        FontStyleOption.MODERN -> JakartaFontFamily
        FontStyleOption.EDITORIAL -> JakartaFontFamily
        FontStyleOption.MONO -> JetBrainsMonoFamily
        FontStyleOption.SYSTEM -> FontFamily.SansSerif
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (46f * s).sp,
            lineHeight = (52f * s).sp,
            letterSpacing = (-1.0f).sp,
            fontFeatureSettings = "tnum"
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (36f * s).sp,
            lineHeight = (42f * s).sp,
            letterSpacing = (-0.75f).sp,
            fontFeatureSettings = "tnum"
        ),
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (28f * s).sp,
            lineHeight = (34f * s).sp,
            letterSpacing = (-0.5f).sp,
            fontFeatureSettings = "tnum"
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * s).sp,
            lineHeight = (30f * s).sp,
            letterSpacing = (-0.3f).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20f * s).sp,
            lineHeight = (26f * s).sp,
            letterSpacing = (-0.2f).sp
        ),
        titleLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * s).sp,
            lineHeight = (24f * s).sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * s).sp,
            lineHeight = (21f * s).sp,
            fontFeatureSettings = "tnum"
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13f * s).sp,
            lineHeight = (18f * s).sp,
            fontFeatureSettings = "tnum"
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * s).sp,
            lineHeight = (22f * s).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * s).sp,
            lineHeight = (19f * s).sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * s).sp,
            lineHeight = (16f * s).sp,
            fontFeatureSettings = "tnum"
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13f * s).sp,
            lineHeight = (18f * s).sp,
            fontFeatureSettings = "tnum"
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f * s).sp,
            lineHeight = (15f * s).sp,
            fontFeatureSettings = "tnum"
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (10f * s).sp,
            lineHeight = (13f * s).sp,
            fontFeatureSettings = "tnum"
        )
    )
}
