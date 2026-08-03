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

package com.looker.kenko.domain.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.looker.kenko.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
@SerialName("exercise")
data class Exercise(
    val name: String,
    val tags: List<Tag> = emptyList(),
    val countType: CountType = CountType.REPS,
    val isBodyweight: Boolean = false,
    val reference: String? = null,
    val id: Int? = null,
)

@Stable
val Exercise.repDurationStringRes: Int
    @StringRes
    get() = when (countType) {
        CountType.MINUTES -> R.string.label_min
        else -> R.string.label_reps
    }

class ExercisesPreviewParameter : PreviewParameterProvider<List<Exercise>> {
    override val values = sequenceOf(
        listOf(
            Exercise("Curls", tags = listOf(Tag(id = 1, name = "二头肌", parentId = 5, parentName = "手臂"))),
            Exercise("Barbell Curls", tags = listOf(Tag(id = 1, name = "二头肌", parentId = 5, parentName = "手臂"))),
            Exercise("Preacher Curls", tags = listOf(Tag(id = 1, name = "二头肌", parentId = 5, parentName = "手臂"))),
            Exercise("B-t-B Curls", tags = listOf(Tag(id = 1, name = "二头肌", parentId = 5, parentName = "手臂"))),
        ),
        listOf(
            Exercise("Push-down", tags = listOf(Tag(id = 2, name = "三头肌", parentId = 5, parentName = "手臂"))),
            Exercise("Skull-Crushers", tags = listOf(Tag(id = 2, name = "三头肌", parentId = 5, parentName = "手臂"))),
            Exercise("Push-overs", tags = listOf(Tag(id = 2, name = "三头肌", parentId = 5, parentName = "手臂"))),
        ),
        listOf(
            Exercise("Lateral Raises", tags = listOf(Tag(id = 3, name = "中束", parentId = 4, parentName = "肩"))),
            Exercise("Shoulder Press", tags = listOf(Tag(id = 3, name = "中束", parentId = 4, parentName = "肩"))),
            Exercise("Face Pulls", tags = listOf(Tag(id = 3, name = "中束", parentId = 4, parentName = "肩"))),
        ),
        listOf(
            Exercise("Squats", tags = listOf(Tag(id = 4, name = "股四头肌", parentId = 3, parentName = "腿"))),
            Exercise("Leg Press", tags = listOf(Tag(id = 4, name = "股四头肌", parentId = 3, parentName = "腿"))),
            Exercise("Hack Squats", tags = listOf(Tag(id = 4, name = "股四头肌", parentId = 3, parentName = "腿"))),
            Exercise("Leg Extensions", tags = listOf(Tag(id = 4, name = "股四头肌", parentId = 3, parentName = "腿"))),
        ),
        listOf(
            Exercise("SDL", tags = listOf(Tag(id = 5, name = "腘绳肌", parentId = 3, parentName = "腿"))),
            Exercise("Lying Leg Curls", tags = listOf(Tag(id = 5, name = "腘绳肌", parentId = 3, parentName = "腿"))),
        ),
        listOf(Exercise("Calve Raises", tags = listOf(Tag(id = 6, name = "小腿", parentId = 3, parentName = "腿")))),
        listOf(
            Exercise("Hip Thrusts", tags = listOf(Tag(id = 7, name = "臀肌", parentId = 3, parentName = "腿"))),
            Exercise("Lunges", tags = listOf(Tag(id = 7, name = "臀肌", parentId = 3, parentName = "腿"))),
        ),
        listOf(
            Exercise("Sit-ups", tags = listOf(Tag(id = 8, name = "上腹", parentId = 6, parentName = "腹"))),
            Exercise("Leg Raises", tags = listOf(Tag(id = 8, name = "上腹", parentId = 6, parentName = "腹"))),
        ),
        listOf(
            Exercise("Bench Press", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1, parentName = "胸"))),
            Exercise("Incline Bench", tags = listOf(Tag(id = 10, name = "上胸", parentId = 1, parentName = "胸"))),
            Exercise("Pec Dec", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1, parentName = "胸"))),
            Exercise("Chest Fly", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1, parentName = "胸"))),
        ),
        listOf(Exercise("Shrugs", tags = listOf(Tag(id = 11, name = "斜方肌", parentId = 2, parentName = "背")))),
        listOf(
            Exercise("Lat Pull-down", tags = listOf(Tag(id = 12, name = "背阔肌", parentId = 2, parentName = "背"))),
            Exercise("Pull-ups", tags = listOf(Tag(id = 12, name = "背阔肌", parentId = 2, parentName = "背"))),
            Exercise("Lat Prayers", tags = listOf(Tag(id = 12, name = "背阔肌", parentId = 2, parentName = "背"))),
        ),
        listOf(
            Exercise("Bent-over Rows", tags = listOf(Tag(id = 13, name = "竖脊肌", parentId = 2, parentName = "背"))),
            Exercise("Chest-Supported Rows", tags = listOf(Tag(id = 13, name = "竖脊肌", parentId = 2, parentName = "背"))),
            Exercise("Rows", tags = listOf(Tag(id = 13, name = "竖脊肌", parentId = 2, parentName = "背"))),
        ),
        listOf(
            Exercise("Treadmill", tags = listOf(Tag(id = 14, name = "跑步", parentId = 7, parentName = "有氧")), countType = CountType.MINUTES),
            Exercise("Cycling", tags = listOf(Tag(id = 15, name = "骑行", parentId = 7, parentName = "有氧")), countType = CountType.MINUTES),
        ),
    )
}
