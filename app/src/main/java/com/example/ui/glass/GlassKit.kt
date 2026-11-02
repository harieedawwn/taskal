package com.example.ui.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.GlassThickness
import com.example.ui.theme.DayPeriodAmbience
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class GlassTokens(
    val isDark: Boolean = true,
    val isAmoled: Boolean = false,
    val accentColor: Color = Color(0xFF38BDF8),
    val secondaryAccent: Color = Color(0xFF818CF8),
    val tertiaryAccent: Color = Color(0xFF2DD4BF),
    val ambience: DayPeriodAmbience = DayPeriodAmbience.DAY,
    val blurStrength: Float = 0.72f,
    val tintOpacity: Float = 0.15f,
    val saturation: Float = 1.15f,
    val edgeGlow: Float = 0.65f,
    val specularSheen: Float = 0.45f,
    val tiltEnabled: Boolean = true,
    val noiseIntensity: Float = 0.18f,
    val thickness: GlassThickness = GlassThickness.REGULAR,
    val cornerRadiusDp: Int = 22,
    val densityScale: Float = 1.0f,
    val animationSpeed: Float = 1.0f,
    val hapticsEnabled: Boolean = true,
    val performanceModeActive: Boolean = false
)

val LocalGlassTokens = staticCompositionLocalOf { GlassTokens() }

@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        scale == 0f
    }
}

