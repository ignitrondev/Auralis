/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.motion.pressScale
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

private val BarHeight = 64.dp
private val BarMaxWidth = 480.dp
private val BarInnerPadding = 6.dp
private val ItemIconSize = 22.dp
private val BadgeSize = 8.dp
private val BadgeOffset = 2.dp

/**
 * Space the floating chrome (navigation bar, and later the mini player) covers at the bottom of
 * the window, including the system navigation inset. Scrolling screens add it to their bottom
 * content padding so their last item ends above the glass while content still scrolls under it.
 */
val LocalBottomChromePadding = compositionLocalOf { 0.dp }

/**
 * Floating capsule navigation bar (spec sections 8.1 and 8.4). Place it in the overlay of
 * [GlassLayers] so it blurs the screen behind it. It keeps clear of the system navigation bar.
 */
@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = AuralisTheme.spacing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier
                .widthIn(max = BarMaxWidth)
                .fillMaxWidth()
                .height(BarHeight),
            shape = AuralisTheme.shapes.capsule,
            style = GlassStyle.Floating,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(BarInnerPadding),
                horizontalArrangement = Arrangement.spacedBy(BarInnerPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** Height [GlassNavigationBar] covers above the system navigation inset, margins included. */
@Composable
private fun glassNavigationBarHeight(): Dp = BarHeight + AuralisTheme.spacing.md

/** Total bottom padding for content under [GlassNavigationBar], system inset included. */
@Composable
fun glassNavigationBarPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + glassNavigationBarHeight()

/**
 * One destination in [GlassNavigationBar]. The selected item sits on a small glass lens and uses
 * a heavier label; state is never shown by color (spec section 8.2.1).
 *
 * @param badge When not null, a small dot on the icon says something new waits there (for example
 * an update in Settings). The text is what screen readers announce for it.
 */
@Composable
fun RowScope.GlassNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val shape = AuralisTheme.shapes.capsule
    val interactionSource = remember { MutableInteractionSource() }
    val pill = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = motion.fade(),
        label = "navPill",
    )
    val contentColor = if (selected) colors.contentPrimary else colors.contentSecondary

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .pressScale(interactionSource),
    ) {
        if (pill.value > 0f) {
            GlassIndicator(alpha = { pill.value }, modifier = Modifier.matchParentSize(), shape = shape)
        }
        Column(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ripple(color = colors.contentPrimary),
                    role = Role.Tab,
                    onClick = onClick,
                )
                .then(if (badge != null) Modifier.semantics { stateDescription = badge } else Modifier)
                .padding(horizontal = AuralisTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(ItemIconSize),
                )
                if (badge != null) {
                    // Neutral like every control (spec 8.2.1): the dot is told apart by shape, not colour.
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = BadgeOffset, y = -BadgeOffset)
                            .size(BadgeSize)
                            .background(colors.contentPrimary, CircleShape),
                    )
                }
            }
            Text(
                text = label,
                style = AuralisTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
