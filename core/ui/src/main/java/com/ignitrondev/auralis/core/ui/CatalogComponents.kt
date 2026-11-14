/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ignitrondev.auralis.core.designsystem.component.Artwork
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.domain.model.CatalogItem
import com.ignitrondev.auralis.domain.model.CatalogSection
import com.ignitrondev.auralis.domain.model.MusicArtist
import com.ignitrondev.auralis.domain.model.MusicTrack

private val CardWidth = 148.dp
private val RowArtworkSize = 52.dp

/** Section heading with screen-gutter padding. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AuralisTheme.typography.sectionTitle,
        color = AuralisTheme.colors.contentPrimary,
        modifier = modifier
            .padding(horizontal = AuralisTheme.spacing.screenGutter)
            .semantics { heading() },
    )
}

/** Square card for carousels. Artists get round artwork, everything else rounded squares. */
@Composable
fun CatalogCard(
    item: CatalogItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isArtist = item is MusicArtist
    Column(
        modifier = modifier
            .width(CardWidth)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
    ) {
        Artwork(
            uri = item.artworkUri,
            contentDescription = null,
            shape = if (isArtist) CircleShape else AuralisTheme.shapes.medium,
            modifier = Modifier.size(CardWidth),
        )
        Text(
            text = item.title,
            style = AuralisTheme.typography.itemTitle,
            color = AuralisTheme.colors.contentPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle(),
            style = AuralisTheme.typography.itemSubtitle,
            color = AuralisTheme.colors.contentSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A titled horizontal row of cards. */
@Composable
fun SectionCarousel(
    section: CatalogSection,
    actions: CatalogActions,
    modifier: Modifier = Modifier,
) {
    val tracks = section.items.filterIsInstance<MusicTrack>()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md)) {
        section.title?.let { SectionTitle(it) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = AuralisTheme.spacing.screenGutter),
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.lg),
        ) {
            items(section.items, key = { it::class.simpleName + it.id }) { item ->
                CatalogCard(item = item, onClick = actions.onClick(item, tracks))
            }
        }
    }
}

/**
 * One row in a list: artwork (or a track number), title, subtitle and an optional trailing text.
 *
 * @param leadingNumber Shown instead of artwork, for album track lists.
 */
@Composable
fun CatalogRow(
    item: CatalogItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = item.subtitle(),
    leadingNumber: Int? = null,
    trailing: String? = null,
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.screenGutter, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        if (leadingNumber != null) {
            Text(
                text = leadingNumber.toString(),
                style = AuralisTheme.typography.timestamp,
                color = colors.contentTertiary,
                modifier = Modifier.widthIn(min = 24.dp),
            )
        } else {
            Artwork(
                uri = item.artworkUri,
                contentDescription = null,
                shape = if (item is MusicArtist) CircleShape else AuralisTheme.shapes.small,
                modifier = Modifier.size(RowArtworkSize),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = AuralisTheme.typography.itemTitle,
                color = colors.contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = AuralisTheme.typography.itemSubtitle,
                color = colors.contentSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Text(text = trailing, style = AuralisTheme.typography.timestamp, color = colors.contentTertiary)
        }
    }
}
