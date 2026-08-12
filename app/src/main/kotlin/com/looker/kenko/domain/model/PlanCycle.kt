package com.looker.kenko.domain.model

/** 训练周期推进规则:基于实际训练日序号(1..dayCount)推进到下一个序号,越界回绕。 */
object PlanCycle {
    fun nextDayIndex(actualDayIndex: Int, dayCount: Int): Int = (actualDayIndex % dayCount) + 1
}
