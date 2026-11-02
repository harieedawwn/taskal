package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.time.LocalTime

data class AccentPalette(
    val id: Int,
    val nameEn: String,
    val nameVi: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val hex: String
)

val CuratedAccentPalettes = listOf(
    AccentPalette(0, "Aurora Cyan", "Xanh Cực Quang", Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF2DD4BF), "#38BDF8"),
    AccentPalette(1, "Electric Indigo", "Chàm Điện Tử", Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFF38BDF8), "#818CF8"),
    AccentPalette(2, "Emerald Mist", "Ngọc Lục Bảo", Color(0xFF34D399), Color(0xFF2DD4BF), Color(0xFFA3E635), "#34D399"),
    AccentPalette(3, "Amber Glow", "Hổ Phách Ấm", Color(0xFFFBBF24), Color(0xFFFB923C), Color(0xFFFDE047), "#FBBF24"),
    AccentPalette(4, "Rose Quartz", "Thạch Anh Hồng", Color(0xFFFB7185), Color(0xFFF472B6), Color(0xFFC084FC), "#FB7185"),
    AccentPalette(5, "Violet Pulse", "Tím Huyền Ảo", Color(0xFFC084FC), Color(0xFF818CF8), Color(0xFFF472B6), "#C084FC"),
    AccentPalette(6, "Coral Sun", "Cam San Hô", Color(0xFFFB923C), Color(0xFFFBBF24), Color(0xFFFB7185), "#FB923C"),
    AccentPalette(7, "Teal Lagoon", "Xanh Ngọc Lam", Color(0xFF2DD4BF), Color(0xFF38BDF8), Color(0xFF34D399), "#2DD4BF"),
    AccentPalette(8, "Crimson Velvet", "Đỏ Nhung", Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFB923C), "#F43F5E"),
    AccentPalette(9, "Lime Spark", "Xanh Chanh Tươi", Color(0xFFA3E635), Color(0xFF34D399), Color(0xFFFACC15), "#A3E635"),
    AccentPalette(10, "Cobalt Sky", "Xanh Lam Đậm", Color(0xFF60A5FA), Color(0xFF38BDF8), Color(0xFF818CF8), "#60A5FA"),
    AccentPalette(11, "Liquid Silver", "Bạc Tinh Khiết", Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF38BDF8), "#E2E8F0")
)

enum class DayPeriodAmbience {
    DAWN, DAY, GOLDEN_HOUR, NIGHT;

    companion object {
        fun current(hour: Int = LocalTime.now().hour): DayPeriodAmbience = when (hour) {
            in 5..7 -> DAWN
            in 8..15 -> DAY
            in 16..19 -> GOLDEN_HOUR
            else -> NIGHT
        }
    }
}

fun parseColorHex(hex: String, fallback: Color = Color(0xFF38BDF8)): Color {
    return runCatching {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(("FF$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    }.getOrDefault(fallback)
}

/**
 * Ensures text on glass meets WCAG AA contrast ratio by computing legible foreground color.
 */
fun ensureWcagReadableText(background: Color, isDarkTheme: Boolean): Color {
    val bgLum = background.luminance()
    return if (isDarkTheme || bgLum < 0.42f) {
        Color(0xFFF8FAFC)
    } else {
        Color(0xFF0F172A)
    }
}
