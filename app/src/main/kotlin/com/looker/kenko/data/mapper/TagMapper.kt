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

import com.looker.kenko.data.local.model.TagEntity
import com.looker.kenko.domain.model.Tag

fun TagEntity.toExternal(): Tag = Tag(
    id = id,
    name = name,
    parentId = parentId,
    sortOrder = sortOrder,
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    parentId = parentId,
    sortOrder = sortOrder,
)
