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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.looker.kenko.data.local.model.ExerciseTagEntity
import com.looker.kenko.data.local.model.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY sortOrder, id")
    fun stream(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE parentId IS NULL ORDER BY sortOrder, id")
    fun streamParents(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE parentId = :parentId ORDER BY sortOrder, id")
    fun streamChildren(parentId: Int): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun get(id: Int): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM exercise_tags WHERE tagId = :tagId")
    suspend fun exerciseCount(tagId: Int): Int

    @Query("SELECT t.* FROM tags t INNER JOIN exercise_tags et ON t.id = et.tagId WHERE et.exerciseId = :exerciseId ORDER BY t.sortOrder, t.id")
    suspend fun getTagsForExercise(exerciseId: Int): List<TagEntity>

    @Query("SELECT t.* FROM tags t INNER JOIN exercise_tags et ON t.id = et.tagId WHERE et.exerciseId = :exerciseId ORDER BY t.sortOrder, t.id")
    fun streamTagsForExercise(exerciseId: Int): Flow<List<TagEntity>>

    @Transaction
    suspend fun replaceExerciseTags(exerciseId: Int, tagIds: List<Int>) {
        deleteExerciseTags(exerciseId)
        tagIds.forEach { tagId ->
            insertExerciseTag(ExerciseTagEntity(exerciseId, tagId))
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseTag(entity: ExerciseTagEntity)

    @Query("DELETE FROM exercise_tags WHERE exerciseId = :exerciseId")
    suspend fun deleteExerciseTags(exerciseId: Int)

    @Query("""
        SELECT e.id FROM exercises e
        INNER JOIN exercise_tags et ON e.id = et.exerciseId
        INNER JOIN tags t ON et.tagId = t.id
        WHERE t.id = :tagId OR t.parentId = :tagId
    """)
    suspend fun getExerciseIdsByTag(tagId: Int): List<Int>

    @Query("SELECT COUNT(*) FROM exercise_tags et INNER JOIN exercises e ON e.id = et.exerciseId WHERE et.tagId = :tagId")
    suspend fun exerciseCountByTagId(tagId: Int): Int
}
