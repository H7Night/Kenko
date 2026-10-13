/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.domain.health

import com.looker.kenko.domain.model.Session

/**
 * Drops the currently-active session from a bulk sync/backfill set.
 *
 * A training day's session can be ended and later resumed; while it is being trained
 * again it still carries the previous end time, so it must not be published to Health
 * Connect until the user actually finishes (End Workout). The end-triggered write
 * happens outside this filter.
 */
fun List<Session>.excludingActiveSession(activeSessionId: Int?): List<Session> =
    if (activeSessionId == null) this else filter { it.id != activeSessionId }
