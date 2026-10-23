package com.looker.kenko.ui.feature.session

import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.testutil.FakePlanRepo
import com.looker.kenko.testutil.FakeSessionRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `timed workout without sets is still listed as a record`() = runTest(dispatcher) {
        val timedCardio = SessionSummary(
            date = LocalDate(2026, 10, 21),
            planId = 1,
            durationSeconds = 1_200,
            setCount = 0,
            id = 1,
        )
        val blank = SessionSummary(
            date = LocalDate(2026, 10, 20),
            planId = 1,
            durationSeconds = null,
            setCount = 0,
            id = 2,
        )
        val vm = SessionsViewModel(
            repo = FakeSessionRepo(summaries = listOf(timedCardio, blank)),
            planRepo = FakePlanRepo(),
        )
        advanceUntilIdle()
        assertEquals(listOf(timedCardio), vm.state.value.sessions)
    }
}
