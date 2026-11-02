package com.example.ui.glass

import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.AppSettings
import com.example.data.BackgroundStyle
import com.example.ui.theme.DayPeriodAmbience
import kotlin.math.cos
import kotlin.math.sin

private const val AURORA_AGSL_SHADER = """
    uniform float2 uResolution;
    uniform float uTime;
    uniform float4 uBaseBg;
    uniform float4 uColor1;
    uniform float4 uColor2;
    uniform float4 uColor3;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        float2 p1 = float2(0.25 + 0.20 * sin(uTime * 0.7), 0.22 + 0.16 * cos(uTime * 0.5));
        float2 p2 = float2(0.76 + 0.18 * cos(uTime * 0.6), 0.42 + 0.20 * sin(uTime * 0.8));
        float2 p3 = float2(0.45 + 0.22 * sin(uTime * 0.5 + 1.4), 0.78 + 0.15 * cos(uTime * 0.65));

        float d1 = exp(-3.4 * length(uv - p1));
        float d2 = exp(-3.8 * length(uv - p2));
        float d3 = exp(-3.6 * length(uv - p3));

        float3 col = uBaseBg.rgb;
        col = mix(col, uColor1.rgb, d1 * uColor1.a);
        col = mix(col, uColor2.rgb, d2 * uColor2.a);
        col = mix(col, uColor3.rgb, d3 * uColor3.a);
        return half4(col, 1.0);
    }
"""

/**
 * Living Light Backdrop:
 * - AGSL RuntimeShader aurora mesh gradient on API 33+
 * - Layered animated radial gradients on API 26..32
 * - Automatically pauses when app is backgrounded or Reduce Motion / Performance Mode is active
 * - Supports optional Custom Photo background with blur & dim
 * - Supports the gentle bloom wave of light when all tasks for the day are completed
 */
@Composable
fun AuroraBackdrop(
    settings: AppSettings,
    bloomTriggerCount: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val reduceMotion = rememberSystemReduceMotion()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val animateAurora = isResumed &&
        !reduceMotion &&
        tokens.animationSpeed > 0f &&
        !tokens.performanceModeActive &&
        settings.backgroundStyle == BackgroundStyle.AURORA

    val phase = if (animateAurora) {
        val infinite = rememberInfiniteTransition(label = "aurora_loop")
        val duration = (16000f / tokens.animationSpeed.coerceAtLeast(0.25f)).toInt().coerceAtLeast(4000)
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 6.2831855f,
            animationSpec = infiniteRepeatable(tween(duration, easing = LinearEasing), RepeatMode.Restart),
            label = "aurora_phase"
        )
        p
    } else 1.15f

    // Gentle wave of light across the background when finishing every task of the day
    val bloomAnim = remember { Animatable(0f) }
    LaunchedEffect(bloomTriggerCount) {
        if (bloomTriggerCount > 0 && !reduceMotion && tokens.animationSpeed > 0f) {
            bloomAnim.snapTo(0f)
            bloomAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (1350f / tokens.animationSpeed.coerceAtLeast(0.4f)).toInt(),
                    easing = FastOutSlowInEasing
                )
            )
            bloomAnim.snapTo(0f)
        }
    }

    val baseBg = MaterialTheme.colorScheme.background
    val ambienceTint = when (tokens.ambience) {
        DayPeriodAmbience.DAWN -> Color(0xFFF472B6)
        DayPeriodAmbience.DAY -> tokens.tertiaryAccent
        DayPeriodAmbience.GOLDEN_HOUR -> Color(0xFFFB923C)
        DayPeriodAmbience.NIGHT -> Color(0xFF312E81)
    }

    val c1 = tokens.accentColor.copy(alpha = if (tokens.isAmoled) 0.24f else if (tokens.isDark) 0.34f else 0.24f)
    val c2 = tokens.secondaryAccent.copy(alpha = if (tokens.isAmoled) 0.20f else if (tokens.isDark) 0.28f else 0.20f)
    val c3 = ambienceTint.copy(alpha = if (tokens.isAmoled) 0.16f else if (tokens.isDark) 0.24f else 0.18f)

    val agslShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(AURORA_AGSL_SHADER) }.getOrNull()
        } else null
    }

    Box(modifier = modifier.fillMaxSize().background(baseBg)) {
        if (settings.backgroundStyle == BackgroundStyle.PHOTO && settings.photoBackgroundUri.isNotBlank()) {
            AsyncImage(
                model = Uri.parse(settings.photoBackgroundUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && settings.photoBlurStrength > 0.05f && !tokens.performanceModeActive) {
                            it.blur((settings.photoBlurStrength * 24f).dp)
                        } else it
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.photoDimAlpha.coerceIn(0f, 0.85f)))
            )
        } else if (settings.backgroundStyle != BackgroundStyle.SOLID) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    agslShader != null &&
                    settings.backgroundStyle == BackgroundStyle.AURORA &&
                    !tokens.performanceModeActive
                ) {
                    agslShader.setFloatUniform("uResolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
                    agslShader.setFloatUniform("uTime", phase)
                    agslShader.setFloatUniform("uBaseBg", baseBg.red, baseBg.green, baseBg.blue, 1f)
                    agslShader.setFloatUniform("uColor1", c1.red, c1.green, c1.blue, c1.alpha)
                    agslShader.setFloatUniform("uColor2", c2.red, c2.green, c2.blue, c2.alpha)
                    agslShader.setFloatUniform("uColor3", c3.red, c3.green, c3.blue, c3.alpha)
                    drawRect(brush = ShaderBrush(agslShader))
                } else {
                    val w = size.width
                    val h = size.height
                    val p1 = Offset(w * (0.25f + 0.18f * sin(phase)), h * (0.20f + 0.15f * cos(phase * 0.8f)))
                    val p2 = Offset(w * (0.78f + 0.16f * cos(phase * 0.7f)), h * (0.45f + 0.18f * sin(phase)))
                    val p3 = Offset(w * (0.42f + 0.20f * sin(phase + 1.2f)), h * (0.82f + 0.12f * cos(phase)))

                    drawCircle(
                        brush = Brush.radialGradient(listOf(c1, Color.Transparent), center = p1, radius = size.maxDimension * 0.62f),
                        radius = size.maxDimension * 0.62f,
                        center = p1
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(c2, Color.Transparent), center = p2, radius = size.maxDimension * 0.58f),
                        radius = size.maxDimension * 0.58f,
                        center = p2
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(c3, Color.Transparent), center = p3, radius = size.maxDimension * 0.55f),
                        radius = size.maxDimension * 0.55f,
                        center = p3
                    )
                }

                // Daily Completion Light Wave Bloom
                val b = bloomAnim.value
                if (b > 0.01f) {
                    val center = Offset(size.width * 0.5f, size.height * 0.55f)
                    val waveRadius = size.maxDimension * (0.15f + b * 0.95f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (1f - b) * 0.32f),
                                tokens.accentColor.copy(alpha = (1f - b) * 0.25f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = waveRadius
                        ),
                        radius = waveRadius,
                        center = center
                    )
                }
            }
        }
        content()
    }
}
