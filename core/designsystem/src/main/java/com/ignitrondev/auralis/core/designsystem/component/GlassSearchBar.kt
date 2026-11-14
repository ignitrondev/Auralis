/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.R
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme

/**
 * Capsule glass search field (spec section 8.1). Stateless: the caller owns [query].
 *
 * @param onSearch Called when the user presses the keyboard search action.
 */
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = FocusRequester(),
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = AuralisTheme.shapes.capsule,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.lg, end = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AuralisIcons.Search,
                contentDescription = null,
                tint = colors.contentSecondary,
                modifier = Modifier.size(22.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(text = placeholder, style = AuralisTheme.typography.body, color = colors.contentTertiary)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = AuralisTheme.typography.body.copy(color = colors.contentPrimary),
                    cursorBrush = SolidColor(colors.contentPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            if (query.isNotEmpty()) {
                GlassIconButton(
                    icon = AuralisIcons.Close,
                    contentDescription = stringResource(R.string.core_designsystem_clear_search),
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}
