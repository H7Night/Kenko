package com.looker.kenko.ui.feature.session.calendar

import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.today
import com.looker.kenko.testutil.FakeSessionRepo
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `timed workout without sets marks the calendar day`() = runTest(dispatcher) {
        val date = today()
        val vm = CalendarViewModel(
            FakeSessionRepo(
                summaries = listOf(
                    SessionSummary(
                        date = date,
                        planId = 1,
                        durationSeconds = 1_200,
                        setCount = 0,
                        id = 1,
                    ),
                ),
            ),
        )
        advanceUntilIdle()
        assertTrue(date in vm.state.value.sessionDates)
    }
}
