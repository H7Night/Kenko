# 自定义训练循环周期设计(Plan Training Cycle)

日期:2026-08-12
状态:已批准(用户已确认三段设计)

## 背景与目标

现状:计划按"固定 7 天一周"组织——`plan_day` 表以 `dayOfWeek`(ISO 1–7)绑定动作,Home 今日训练由 `today().dayOfWeek` 推导,编辑页用星期选择器。

用户需求:

1. **调整日期**:计划编辑时希望把某训练日(如周一)整体改期到另一天(如周三)——当前只能切换星期增删动作,无法搬移。
2. **自定义训练周期**:存在三分化训练方案(练 3 休 1,8 天一循环),需要支持用户自选训练循环周期,做定制化训练方案。
3. **Home 同步适配**:今日训练界面的推导逻辑随新模型调整。

## 需求澄清结论(与用户逐项确认)

| 问题 | 结论 |
|---|---|
| 今日推导方式 | **训练日序列推进式**:计划 = 一串训练日(如三分化 = 3 个训练日),练完一天自动推进到下一个,循环轮转;休息日自行决定 |
| 星期几 | **彻底去掉**:训练日只按"第 N 天"或自定义名,不再绑定星期几 |
| 推进控制 | **自动推进 + 手动覆盖**:完成训练自动推进;Home 菜单可手动切换当前训练日 |
| 休息日推进 | 推进到休息日后再**手动/按日期推进**;休息日当天仍可开始训练并选择计划中任意训练日 |
| 旧数据迁移 | **完整 7 训练日迁移**,无动作的天标记为休息日 |
| 编辑交互 | **训练日标签条 + 动态增删**(标签横向滚动、末尾"+"新增、长按拖动排序、菜单重命名/设休息/删除) |
| 训练日命名 | 可自定义,默认"第 N 天" |
| Home 切换入口 | 收入菜单(卡片 ⋮ → 切换训练日) |
| 实现方案 | **方案 A(最小改造)**:dayOfWeek 重语义为 dayIndex,plans 加 dayCount/currentDayIndex,休息日隐式 |
| 新建默认 | 默认 7 个训练日 |

## 数据模型(数据库 v12 → v13)

| 表 | 变更 |
|---|---|
| `plans` | 新增 `dayCount INT NOT NULL DEFAULT 7`(序列长度 = 周期长度)、新增 `currentDayIndex INT NOT NULL DEFAULT 1`(推进位置,1..dayCount) |
| `plan_day` | `dayOfWeek` 重命名 `dayIndex`(训练日序号 1..N,不再指星期);`sortOrder` 保留;数据值 1–7 原样保留 |
| `sessions` | `planDayOverride` 重命名 `dayIndexOverride`(记录本次实际训练日序号,可空);数据值保留 |

**不变量**:休息日 = 序列中(1..dayCount)内没有 `plan_day` 行的位置。`plans.dayTitles` JSON 的 key 由 isoDayNumber 重语义为 dayIndex(训练日自定义名),迁移后值不变。

### 迁移 MIGRATION_12_13(数据零改动)

```sql
ALTER TABLE plans ADD COLUMN dayCount INTEGER NOT NULL DEFAULT 7
ALTER TABLE plans ADD COLUMN currentDayIndex INTEGER NOT NULL DEFAULT 1
ALTER TABLE plan_day RENAME COLUMN dayOfWeek TO dayIndex
ALTER TABLE sessions RENAME COLUMN planDayOverride TO dayIndexOverride
```

现有"周一/周三/周五有动作"的计划自动成为 `dayCount=7` 序列:Day1/3/5 有动作,Day2/4/6/7 为休息日;历史 session 的 override 原值即训练日序号。

## 行为规则(推进与关联)

1. **完成训练**(保存 session)时:`currentDayIndex = 实际训练日 % dayCount + 1`——基于本次**实际训练**的训练日推进,而非当前显示位置(休息日练了 Day2 → 推进到 Day3)。
2. **手动覆盖**:Home 菜单选择任意训练日 → 更新 `currentDayIndex`;本次 session 的 `dayIndexOverride` 记录所选日。
3. **休息日**:`currentDayIndex` 指向无动作位置时,Home 显示"今日休息"卡片 + "选择训练日训练"入口(弹层列全部训练日);位置不自动跳变。
4. **删除训练日**:若删除位置恰为 `currentDayIndex`,回退到 1。
5. `SessionDao.getPreviousSessionDate` 的 `(date+3)%7+1` weekday 推导**删除**(新模型不依赖日期推星期,关联靠 `dayIndexOverride`)。

