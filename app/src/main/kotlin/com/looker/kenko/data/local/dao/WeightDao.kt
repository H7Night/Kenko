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

package com.looker.kenko.data.local.dao

import androidx.room.*
import com.looker.kenko.data.local.model.WeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weight: WeightEntity)

    @Update
    suspend fun update(weight: WeightEntity)

    @Query("DELETE FROM weights WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM weights ORDER BY date ASC")
    fun stream(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights ORDER BY date DESC LIMIT 1")
    fun getLatest(): Flow<WeightEntity?>
}
