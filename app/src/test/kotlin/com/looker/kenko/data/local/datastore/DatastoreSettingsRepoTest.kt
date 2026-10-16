package com.looker.kenko.data.local.datastore

import com.looker.kenko.domain.model.settings.ColorPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatastoreSettingsRepoTest {

    @Test
    fun `body weight defaults to enabled`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        assertTrue(repo.stream.first().bodyWeightEnabled)
    }

    @Test
    fun `setBodyWeightEnabled persists the flag`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        repo.setBodyWeightEnabled(false)
        assertFalse(repo.stream.first().bodyWeightEnabled)
        repo.setBodyWeightEnabled(true)
        assertTrue(repo.stream.first().bodyWeightEnabled)
    }

    @Test
    fun `color palette defaults to Material`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        assertEquals(ColorPalette.Material, repo.stream.first().colorPalette)
    }

    @Test
    fun `health connect defaults to disabled`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        assertFalse(repo.stream.first().healthConnectEnabled)
    }

    @Test
    fun `setColorPalette persists Material`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        repo.setColorPalette(ColorPalette.Material)
        assertEquals(ColorPalette.Material, repo.stream.first().colorPalette)
    }
}