## UI 设计

### Home(今日训练)

- **训练日状态**:训练卡显示训练日名(自定义名或"第 N 天")+ 序列进度"第 N/M 天"+ 动作列表 + 开始训练按钮。
- **休息日状态**:"今日休息"卡片 + 进度 + "选择训练日训练"入口。
- **手动覆盖**:训练卡菜单(⋮)→"切换训练日"→ 弹层选任意训练日(含休息标记)。
- `PlanInfoCard`:显示"第 N/M 天 + 训练日名"替代 weekday。

### 计划编辑(PlanEdit)

- 顶部**训练日标签条**(替代星期选择器):标签显示训练日名(自定义名或"第 N 天"),休息日灰显 + "休"角标;末尾"+"新增;长按拖动排序(dayIndex 联动);标签菜单:重命名 / 设为休息日(清空动作)/ 删除该训练日。
- 选中标签后,下方为当天动作列表(现有拖拽排序/删除/添加逻辑不变);休息日显示空态 + "添加动作"(添加后自动转训练日)。
- 动作经 `SelectExercise` 绑定到选中 `dayIndex`;`dayTitles` 存训练日名。
- 新建计划:默认 7 个训练日(全空),用户增删/标休息。
- `PlanItem`(列表卡)文案 `7 - workDays` 改为基于 `dayCount` 的实际训练日数。

### Records(历史)

- `SessionSummary.dayTitles` 改为 `Map<Int, String>`(dayIndex → 训练日名);卡片副标题显示训练日名替代"周几"。
- `streamSummaries` 中 Int→DayOfWeek 转换改为直接 Int(dayIndex)。

### 删除项

`HorizontalDaySelector`、`DaySwitcher.kt`、`Days.kt` 的 weekday 遍历、`day_of_week`/`kenko_day_of_week` 字符串数组(7 项);新增"第 N 天 / 休息 / 切换训练日"等字符串。`Heatmap` 日历表头(星期缩写)保留不动。

## 测试

| 层 | 内容 |
|---|---|
| androidTest 迁移 | `MIGRATION_12_13`:12 库插样本(plans 含 dayTitles、plan_day dayOfWeek=1/3/5、sessions override),迁移后断言 dayCount=7、currentDayIndex=1、dayIndex/dayIndexOverride 值保留、FK 完整 |
| JVM 单测 | 推进逻辑(练 DayN → N+1、N=dayCount → 回 1、休息日场景);`titlesMap` 新 key 转换;计划统计文案(dayCount 与训练日数) |
| androidTest 仓库 | 更新 `checkPlanDeletion` 的 dayOfWeek→dayIndex 语义;补推进/增删训练日断言 |

## 改动文件范围

- **数据**:`PlanEntity`(+dayCount/currentDayIndex)、`Plan` domain、`PlanMapper`(weekday→dayIndex)、`PlanDao`(day 参数语义、`getWorkDaysByPlanId` 按 dayCount、新增 `updateCurrentDayIndex`)、`LocalPlanRepo`(推进逻辑)、`SessionDao`(删 `(date+3)%7+1`、列重命名)、`LocalSessionRepo`、`KenkoDatabase` v13 + `MIGRATION_12_13` + schema 13.json
- **UI**:`HomeViewModel`、`Home.kt`、`PlanEditViewModel`、`PlanEdit.kt`、`PlanExercise.kt`、`PlanItem.kt`、`Sessions.kt`、`SessionDetail`(如有 weekday 显示)、新增训练日标签条组件、`strings.xml`/`values-zh`
- **删除**:`DaySwitcher.kt`、`Days.kt` 的 weekday 遍历、`day_of_week`/`kenko_day_of_week` 字符串数组

## 实施边界

- 不引入新依赖;统计页/性能页、Heatmap 日历(星期缩写表头)不动。
- 数据库 v13 走标准增量迁移 + schema 测试。
- 按模型迁移 / Home / 计划编辑 / Records 拆 4 个提交(遵循一个 feature 一个提交原则)。
