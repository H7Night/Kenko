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

package com.looker.kenko.data.repository

import android.util.Log
import com.looker.kenko.BuildConfig

/**
 * 调试打点：统计聚合耗时与所在线程，用于验证聚合是否在后台执行、冷启动成本。
 * 仅在 debug 构建输出（logcat tag `KenkoStats`）。
 */
internal object StatisticsTiming {
    private const val TAG = "KenkoStats"

    fun logAggregation(durationNanos: Long, threadName: String, cold: Boolean) {
        if (!BuildConfig.DEBUG) return
        val ms = durationNanos / 1_000_000
        Log.d(TAG, "aggregate ${ms}ms on $threadName${if (cold) " (cold)" else ""}")
    }
}
