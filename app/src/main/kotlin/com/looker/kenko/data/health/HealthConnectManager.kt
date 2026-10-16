/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.looker.kenko.domain.health.HealthExerciseSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client(): HealthConnectClient? =
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasAllPermissions(): Boolean {
        val client = client() ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /** Best-effort single write; returns true on success. */
    suspend fun write(session: HealthExerciseSession): Boolean {
        val client = client() ?: return false
        return runCatching {
            client.insertRecords(listOf(session.toRecord()))
            true
        }.onFailure { Log.w(TAG, "Health Connect write failed for ${session.clientRecordId}", it) }
            .getOrDefault(false)
    }

    /** Writes every session not yet present, using clientRecordId idempotency. Returns count written. */
    suspend fun syncAll(sessions: List<HealthExerciseSession>): Int {
        val client = client() ?: return 0
        var written = 0
        for (session in sessions) {
            val ok = runCatching {
                client.insertRecords(listOf(session.toRecord()))
                true
            }.onFailure { Log.w(TAG, "Health Connect sync failed for ${session.clientRecordId}", it) }
                .getOrDefault(false)
            if (ok) written++
        }
        return written
    }

    private companion object {
        const val TAG = "HealthConnectManager"
    }
}

private fun HealthExerciseSession.toRecord(): ExerciseSessionRecord {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis)
    val end = Instant.ofEpochMilli(endMillis)
    return ExerciseSessionRecord(
        start,
        zone.rules.getOffset(start),
        end,
        zone.rules.getOffset(end),
        Metadata.manualEntry(clientRecordId = clientRecordId),
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        title.takeIf { it.isNotBlank() },
    )
}
