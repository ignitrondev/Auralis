/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ignitrondev.auralis.core.designsystem.R

// Both typefaces are bundled variable fonts under the SIL Open Font License (spec section
// 8.2.2). Downloadable Fonts are not used: they need Google Play Services. Scripts the fonts do
// not cover (Japanese, Korean, Arabic and others) fall back to the system font automatically.

private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Display face: wordmark, screen titles, large headings, the Now Playing title. */
val SpaceGrotesk = FontFamily(
    variableFont(R.font.space_grotesk, FontWeight.Normal),
    variableFont(R.font.space_grotesk, FontWeight.Medium),
    variableFont(R.font.space_grotesk, FontWeight.SemiBold),
    variableFont(R.font.space_grotesk, FontWeight.Bold),
)

/** Text and UI face: lists, buttons, labels, settings, body text, timestamps. */
val Inter = FontFamily(
    variableFont(R.font.inter, FontWeight.Normal),
    variableFont(R.font.inter, FontWeight.Medium),
    variableFont(R.font.inter, FontWeight.SemiBold),
    variableFont(R.font.inter, FontWeight.Bold),
)

/**
 * The full Auralis type scale. Feature modules use these styles and never set font family,
 * size or weight themselves. Hierarchy comes from size and weight; text color stays neutral.
 */
@Immutable
data class AuralisTypography(
    /** "Auralis" wordmark: Space Grotesk Medium, tight letter spacing. */
    val wordmark: TextStyle,
    /** Large screen title, such as "Good evening". */
    val screenTitle: TextStyle,
    /** Track title on the Now Playing screen. */
    val nowPlayingTitle: TextStyle,
    /** Section heading inside a screen. */
    val sectionTitle: TextStyle,
    /** Primary line of a list item: track, album or playlist name. */
    val itemTitle: TextStyle,
    /** Secondary line of a list item: artist or details. */
    val itemSubtitle: TextStyle,
    /** Body text. */
    val body: TextStyle,
    /** Smaller body text and captions. */
    val bodySmall: TextStyle,
    /** Button and control labels. */
    val label: TextStyle,
    /** Small labels, such as navigation bar items. */
    val labelSmall: TextStyle,
    /** Playback position and durations. Tabular digits, so numbers do not shift while ticking. */
    val timestamp: TextStyle,
)

internal val DefaultAuralisTypography = AuralisTypography(
    wordmark = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.03).em,
    ),
    screenTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    ),
    nowPlayingTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    sectionTitle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    itemTitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    itemSubtitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    label = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    timestamp = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    ),
)

internal val LocalAuralisTypography = staticCompositionLocalOf { DefaultAuralisTypography }
