/*
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

import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.LocalDate

@JvmInline
value class DateFormat(private val value: String) {

    fun format(
        localDate: LocalDate,
        locale: Locale = Locale.getDefault(Locale.Category.FORMAT),
    ): String {
        val date = Date(localDate.toEpochDays().days.inWholeMilliseconds)
        val javaFormat = SimpleDateFormat(value, locale)
        return javaFormat.format(date)
    }

    companion object {
        val BackupName = DateFormat("yyyyMMdd")
        val YearMonthDay = DateFormat("yyyy-MM-dd")
    }
}

fun formatDate(
    date: LocalDate,
    dateTimeFormat: DateFormat = DateFormat.YearMonthDay,
    locale: Locale = Locale.getDefault(Locale.Category.FORMAT),
): String = dateTimeFormat.format(localDate = date, locale = locale)