@Composable
fun rememberDeviceTiltOffset(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val tiltState = remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled, context) {
        if (!enabled) {
            tiltState.value = Offset.Zero
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val v = event?.values ?: return
                    if (v.size >= 2) {
                        val nx = (v[0] / 9.8f).coerceIn(-1f, 1f)
                        val ny = (v[1] / 9.8f).coerceIn(-1f, 1f)
                        tiltState.value = Offset(nx, ny)
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (sensor != null) {
                sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        }
    }
    return tiltState
}

/**
 * Core Liquid Glass surface used by cards, dock, sheets, dialogs, pickers, chips, and text fields.
 * Draws translucent tint + saturation boost + 1px luminous diagonal edge + soft inner shadow +
 * fine procedural noise + slow specular sheen that drifts and reacts to device tilt.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    thickness: GlassThickness? = null,
    cornerRadius: Dp? = null,
    accentTint: Color? = null,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val reduceMotion = rememberSystemReduceMotion()
    val effectiveThickness = thickness ?: tokens.thickness
    val radius = cornerRadius ?: tokens.cornerRadiusDp.dp
    val shape = RoundedCornerShape(radius)
    val tilt by rememberDeviceTiltOffset(tokens.tiltEnabled && !reduceMotion && tokens.animationSpeed > 0f)

    val driftPhase = if (!reduceMotion && tokens.animationSpeed > 0f && !tokens.performanceModeActive) {
        val infinite = rememberInfiniteTransition(label = "glass_sheen")
        val duration = (7500f / tokens.animationSpeed.coerceAtLeast(0.25f)).toInt().coerceAtLeast(2000)
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duration, easing = LinearEasing), RepeatMode.Reverse),
            label = "sheen_phase"
        )
        p
    } else 0.35f

    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = tokens.accentColor.copy(alpha = 0.25f)),
            onClick = {
                if (tokens.hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
        )
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .then(clickModifier)
            .drawWithCache {
                val rPx = radius.toPx()
                val cr = CornerRadius(rPx, rPx)
                val baseAlpha = (tokens.tintOpacity + effectiveThickness.alphaBoost + if (elevated) 0.06f else 0f)
                    .coerceIn(0.06f, 0.88f)
                val baseSurfaceColor = if (tokens.isDark) {
                    if (tokens.isAmoled) Color(0xFF0A0F1D) else Color(0xFF141E33)
                } else {
                    Color(0xFFFFFFFF)
                }
                val tintColor = accentTint ?: tokens.accentColor
                val topLeftTint = baseSurfaceColor.copy(alpha = (baseAlpha * 1.12f).coerceAtMost(0.92f))
                val bottomRightTint = baseSurfaceColor.copy(alpha = (baseAlpha * 0.78f).coerceAtMost(0.85f))
                val colorSplash = tintColor.copy(alpha = (0.06f * tokens.saturation).coerceIn(0.01f, 0.22f))

                val fillBrush = Brush.linearGradient(
                    colors = listOf(topLeftTint, bottomRightTint),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )

                // Specular sheen responsive to drift + device tilt
                val sheenCenterX = size.width * (0.2f + 0.6f * driftPhase + tilt.x * 0.25f)
                val sheenCenterY = size.height * (0.15f + 0.4f * (1f - driftPhase) + tilt.y * 0.25f)
                val sheenAlpha = (tokens.specularSheen * 0.16f).coerceIn(0f, 0.25f)
                val sheenBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = sheenAlpha),
                        tintColor.copy(alpha = sheenAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(sheenCenterX, sheenCenterY),
                    radius = (size.maxDimension * 0.75f).coerceAtLeast(60f)
                )

                // 1px luminous diagonal edge (bright at top-left, faint at bottom-right)
                val edgeAlphaTop = (tokens.edgeGlow * effectiveThickness.borderBoost * 1.15f).coerceIn(0.08f, 0.85f)
                val edgeAlphaBottom = (edgeAlphaTop * 0.18f).coerceIn(0.02f, 0.25f)
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = edgeAlphaTop),
                        tintColor.copy(alpha = edgeAlphaTop * 0.55f),
                        Color.White.copy(alpha = edgeAlphaBottom)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )

                onDrawBehind {
                    // 1. Base frosted glass fill
                    drawRoundRect(brush = fillBrush, cornerRadius = cr)
                    // 2. Saturation / accent chromatic tint
                    drawRoundRect(color = colorSplash, cornerRadius = cr)
                    // 3. Drifting + tilt-reactive specular sheen
                    if (sheenAlpha > 0.005f) {
                        drawRoundRect(brush = sheenBrush, cornerRadius = cr)
                    }
                    // 4. Procedural fine micro-noise dots (cheap deterministic stipple)
                    if (tokens.noiseIntensity > 0.02f && !tokens.performanceModeActive) {
                        val noiseAlpha = (tokens.noiseIntensity * 0.045f).coerceIn(0f, 0.04f)
                        val step = 28f
                        var y = 10f
                        while (y < size.height) {
                            var x = ((y * 17f) % step)
                            while (x < size.width) {
                                drawCircle(
                                    color = Color.White.copy(alpha = noiseAlpha),
                                    radius = 0.8f,
                                    center = Offset(x, y)
                                )
                                x += step
                            }
                            y += step
                        }
                    }
                    // 5. 1px luminous gradient border
                    drawRoundRect(
                        brush = borderBrush,
                        cornerRadius = cr,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            .padding(contentPadding),
        content = content
    )
}

/**
 * Glass Pill Chip used across filters, natural language composer tokens, and tags.
 */
@Composable
fun GlassChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accentColor: Color = LocalGlassTokens.current.accentColor,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val tokens = LocalGlassTokens.current
    GlassSurface(
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        thickness = if (selected) GlassThickness.THICK else GlassThickness.THIN,
        cornerRadius = 999.dp,
        accentTint = if (selected) accentColor else null,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) leadingIcon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
            )
            if (trailingIcon != null) trailingIcon()
        }
    }
}

/**
 * Signature #2: Droplet Check.
 * Completing a task turns the checkbox into a filled liquid droplet, sends a soft ripple,
 * and bursts subtle light particles around the droplet.
 */
