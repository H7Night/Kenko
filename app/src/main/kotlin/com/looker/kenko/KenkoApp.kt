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

package com.looker.kenko

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.looker.kenko.data.local.KenkoDatabase
import com.looker.kenko.ui.CrashHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class KenkoApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var crashHandler: CrashHandler
    @Inject lateinit var database: KenkoDatabase

    override fun onCreate() {
        super.onCreate()
        crashHandler.install()
        warmUpDatabase()
    }

    /**
     * 预热数据库连接与关键查询（后台执行，不阻塞启动），
     * 消除首次进入 Records / Profile 页面时的数据库打开与冷查询等待。
     */
    private fun warmUpDatabase() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                database.openHelper.writableDatabase
                database.weightDao().stream().first()
                database.sessionDao().streamPlanDates().first()
                database.planDao().plansFlow().first()
                database.exerciseDao().stream().first()
            } catch (_: Exception) {
                // 预热失败不影响正常使用
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
