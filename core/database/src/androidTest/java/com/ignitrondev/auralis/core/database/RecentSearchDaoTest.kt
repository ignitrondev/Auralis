/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.auralis.core.database.entity.RecentSearchEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentSearchDaoTest {
    private lateinit var database: AuralisDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AuralisDatabase::class.java).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun newestFirstRepeatsMoveUpAndTrimKeepsNewest() = runTest {
        val dao = database.recentSearchDao()
        dao.upsert(RecentSearchEntity("m83", 1))
        dao.upsert(RecentSearchEntity("daft punk", 2))
        dao.upsert(RecentSearchEntity("m83", 3))
        assertEquals(listOf("m83", "daft punk"), dao.observeRecent(10).first().map { it.query })

        dao.upsert(RecentSearchEntity("justice", 4))
        dao.trim(keep = 2)
        assertEquals(listOf("justice", "m83"), dao.observeRecent(10).first().map { it.query })

        dao.delete("m83")
        assertEquals(listOf("justice"), dao.observeRecent(10).first().map { it.query })
    }
}
