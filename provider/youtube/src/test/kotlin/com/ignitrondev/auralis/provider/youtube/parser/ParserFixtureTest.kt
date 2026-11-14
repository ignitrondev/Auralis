/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube.parser

import com.ignitrondev.auralis.domain.model.MusicAlbum
import com.ignitrondev.auralis.domain.model.MusicArtist
import com.ignitrondev.auralis.domain.model.MusicPlaylist
import com.ignitrondev.auralis.domain.model.MusicTrack
import com.ignitrondev.auralis.provider.youtube.InnerTubeSession
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parses response fixtures recorded from YouTube Music on 2026-11-14. */
class ParserFixtureTest {

    private fun fixture(name: String): JsonObject {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name.json")) { "missing fixture $name" }
        return InnerTubeSession.json.parseToJsonElement(stream.bufferedReader().readText()).jsonObject
    }

    @Test
    fun searchGroupsResultsByType() {
        val results = SearchParser.parse("Midnight City M83", fixture("search"))

        val top = results.topResult as MusicTrack
        assertEquals("Midnight City", top.title)
        assertTrue(top.isVideo)

        val song = results.tracks.first()
        assertEquals("K8v_DaCcORQ", song.id)
        assertEquals("Midnight City", song.title)
        assertEquals("M83", song.artistNames)
        assertFalse(song.isVideo)
        assertTrue("songs come before videos", results.tracks.indexOfFirst { it.isVideo } > results.tracks.indexOfLast { !it.isVideo })

        assertTrue(results.artists.any { it.name == "M83" && it.id == "UC0UUzwcmD3s1S7NYAxHfs1w" })
        val album = results.albums.first { it.title == "Hurry up, We're Dreaming" }
        assertEquals("MPREb_VTWUulRLLd6", album.id)
        assertEquals("2011", album.year)
        assertTrue(results.playlists.isNotEmpty())
        assertTrue("every playlist id is a browse id", results.playlists.all { it.id.startsWith("VL") })
    }

    @Test
    fun searchSkipsPodcastsAndProfiles() {
        val results = SearchParser.parse("Midnight City M83", fixture("search"))
        val titles = (results.tracks + results.albums + results.artists + results.playlists).map {
            when (it) {
                is MusicTrack -> it.title
                is MusicAlbum -> it.title
                is MusicArtist -> it.name
                is MusicPlaylist -> it.title
            }
        }
        assertFalse(titles.contains("Band Shining Moments"))
        assertFalse(titles.contains("Triberg Travel Podcast"))
        assertFalse(titles.contains("Aulen"))
    }

    @Test
    fun homeAndExploreFeedsHaveTitledSections() {
        listOf("home", "explore").forEach { name ->
            val sections = FeedParser.parse(fixture(name))
            assertTrue("$name has sections", sections.isNotEmpty())
            sections.forEach { section ->
                assertNotNull(section.title)
                assertTrue(section.items.isNotEmpty())
            }
        }
        val explore = FeedParser.parse(fixture("explore"))
        assertTrue("explore has a row of albums", explore.any { section -> section.items.all { it is MusicAlbum } })
    }

    @Test
    fun albumHasHeaderAndAllTracks() {
        val detail = AlbumParser.parse("MPREb_VTWUulRLLd6", fixture("album"))
        assertEquals("Hurry up, We're Dreaming", detail.album.title)
        assertEquals("M83", detail.album.artistNames)
        assertEquals("22 songs", detail.trackCountText?.substringBefore(" •"))
        assertEquals(22, detail.tracks.size)
        assertEquals("the album's playlist is what the library saves", "OLAK5uy_lLGsT6ZVnWyAwEu1CRkZBTLvvloriMRsk", detail.libraryPlaylistId)
        assertFalse("signed out, nothing is saved", detail.isSavedToLibrary)

        val intro = detail.tracks.first()
        assertEquals("Intro", intro.title)
        assertEquals("yr_HIrfr7is", intro.id)
        assertEquals(5 * 60_000L + 23_000L, intro.durationMs)
        assertEquals("tracks inherit the album", "MPREb_VTWUulRLLd6", intro.album?.id)
        assertEquals("tracks inherit the album artist", "M83", intro.artistNames)
        assertNotNull("tracks fall back to the album artwork", intro.artworkUri)
    }

    @Test
    fun artistHasTopTracksAndSections() {
        val detail = ArtistParser.parse("UC0UUzwcmD3s1S7NYAxHfs1w", fixture("artist"))
        assertEquals("M83", detail.artist.name)
        assertNotNull(detail.artist.artworkUri)
        assertEquals(5, detail.topTracks.size)
        assertTrue(detail.sections.size >= 4)
        assertTrue(detail.sections.any { section -> section.items.all { it is MusicAlbum } })
    }

    @Test
    fun playlistHasTracksAndMore() {
        val detail = PlaylistParser.parse("VLOLAK5uy_mYUMpHbB4NTov2vjyMFINnLNOKqfHmZlI", fixture("playlist"))
        assertEquals("Top songs", detail.playlist.title)
        assertEquals(100, detail.tracks.size)
        assertTrue(detail.hasMore)

        val first = detail.tracks.first()
        assertEquals("Midnight City", first.title)
        assertEquals("M83", first.artistNames)
        assertEquals("Hurry up, We're Dreaming", first.album?.title)
        assertEquals(244_000L, first.durationMs)
    }

    @Test
    fun songsWithoutArtistNeverUseTheDurationAsArtist() {
        val results = SearchParser.parse("daft punk", fixture("search_daftpunk"))
        val veridis = results.tracks.first { it.title == "Veridis Quo" }
        assertTrue("the row lists no artist", veridis.artists.isEmpty())
        assertEquals(346_000L, veridis.durationMs)
        results.tracks.forEach { track ->
            assertFalse("${track.title} has a duration as artist", track.artists.any { it.name.matches(Regex("\\d+:\\d{2}")) })
        }
    }

    @Test
    fun topResultSubtitleHasNoTypeLabel() {
        val top = SearchParser.parse("daft punk", fixture("search_daftpunk")).topResult as MusicArtist
        assertEquals("Daft Punk", top.name)
        assertEquals("66.1M monthly audience", top.subtitle)
    }

    @Test
    fun durationsAndArtworkHelpers() {
        assertEquals(244_000L, parseDuration("4:04"))
        assertEquals(3_723_000L, parseDuration("1:02:03"))
        assertEquals(null, parseDuration("798M plays"))
        assertEquals(
            "https://lh3.googleusercontent.com/abc=w544-h544-l90-rj",
            upscaleArtwork("https://lh3.googleusercontent.com/abc=w120-h120-l90-rj"),
        )
        assertEquals("https://i.ytimg.com/vi/x/hqdefault.jpg", upscaleArtwork("https://i.ytimg.com/vi/x/hqdefault.jpg"))
    }
}
