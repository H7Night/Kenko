/*
 * Copyright (C) 2025 LooKeR & Contributors
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

package com.looker.kenko.data.mapper

import com.looker.kenko.data.local.model.SessionDataEntity
import com.looker.kenko.data.local.model.SessionEntity
import com.looker.kenko.data.local.model.SetEntity
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.Set
import com.looker.kenko.utils.EpochDays
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

fun Session.data(): SessionDataEntity = SessionDataEntity(
    date = EpochDays(date.toEpochDays().toInt()),
    planId = planId,
    planDayOverride = planDayOverride?.isoDayNumber,
    id = id ?: 0,
)

fun Session.sets(): List<SetEntity> = sets.map {
    it.toEntity(id!!, sets.indexOf(it))
}

fun SessionEntity.toExternal(
    setsMap: List<Set>,
): Session = Session(
    planId = data.planId,
    date = LocalDate.fromEpochDays(data.date.value),
    sets = setsMap,
    planDayOverride = data.planDayOverride?.let { DayOfWeek(it) },
    id = data.id,
)
