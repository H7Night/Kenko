/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.domain.model

/**
 * Whether a just-ended training session is worth keeping and publishing.
 *
 * A session with no sets and under 60 seconds is treated as an accidental tap: the app
 * discards it, so it must never be written to an external store such as Health Connect.
 */
fun shouldKeepSession(setCount: Int, elapsedSeconds: Long): Boolean =
    setCount > 0 || elapsedSeconds >= 60
