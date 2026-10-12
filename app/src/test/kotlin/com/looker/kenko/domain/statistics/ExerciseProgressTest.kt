/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.looker.kenko.domain.statistics

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExerciseProgressTest {

    @Test
    fun `epley estimates one rep max`() {
        // reps == 1 -> actual weight
        assertEquals(100f, estimatedOneRepMax(100f, 1))
        // 60 x 10 -> 60 * (1 + 10/30) = 80
        assertEquals(80f, estimatedOneRepMax(60f, 10))
        // 80 x 8 -> 101.33
        assertEquals(101.3333f, estimatedOneRepMax(80f, 8), 0.01f)
        // cardio / bodyweight / invalid
        assertEquals(0f, estimatedOneRepMax(0f, 8))
        assertEquals(0f, estimatedOneRepMax(80f, 0))
    }

    @Test
    fun `entries aggregate per date and pick best set by estimated 1rm`() {
        val d1 = LocalDate(2026, 1, 1)
        val d2 = LocalDate(2026, 1, 3)
        val records = listOf(
            ExerciseSetRecord(d1, 100f, 5),
            ExerciseSetRecord(d1, 90f, 3),
            ExerciseSetRecord(d2, 105f, 3),
        )
        val entries = buildExerciseEntries(records)

        assertEquals(2, entries.size)
        val first = entries[0]
        assertEquals(d1, first.date)
        assertEquals(100f, first.heaviestWeight)
        // 100x5 (est 116.7) beats 90x3 (est 99.0)
        assertEquals(100f, first.bestSetWeight)
        assertEquals(5, first.bestSetReps)
        assertEquals(116.6667f, first.estimatedOneRepMax, 0.01f)
        assertEquals(770f, first.volume)
        assertEquals(2, first.setCount)
        // ordering is ascending by date
        assertEquals(d2, entries[1].date)
    }

    @Test
    fun `progress series maps metric to value`() {
        val entries = buildExerciseEntries(
            listOf(ExerciseSetRecord(LocalDate(2026, 1, 1), 100f, 5)),
        )
        assertEquals(100f, buildExerciseProgress(entries, ExerciseMetric.HEAVIEST_WEIGHT).single().value)
        assertEquals(500f, buildExerciseProgress(entries, ExerciseMetric.VOLUME).single().value)
        assertEquals(100f, buildExerciseProgress(entries, ExerciseMetric.BEST_SET).single().value)
        assertEquals(116.6667f, buildExerciseProgress(entries, ExerciseMetric.ESTIMATED_1RM).single().value, 0.01f)
    }

    @Test
    fun `bodyweight sets are dropped for non-bodyweight exercises`() {
        val bodyweightDay = LocalDate(2026, 1, 1)
        val weightedDay = LocalDate(2026, 1, 5)
        val bodyweightOnly = listOf(
            ExerciseSetRecord(bodyweightDay, 0f, 10),
            ExerciseSetRecord(bodyweightDay, 0f, 12),
        )
        assertEquals(0, buildExerciseEntries(bodyweightOnly).size)

        val entries = buildExerciseEntries(bodyweightOnly + ExerciseSetRecord(weightedDay, 10f, 8))
        assertEquals(1, entries.size)
        assertEquals(weightedDay, entries.single().date)
        assertEquals(10f, entries.single().heaviestWeight)
    }

    @Test
    fun `bodyweight load uses the body weight recorded at the session date`() {
        val d1 = LocalDate(2026, 1, 1)
        val d2 = LocalDate(2026, 1, 5)
        val bodyWeights = listOf(
            BodyWeightRecord(LocalDate(2025, 12, 1), 70f),
            BodyWeightRecord(d1, 65f),
        )

        // pure bodyweight set -> body weight at that date = 65 (not 0)
        val alone = buildExerciseEntries(
            records = listOf(ExerciseSetRecord(d1, 0f, 10)),
            isBodyweight = true,
            bodyWeights = bodyWeights,
        )
        assertEquals(1, alone.size)
        assertEquals(65f, alone.single().heaviestWeight)
        assertEquals(65f * (1f + 10f / 30f), alone.single().estimatedOneRepMax, 0.01f)

        // weighted set -> body weight (65, latest <= d2) + added 10 = 75
        val mixed = buildExerciseEntries(
            records = listOf(ExerciseSetRecord(d2, 0f, 10), ExerciseSetRecord(d2, 10f, 10)),
            isBodyweight = true,
            bodyWeights = bodyWeights,
        )
        assertEquals(75f, mixed.single().heaviestWeight)
    }

    @Test
    fun `body weight lookup picks nearest not later and falls back to earliest`() {
        val records = listOf(
            BodyWeightRecord(LocalDate(2026, 2, 1), 64f),
            BodyWeightRecord(LocalDate(2026, 1, 1), 70f),
        )
        assertEquals(70f, bodyWeightOnOrBefore(records, LocalDate(2026, 1, 15)))
        assertEquals(64f, bodyWeightOnOrBefore(records, LocalDate(2026, 3, 1)))
        // earlier than every record -> earliest known
        assertEquals(70f, bodyWeightOnOrBefore(records, LocalDate(2025, 1, 1)))
        assertEquals(null, bodyWeightOnOrBefore(emptyList(), LocalDate(2026, 1, 1)))
    }
}
