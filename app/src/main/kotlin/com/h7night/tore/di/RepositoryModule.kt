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

package com.h7night.tore.di

import com.h7night.tore.data.local.datastore.DatastoreSettingsRepo
import com.h7night.tore.data.repository.ExerciseRepo
import com.h7night.tore.data.repository.PerformanceRepo
import com.h7night.tore.data.repository.PlanRepo
import com.h7night.tore.data.repository.SessionRepo
import com.h7night.tore.data.repository.SettingsRepo
import com.h7night.tore.data.repository.TagRepo
import com.h7night.tore.data.repository.WeightRepo
import com.h7night.tore.data.repository.local.LocalExerciseRepo
import com.h7night.tore.data.repository.local.LocalPerformanceRepo
import com.h7night.tore.data.repository.local.LocalPlanRepo
import com.h7night.tore.data.repository.local.LocalSessionRepo
import com.h7night.tore.data.repository.local.LocalTagRepo
import com.h7night.tore.data.repository.local.LocalWeightRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSessionRepo(
        repo: LocalSessionRepo,
    ): SessionRepo

    @Binds
    abstract fun bindPlanRepo(
        repo: LocalPlanRepo,
    ): PlanRepo

    @Binds
    abstract fun bindExerciseRepo(
        repo: LocalExerciseRepo,
    ): ExerciseRepo

    @Binds
    abstract fun bindPerformanceRepo(
        repo: LocalPerformanceRepo,
    ): PerformanceRepo

    @Binds
    abstract fun bindSettingsRepo(
        repo: DatastoreSettingsRepo,
    ): SettingsRepo

    @Binds
    abstract fun bindWeightRepo(
        repo: LocalWeightRepo,
    ): WeightRepo

    @Binds
    abstract fun bindTagRepo(
        repo: LocalTagRepo,
    ): TagRepo
}
