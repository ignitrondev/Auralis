/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ignitrondev.auralis.core.database.entity.QueueItemEntity
import com.ignitrondev.auralis.core.database.entity.QueueStateEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackQueueDaoTest {
    private lateinit var database: AuralisDatabase

    @get:Rule
    val migrations = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AuralisDatabase::class.java)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AuralisDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() = database.close()

    private fun item(position: Int, id: String) = QueueItemEntity(
        position = position, trackId = id, sourceId = "youtube", title = id, artistNames = "", artistIds = "",
        albumId = null, albumTitle = null, durationMs = null, artworkUri = null, isVideo = false, isExplicit = false,
    )

    @Test
    fun replacingTheQueueDropsTheOldTracks() = runTest {
        val dao = database.playbackQueueDao()
        dao.replaceItems(listOf(item(0, "a"), item(1, "b"), item(2, "c")))
        dao.replaceItems(listOf(item(0, "d"), item(1, "e")))
        assertEquals(listOf("d", "e"), dao.items().map { it.trackId })
        dao.replaceItems(emptyList())
        assertEquals(emptyList<QueueItemEntity>(), dao.items())
    }

    @Test
    fun progressIsASingleRow() = runTest {
        val dao = database.playbackQueueDao()
        assertNull(dao.state())
        dao.upsertState(QueueStateEntity(currentIndex = 1, positionMs = 1_000, shuffleEnabled = false, repeatMode = "OFF"))
        val latest = QueueStateEntity(currentIndex = 2, positionMs = 5_000, shuffleEnabled = true, repeatMode = "ALL")
        dao.upsertState(latest)
        assertEquals(latest, dao.state())
    }

    @Test
    fun migratesFromVersion1() {
        migrations.createDatabase(DB, 1).close()
        migrations.runMigrationsAndValidate(DB, 2, true).close()
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
