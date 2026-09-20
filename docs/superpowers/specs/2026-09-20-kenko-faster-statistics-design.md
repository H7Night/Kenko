# Kenko 统计页性能优化设计（feat/kenko-faster）

- 日期：2026-09-20
- 状态：待用户 review
- 分支：`feat/kenko-faster`（基线 `feat/v1.9.0`）
- 范围：**统计页（Statistics）性能与首屏体验**；允许兼容性修改共享数据层

## 1. 背景

用户反馈：进入统计页需要等待约 1~2 秒。经代码探查，根因**不是 SQLite 存储引擎**（预置库约 140KB），而是：

- **N+1**：`data/local/repository/local/LocalSessionRepo.kt` 的 `stream`（`:55`）与 `toExternal`（`:234`）对**每个 set** 逐条 `exerciseDao.get()`。
- **主线程重活**：`ui/feature/statistics/StatisticsViewModel.kt:51` 用一个 `combine(streamSummaries, stream, exercises, plan, tags)` 做全量聚合；`utils/ViewModel.kt:33` 的 `asStateFlow()` 是 `stateIn(viewModelScope, …)`，`combine`/`map` 的变换默认落在 **Main**。
- **无缓存 / 反复重算**：`WhileSubscribed(5000)`，每次进入按需重新订阅并重算；`stream` 通过 `@Relation`（`data/local/dao/SessionDao.kt:131`）加载全部 session + 全部 set。

对照 NzHelper（同样是 Room/SQLite）：它**一次性在 IO 线程全量读入内存**，聚合为纯内存 O(n) 函数，Tab 激活才加载，并用 `derivedStateOf`/`remember` 缓存结果——因此快而流畅。

## 2. 目标 / 成功标准 / 非目标

### 成功标准（可衡量）

- **首帧 <100ms**：有缓存时立即渲染统计内容；无缓存时立即渲染骨架。
- **稳态无顿挫**：再次进入、数据变化后的刷新，主线程无 >16ms 卡顿（聚合在后台线程）。
- **首次加载**总时长不劣于现状，且加载期间 UI 可交互。
- 统计口径与展示内容与优化前**完全一致**。

### 非目标

- 不引入 FTS/文本存储（属于 `feat/fitness-file`）。
- 不做统计 SQL 全量下推、不建物化统计表（方案 B/C，后续按需）。
- 不改统计口径、不改图表视觉（仅新增骨架占位）。
- 不改 Home/Records 的交互（共享层修复仅带来性能收益）。

## 3. 方案选择

- **A. 后台聚合 + 进程内缓存（SWR）+ 修复 N+1（本次采用）**：投入小、风险低，正中病根。
- B. 统计下推 SQLite（GROUP BY/CTE）：传输最小、规模大时快；但要在 SQL 重写部位/标签解析，复杂、测试成本高。
- C. 物化 `stats_daily` + 增量更新：读最快，一致性/失效/迁移复杂度最高。

采用 **A**：个人规模下，后台内存聚合即可达毫秒级稳态；若未来数据量增大再评估 B/C。

## 4. 数据路径设计

统计**不再订阅** `sessionRepo.stream`（N+1 重流），改用廉价来源，并全部在后台计算。

| 输出 | 来源 |
|---|---|
| 得分/热力图/趋势/依从/计划计数 | `sessionRepo.streamSummaries`（单次 JOIN + `GROUP_CONCAT`，无 N+1） |
| 有氧分钟（按日期/周期） | 新增 DAO 聚合查询（见下） |
| 动作/标签（部位解析） | `exerciseRepo.stream`、`tagRepo.stream`（小表） |
| 当前计划 | `planRepo.current` |

- 有氧判定仍按「动作解析部位 == 有氧」在内存完成；由 exercises+tags 解析出**有氧动作 id 集合**（小），再查：
  ```sql
  SELECT s.date AS date, SUM(st.repsOrDuration) AS minutes
  FROM sets st JOIN sessions s ON s.id = st.sessionId
  WHERE st.exerciseId IN (:cardioExerciseIds)
  GROUP BY s.date
  ```
  新增 `SetsDao`（或 `SessionDao`）方法返回 `Flow<List<CardioMinutesByDate>>`。空集合时返回空流（避免 `IN ()`）。
- 线程：IO 读取在 `Dispatchers.IO`，聚合在 `Dispatchers.Default`（`flowOn(Dispatchers.Default)` 或显式 `withContext`）。
- 聚合纯函数保持现有 `domain/statistics/Aggregation.kt`（`buildTagDict`、`aggregateByBodyPart`、`aggregateCardioMinutes` 等）逻辑不变；仅调整输入来源与执行线程。

> 兼容性：`SessionRepo` / `ExerciseRepo` / `TagRepo` / `PlanRepo` 接口签名不变。

## 5. 共享层 N+1 修复（向后兼容）

