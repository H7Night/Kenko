package com.h7night.tore.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Retains exercise ordering from Kenko version 20 backups. */
@Entity(
    tableName = "session_exercises",
    primaryKeys = ["sessionId", "exerciseId"],
    foreignKeys = [
        ForeignKey(entity = SessionDataEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExerciseEntity(val sessionId: Int, val exerciseId: Int, val order: Int)
