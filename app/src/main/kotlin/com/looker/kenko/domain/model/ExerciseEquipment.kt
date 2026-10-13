/*
 * Copyright (C) 2025 LooKeR & Contributors
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

package com.looker.kenko.domain.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import com.looker.kenko.R

/** 动作使用的器械类型（动作级，区别于计划级的 [Labels.Equipment]）。 */
@Stable
enum class ExerciseEquipment {
    BARBELL,
    DUMBBELL,
    MACHINE,
    CABLE,
    BODYWEIGHT,
    OTHER,
}

@get:StringRes
val ExerciseEquipment.labelRes: Int
    get() = when (this) {
        ExerciseEquipment.BARBELL -> R.string.label_equipment_barbell
        ExerciseEquipment.DUMBBELL -> R.string.label_equipment_dumbbell
        ExerciseEquipment.MACHINE -> R.string.label_equipment_machine
        ExerciseEquipment.CABLE -> R.string.label_equipment_cable
        ExerciseEquipment.BODYWEIGHT -> R.string.label_equipment_bodyweight
        ExerciseEquipment.OTHER -> R.string.label_equipment_other
    }
