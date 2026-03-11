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

package com.looker.kenko.data.model

import androidx.compose.runtime.Immutable
import com.looker.kenko.data.local.model.WeightEntity
import com.looker.kenko.utils.EpochDays
import kotlinx.datetime.LocalDate

@Immutable
data class Weight(
    val date: LocalDate,
    val value: Float,
    val id: Int = 0,
)

fun Weight.toEntity(): WeightEntity = WeightEntity(
    date = EpochDays(date.toEpochDays().toInt()),
    value = value,
    id = id
)

fun WeightEntity.toExternal(): Weight = Weight(
    date = LocalDate.fromEpochDays(date.value),
    value = value,
    id = id
)
