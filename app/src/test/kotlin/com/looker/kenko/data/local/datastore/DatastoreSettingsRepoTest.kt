package com.looker.kenko.data.local.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
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
}
