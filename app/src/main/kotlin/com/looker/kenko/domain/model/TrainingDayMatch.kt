package com.looker.kenko.domain.model

import kotlin.collections.Set

/**
 * 从 session 实际训练的动作名反查"最可能对应的训练日序号"。
 *
 * 用途:迁移前的历史 session 没有 dayIndexOverride 快照,但动作本身记录了当时练了什么;
 * 用动作集合与计划的训练日动作集合匹配,为 Records / SessionDetail 显示训练日名提供依据。
 *
 * 匹配规则(优先级):
 * 1. 完全匹配:session 动作集合 == 某训练日动作集合;
 * 2. 子集匹配:session 动作是某训练日动作的真子集(用户练了计划日的部分动作),取动作数最少的天;
 * 3. 多个天同分时取最小的 dayIndex;
 * 4. 均不满足(如练了计划外动作、无动作、无计划)返回 null。
 */
object TrainingDayMatch {

    fun matchDayIndex(
        sessionExerciseNames: Set<String>,
        planDayExerciseNames: Map<Int, Set<String>>,
    ): Int? {
        if (sessionExerciseNames.isEmpty()) return null
        val candidates = planDayExerciseNames.filterValues { it.isNotEmpty() }
        if (candidates.isEmpty()) return null

        // 1) 完全匹配
        val exact = candidates.filterValues { it == sessionExerciseNames }.keys
        if (exact.isNotEmpty()) return exact.minOrNull()

        // 2) 子集匹配:该训练日动作包含 session 全部动作
        val covering = candidates
            .filterValues { daySet -> sessionExerciseNames.all { it in daySet } }
        if (covering.isEmpty()) return null
        // 多天覆盖时取动作数最少的天(最接近实际训练内容);同动作数取最小 dayIndex
        return covering.entries
            .sortedWith(compareBy({ it.value.size }, { it.key }))
            .first()
            .key
    }
}
