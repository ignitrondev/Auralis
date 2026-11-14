/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.ignitrondev.auralis.core.designsystem.motion.pressScale
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/**
 * A glass card for grouped content, such as a "Recently played" section.
 *
 * @param onClick Makes the whole card tappable, with press scale and ripple. Leave null for a
 * card that only holds content (it then has no click semantics).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = AuralisTheme.shapes.large
    val clickModifier = if (onClick != null) Modifier.pressScale(interactionSource) else Modifier
    GlassSurface(
        modifier = modifier.then(clickModifier),
        shape = shape,
    ) {
        val inner = if (onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = ripple(color = AuralisTheme.colors.contentPrimary),
                role = Role.Button,
                onClick = onClick,
            )
        } else {
            Modifier
        }
        Column(
            modifier = inner.padding(AuralisTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
            content = content,
        )
    }
}
