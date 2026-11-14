/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.motion.pressScale
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

private val ControlHeight = 48.dp
private val ControlIconSize = 20.dp

/** Capsule glass button with a label and an optional leading icon. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = AuralisTheme.colors
    val contentColor = if (enabled) colors.contentPrimary else colors.contentDisabled
    GlassSurface(
        modifier = modifier
            .defaultMinSize(minHeight = ControlHeight)
            .pressScale(interactionSource),
        shape = AuralisTheme.shapes.capsule,
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = colors.contentPrimary),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .defaultMinSize(minHeight = ControlHeight)
                .padding(horizontal = AuralisTheme.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(ControlIconSize),
                )
            }
            Text(text = text, style = AuralisTheme.typography.label, color = contentColor)
        }
    }
}

/**
 * Circular glass button with only an icon.
 *
 * @param contentDescription Required: the button has no visible label.
 * @param selected Shows a selection fill (for toggles such as shuffle). State is never shown by color.
 * @param size Touch target and glass diameter; at least 48dp.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    size: Dp = ControlHeight,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = AuralisTheme.colors
    GlassSurface(
        modifier = modifier
            .size(size)
            .pressScale(interactionSource),
        shape = AuralisTheme.shapes.capsule,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .then(if (selected) Modifier.background(colors.selection, AuralisTheme.shapes.capsule) else Modifier)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = colors.contentPrimary),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics { this.selected = selected },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) colors.contentPrimary else colors.contentDisabled,
                modifier = Modifier.size(size * 0.46f),
            )
        }
    }
}
