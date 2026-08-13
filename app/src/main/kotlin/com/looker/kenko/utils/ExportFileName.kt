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

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 统一导出/备份文件名规范：`kenko_<project>_<yyyyMMddHHmmss>.<extension>`
 *
 * 示例：
 * - 计划导出：`kenko_plans_20260813105304.json`
 * - 应用备份：`kenko_app_202608011533.zip`
 * - 训练数据导出：`kenko_data_20260813105304.json`
 * - 崩溃日志：`kenko_crash_20260813105304.log`
 *
 * 所有导出功能（计划、数据、备份、崩溃日志）都应通过本工具生成文件名，
 * 禁止在各处硬编码/重复实现文件名拼接。
 */
object ExportFileName {

    fun forProject(project: String, extension: String): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val timestamp = buildString {
            append(now.year)
            append(now.monthNumber.toString().padStart(2, '0'))
            append(now.dayOfMonth.toString().padStart(2, '0'))
            append(now.hour.toString().padStart(2, '0'))
            append(now.minute.toString().padStart(2, '0'))
            append(now.second.toString().padStart(2, '0'))
        }
        return "kenko_${project}_$timestamp.$extension"
    }
}
