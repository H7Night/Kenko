package com.looker.kenko.ui.feature.profile

import com.looker.kenko.testutil.FakeExerciseRepo
import com.looker.kenko.testutil.FakePlanRepo
import com.looker.kenko.testutil.FakeSettingsRepo
import com.looker.kenko.testutil.FakeWeightRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `body weight enabled reflects settings`() = runTest(dispatcher) {
        val settings = FakeSettingsRepo()
        val vm = ProfileViewModel(FakePlanRepo(), FakeWeightRepo(), FakeExerciseRepo(), settings)
        advanceUntilIdle()
        assertTrue(vm.state.value.bodyWeightEnabled)

        settings.setBodyWeightEnabled(false)
        advanceUntilIdle()
        assertFalse(vm.state.value.bodyWeightEnabled)
    }
}
