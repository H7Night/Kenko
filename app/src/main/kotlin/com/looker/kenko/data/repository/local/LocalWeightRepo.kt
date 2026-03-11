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

package com.looker.kenko.data.repository.local

import com.looker.kenko.data.local.dao.WeightDao
import com.looker.kenko.data.local.model.WeightEntity
import com.looker.kenko.data.model.Weight
import com.looker.kenko.data.model.toEntity
import com.looker.kenko.data.model.toExternal
import com.looker.kenko.data.repository.WeightRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalWeightRepo @Inject constructor(
    private val weightDao: WeightDao,
) : WeightRepo {

    override val weights: Flow<List<Weight>> = weightDao.stream().map { entities ->
        entities.map(WeightEntity::toExternal)
    }

    override val latestWeight: Flow<Weight?> = weightDao.getLatest().map { it?.toExternal() }

    override suspend fun addWeight(weight: Weight) {
        weightDao.insert(weight.toEntity())
    }

    override suspend fun updateWeight(weight: Weight) {
        weightDao.update(weight.toEntity())
    }

    override suspend fun deleteWeight(id: Int) {
        weightDao.delete(id)
    }
}
