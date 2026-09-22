/*
 * Copyright (C) 2025 LooKeR & Contributors
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
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

package com.looker.kenko.utils

/**
 * Central place for otherwise-hardcoded app-wide constants (file names, MIME
 * types, timings). Keep literals out of call sites and reference these instead.
 */
object AppConstants {

    const val DATABASE_NAME = "kenko_database"
    const val DATABASE_ASSET = "kenko.db"
    const val TEMP_BACKUP_FILE = "temp_backup.zip"

    const val MIME_JSON = "application/json"
    const val MIME_ZIP = "application/zip"

    const val TIMER_TICK_MILLIS = 1_000L

    const val DEBOUNCE_SHORT_MILLIS = 200L
    const val DEBOUNCE_LONG_MILLIS = 500L
}
