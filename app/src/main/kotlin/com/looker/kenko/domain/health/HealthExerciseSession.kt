/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.domain.health

import com.looker.kenko.domain.model.Session

/** A Health-Connect-ready training session, decoupled from the HC SDK for testing. */
data class HealthExerciseSession(
    val clientRecordId: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
)

/**
 * Maps a Kenko [Session] to [HealthExerciseSession], or null when it cannot become a
 * valid HC interval: no id, missing start/end, or end <= start.
 */
fun Session.toHealthExerciseSession(exerciseNames: List<String>): HealthExerciseSession? {
    val sessionId = id ?: return null
    val start = startedAt ?: return null
    val end = endedAt ?: return null
    if (end <= start) return null
    val title = dayTitleOverride?.takeIf { it.isNotBlank() }
        ?: exerciseNames.joinToString(", ").take(100)
    return HealthExerciseSession(
        clientRecordId = "kenko:session:$sessionId",
        startMillis = start,
        endMillis = end,
        title = title,
    )
}