- 新增 `ExerciseDao.getByIds(ids: List<Int>): List<ExerciseEntity>`（一次 `IN` 查询）。
- `LocalSessionRepo` 中 `List<SetEntity>.toExternal()` 改为：先收集全部 `exerciseId`，一次取回建 `Map<Int, Exercise>`，再映射；避免逐组查询。
- `stream`、`streamByDate`、`getSets` 等调用点共用该批量映射。
- 空集合保护：`ids` 为空时不查询。

## 6. 缓存与失效（SWR）

- 新增 `@Singleton StatisticsRepository`，用应用作用域 `StateFlow` 同时承担「缓存」与「失效」：
  ```kotlin
  class StatisticsRepository @Inject constructor(
      private val sessionRepo: SessionRepo,
      private val setsDao: SetsDao,
      private val exerciseRepo: ExerciseRepo,
      private val tagRepo: TagRepo,
      private val planRepo: PlanRepo,
      @ApplicationScope private val appScope: CoroutineScope,
  ) {
      // null = 尚无结果（首帧显示骨架）；非 null = 缓存结果
      val state: StateFlow<StatisticsUiState?> = combine(
          sessionRepo.streamSummaries,
          setsDao.streamCardioMinutesByDate(),
          exerciseRepo.stream,
          tagRepo.stream,
          planRepo.current,
      ) { summaries, cardio, exercises, tags, plan ->
          aggregateStatistics(summaries, cardio, exercises, tags, plan) // 纯内存聚合
      }
          .flowOn(Dispatchers.Default)                       // 聚合在后台
          .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)
  }
  ```
- **缓存**：`stateIn` 的 `StateFlow` 在无订阅后**保留最后一个值**；再次进入统计页时先拿到上次结果（瞬时渲染），随后上游重启并刷新。
- **失效**：由 Room 对输入表的无效化**自动**触发重算，无需手动失效信号。
- **去抖/合并**：`stateIn` 的 conflate 语义天然合并高频变更；`WhileSubscribed(5_000)` 让离开页面 5 秒内保持上游存活，避免来回切换重复计算。
- 不使用手动 `refresh()`/计数器；`isLoading` 由 `state == null` 判定。

## 7. ViewModel 与 UI

- `StatisticsViewModel`：
  - `state = repository.state`（应用作用域缓存）。
  - `isLoading = repository.state.value == null`。
  - 订阅 `state` 即启动上游（`stateIn(WhileSubscribed)`），无需手动 `refresh()`。
- `Statistics.kt`（`LazyColumn`，`:61`）：
  - 有缓存：立即渲染现有 7 张卡片。
  - 无缓存：渲染**骨架卡片**（与真实卡片同尺寸/圆角的占位），保证首帧 <100ms。
  - 数据到达后自然替换为内容。
- 保留 `HeatmapCard` 现有 600ms 入场动画（`HeatmapCard.kt:162`）；其余卡片的淡入为可选增强，不作为验收项。

## 8. 测试与验收

- **单测**：
  - 聚合逻辑回归（现有 `AggregationTest` 等保持不变、必须通过）。
  - 批量取 exercise 只查一次（可用 fake DAO 计数或 Room `Test` 验证行数为 1）。
  - 缓存行为：首次 `state == null` → 聚合后更新；离开再进入时先返回上次值，上游重启后更新；高频变更由 conflate 合并。
  - 有氧聚合查询：空 id 集合、无数据、跨周期过滤。
- **设备验收**（真机/模拟器）：
  - 打点日志记录「首帧时间 / 聚合耗时 / 是否在后台线程」。
  - 断言：有缓存进入首帧 <100ms；聚合不在 Main（例如日志输出 `Dispatchers.Default` 线程名 / `Looper` 检查）。
- **回归**：Home/Records/统计三处数据与优化前一致。

## 9. 风险与回退

- 风险：缓存带来短暂“陈旧数据”。缓解：离开页面 5 秒内上游保持存活、再次进入即重启刷新；缓存仅作首帧加速，随后被后台结果覆盖。
- 风险：有氧聚合 SQL 与内存解析口径不一致。缓解：单测覆盖有氧分钟与旧实现结果一致性。
- 回退：改动集中在 `StatisticsViewModel`/新 `StatisticsRepository`/`SetsDao`/`ExerciseDao`/`LocalSessionRepo`，可独立回退；不改数据库 schema。

## 10. 验收清单（Definition of Done）

- [ ] 进入统计页有缓存时首帧 <100ms，无缓存时骨架首帧 <100ms。
- [ ] 聚合在后台线程完成，主线程无 >16ms 卡顿。
- [ ] 统计数值/图表与优化前一致（回归通过）。
- [ ] `./gradlew test` 通过；`assembleDebug` 通过。
- [ ] Home/Records 无行为回归，且 N+1 修复带来读取次数下降。