@Composable
fun DropletCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalGlassTokens.current.accentColor,
    contentDesc: String = "Toggle task completion"
) {
    val tokens = LocalGlassTokens.current
    val haptics = LocalHapticFeedback.current
    val fillProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "droplet_fill"
    )

    val burstAnim = remember { Animatable(0f) }
    LaunchedEffect(checked) {
        if (checked && tokens.animationSpeed > 0f && !tokens.performanceModeActive) {
            burstAnim.snapTo(0f)
            burstAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (520f / tokens.animationSpeed.coerceAtLeast(0.4f)).toInt(),
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            burstAnim.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                this.contentDescription = contentDesc
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onClick = {
                    if (tokens.hapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onCheckedChange(!checked)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = size.minDimension * 0.34f

            // Light particle burst when completing
            val b = burstAnim.value
            if (b > 0.01f && b < 0.99f) {
                val rippleR = baseRadius * (1f + b * 1.45f)
                drawCircle(
                    color = tint.copy(alpha = (1f - b) * 0.45f),
                    radius = rippleR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
                for (i in 0 until 8) {
                    val angle = (i * 45.0) * (PI / 180.0)
                    val dist = baseRadius * (0.9f + b * 1.3f)
                    val px = cx + (cos(angle) * dist).toFloat()
                    val py = cy + (sin(angle) * dist).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = (1f - b) * 0.85f),
                        radius = (2.6f * (1f - b)).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            // Morphing droplet path when filled, rounded circle when unchecked
            val dropPath = Path().apply {
                val topY = cy - baseRadius * (1f + 0.22f * fillProgress)
                moveTo(cx, topY)
                cubicTo(
                    cx + baseRadius * 1.15f, cy - baseRadius * 0.2f,
                    cx + baseRadius * 1.05f, cy + baseRadius * 1.05f,
                    cx, cy + baseRadius
                )
                cubicTo(
                    cx - baseRadius * 1.05f, cy + baseRadius * 1.05f,
                    cx - baseRadius * 1.15f, cy - baseRadius * 0.2f,
                    cx, topY
                )
                close()
            }

            if (fillProgress > 0.02f) {
                drawPath(
                    path = dropPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            tint.copy(alpha = fillProgress),
                            tokens.secondaryAccent.copy(alpha = fillProgress)
                        ),
                        start = Offset(cx - baseRadius, cy - baseRadius),
                        end = Offset(cx + baseRadius, cy + baseRadius)
                    )
                )
            }

            drawPath(
                path = dropPath,
                color = if (checked) Color.White.copy(alpha = 0.7f) else tint.copy(alpha = 0.75f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            if (fillProgress > 0.35f) {
                val checkPath = Path().apply {
                    moveTo(cx - baseRadius * 0.42f, cy + baseRadius * 0.05f)
                    lineTo(cx - baseRadius * 0.1f, cy + baseRadius * 0.38f)
                    lineTo(cx + baseRadius * 0.46f, cy - baseRadius * 0.24f)
                }
                drawPath(
                    path = checkPath,
                    color = Color(0xFF04101E).copy(alpha = fillProgress),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/**
 * Illustrated glass vector empty state with one friendly line.
 */
@Composable
fun GlassEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    accentColor: Color = LocalGlassTokens.current.accentColor
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassSurface(
            modifier = Modifier.size(84.dp),
            cornerRadius = 28.dp,
            thickness = GlassThickness.REGULAR,
            accentTint = accentColor
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.26f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r * 1.8f
                    ),
                    radius = r * 1.8f,
                    center = Offset(cx, cy)
                )
                val droplet = Path().apply {
                    moveTo(cx, cy - r * 1.15f)
                    cubicTo(cx + r * 1.1f, cy - r * 0.1f, cx + r, cy + r, cx, cy + r)
                    cubicTo(cx - r, cy + r, cx - r * 1.1f, cy - r * 0.1f, cx, cy - r * 1.15f)
                    close()
                }
                drawPath(
                    path = droplet,
                    color = accentColor.copy(alpha = 0.85f),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 3.dp.toPx(),
                    center = Offset(cx + r * 0.65f, cy - r * 0.65f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
