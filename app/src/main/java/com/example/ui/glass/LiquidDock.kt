package com.example.ui.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppMode
import com.example.data.GlassThickness
import com.example.util.S
import kotlin.math.abs

fun AppMode.icon(): ImageVector = when (this) {
    AppMode.HOME -> Icons.Filled.Dashboard
    AppMode.CALENDAR -> Icons.Filled.CalendarMonth
    AppMode.TASKS -> Icons.Filled.CheckCircle
    AppMode.BOARD -> Icons.Filled.ViewKanban
    AppMode.MATRIX -> Icons.Filled.GridView
    AppMode.NOTES -> Icons.Filled.EditNote
    AppMode.HABITS -> Icons.Filled.Loop
    AppMode.FOCUS -> Icons.Filled.Timer
    AppMode.STATS -> Icons.Filled.Analytics
    AppMode.WIDGETS -> Icons.Filled.Widgets
    AppMode.SETTINGS -> Icons.Filled.Settings
}

@Composable
fun AppMode.localizedTitle(): String {
    val s = S.current
    return when (this) {
        AppMode.HOME -> s.modeHome
        AppMode.CALENDAR -> s.modeCalendar
        AppMode.TASKS -> s.modeTasks
        AppMode.BOARD -> s.modeBoard
        AppMode.MATRIX -> s.modeMatrix
        AppMode.NOTES -> s.modeNotes
        AppMode.HABITS -> s.modeHabits
        AppMode.FOCUS -> s.modeFocus
        AppMode.STATS -> s.modeStats
        AppMode.WIDGETS -> s.modeWidgets
        AppMode.SETTINGS -> s.modeSettings
    }
}

/**
 * Signature #1: Liquid Dock.
 * Bottom navigation is a floating glass pill. The active indicator is a liquid lens that stretches
 * and snaps between tabs (metaball feel), spring physics, haptic tick.
 */
@Composable
fun LiquidDock(
    dockModes: List<AppMode>,
    currentMode: AppMode,
    floating: Boolean,
    showLabels: Boolean,
    onSelectMode: (AppMode) -> Unit,
    onOpenMoreSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val haptics = LocalHapticFeedback.current
    val s = S.current

    val activeIndexInDock = dockModes.indexOf(currentMode)
    val targetSlot = if (activeIndexInDock >= 0) activeIndexInDock.toFloat() else dockModes.size.toFloat()
    val totalSlots = (dockModes.size + 1).coerceAtLeast(2)

    val animatedSlot by animateFloatAsState(
        targetValue = targetSlot,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquid_lens_slot"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                horizontal = if (floating) 16.dp else 0.dp,
                vertical = if (floating) 10.dp else 0.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("liquid_dock"),
            thickness = GlassThickness.THICK,
            cornerRadius = if (floating) 32.dp else 0.dp,
            elevated = true,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Liquid lens indicator drawn behind tabs with metaball stretch during velocity
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    val slotWidth = size.width / totalSlots
                    val velocityStretch = abs(targetSlot - animatedSlot).coerceIn(0f, 1.2f)
                    val lensWidth = slotWidth * (0.86f + velocityStretch * 0.38f)
                    val lensHeight = size.height * (0.88f - velocityStretch * 0.12f)
                    val centerX = (animatedSlot + 0.5f) * slotWidth
                    val left = (centerX - lensWidth / 2f).coerceIn(0f, (size.width - lensWidth).coerceAtLeast(0f))
                    val top = (size.height - lensHeight) / 2f
                    val cr = CornerRadius(lensHeight / 2f, lensHeight / 2f)

                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tokens.accentColor.copy(alpha = 0.30f),
                                tokens.secondaryAccent.copy(alpha = 0.22f)
                            ),
                            start = Offset(left, top),
                            end = Offset(left + lensWidth, top + lensHeight)
                        ),
                        topLeft = Offset(left, top),
                        size = Size(lensWidth, lensHeight),
                        cornerRadius = cr
                    )
                    drawRoundRect(
                        color = tokens.accentColor.copy(alpha = 0.65f),
                        topLeft = Offset(left, top),
                        size = Size(lensWidth, lensHeight),
                        cornerRadius = cr,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockModes.forEach { mode ->
                        val selected = mode == currentMode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 52.dp)
                                .testTag("dock_tab_${mode.id}")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (tokens.hapticsEnabled) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        onSelectMode(mode)
                                    }
                                )
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = mode.icon(),
                                contentDescription = mode.localizedTitle(),
                                tint = if (selected) tokens.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            if (showLabels) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.localizedTitle(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // "More" slot
                    val moreSelected = activeIndexInDock < 0
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("dock_tab_more")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (tokens.hapticsEnabled) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onOpenMoreSheet()
                                }
                            )
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (moreSelected) currentMode.icon() else Icons.Filled.MoreHoriz,
                            contentDescription = s.modeMore,
                            tint = if (moreSelected) tokens.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        if (showLabels) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (moreSelected) currentMode.localizedTitle() else s.modeMore,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (moreSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Adaptive Glass Navigation Rail for Tablets / Expanded Foldables.
 */
@Composable
fun LiquidNavigationRail(
    allModes: List<AppMode>,
    currentMode: AppMode,
    onSelectMode: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val haptics = LocalHapticFeedback.current

    GlassSurface(
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        thickness = GlassThickness.THICK,
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            allModes.forEach { mode ->
                val selected = mode == currentMode
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    thickness = if (selected) GlassThickness.THICK else GlassThickness.THIN,
                    cornerRadius = 18.dp,
                    accentTint = if (selected) tokens.accentColor else null,
                    onClick = {
                        if (tokens.hapticsEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onSelectMode(mode)
                    },
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = mode.icon(),
                            contentDescription = mode.localizedTitle(),
                            tint = if (selected) tokens.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mode.localizedTitle(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
