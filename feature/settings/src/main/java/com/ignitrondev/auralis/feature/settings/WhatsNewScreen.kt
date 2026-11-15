/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignitrondev.auralis.core.designsystem.component.GlassButton
import com.ignitrondev.auralis.core.designsystem.component.GlassCard
import com.ignitrondev.auralis.core.designsystem.component.GlassIconButton
import com.ignitrondev.auralis.core.designsystem.component.LocalBottomChromePadding
import com.ignitrondev.auralis.core.designsystem.icon.AuralisIcons
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.core.ui.LoadingState
import com.ignitrondev.auralis.core.ui.MessageState
import com.ignitrondev.auralis.core.ui.R as UiR
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun WhatsNewScreen(onBack: () -> Unit, viewModel: WhatsNewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = AuralisTheme.spacing
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            WhatsNewState.Loading -> LoadingState()
            WhatsNewState.Failed -> MessageState(stringResource(R.string.feature_settings_whats_new_failed)) {
                GlassButton(
                    text = stringResource(UiR.string.core_ui_retry),
                    icon = AuralisIcons.Retry,
                    onClick = viewModel::retry,
                )
            }
            is WhatsNewState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = spacing.screenGutter,
                    end = spacing.screenGutter,
                    top = top + BackButtonArea,
                    bottom = LocalBottomChromePadding.current + spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                item(key = "title") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = stringResource(R.string.feature_settings_release_notes),
                            style = AuralisTheme.typography.screenTitle,
                            color = AuralisTheme.colors.contentPrimary,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(R.string.feature_settings_whats_new_intro),
                            style = AuralisTheme.typography.bodySmall,
                            color = AuralisTheme.colors.contentSecondary,
                        )
                    }
                }
                if (current.entries.isEmpty()) {
                    item(key = "none") {
                        Text(
                            text = stringResource(R.string.feature_settings_whats_new_empty),
                            style = AuralisTheme.typography.body,
                            color = AuralisTheme.colors.contentSecondary,
                        )
                    }
                }
                items(current.entries, key = { it.version }) { entry -> EntryCard(entry) }
                item(key = "footer") {
                    Text(
                        text = stringResource(R.string.feature_settings_whats_new_ignitetunes),
                        style = AuralisTheme.typography.bodySmall,
                        color = AuralisTheme.colors.contentTertiary,
                    )
                }
            }
        }
        GlassIconButton(
            icon = AuralisIcons.Back,
            contentDescription = stringResource(UiR.string.core_ui_back),
            onClick = onBack,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(spacing.sm),
        )
    }
}

@Composable
private fun EntryCard(entry: ChangelogEntry) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val date = entry.publishedAt?.let(::formatDate)
    val meta = listOfNotNull(
        stringResource(if (entry.isPreRelease) R.string.feature_settings_whats_new_pre_release else R.string.feature_settings_whats_new_release),
        date,
    ).joinToString(" · ")
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = entry.version,
                    style = AuralisTheme.typography.sectionTitle,
                    color = colors.contentPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                StatusChip(entry.status)
            }
            Text(text = meta, style = AuralisTheme.typography.bodySmall, color = colors.contentTertiary)
            if (entry.notes.isEmpty()) {
                Text(
                    text = stringResource(R.string.feature_settings_whats_new_no_notes),
                    style = AuralisTheme.typography.body,
                    color = colors.contentSecondary,
                )
            } else {
                entry.notes.forEach { note ->
                    if (note.isHeading) {
                        // "Added", "Fixed": a small label over the changes that follow it.
                        Text(
                            text = note.text,
                            style = AuralisTheme.typography.label,
                            color = colors.contentPrimary,
                            modifier = Modifier.padding(top = spacing.xs),
                        )
                    } else {
                        Text(text = "•  ${note.text}", style = AuralisTheme.typography.body, color = colors.contentSecondary)
                    }
                }
            }
        }
    }
}

/** Available is a filled chip, Installed an outlined one: told apart by shape, not colour (§8.2.1). */
@Composable
private fun StatusChip(status: ReleaseStatus) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val label = when (status) {
        ReleaseStatus.Newer -> R.string.feature_settings_whats_new_available
        ReleaseStatus.Installed -> R.string.feature_settings_whats_new_installed
        ReleaseStatus.Earlier -> return
    }
    val filled = status == ReleaseStatus.Newer
    Text(
        text = stringResource(label),
        style = AuralisTheme.typography.labelSmall,
        color = if (filled) colors.background else colors.contentPrimary,
        modifier = Modifier
            .clip(AuralisTheme.shapes.capsule)
            .background(if (filled) colors.contentPrimary else colors.selection)
            .border(1.dp, if (filled) colors.contentPrimary else colors.borderStrong, AuralisTheme.shapes.capsule)
            .padding(horizontal = spacing.md, vertical = spacing.xs),
    )
}

/** An ISO instant as a short date in the user's language, or null when it cannot be read. */
private fun formatDate(iso: String): String? = runCatching {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
}.getOrNull()

private val BackButtonArea = 72.dp
