/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/** The kinds of items an account keeps in its library, each listed on its own page. */
enum class LibraryCollection {
    /** Playlists the account made or saved. Liked music is listed apart, not here. */
    PLAYLISTS,

    /** Albums saved to the library. */
    ALBUMS,

    /** Artists the account subscribed to. */
    ARTISTS,
}
