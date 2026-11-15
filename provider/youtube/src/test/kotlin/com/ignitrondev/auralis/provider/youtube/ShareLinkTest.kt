/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube

import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.MusicArtist
import com.ignitrondev.auralis.domain.model.MusicPlaylist
import com.ignitrondev.auralis.domain.model.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareLinkTest {
    @Test
    fun eachKindLinksToItsYouTubeMusicPage(): Unit {
        assertEquals(
            "https://music.youtube.com/watch?v=dX3k_QDnzHE",
            shareLink(MusicTrack("dX3k_QDnzHE", "youtube", "Midnight City", emptyList(), null, null, null)),
        )
        assertEquals(
            "https://music.youtube.com/playlist?list=PLabc",
            shareLink(MusicPlaylist("VLPLabc", "youtube", "Mix", null, null)),
        )
        assertEquals(
            "https://music.youtube.com/channel/UC0UUzwcmD3s1S7NYAxHfs1w",
            shareLink(MusicArtist("UC0UUzwcmD3s1S7NYAxHfs1w", "youtube", "M83", null)),
        )
        assertEquals(
            "https://music.youtube.com/browse/MPREb_VTWUulRLLd6",
            shareLink(MusicAlbum("MPREb_VTWUulRLLd6", "youtube", "Hurry Up, We're Dreaming", emptyList(), "2011", null)),
        )
    }

    @Test
    fun itemsFromOtherSourcesHaveNoLink(): Unit {
        assertNull(shareLink(MusicTrack("x", "local", "Song", emptyList(), null, null, null)))
    }
}
