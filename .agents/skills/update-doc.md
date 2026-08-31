---
name: update-doc
description: Kenko 项目文档维护技能。用户要求"更新 changelog / readme / agents"或功能完成后需同步文档时调用。规定 CHANGELOG.md、README.md、README_cn.md、.agents/AGENTS.md 的更新规则、内容来源、提交粒度与验证方式。
allowed-tools: Read, Glob, Grep, Bash, Edit
---

# 文档更新规范（update-doc）

> **用途：** 在 Kenko 仓库中按统一规范维护用户可见与 agent 可见的文档。
> **触发时机：** 用户明确要求更新 changelog / readme / agents；或在功能/修复完成后需要同步文档时（用户要求或提交前自查发现文档过期）。

---

## 一、涉及文件与职责

| 文件 | 受众 | 内容 |
| --- | --- | --- |
| `CHANGELOG.md` | 用户/发布 | 用户可见的改动记录（Keep a Changelog 格式） |
| `README.md` | 用户（英文） | 功能特性、技术栈、构建/发布说明（英文） |
| `README_cn.md` | 用户（中文） | 功能特性、技术栈、构建/发布说明（简体中文） |
| `.agents/AGENTS.md` | agent | 工作流、构建命令、架构说明、文档维护规范 |

> ⚠️ **`.agents/` 目录从未被 git 跟踪**：对其修改（含本技能文件本身）只落在工作区，不纳入提交；如需纳入版本控制须先与用户确认。`CHANGELOG.md`、`README.md`、`README_cn.md` 是被跟踪文件，正常提交。

## 二、CHANGELOG.md 规则

1. **格式**：Keep a Changelog（`## [版本] - 日期` 区块；未发布改动放 `## [Unreleased]`），SemVer 版本。
2. **分组**：`### Added` / `### Changed` / `### Fixed` / `### Removed`；无对应条目时省略该组。
3. **内容来源**：`git log main..HEAD --oneline` 提取本分支全部提交 → 只归纳**用户可见**的行为变化；内部重构、lint 修复、纯 docs 提交不列（除非影响用户可见行为）。
4. **条目写法**：一句完整英文，聚焦用户价值，可含功能名/入口位置；不贴代码、不写实现细节。
5. **发布版本化**：由 Release Process 执行（`./gradlew fastlaneChangelog` 或手动），把 Unreleased 条目归入具体版本区块——日常更新只填 Unreleased，不擅自创建版本号。

## 三、README.md / README_cn.md 规则

1. **双语同步**：`README.md`（en）与 `README_cn.md`（zh）必须**同时更新、语义一致**（翻译而非各自发挥）。
2. **功能特性**：`## Features` 列表按字母序/逻辑分组；新增功能加粗条目（`- **Name**: description`）；已有条目内容变化时更新该条目（如 Custom Plans 描述变化）。
3. **技术栈/构建/发布**：仅在对应部分变化时更新（如新增依赖、构建方式变更）。
4. **克制原则**：只反映真实存在的功能，不写规划中/未实现的内容。

## 四、.agents/AGENTS.md 规则

1. **架构说明**（`## Architecture` → `Key patterns`）：新增功能落地后，用 2-4 行补充核心类/文件/入口位置/关键约定（如"反馈走 snackbarState 而非 _snackbar"这类易踩坑约定必写）。
2. **文档维护小节**：本技能被引用处（`## Documentation Maintenance`）保持与技能名/路径一致，不重复技能内容。
3. **工作流/构建命令**：仅当流程本身变化时更新。

## 五、执行步骤

1. **确认范围**：与用户澄清"更新 changelog / readme / agents"各指哪些文件；默认 = CHANGELOG + README 双语 + AGENTS。
2. **收集改动**：`git log main..HEAD --oneline`（或最近未发布提交）梳理全部改动，分类到 Added/Changed/Fixed/Removed。
3. **更新 CHANGELOG**：填入 `## [Unreleased]` 对应分组（英文，用户可见）。
4. **更新 README 双语**：Features 增改条目，en/zh 同步。
5. **更新 AGENTS**：架构/约定补充；若新增了文档规则，同步 `Documentation Maintenance` 引用。
6. **验证**：
   - `git diff --stat` 确认改动范围符合预期；
   - 无 markdown 语法破坏（标题层级、列表缩进）；
   - CHANGELOG 分组与条目数量合理（不夸大、不漏大功能）。
7. **提交**（仅跟踪文件）：按"一个主题一个提交"，文档更新作为一个提交（`docs: update changelog and readme for <feature>`）。`.agents/` 改动不入提交。

## 六、常见错误

- **把内部实现写进 CHANGELOG**（如"新增 MIGRATION_12_13"）——应写用户可见效果（"计划支持可变训练日数量"）。
- **README 只改 en 漏 zh**——双语必须同步。
- **Unreleased 里擅自写版本号**——版本号由发布流程决定。
- **把 .agents/ 的改动 add 进提交**——该目录未跟踪，保持原状（除非用户明确要求跟踪）。
