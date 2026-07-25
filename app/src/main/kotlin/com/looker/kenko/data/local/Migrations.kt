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

package com.looker.kenko.data.local

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.looker.kenko.data.local.model.ExerciseEntity
import com.looker.kenko.domain.model.Set
import com.looker.kenko.domain.model.localDate
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.serializers.DayOfWeekSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.migrateExercises()
        db.migratePlans()
        db.migrateSessions()
    }

    private fun SupportSQLiteDatabase.migratePlans() {
        execSQL(
            """
            CREATE TABLE plans (
            `name` TEXT NOT NULL,
            `description` TEXT DEFAULT NULL,
            `difficulty` TEXT DEFAULT NULL,
            `focus` TEXT DEFAULT NULL,
            `equipment` TEXT DEFAULT NULL,
            `time` TEXT DEFAULT NULL,
            `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)
            """.trimIndent(),
        )
        execSQL(
            """
            CREATE TABLE plan_day (
            `planId` INTEGER NOT NULL CONSTRAINT `fk_sessions_plans_id` REFERENCES `plans` (`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
            `exerciseId` INTEGER NOT NULL CONSTRAINT `fk_sets_exercises_id` REFERENCES `exercises` (`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
            `dayOfWeek` INTEGER NOT NULL,
            `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)
            """.trimIndent(),
        )
        createIndex("plan_day", "planId", "exerciseId")
        execSQL(
            """
            CREATE TABLE plan_history (
            `planId` INTEGER CONSTRAINT `fk_sessions_plans_id` REFERENCES `plans` (`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
            `start` INTEGER NOT NULL,
            `end` INTEGER DEFAULT NULL,
            `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)
            """.trimIndent(),
        )
        createIndex("plan_history", "planId", "start", "end")
        execSQL(
            """
            INSERT INTO `plans` (`name`)
            SELECT `name` FROM `plan_table`
            """.trimIndent(),
        )
        val planHistorySelectStatement = query("SELECT `id`, `isActive` FROM `plan_table`")
        val planHistoryInsertStatement = compileStatement(
            """
            INSERT INTO `plan_history` (`planId`, `start`)
            VALUES (?, ?)
            """.trimIndent(),
        )
        val selectStatement = query("SELECT `id`, `exercisesPerDay` FROM `plan_table`")
        val insertStatement = compileStatement(
            """
            INSERT INTO `plan_day` (`dayOfWeek`, `planId`, `exerciseId`)
            VALUES (?, ?, ?)
            """.trimIndent(),
        )
        planHistorySelectStatement.toPlanHistory(planHistoryInsertStatement)
        selectStatement.toPlanDay(this, insertStatement)
        execSQL("DROP TABLE `plan_table`")
    }

    private fun SupportSQLiteDatabase.migrateExercises() {
        execSQL(
            """
            CREATE TABLE exercises (
            `name` TEXT NOT NULL,
            `target` TEXT NOT NULL,
            `reference` TEXT,
            `isIsometric` INTEGER NOT NULL,
            `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO `exercises` (`name`,`target`,`reference`,`isIsometric`)
            SELECT `name`,`target`,`reference`,`isIsometric` FROM `Exercise`
            """.trimIndent(),
        )
        execSQL("DROP TABLE `Exercise`")
    }

    private fun SupportSQLiteDatabase.migrateSessions() {
        /**
         * This `id` can be treated as session id because previous session entity
         * didn't have any id and creating new id per session is like
         * creating new session id
         */
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS _tmp_session (
            `sets` TEXT NOT NULL,
            `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO `_tmp_session` (`sets`)
            SELECT (`sets`)
            FROM `Session`
            """.trimIndent(),
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS sessions (
            `date` INTEGER NOT NULL,
            `planId` INTEGER CONSTRAINT `fk_sessions_plans_id` REFERENCES `plans` (`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
            `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)
            """.trimIndent(),
        )
        createIndex("sessions", "planId")
        execSQL(
            """
            INSERT INTO `sessions` (`date`, `planId`)
            SELECT Session.`date`, plan_history.`planId`
            FROM `Session`
            INNER JOIN `plan_history`
            ON (plan_history.`end` IS NULL
            AND plan_history.`start` IS NOT NULL)
            """.trimIndent(),
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS set_type (
            `type` TEXT NOT NULL PRIMARY KEY,
            `modifier` REAL NOT NULL)
            """.trimIndent()
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS sets (
            `reps` INTEGER NOT NULL,
            `exerciseId` INTEGER NOT NULL CONSTRAINT `fk_sets_exercises_id` REFERENCES `exercises` (`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
            `weight` REAL NOT NULL,
            `sessionId` INTEGER NOT NULL CONSTRAINT `fk_sets_sessions_id` REFERENCES `sessions` (`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
            `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            `type` TEXT NOT NULL,
            `order` INTEGER NOT NULL)
            """.trimIndent(),
        )
        createIndex("sets", "sessionId", "exerciseId")
        val selectStatement = query("SELECT `id`, `sets` FROM `_tmp_session`")
        val insertStatement = compileStatement(
            """
            INSERT INTO `sets` (`reps`, `weight`, `type`, `order`, `sessionId`, `exerciseId`)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        )
        selectStatement.toSetEntity(this, insertStatement)
        execSQL("DROP TABLE `Session`")
        execSQL("DROP TABLE `_tmp_session`")
    }

    private fun Cursor.toPlanHistory(insert: SupportSQLiteStatement) {
        if (moveToFirst()) {
            val idIndex = getColumnIndex("id")
            val isActiveIndex = getColumnIndex("isActive")
            do {
                val id = getInt(idIndex)
                val isActive = getInt(isActiveIndex) == 1
                if (isActive) insert.insertPlanHistory(id)
            } while (moveToNext())
        }
    }

    private fun Cursor.toPlanDay(db: SupportSQLiteDatabase, insert: SupportSQLiteStatement) {
        if (moveToFirst()) {
            val idIndex = getColumnIndex("id")
            val exerciseMapIndex = getColumnIndex("exercisesPerDay")
            do {
                val id = getInt(idIndex)
                val exerciseMapString = getString(exerciseMapIndex)
                val exerciseMap = Json.decodeFromString(exerciseMapSerializer, exerciseMapString)
                exerciseMap.forEach { (day, exercises) ->
                    exercises.forEach { exercise ->
                        insert.insertPlanDays(day, id, db.exerciseId(exercise.name))
                    }
                }
            } while (moveToNext())
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun SupportSQLiteStatement.insertPlanDays(
        dayOfWeek: DayOfWeek,
        planId: Int,
        exerciseId: Int,
    ) {
        clearBindings()
        bindLong(1, dayOfWeek.isoDayNumber.toLong())
        bindLong(2, planId.toLong())
        bindLong(3, exerciseId.toLong())
        executeInsert()
    }

    private fun SupportSQLiteDatabase.exerciseId(name: String): Int {
        val getId = query("SELECT id FROM exercises WHERE name = ?", arrayOf(name))
        getId.moveToFirst()
        return getId.getInt(getId.getColumnIndexOrThrow("id"))
    }

    private fun Cursor.toSetEntity(db: SupportSQLiteDatabase, insert: SupportSQLiteStatement) {
        if (moveToFirst()) {
            val idIndex = getColumnIndex("id")
            val setsIndex = getColumnIndex("sets")
            do {
                val sessionId = getInt(idIndex)
                val setsString = getString(setsIndex)
                val sets = Json.decodeFromString(setsSerializer, setsString)
                for (i in sets.indices) {
                    insert.insertSet(db, sets[i], sessionId, i)
                }
            } while (moveToNext())
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun SupportSQLiteStatement.insertSet(
        db: SupportSQLiteDatabase,
        set: Set,
        sessionId: Int,
        order: Int
    ) {
        clearBindings()
        bindLong(1, set.repsOrDuration.toLong())
        bindDouble(2, set.weight.toDouble())
        bindString(3, set.type.name)
        bindLong(4, order.toLong())
        bindLong(5, sessionId.toLong())
        val exerciseId = db.exerciseId(set.exercise.name)
        bindLong(6, exerciseId.toLong())
        executeInsert()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun SupportSQLiteStatement.insertPlanHistory(id: Int) {
        clearBindings()
        bindLong(1, id.toLong())
        bindLong(2, localDate.toEpochDays().toLong())
        executeInsert()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun SupportSQLiteDatabase.createIndex(
        tableName: String,
        vararg column: String,
    ) {
        val columns = column.joinToString("_")
        val columnInTable = column.joinToString(",") { "`$it`" }
        execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_${tableName}_$columns`
            ON `$tableName` ($columnInTable)
            """.trimIndent()
        )
    }

    private val exerciseMapSerializer =
        MapSerializer(DayOfWeekSerializer, ListSerializer(ExerciseEntity.serializer()))

    private val setsSerializer = ListSerializer(Set.serializer())
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sets ADD COLUMN rir INTEGER NOT NULL DEFAULT 2")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `weights` (
            `date` INTEGER NOT NULL,
            `value` REAL NOT NULL,
            `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)
            """.trimIndent(),
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plans ADD COLUMN dayTitles TEXT DEFAULT NULL")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN planDayOverride INTEGER DEFAULT NULL")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN isBodyweight INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create tags table - must match Room schema exactly
        db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`name` TEXT NOT NULL, `parentId` INTEGER, `sortOrder` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")

        // 2. Create exercise_tags junction table - must match Room schema exactly
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_tags` (`exerciseId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`, `tagId`), FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")

        // 3. Insert parent tags (body parts)
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (1, '胸', NULL, 1)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (2, '背', NULL, 2)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (3, '腿', NULL, 3)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (4, '肩', NULL, 4)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (5, '手臂', NULL, 5)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (6, '腹', NULL, 6)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (7, '有氧', NULL, 7)")

        // 4. Insert child tags (specific muscles)
        // 胸
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (8, '上胸', 1, 1)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (9, '中胸', 1, 2)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (10, '下胸', 1, 3)")
        // 背
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (11, '背阔肌', 2, 1)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (12, '斜方肌', 2, 2)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (13, '竖脊肌', 2, 3)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (14, '菱形肌', 2, 4)")
        // 腿
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (15, '股四头肌', 3, 1)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (16, '腘绳肌', 3, 2)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (17, '小腿', 3, 3)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (18, '臀肌', 3, 4)")
        // 肩
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (19, '前束', 4, 1)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (20, '中束', 4, 2)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (21, '后束', 4, 3)")
        // 手臂
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (22, '二头肌', 5, 1)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (23, '三头肌', 5, 2)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (24, '前臂', 5, 3)")
        // 腹
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (25, '上腹', 6, 1)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (26, '下腹', 6, 2)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (27, '腹斜肌', 6, 3)")
        // 有氧
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (28, '跑步', 7, 1)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (29, '骑行', 7, 2)")
        db.execSQL("INSERT INTO `tags` (`id`, `name`, `parentId`, `sortOrder`) VALUES (30, '划船', 7, 3)")

        // 5. Add countType column to exercises
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `countType` TEXT NOT NULL DEFAULT 'REPS'")
        // Set countType to MINUTES for cardio exercises
        db.execSQL("UPDATE `exercises` SET `countType` = 'MINUTES' WHERE `target` = 'Cardio'")

        // 6. Map existing exercises' target to tags in exercise_tags
        // Map each MuscleGroups value to the appropriate child tag
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 8 FROM `exercises` WHERE `target` = 'Chest'")    // 胸→上胸
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 11 FROM `exercises` WHERE `target` = 'Lats'")     // 背→背阔肌
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 11 FROM `exercises` WHERE `target` = 'Traps'")    // 斜方肌→背阔肌（最接近的匹配）
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 13 FROM `exercises` WHERE `target` = 'UpperBack'") // 上背→竖脊肌
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 15 FROM `exercises` WHERE `target` = 'Quads'")    // 腿→股四头肌
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 16 FROM `exercises` WHERE `target` = 'Hamstrings'")
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 17 FROM `exercises` WHERE `target` = 'Calves'")
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 18 FROM `exercises` WHERE `target` = 'Glutes'")
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 20 FROM `exercises` WHERE `target` = 'Shoulders'") // 肩→中束
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 22 FROM `exercises` WHERE `target` = 'Biceps'")
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 23 FROM `exercises` WHERE `target` = 'Triceps'")
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 25 FROM `exercises` WHERE `target` = 'Core'")     // 核心→上腹
        db.execSQL("INSERT INTO `exercise_tags` (`exerciseId`, `tagId`) SELECT `id`, 28 FROM `exercises` WHERE `target` = 'Cardio'")   // 有氧→跑步

        // 7. Remove target column from exercises
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercises_new` (`name` TEXT NOT NULL, `countType` TEXT NOT NULL, `reference` TEXT, `isIsometric` INTEGER NOT NULL, `isBodyweight` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL("INSERT INTO `exercises_new` (`name`, `countType`, `reference`, `isIsometric`, `isBodyweight`, `id`) SELECT `name`, `countType`, `reference`, `isIsometric`, `isBodyweight`, `id` FROM `exercises`")
        db.execSQL("DROP TABLE `exercises`")
        db.execSQL("ALTER TABLE `exercises_new` RENAME TO `exercises`")
    }
}
