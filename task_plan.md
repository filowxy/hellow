# TISAM — There Is Still A Mod 实施计划

## 项目目标（Project Goal）

按照 `项目设计.md` 的规格，构建一个 **NeoForge 1.21.1** 客户端模组，作为整合包作者的**合规推荐系统**：
- 启动时扫描 `mods` 文件夹，对比 `config/tisam/modules.json` 中的"选择性模组列表"
- 若有未安装的模组，进入标题界面后弹出简洁提示
- 玩家点击"查看模组"后展示详细列表，每个模组可点击官方链接
- 链接跳转由 NeoForge 浏览器 API 处理（不内置任何 ARR 模组文件，不自动下载）
- 支持提醒频率递减、永久忽略、预览模式、自定义安装说明

## 关键决策（Key Decisions）

| # | 决策点 | 选择 | 理由 |
|---|--------|------|------|
| D1 | Mod Loader | **NeoForge 1.21.1**（21.1.x） | 用户技术栈首选 + 设计文档明确指定 |
| D2 | 构建系统 | **Gradle（ModDevGradle MDK）** | NeoForge 1.21.1 官方模板，GitHub Actions 友好 |
| D3 | 语言 | **Java 21**（主），可在 UI 层用 Kotlin | 用户允许部分 Kotlin，但核心模组逻辑用 Java 保持兼容性 |
| D4 | Mod ID | `tisam` | 与项目名一致（小写+下划线符合 NeoForge 规范） |
| D5 | 包路径 | `com.tisam` | 与 Mod ID 一致 |
| D6 | 配置文件 | `config/tisam/modules.json` + `config/tisam/preferences.json` | 完全遵循设计文档第三节 |
| D7 | 许可证 | **Apache-2.0** | 用户确认（设计文档第一节指定） |
| D8 | 持久化库 | **Gson**（MDK 自带，不引入新依赖） | 复用现有依赖，符合"ponytail"最少代码原则 |
| D9 | UI 框架 | **NeoForge Screen API**（vanilla Minecraft Screen） | 不引入 Cloth/Jeed 等额外 UI 库 |
| D10 | 浏览器跳转 | `net.neoforged.neoforge.client.ClientHooks#openUrl` | NeoForge 标准 API，自动处理隐私提示 |
| D11 | Mod 检测方式 | **对比 jar 文件名** | 用户确认；扫描 `mods/*.jar` 文件名与 `modules[].id` |
| D12 | 构建系统 | **ModDevGradle（Kotlin DSL）** | 用户确认；mochi-753/MDK-1.21.1-ModDevGradle |
| D13 | 永久忽略流程 | **必须经过免责声明弹窗**（新增需求） | 用户补充：点击"永久忽略"→ 弹免责声明（内容来自 `modules.json` 中 `disclaimer_text` 字段）→ 提供「返回」「确认」两按钮 → 确认按钮 5 秒倒计时后才可点击 |

## 阶段划分（Phases）

### 阶段 1：项目骨架与构建系统
**目标**：用 NeoForge 1.21.1 ModDevGradle MDK 建立可编译的项目骨架。
**产物**：
- `gradle.properties`（mod_id=tisam, mod_name=There Is Still A Mod, mod_license=待定）
- `build.gradle` / `build.gradle.kts`（MDK 标准配置）
- `settings.gradle` / `settings.gradle.kts`
- `gradle/wrapper/` + `gradlew` + `gradlew.bat`
- `gradlew build` 通过（即：能成功构建出空 jar）

**验证**：`./gradlew build` 退出码 0，产物 `build/libs/tisam-*.jar` 存在。

---

### 阶段 2：Mod 元数据与主类
**目标**：注册模组，定义模组 ID、显示名、描述、依赖。
**产物**：
- `src/main/resources/META-INF/neoforge.mods.toml`（modId, version, displayName, description, authors, license, dependencies）
- `src/main/resources/pack.mcmeta`
- `src/main/java/com/tisam/Tisam.java`（主类，订阅 `FMLClientSetupEvent` / `FMLCommonSetupEvent`）
- `src/main/java/com/tisam/Constants.java`（modId, logPrefix, etc. — 满足用户"使用常量替代魔法数字"偏好）

**验证**：`./gradlew build` 仍通过；将 jar 放入测试环境后能在 mods 列表中显示 "There Is Still A Mod"。

---

### 阶段 3：配置系统（ConfigManager）
**目标**：加载 `config/tisam/modules.json` 与 `config/tisam/preferences.json`，缺失时自动生成默认配置。
**产物**：
- `src/main/java/com/tisam/config/ModuleEntry.java`（Gson 数据类：id, name, description, license, url, canIgnore）
- `src/main/java/com/tisam/config/ModulesConfig.java`（含 modules[], reminderFrequency[], installGuideText, preview）
- `src/main/java/com/tisam/config/PreferencesData.java`（permanentlySkipped, reminderCount, lastReminderDate）
- `src/main/java/com/tisam/config/ConfigManager.java`（单例，负责 load/save/getOrCreateDefault，线程安全）

**默认配置文件**（首次启动自动生成到 `config/tisam/`）：
- `modules.json` —— 与设计文档第三节示例完全一致
- `preferences.json` —— 与设计文档第四节示例完全一致

**验证**：
- `./gradlew build` 通过
- 单元测试：用 JUnit 5 验证 ConfigManager 在文件缺失时生成默认、文件存在时正确反序列化、字段缺失时降级处理

---

### 阶段 4：Mod 文件夹扫描（ModScanner）
**目标**：扫描 `.minecraft/mods` 目录，根据 `id` 字段判断哪些推荐模组已安装。
**产物**：
- `src/main/java/com/tisam/scanner/ModScanner.java`（public static Set<String> scanInstalled(ModuleEntry... modules)）

**算法**：
- 读取 `minecraft/mods/` 下所有 `.jar` 文件
- 对每个 jar 读取 `META-INF/mods.toml`（或 `META-INF/MANIFEST.MF` 中 `Mod-Id` 字段）
- 收集所有 modId
- 返回匹配 `modules.json` 中 `id` 字段的子集

**注**：设计文档第三节字段说明写"`modules[].id` 唯一标识符，用于检测 mods 文件夹中是否已有对应文件"——按字面意思可解释为"对比文件名"或"对比 jar 内的 modId"。我选择**对比 modId**（更可靠，因为文件名可能被改但 modId 是 jar manifest 的固定字段）。**待用户确认** Q2。

**验证**：
- `./gradlew build` 通过
- 单元测试：模拟 mods 文件夹，验证已安装/未安装/损坏文件的检测

---

### 阶段 5：提醒策略（ReminderStrategy）
**目标**：根据 `reminder_count` 与 `reminder_frequency[]` 数组决定是否弹窗。
**产物**：
- `src/main/java/com/tisam/reminder/ReminderStrategy.java`
  - `public static boolean shouldRemind(int reminderCount, List<Integer> frequency)`
  - `public static int nextReminderInterval(int reminderCount, List<Integer> frequency)`

**算法**：
- 若 `reminderCount == 0` → 必弹（首次）
- 若 `reminderCount < frequency.size()` → 取 `frequency[reminderCount]`
- 否则取最后一个 `frequency[frequency.size() - 1]`
- `shouldRemind` 返回 `sinceLastReminder >= interval`

**ponytail 注释**：内部用 `gameLaunchCount` 累加器；不持久化，因为 `preferences.json` 已有 `reminderCount` 字段。

**验证**：
- 单元测试：频率数组 `[1,1,3,3,3,7,7,7,7,15]` 的边界值（reminderCount=0,2,4,9,11+）

---

### 阶段 6：UI 屏幕 1 — 首次简洁提示
**目标**：进入标题界面后，若有未安装且未被忽略的模组，弹出简洁提示。
**产物**：
- `src/main/java/com/tisam/client/screen/FirstPromptScreen.java`
- `src/main/java/com/tisam/client/TisamClientEvents.java`（订阅 `ScreenEvent.Opening` 或 `ClientTickEvent` 检测 TitleScreen）
- `src/main/java/com/tisam/client/ModListCache.java`（客户端缓存：未安装模组列表 + 当前 reminderCount）
- `src/main/java/com/tisam/client/screen/DisclaimerScreen.java`（**新增** —— 永久忽略前的免责声明）

**行为**：
- 检测时机：进入 TitleScreen 时检查（`GuiEventListener` 不太合适，用 `ScreenEvent.Opening` 监听 TitleScreen）
- 弹出条件：缓存非空 && !permanentlySkipped && ReminderStrategy.shouldRemind()
- 三个按钮：
  - **View Mods** → 打开 ModListScreen
  - **Later Reminder** → 关闭弹窗，reminderCount++，save preferences
  - **Permanently Skip** → 打开 DisclaimerScreen（**不直接关闭**）

**DisclaimerScreen 行为**：
- 显示来自 `modules.json` 的 `disclaimer_text` 字段
- 两个按钮：
  - **返回** → 关闭 DisclaimerScreen，回到 FirstPromptScreen
  - **确认** → 设置 permanentlySkipped = true，关闭所有弹窗
- **确认按钮 5 秒倒计时**：5 秒内按钮灰显且不可点击，显示「确认 (Xs)」

**验证**：
- `./gradlew build` 通过
- 单元测试：ReminderStrategy 集成测试（首启必弹、跳过 2 次后每 3 次弹 1 次）
- 手动测试：点 Permanently Skip → 弹免责声明 → 5 秒内确认按钮不可点 → 5 秒后可点

---

### 阶段 7：UI 屏幕 2 — 模组详情列表
**目标**：展示所有未安装模组的卡片，每张卡片含名称、描述、许可证、官方链接。
**产物**：
- `src/main/java/com/tisam/client/screen/ModListScreen.java`
- `src/main/java/com/tisam/client/widget/ModCardWidget.java`（每张卡片是一个自定义 Widget）

**布局**（参考设计文档第二步的 ASCII 图）：
- 顶部按钮：**Installation Guide**（切换折叠面板）
- 中部可滚动列表：每张卡片高度 ~80px，显示 name + description + license + URL
- 点击 URL → 调用 `ClientHooks.openUrl(url)`（设计文档明确"NeoForge 自动处理隐私提示"）
- 底部按钮：**Skip for Now** / **Never Remind Again** / **Exit Game**

**验证**：
- `./gradlew build` 通过
- 手动测试：进入游戏后弹出 → 点击 View Mods → 显示卡片 → 点击 URL 浏览器打开

---

### 阶段 8：UI 屏幕 3 — 安装指南折叠面板
**目标**：在 ModListScreen 内点击"Installation Guide"后展开可折叠面板。
**产物**：
- 在 `ModListScreen.java` 内增加 `showInstallGuide` 状态
- 渲染方式：用 `MultiLineTextWidget` 或手动拆分字符串分行绘制
- 文本来源：`config/tisam/modules.json` 的 `installGuideText` 字段

**行为**：
- 展开时覆盖在卡片列表上方
- 顶部显示 "安装指南" 标题
- 底部 **Return** 按钮收起面板

**验证**：`./gradlew build` 通过 + 手动测试展开/收起。

---

### 阶段 9：预览模式（Preview Mode）
**目标**：当 `modules.json` 包含 `"preview": true` 时，每次启动都强制弹出。
**产物**：
- 修改 `TisamClientEvents.java` 的弹窗判断逻辑：`if (config.preview || (cache非空 && !permanentlySkipped && shouldRemind))`

**验证**：手动测试设 `"preview": true` 启动游戏，弹窗必弹。

---

### 阶段 10：整合测试与最终化
**目标**：完整流程跑通；生成 README、CHANGELOG；首次 commit。
**产物**：
- `README.md`（英文版，说明用法、配置文件格式、整合包作者集成步骤）
- `CHANGELOG.md`（v1.0.0 条目）
- `LICENSE`（待定）
- `TODO.md`（可能的未来功能：服务器端同步 / CurseForge 链接兼容 / 翻译文件）
- 第一次 `git commit`

**验证**：
- `./gradlew build` 通过
- `./gradlew test` 通过
- 手动运行 `./gradlew runClient` 完整走一遍 3 步流程

---

## 遇到的问题与解决方案（Issues Log）

| # | 问题 | 状态 | 解决方案 |
|---|------|------|---------|
| 1 | 多模块项目中 `:tisam` 子项目编译失败：`package net.minecraft does not exist` | ✅ 已修复 | 在 `:tisam/build.gradle.kts` 中显式应用 `net.neoforged.moddev` 插件（同样版本号）。根项目的 `neoForge.mods` 块虽然指向 `:tisam` 的 source set，但 moddev 的 classpath 不会传播到子项目自己的 `compileJava`。`:hellow` 已经走的是「library mode」——同一插件在多模块中可复用 |
| 2 | `generateModMetadata` 任务输出到 `build/generated/sources/modMetadata`，moddev 的 `createMinecraftArtifacts` 报"隐式依赖"错误 | ✅ 已修复 | 改为输出到 `build/generated/resources/modMetadata`。`sources/` 是 moddev 自己生成的命名空间，任何写入都会被它当作自己的产物校验 |
| 3 | v2.0 多类型扫描测试在 Windows 上失败：`NoSuchFileException` | ✅ 已修复 | `Files.createFile()` 在 Windows 上不会自动创建父目录（POSIX 行为不一致）。在每个 v2.0 测试中显式调用 `Files.createDirectories(...)` |

## v2.0 增量（Latest Update 增量实施）

`最新更新.md` 描述了从 v1.0 → v2.0 的变更。本节列出已落地的项，避免与上文的 v1.0 阶段混淆。

### v2.0-A：内容类型扩展
- `ModuleEntry` 新增 `ContentType` 枚举：`MOD` / `RESOURCEPACK` / `SHADERPACK` / `DATAPACK`，默认 `MOD`（兼容 v1.0）
- `ModuleEntry` 新增 `installGuideText` 字段（可选覆盖）
- `TisamConstants` 新增 `DEFAULT_INSTALL_GUIDE_BY_TYPE` 映射，提供每种类型的默认安装指南
- `ModScanner` 重写为按类型扫描：
  - `mods/*.jar`、`resourcepacks/*.zip`、`shaderpacks/*.zip`、`datapacks/*.zip`
  - 返回 `Set<String>`，key 为 `type:id` 形式（避免同 id 跨类型冲突）
  - `layoutFor(ContentType)` 是单一目录/扩展名真相源

### v2.0-B：ModListScreen 四类型标签
- 新增标签行（`Mod` / `Resourcepack` / `Shaderpack` / `Datapack`），每个标签带计数
- 默认激活第一个非空类型；空类型仍渲染但禁用
- 安装指南根据激活标签类型自动切换（条目覆盖 > 全局 config > 类型默认）

### v2.0-C：外部链接安全弹窗
- 新增 `ExternalLinkConfirmScreen`，继承 `HellowModal` 复用 dialog/标题/底部按钮布局
- URL 渲染使用 `TextRenderer` 的 `StringMode.CLAMPED` 模式
- 行为（per 最新更新.md §4）：
  - `length <= EXTERNAL_LINK_URL_LIMIT` (60) → `[Copy URL] [Cancel] [Open]`
  - `length > 60` → `[Copy URL] [Cancel]`（无 Open），追加红字警告

### v2.0-D：测试
- `ModScannerTest` 新增 5 个测试：四目录扫描、同 id 跨类型独立、zip 扩展名、per-type 隔离、空目录

### 风险与缓解（Risks & Mitigations）

| 风险 | 影响 | 缓解 |
|------|------|------|
| NeoForge 1.21.1 MDK 下载被 GitHub 限流 | 项目骨架无法生成 | 使用 mirror.ghproxy.com / gh-proxy.com 代理下载 |
| Gradle 首次构建耗时 > 10 分钟 | 用户体验差 | 提醒用户耐心等待；预热 build cache |
| VPN 不稳定导致下载中断 | Gradle 依赖解析失败 | 失败时直接重试（Neoforge 镜像有多个） |
| 设计文档自相矛盾（Apache-2.0 vs MIT） | 许可证无法确定 | **Q1 待用户确认** |
| `modules[].id` 字段语义模糊（文件名 vs modId） | 扫描逻辑可能与设计意图不符 | **Q2 待用户确认** |

## 待用户确认问题（Questions to User）

### ✅ Q1：许可证 — 已确认 **Apache-2.0**

### ✅ Q2：Mod ID 检测方式 — 已确认 **对比 jar 文件名**

### ✅ Q3：构建系统 — 已确认 **ModDevGradle（Kotlin DSL）**

### Q4（新增，由用户补充）：永久忽略前的免责声明
**需求**：点击"永久忽略"按钮后，**不能直接关闭弹窗**，必须弹出免责声明弹窗：
- 弹窗内容来自 `config/tisam/modules.json` 中**新增字段** `disclaimer_text`（字符串）
- 弹窗提供两个按钮：
  - **返回**：关闭免责声明弹窗，回到上一级界面
  - **确认（永久忽略）**：点击后设置 `permanentlySkipped = true`
- **「确认」按钮必须等待 5 秒后**才允许点击（防止误操作）
- 5 秒倒计时期间：按钮灰显、显示剩余秒数（如"确认 (3s)"）

**示例配置**（即将更新到 `modules.json` 默认值）：
```json
{
  "disclaimer_text": "确认永久忽略后，模组将不再提示任何可选模组。如需重新启用，请删除 config/tisam/preferences.json 中的 permanently_skipped 字段。",
  ...
}
```

---

# Part 2 — Hellow v3.0 增强计划（2026-08-28）

## 背景与目标

上一轮（2026-08-28）完成 hellow 公共门面：`Hellow` facade（when / whenEvent / whenApi 事件触发 + show / closeTop / closeAll / showDialog 窗口管理）+ `HellowModal` / `HellowDialog` / `Elements` / `PageBuilder`，并重写窗口栈与 `PageScreen`。构建与全部测试通过。

能力审计（findings.md R7）结论：**布局"骨架"齐了，但"能力"停在能画出来**——缺弹性布局、多行文本、交互反馈、表单状态，且有几处交互转发 bug 让现有控件点不了。

**目标**：把 hellow 从"能画"提升到"能做好布局、能交互、能表单"。
**坚持原则**：
- 调用方定义内容，Hellow 管理呈现；不引入新依赖
- YAGNI：明确列出"不做"的能力上限（H7），避免过度设计
- 每阶段 `gradlew build` 通过；纯几何/状态逻辑用 JUnit 验证

## 能力上限（当前 hellow 完成不了的事，完整版见 findings.md R7）

| 级别 | 完成不了的事情 |
|------|----------------|
| P0 正确性 | WrapRow/Absolute 内元素点不了；ExpandableList 无法点击展开 |
| P1 布局 | 弹性/填充（占满剩余空间）、垂直居中/右对齐、比例分配宽度、多行自动换行文本元素 |
| P1 交互 | hover 反馈、tooltip、鼠标拖拽（slider/scroll）、表单值读回、**元素按下只有直接回调（无元素身份、无 NeoForge 事件供第三方订阅）** |
| P1 列表 | 长列表按需构建（500 条 = 500 个元素对象）、滚动位置重建后丢失 |
| P2 不做 | 键盘焦点遍历、富文本、窗口拖拽/缩放、虚拟化、点击外部关闭 |

## 阶段划分

### 阶段 H1（P0 · 正确性修复）
**目标**：修掉"控件点不了"的三处交互转发缺口。
**产物**：
- `WrapRowElement`：补 `mouseClicked` / `mouseScrolled` 转发
- `AbsoluteElement`：补 `mouseScrolled` 转发
- `ExpandableListElement`：内置可点击的展开/收起头部（`[+] / [-] label`），点击切换 `expanded`
**验证**：`gradlew build`；手动（对话框中 WrapRow 内按钮可点、ExpandableList 可展开）。

### 阶段 H2（P1 · 弹性布局与对齐）
**目标**：把 `ContentAlign` 真正接进布局容器，并支持弹性填充。
**产物**：
- `RowElement` / `ColumnElement`：新增 `ContentAlign` 对齐参数（默认 `LEFT_TOP` 保持兼容）
- 弹性子项：子元素带 `weight`（>0 时吸收剩余空间）——新增 `FlexElement` 或在 Row/Column 内支持 weight 子项
- `BoxElement`：支持 `ContentAlign` 内容对齐
- `Elements`：新增 `align(...)` / `flex(...)` / `weighted(...)` 便捷工厂
**验证**：JUnit（纯几何：weight 分配、对齐坐标）+ `gradlew build`。

### 阶段 H3（P1 · 多行文本）
**目标**：让文本元素支持自动换行。
**产物**：
- `TextElement` 新增 wrap 构造（`wrap(Component, color)`），内部用 `TextRenderer.layout(WRAP)` 自动换行 + 自动高度
- 单行构造保持现状（不破坏既有调用）
**验证**：JUnit（行数/高度随宽度变化）+ `gradlew build`。

### 阶段 H4（P1 · 交互反馈）
**目标**：hover 态 + tooltip，提升可发现性。
**产物**：
- `ButtonElement` / `LinkElement`：hover 高亮（边框/文字变色）
- `Interactive.tooltip()` 钩子；`PageScreen` / `HellowModal` / `HellowDialog` 渲染跟随鼠标的 tooltip（复用 vanilla `Screen.renderTooltip`）
**验证**：手动 + `gradlew build`。

### 阶段 H5（P1 · 元素事件回传：回调 API + NeoForge 事件）
**目标**：交互元素按下/值变化后，调用方既能拿到**直接回调 API**，也能通过 **NeoForge 事件总线**订阅（携带"哪个页面 / 哪个元素 / 什么值"的上下文），让 TISAM 之外的任何 mod 都能以 `@SubscribeEvent` 方式响应。
**产物**：
- `ElementId` 轻量标识（`pageId:elementId`），交互元素构造时可选传入
- 直接回调（保留现有 `Action` 构造不破坏）：新增带上下文回调 `ButtonElement(label, ElementId, Consumer<ElementEvent>)`
- **NeoForge 事件**（发布到 game bus，新增 `com.hellow.event` 包，均继承 `net.neoforged.bus.api.Event`）：
  - `HellowElementPressedEvent`（按下：携带 pageId / elementId / 元素类型）
  - `HellowValueChangedEvent`（Toggle/Slider/TextInput/Enum 值变化：携带 elementId + 新值）
  - `Hellow` 增加便捷构造：允许"只发事件、不传回调"
- `PageScreen` / `HellowDialog` 在鼠标事件命中后统一把元素事件投递到总线
**验证**：JUnit（事件负载字段、`ElementId` 解析，纯）+ `gradlew build` + 手动（用一个仅订阅事件的测试 mod 验证按下/值变化均能收到）。

### 阶段 H6（P1 · 表单状态）
**目标**：表单值可读回（受控组件）。
**产物**：
- 轻量 `State<T>` 值持有者（`get` / `set` / `observe`），纯逻辑、可测
- `ToggleElement` / `SliderElement` / `TextInputElement` / `EnumElement` 接受 `State<T>`（保留现有 Consumer 构造兼容）
- `Elements.state(...)` 便捷构造
**验证**：JUnit（State 读写与观察通知）+ `gradlew build`。

### 阶段 H7（P1 · 滚动与长列表）
**目标**：滚动可见、可拖拽；长列表按需构建。
**产物**：
- `ScrollElement`：可见滚动条 + `mouseDragged` 拖拽
- `ListView`：`ItemProvider` 按需构建子元素（解决"500 条 = 500 元素"）
**验证**：JUnit（ListView 索引/范围边界）+ 手动 + `gradlew build`。

### 阶段 H8（P2 · 明确不做，记录能力上限）
不实现（避免过度设计）；如用户后续需要再单开阶段：
- 键盘焦点 / Tab 遍历
- 富文本内联样式、文本选择复制
- 窗口拖拽 / 缩放
- 元素虚拟化（以 `ListView` 按需构建替代）
- 点击外部关闭

## 决策点

| # | 决策 | 建议 |
|---|------|------|
| D-H1 | `ElementRegistry` 标签系统（半成品，`register` 从未被调用） | 标记 `@Deprecated`/实验性注释，不补默认注册；除非后续要 DSL/JSON 描述 UI |
| D-H2 | 表单状态：`State<T>` 还是纯回调 | 两者并存：`State<T>` 做受控读写，Consumer 构造保留兼容 |
| D-H3 | `ListView` 是否进入 v3.0 | 先做 H7 简易版（IndexRange 缓存 + 视口裁剪），虚拟化本身不承诺 |
| D-H4 | 元素回传：直接回调 vs NeoForge 事件 | **两者都做**：直接回调保留（低开销、页内联动），NeoForge 事件（`HellowElementPressedEvent` / `HellowValueChangedEvent`）用于跨 mod 订阅。事件负载必须携带 pageId + elementId，避免"不知道是谁按的" |
| D-H5 | 事件类命名空间 | 统一放 `com.hellow.event`，与 TISAM 的 `com.tisam.client.event` 区分；事件类无业务字段，仅承载 Hellow 元素上下文 |

## 验证总览
- 每阶段 `gradlew build` 必须通过（含测试）
- H2 / H3 / H5 / H6 / H7 有纯 JUnit 可测部分
- 全部完成后 `gradlew runClient` 手动验证：TISAM 弹窗 + 一个含表单/滚动/多行文本的示例页；并用一个仅订阅 `@SubscribeEvent` 的测试 mod 验证"按下/值变化"事件确实投递到总线

## 执行状态（2026-08-28 已完成 H1–H7，H8 按计划不实现）

| 阶段 | 状态 | 备注 |
|------|------|------|
| H1 P0 正确性 | ✅ | WrapRow/Absolute/Grid 补鼠标事件转发；ExpandableList 可点击展开头部 |
| H2 弹性布局与对齐 | ✅ | Row 支持 flex 权重分配 + 行对齐；Column 支持子项水平对齐；新增 `FlexElement`/`Elements.flex`/`fillSpacer`；**决策：BoxElement 不加对齐**（内容自适应尺寸下无意义，见 D-H6） |
| H3 多行文本 | ✅ | `TextElement` 支持 WRAP（`wrapped(...)` 工厂 + `Elements.wrappedText`） |
| H4 交互反馈 | ✅ | Button/Link hover 高亮；`Interactive.tooltipAt` + 全容器转发；PageScreen/HellowDialog 渲染 tooltip |
| H5 元素事件回传 | ✅ | `ElementId` + `HellowElementPressedEvent`/`HellowValueChangedEvent`（game bus）+ `ElementEvents.post*`；Button/Toggle/Slider/TextInput/Enum 接入；`Elements.button(label, action, ElementId)` 支持"只发事件" |
| H6 表单状态 | ✅ | `State<T>` 值持有者；`Elements.state/toggle/slider/textInput/enumSelect/enumSelectIndex` 受控工厂（**决策：`enumSelect(State<String>)` 与 `enumSelect(State<Integer>)` 因类型擦除冲突，索引版改名 `enumSelectIndex`，见 D-H7**） |
| H7 滚动与列表 | ✅ | ScrollElement 可见滚动条 + 点击跳转 + mouseDragged 拖拽；新增 `ListView`（固定行高、按需构建、视口裁剪）+ `Elements.listView` |
| H8 P2 明确不做 | ⏭ | 键盘焦点/Tab、富文本、窗口拖拽缩放、虚拟化、点击外部关闭（记录为能力上限） |

### 执行中遇到的问题（记录）
| # | 问题 | 解决方案 |
|---|------|---------|
| 1 | `Screen.renderTooltip(GuiGraphics, Component, int, int)` 在 1.21.1 不存在 | 改用 `g.renderTooltip(font, List.of(tooltip), Optional.empty(), x, y)` |
| 2 | `enumSelect(String, List, State<String>)` 与 `State<Integer>` 重载冲突（类型擦除） | 索引版改名 `enumSelectIndex` |
| 3 | hellow 单元测试 classpath 不加载 MC 类 | 测试只覆盖纯逻辑（`State`/`ElementId`/`FlexElement` 构造）；需要 Font/Minecraft 的行为留待 `runClient` 手动验证 |
| 4 | 新发现的同类 P0 bug：`GridElement` 也无鼠标事件转发 | 一并修复（补 mouseClicked/mouseScrolled/mouseDragged/tooltipAt） |

### 新增决策（执行期补充）
| # | 决策 | 结论 |
|---|------|------|
| D-H6 | BoxElement 是否支持 ContentAlign | **不加**：Box 是内容自适应尺寸，垂直/水平都无剩余空间可对齐；对齐能力已由 Row/Column 提供 |
| D-H7 | State 版 enum 两个重载 | `enumSelect` 保留 `State<String>`（值）；索引版改名 `enumSelectIndex`（`State<Integer>`） |
| D-H8 | ListView 行构建策略 | 每帧按可见行重建（视口裁剪已限制数量）；行元素贵时再按索引缓存（已 ponytail 注释） |

### 修复补充（2026-08-28 会话 3）
审计发现 6 个容器「度量宽度 ≠ 渲染宽度」（Row/Column/Grid/WrapRow/Absolute/ExpandableList）——含多行文本/flex 子项时高度被低估导致父容器溢出。修复原则：**render() 是真相源，height() 必须用同一子元素宽度度量**。`layoutWidths` 委托新纯函数 `FlexMath.rowWidths`（+7 项 JUnit）。详见 findings R8「修复落地」。

---

# Part 3 — 全面修复 + 自定义空间补齐（2026-08-28 会话 5）

## 目标

把 findings R9 清点的所有遗留问题 + R8 声称已修但实际未修的缺口全部落地，同时补齐"调用方自定义空间"缺口。每阶段 `gradlew :hellow:build` 验证，允许的纯逻辑部分加 JUnit。

## 修复清单（按阶段）

### 阶段 F1（P0 · 交互转发缺口——R8 声称已修实际漏修）
**问题**：grep + 逐文件复核发现 3 处转发缺口：
- `FlexElement`（Row 内 flex 子项）只有 mouseClicked/keyPressed/charTyped，**缺 mouseScrolled/mouseDragged/tooltipAt** → Row 里 flex(slider)/flex(scroll) 滚轮、拖拽、tooltip 全部失效
- `SliderElement` **无 mouseDragged**（点一次能设值但不能拖）
- `ExpandableListElement` **无 mouseScrolled 转发**（行内嵌 scroll 时滚轮失效）
**改动**：补 3 个元素的转发方法，与既有容器转发模式一致（先递归调用子元素，命中返回 true）。
**验证**：`gradlew :hellow:build`。

### 阶段 F2（P1 · 滚动位置跨重建保持——R9 #3 / R7 #13 剩余）
**问题**：`ScrollElement` 与 `ListView` 的 scroll 偏移存在元素实例字段里，页面/对话框重建后位置丢失。
**改动（最小侵入）**：
- 给 `ScrollElement` 增加 `State<Integer>` 外部持有可选构造（`scroll(viewportH, gap, Element..., State<Integer> scrollState)`）：构造时把当前 scroll 写入 State，滚动时更新 State；渲染/命中用 State 值（若提供），否则回退内部字段。保持旧构造完全兼容。
- 同理 `ListView`（`listView(count, itemH, gap, viewportH, provider, State<Integer>)`）。
- `Elements` 增加对应可选重载。
**原理**：State 是持久对象，调用方把 State 存在页面级别，重建元素树时把同一 State 传回 → 滚动位置保持。（比"按索引缓存行元素"更通用，`ponytail: 行元素仍每帧重建，仅位置持久化`）
**验证**：`gradlew :hellow:build` + JUnit（State 读写回滚逻辑纯逻辑可测——用一个假 State 验证 ScrollElement 通过 State 读写 scroll；但 ScrollElement 依赖 Font/MC 类型，纯逻辑受限，见验证总览）。

### 阶段 F3（P1 · Grid 单元格拉伸 + 比例列——R9 #1 / R7 #7）
**问题**：`GridElement` 单元格固定自然宽度，无拉伸、无比例列（宽度表达力缺口）。
**改动**：
- 新增构造 `GridElement(children, columns, gap, int[] columnWeights)`：`columnWeights` 长度 == columns；权重均为 0 时行为与旧构造完全一致（自然宽度，兼容）。
- 有非零权重时：Grid 在**有限宽容器内**（render 时 ctx.width > 自然总宽）把剩余宽度按权重比例分给各列，单元格拉伸到列宽（percent 列效果）。
- 抽取/复用 `FlexMath.gridColumnWidths(naturals, available, gap, weights)` 纯函数，JUnit 覆盖（比例分配/余数兜底/无余量不缩/权重全 0 退化）。
- render/height/mouse 系列统一用列宽（延续 R8 "render 是真相源"不变量）。
**验证**：`gradlew build` + `FlexMathTest` 扩展。

### 阶段 F4（P1 · wrappedText 在 Column/Grid 内实际换行——R9 #2）
**问题**：R8 记录"wrappedText 在 Column/Grid 里实际换行需要'填充/限宽子元素'概念"——Column 按子元素自然宽度渲染，子元素拿不到容器宽度，`TextElement.wrap` 换行失效。
**方案（最小，不引入新抽象大改）**：利用阶段 F3 的机制 + 一个 `FillElement`（极薄包装）：
- 新增 `FillElement(Element child)`：语义"请求填满父容器宽度"。`width(font)` 返回父限定量（在有限宽容器内）；自然宽度下行为等同 child（兼容）。
- 在 `ColumnElement` / `GridElement` 内：若子元素是 `FillElement`，则渲染宽度 = 剩余容器宽度，其余元素保持自然宽度。
- `Elements.fill(child)` 便捷工厂 + `Elements.wrappedText` 已有 → 调用方写 `Elements.fill(Elements.wrappedText(...))` 即可在 Column/Grid 内换行。
**验证**：`gradlew build`；纯逻辑（FillElement 是薄包装）+ 手动 runClient 待办。

### 阶段 F5（P1 · 自定义空间：主题/配色参数化——R9 §2 缺口 1）
**问题**：hellow 元素配色/尺寸散落在各元素类的 `private static final`，无集中主题入口；`PageScreen`/`HellowDialog` 是 final 且 chrome 色硬编码。
**方案（API 向前兼容）**：
- 新增 `com.hellow.style.HellowTheme`（不可变值对象，集中所有可配色：背景/边框/标题/文本/按钮 hover/链接色等），默认实例即现有硬编码值（视觉零变化）。
- 元素类新增可选 `HellowTheme` 构造参数（默认 `HellowTheme.DEFAULT`），渲染时从 theme 读取颜色；`Elements` 工厂现有签名不动，新增带 theme 的可选重载。
- `HellowModal`/`HellowDialog`：把硬编码颜色提升为 protected/theme 可覆盖；`PageScreen` 保持 final 但支持传入 theme（构造参数）。这满足"调用方直接用 hellow 页面但可改配色"。
**注意**：此为最易膨胀的改动——只参数化**颜色**（缺口是配色），不相干尺寸/间距不动的字段不进 theme，避免过度设计。
**验证**：`gradlew build` + 现有测试不回退。

### 阶段 F6（P2 · 明确不做，复核确认）
- ElementRegistry：维持 D-H1（实验性标注，不补默认注册）——R9 缺口 6 已记录"无激活路径"，但补默认注册 = 新 DSL 层，YAGNI，不改。
- 键盘焦点/Tab、富文本、窗口缩放等 H8 上限保持不变。
- `runClient` 手动验证（弹性布局/多行文本/hover/tooltip/表单读回/滚动/@SubscribeEvent 事件投递）仍为环境外待办，记录不执行。

## 验证总览
- 每个阶段：`gradlew :hellow:build`（hellow + tisam 编译 + 全部测试绿）
- F3：`FlexMath.gridColumnWidths` JUnit（纯函数，无 MC 依赖）
- F2/F4/F5：涉及 Font/MC 渲染路径，纯逻辑有限，靠编译 + 结构调整保证（结构同 R8 方式：测量与渲染共用同一 helper）
- 最终：`gradlew build` 全绿

## 执行状态
| 阶段 | 状态 | 备注（2026-08-28 会话 5 审计复核） |
|------|------|------|
| F1 交互转发缺口 | ✅ 已落地（原表误标 ⏳） | FlexElement 已有 mouseScrolled/mouseDragged/tooltipAt；SliderElement 有 mouseDragged；ExpandableListElement 有 mouseScrolled 转发 |
| F2 滚动位置跨重建保持 | ✅ 已落地（原表误标 ⏳） | ScrollElement/ListView 均有可选 `State<Integer>` 构造，scroll()/setScroll() 读写 State；`Elements.scroll(State,…)`/`listView(State,…)` 重载存在 |
| F3 Grid 拉伸/比例列 | ⏳ 未实现 | GridElement 只有 `(children, columns, gap)` 构造；FlexMath 只有 rowWidths，无 gridColumnWidths |
| F4 wrappedText 在 Column/Grid 换行 | ⏳ 未实现 | 全代码库无 FillElement |
| F5 主题/配色参数化 | ⏳ 未实现 | 无 com.hellow.style 包 / HellowTheme；配色仍为各元素 private static final |
| F6 明确不做复核 | ✅ 已落地（原表误标 ⏳） | ElementRegistry 已加实验性 Javadoc（D-H1 落地） |

**审计结论（会话 5）**：F1/F2/F6 代码已落地但执行状态表未同步（误标 ⏳）；F3/F4/F5 确实未实现。`:hellow:build` 通过。另：`:tisam:compileJava` 当前被 [ModListScreen.java](file:///d:/.trae-project/there%20is%20still%20a%20mod/tisam/src/main/java/com/tisam/client/screen/ModListScreen.java) 的坏重构阻断（`int this.dialogLeft = ...` 等 25 个错误，见根目录 compile_errors.txt），plan 的「最终 gradlew build 全绿」门禁无法达成——属 tisam 侧，非 hellow 范围，单独记录。

---

# Part 4 — F3/F4/F5 实施 + tisam 编译修复（2026-08-28 会话 5，用户确认"完成这些所有"）

## 目标
把 R10 审计确认的 F3、F4、F5 三阶段全部落地，并修复审计中顺带发现的 tisam `ModListScreen` 编译错误。发现的新问题随时加入本部分。

## 阶段划分
### 阶段 G1（＝F3）：Grid 比例列
- `FlexMath.gridColumnWidths(int[] naturals, int available, int gap, int[] weights)` 纯函数
- `GridElement` 新增 `(children, columns, gap, int[] columnWeights)` 构造；有权重时在有限宽容器内按权重分配剩余宽度；weight 全 0 退化为旧行为
- `FlexMathTest` 扩展

### 阶段 G2（＝F4）：FillElement 限宽子元素
- `FillElement(Element child)`：请求填满父容器宽度；自然宽度下等同 child
- `ColumnElement` / `GridElement` 内：FillElement 渲染宽度 = 剩余容器宽度，其余保持自然宽度
- `Elements.fill(child)` 工厂 → wrappedText 在 Column/Grid 内可换行

### 阶段 G3（＝F5）：HellowTheme 主题/配色参数化
- `com.hellow.style.HellowTheme` 不可变值对象（默认实例 = 现有硬编码值，视觉零变化）
- 元素类可选 theme 构造参数（默认 DEFAULT）；`Elements` 新增可选 theme 重载
- `HellowModal`/`HellowDialog`/`PageScreen` 支持 theme

### 阶段 G4（tisam 编译修复）
- 修复 `ModListScreen.java` 中 `int this.dialogLeft = ...` 等 25 个编译错误（坏重构产物）

### 阶段 G5（最终验证）
- `gradlew build` 全绿（hellow + tisam + 全部测试）
- 更新 task_plan / findings / progress

## 验证
- 每阶段 `gradlew :hellow:build`（G4 用 `:tisam:compileJava`）
- 最终 `gradlew build` 全绿

## 执行状态
| 阶段 | 状态 | 备注 |
|------|------|------|
| G1 Grid 比例列 | ✅ | FlexMath.gridColumnWidths（委托 rowWidths，共享数学）+ GridElement 4 参构造（columnWeights）+ Elements.grid(weights)，权重全 0 退化旧行为；FlexMathTest +4 项 |
| G2 FillElement | ✅ | 新增 FillElement（薄包装 record）+ Column ensureMeasures 按渲染宽度量测 + Grid rowRenderWidths 分配行剩余宽度 + Elements.fill/scroll(theme) |
| G3 HellowTheme | ✅ | 新增 com.hellow.style.HellowTheme（26 色字段，DEFAULT=旧硬编码值）；Button/ExpandableList/Scroll/Toggle/Slider/TextInput/Enum + PageScreen/HellowModal 全部接入主题参数；Elements 新增 theme 重载；HellowThemeTest 2 项 |
| G4 tisam 编译修复 | ✅ 无需修改 | **复核发现 ModListScreen.java 已是干净版本**（`int this.dialogLeft = ...` 垃圾代码已不存在）；根目录 compile_errors.txt 是过期日志；`:tisam:compileJava` 全量构建通过 |
| G5 最终验证 | ✅ | `gradlew build` 全绿（hellow + tisam 编译 + 全部测试，含新增 FlexMathTest +4 / HellowThemeTest 2） |

### 执行中发现的新问题/更正（会话 5 第二段）
| # | 问题 | 处置 |
|---|------|------|
| 1 | R10 结论 #3「tisam 编译被 ModListScreen 坏重构阻断」基于过期 compile_errors.txt | 现已核实文件干净 + 构建通过，更正为"已修复/日志过期"；compile_errors.txt 保留为历史记录 |
| 2 | Column/Grid 的 FillElement 多重填充会均分剩余宽度 | 已在代码注释声明；单填充为常见场景，无现实缺口 |
| 3 | GridElement.cellWidthIn 每单元格重算行宽（O(cells²) 小行级重生算） | 列数通常 2-4，可接受；如需可改为每行缓存一次 |

---

# Part 5 — TISAM UI 迁移到最新 hellow API（2026-08-28 会话 5 第三段）

## 背景与目标

用户要求"补全 tisam 让他使用最新 hellow 的 API 并设计良好的界面（尽量与原来画风一致）"。

**现状**：4 个屏幕中仅 `ExternalLinkConfirmScreen` 已用 hellow（HellowModal 骨架 + 自绘 body + vanilla 底部按钮，R9 认可的样板）；其余 3 个（FirstPrompt / ModList / Disclaimer）各自手写同一套 chrome（dim + 对话框 + 标题 + 边框），颜色散落。

**目标**：3 个屏幕全部迁移到 `HellowModal`（chrome 收敛到一处，画风零变化——HellowTheme.DEFAULT 色值与现在完全一致）；ModList 卡片列表改用 hellow `ListView`（按需构建 + 滚轮）＋自定义 `ModuleCardElement`（实现 `Element` 接口，像素级复刻现卡片、免费获得 hover/tooltip）；Disclaimer 用自定义 `HellowTheme` 保持橙色警示风。

## 阶段划分

### 阶段 P1：FirstPromptScreen → HellowModal
- 继承 HellowModal（320×160，titleArea/buttonArea 默认）；标题与边框来自 HellowTheme.DEFAULT（与现画风一致）
- body：两行文案 + "▶ N mods found" 提示色（颜色收进 TisamConstants）+ 右上 "ESC = Later"
- 底部三按钮（View Mods / Later / Never Remind）改用 `addBottomButton`（居中自动布局）
- ESC → laterReminder（reminderCount++），override keyPressed 保持原行为
- 删除自绘 chrome 代码

### 阶段 P2：ModListScreen → HellowModal + ListView
- 继承 HellowModal（420×280）；tabs（4 类型+计数）与 guide 切换按钮继续用 vanilla Button（与风一致），底部三按钮改 `addBottomButton`
- 卡片列表：hellow `ListView`（固定行高 70、gap 6、视口裁剪、滚轮滚动，替代手写 scrollOffset 数学）
- 卡片 = 私有 `ModuleCardElement implements Element`：像素级复刻原 renderCard（按 priority 的 bg/border 色、名称/截断描述/类型/优先级标签/🔗），`mouseClicked` 命中 URL 区 → ExternalLinkConfirmScreen，`tooltipAt` 悬停 URL 显示链接（免费 hover/tooltip）
- renderBody 用 RenderContext 渲染 ListView + tooltip 渲染；mouseClicked/Scrolled/Dragged 转发到 ListView（复用 PageScreen 的 null-g 模式）
- guide 面板：font.split 换行绘制（同现实现），guide 按钮切换
- ESC → laterReminder；switchTab 只更新 activeList + 重建 ListView（无需 clearWidgets/init）

### 阶段 P3：DisclaimerScreen → HellowModal + 警示主题
- 继承 HellowModal（360×200）；自定义 `HellowTheme` 常量只改 dialogBorder/titleColor = 0xFFFF8866（其余沿用 DEFAULT），保持橙色警示风
- body：disclaimerText 用 font.split 换行绘制
- 底部：Back + Confirm（5 秒冷却 active 切换逻辑原样保留，vanilla Button）
- ESC → backTarget（override keyPressed）

### 阶段 P4：常量收敛与清理（小改）
- TisamConstants 新增 UI 色常量（card 色×3 优先级、文本灰、提示黄/绿、url 蓝系、icon 灰）
- TitleScreenButton 颜色引用常量；删除屏幕中散落魔法色
- 确认无死代码（FirstPrompt 旧 dialog 尺寸字段等移除）

### 阶段 P5：验证与文档
- `gradlew build` 全绿（含全部测试）
- 更新 task_plan / findings / progress
- `runClient` 手动视觉验证仍为环境外待办

## 执行状态
| 阶段 | 状态 | 备注 |
|------|------|------|
| P1 FirstPrompt → HellowModal | ✅ | 继承 HellowModal（320×160）；body 自绘 3 行文案 + ESC 提示 + 计数；底部 3 按钮改 addBottomButton；ESC=later；删除全部自绘 chrome |
| P2 ModList → HellowModal + ListView | ✅ | 继承 HellowModal（420×280）；tabs/guide 用 vanilla Button；卡片列表换 hellow ListView（按需构建/视口裁剪/滚轮）；卡片=私有 ModuleCardElement implements Element（复刻原画风 + URL hover tooltip + 点击→ExternalLinkConfirmScreen）；body 鼠标事件 PageScreen 式转发；guide 改为按活动标签类型显示对应安装指南（原为硬编码 MOD 指南）；ESC=later |
| P3 Disclaimer → HellowModal + 警示主题 | ✅ | WARN_THEME 只改 border/title=0xFFFF8866；body 换行绘制 disclaimerText；Back+Confirm（5s 冷却原逻辑保留）；ESC=back |
| P4 常量与清理 | ✅ | TisamConstants 新增 15 个 UI 色常量；删除 ModList/Disclaimer 死代码与无用 import；DisclaimerScreen confirmTarget 参数放宽为 Screen（HellowModal.parent 是 Screen） |
| P5 验证 | ✅ | `gradlew build` 14 tasks 全绿（hellow + tisam 编译 + 全部测试） |

### 执行中发现的新问题/改进（会话 5 第三段）
| # | 问题 | 处置 |
|---|------|------|
| 1 | DisclaimerScreen 构造参数 confirmTarget 为 TitleScreen，与 HellowModal.parent(Screen) 冲突 | 放宽为 Screen（仅用于 setScreen），编译修复，逐字记录 |
| 2 | ModList 安装指南原为硬编码 MOD 指南 | 改为按活动标签类型取 DEFAULT_INSTALL_GUIDE_BY_TYPE（全局 install_guide_text 覆盖优先）——符合 最新更新.md 设计 |
| 3 | FirstPrompt 改为固定 320×160（旧版按屏幕缩放） | HellowModal 固定尺寸；与 ExternalLinkConfirmScreen 固定 420×180 的既有先例一致，小屏时不再缩放（可接受，记录） |

## Part 5 补丁（2026-08-28 会话 5 第四段 · 窗口模式溢出/文本按钮重合排查修复）

用户报告：窗口模式下文本与按钮重叠、对话框溢出。排查结论 = 1 个 hellow 库级布局缺口 + 1 个库级窗口钳制缺失 + 1 个 TISAM 布局细点：

| # | 根因 | 层面 | 修复 |
|---|------|------|------|
| 1 | `HellowModal.render()` 对 renderBody **无 scissor** 裁剪 → body 内容（ListView 局部可见行、超长多行文本）溢进底部按钮条 → **文本与按钮重合** | hellow | render 中 `enableScissor(bodyLeft, bodyTop, +bodyWidth, +bodyHeight)` 包住 renderBody（bodyHeight≤0 时跳过） |
| 2 | `HellowModal.init()` **无窗口钳制** → 固定 420×280 等对话框在窗口模式下超出屏幕/按钮被推出屏幕 | hellow | init 顶部 clamp dialogWidth/Height 到 `width-20/height-20`；底部按钮行改按 avail 自适应缩宽，row 永远落在对话框内 |
| 3 | FirstPrompt「ESC = Later」提示画在 body 顶部右边，与 body 第一行 **同 y** → 长译文与提示撞行 | tisam | 提示并入标题 Component（灰色后缀，回到原标题栏位置），从 body 移除 |
| 4 | ListView 部分可见行的下半段越过视口下缘 | hellow | 由 #1 的 body scissor 统一兜底裁剪（记录，不单独给 ListView 加内部 scissor） |

附：tab 文字在窄窗超按钮宽溢出为纯外观小瑕疵（vanilla Button 不裁剪文本），未列为问题。

**验证**：`gradlew build` 全绿（14 tasks，hellow + tisam + 全部测试）。

## Part 5 补丁 2（2026-08-28 会话 5 第五段 · 最大化仍部分超出屏幕）

用户反馈最大化（大窗口）下依旧部分超出屏幕。复核几何发现两个更深层问题：

| # | 根因 | 层面 | 修复 |
|---|------|------|------|
| 1 | **ListView/ScrollElement 只按"行起点"裁剪**：起点在视口内、底部越出视口的行仍**完整绘制**（列表实际高度 > 视口）。最大化窗口下对话框内呈"列表溢出视口下缘"，会顶到下方内容 | hellow | `ListView.render` / `ScrollElement.render` 在渲染时对**自身视口**加 scissor（`ctx.x/y + width/viewportHeight`，GUI 坐标，与 HellowModal body scissor 嵌套求交）；超出视口的行下半段被正确裁掉 |
| 2 | HellowModal 逐轴 clamp 只防越界、不保**宽高比**，且未按迁移前响应式行为降尺寸 | hellow | 改为 `fitToScreen()`：能放下用设计尺寸，放不下按**同比例**（保持宽高比）缩到窗口内（margin 20），任何窗口尺寸整窗在屏内 |

**验证**：`gradlew build` 全绿（14 tasks）。

## Part 5 补丁 3（2026-08-28 会话 5 第六段 · 运行目录旧 jar 问题 + 两项界面调整）

用户确认排查方向：**runs/client/mods 里是旧 jar，一直加载旧版**（这解释了"intro 文字仍在"与部分溢出现象）。同时提了两项界面调整：

| # | 问题 | 处置 |
|---|------|------|
| 1 | `runs/client/mods/` 遗留旧 jar（hellow-2.0.0.jar / 2.0.0-fixed.jar / tisam-1.0.0.jar 均为旧构建） | 删除旧 jar，把最新构建（hellow-1.0.0.jar / tisam-1.0.0.jar）复制进 runs/client/mods；注明 gradlew runClient 直接用构建产物、无需 jar，真实启动器才需要手动同步 |
| 2 | 窗口模式下对话框上下被 OS 标题栏/任务栏遮挡 → "需要略微缩短上下距离" | HellowModal 拟合边距拆分为 X=20 / Y=44（垂直多留），缩小对话框高度、上下留白更大 |
| 3 | 旧版 ModList 的 "here are the … recommended" 介绍行与安装指南按钮重合 → 删除该文字 | 该行只存在于旧 jar（新代码已不渲染）；同时删除两条 lang 里不再使用的 `tisam.modlist.intro` 键 |

**验证**：`gradlew build` 全绿；runs/client/mods 现仅含 2 个最新 jar。

## Part 5 补丁 4（2026-08-28 会话 5 第七段 · hellow 加载崩溃修复）

用户报 hellow 加载失败。crash report（crash-2026-08-28_21.42.43-fml.txt）：

```
java.lang.IllegalArgumentException: Cannot register listeners for abstract class
net.neoforged.bus.api.Event. Register a listener to one of its subclasses instead!
```

| # | 根因 | 修复 |
|---|------|------|
| 1 | [HellowMod.java](file:///d:/.trae-project/there%20is%20still%20a%20mod/hellow/src/main/java/com/hellow/HellowMod.java) `ClientEvents.onGameBusEvent(net.neoforged.bus.api.Event)`：`@SubscribeEvent` 方法参数是**抽象基类 Event**，NeoForge EventBus 在 FML 自动注册该订阅类时拒绝（这正是崩溃信息） | 删除通吃监听。改为 [Hellow.java](file:///d:/.trae-project/there%20is%20still%20a%20mod/hellow/src/main/java/com/hellow/Hellow.java)：`whenEvent` 用 `NeoForge.EVENT_BUS.addListener(具体事件类型, 处理)` 按类型挂监听（`HOOKED_EVENT_TYPES` 去重，每类型一个）；`whenApi` 挂一条 `ClientTickEvent.Post` 驱动轮询；`dispatchGameEvent` 只做规则分发 |

**说明**：旧 runs/client/mods 里的 hellow-2.0.0.jar 是旧代码（无该监听）所以能加载；本次新构建（含监听）一加载就崩——这也是"一直看到旧版才算正常"的另一面。

**验证**：`gradlew build` 全绿（14 tests 绿）；最新 jar 已重新同步到 runs/client/mods。

## Part 5 补丁 5（2026-08-28 会话 5 第八段 · 标题界面 View Mods 按钮不显示）

用户报标题界面 View Mods 按钮不显示。日志显示：

```
[tisam] showing first prompt with 8 missing modules
[tisam] Failed to add button via reflection
java.lang.NoSuchMethodException: Screen.addRenderableWidget(net.minecraft.client.gui.components.AbstractWidget)
```

| # | 根因 | 修复 |
|---|------|------|
| 1 | [TisamClientEvents.java](file:///d:/.trae-project/there%20is%20still%20a%20mod/tisam/src/main/java/com/tisam/client/TisamClientEvents.java) 用 `Screen.class.getDeclaredMethod("addRenderableWidget", AbstractWidget.class)` 反射调 protected 方法。但 vanilla `Screen.addRenderableWidget` 是**泛型方法**（`<T extends GuiEventListener & Widget & NarratableEntry> T addRenderableWidget(T)`），擦除后参数 = **GuiEventListener**（第一个边界），`getDeclaredMethod` 精确匹配 AbstractWidget 永远找不到 → NoSuchMethodException → 按钮从未加入 → 标题界面按钮不显示（每次标题界面 init 都打一条 ERROR） | 改为扫描 `Screen.getDeclaredMethods()`，找名字="addRenderableWidget"、单参、且参数类型 `isAssignableFrom(AbstractWidget)` 的重载再 invoke；找不到再抛错 |

顺带确认：按钮/弹窗不显示的另两类情况是**设计行为**（modules.json 空 / 全部已安装 → missing 为空 → 不显示），与本次 bug 无关。本次运行目录有 8 个 missing 条目，按钮本应显示，确为反射 bug。

**验证**：`gradlew build` 全绿（14 tests）；最新 tisam jar 已同步 runs/client/mods。

---

# Part 6 — 按钮根治 + 配置全有用 + hellow 全面审计修复（2026-08-28 会话 5 第九段，用户要求最大思考力度 + 子 agent）

## A. 按钮仍不显示的根治（A1/A2）

上一轮只修了反射，但按钮仍不显示。根因是两个**时序/生命周期 bug**（日志证据：22:40 无反射报错但仍无按钮）：
1. 提示从 `ScreenEvent.Opening` 抢占弹出 → 触发时 TitleScreen 尚未 init → 按钮无屏可挂；且嵌套 setScreen 与外部 setScreen 争用最终屏幕
2. `Later → setScreen(title)` 会重跑 `TitleScreen.init()` → `clearWidgets()` 清掉反射加的按钮，而 `TITLES_WITH_BUTTON.contains` 又阻止二次添加 → 按钮第二次起永久消失

**修复（TisamClientEvents 重构）**：删除 Hellow.when(Opening) 注册；改为
- `ScreenEvent.Init.Post`：每次 TitleScreen init 都 `addButton`（重跑也重加）+ 判定弹窗条件，命中则把 title 记入 `pendingPromptTitle`；
- `ClientTickEvent.Post`：下一 tick 且 `screen==pendingTitle` 时才 `setScreen(FirstPromptScreen)`（标题已初始化、按钮已挂载，嵌套争用消失）；
- Tisam.java 移除 registerWindowRules 调用。

## B. 配置项全有用（A3，子 agent 审计结果 → 修复）

依 findings R17 的字段消费清单：
| 字段 | 处置 |
|------|------|
| 顶层 `version` | 无 Java 字段 → ModulesConfig 增加 `@SerializedName("version") int version=1`（round-trip 不丢键） |
| `modules[].license` | 新卡片未渲染 → ModuleCardElement 左下角显示 license |
| `modules[].can_ignore` | 全无消费 → 卡片 name 右侧显示 🔒（can_ignore=false 时） |
| `modules[].install_guide_text`（条目级） | guideFor 不读 → 实现"per-entry 覆盖 > 全局 > 类型默认"（当前标签首个带指南条目优先） |
| `preferences.last_reminder_date` | 只写不读 → 属 informational（javadoc 已注明），保留并记录 |

## C. GUI scale 缩放一致性（A4，子 agent 结论）

hellow 全部运行在 vanilla 缩放后 GUI 坐标上（Screen.width/height、fill、enableScissor(x1,y1,x2,y2) 绝对坐标、font.width/split 缩放字体），无物理像素假设 → 坐标系一致 ✅。唯一真实问题：`fitToScreen` 原地改小 dialog 尺寸，窗口先小后大**累积缩小** → 改为保存 designDialogWidth/Height，每次按设计值重算（scale/窗口变化经 init 自动重算）。

## D. hellow 全面审计修复（A5a/A5b，子 agent 报告 → 落地/记录）

**已落地**：
- WrapRowElement.height() 用 cw（原来误用容器 width）→ 测量==渲染不变量（P0）
- PageScreen 生命周期幂等：`removed()` 也清理（程序性换屏不泄漏窗口栈/onUnmount），onClose 只负责回 parent（P1）
- PageScheduler 栈操作改**引用身份**（record 值相等误删/误报，P1）
- Hellow.clearRules 清 HOOKED_EVENT_TYPES；DISPATCHING 加 volatile（P2）
- ColumnElement fill 改为 `max(自然宽, spare/fillCount)`（原"自然宽+spare"会超宽）（P2）
- TextRenderer.layout 钳制 `maxWidth>=1`（P2）
- PageStack 标注 ponytail 死类（P2/P3）

**记录不修**（findings R17 附录）：嵌套 scissor 无栈、ScrollElement O(n) 遍历、控件右缘锚定与 width() 不一致、CLAMPED 截断丢格式/代理对、空文本高度口径、Box border=0 占位、LinkElement 不发事件、Slider 高频重复 post、whenApi everyTime 反复弹、I18n 无调用者、HellowDialog 无生命周期（均在 R17 附录标注升级路径）。

## 验证
- `gradlew build` 全绿（14 tasks 全测试绿）
- 最新 hellow/tisam jar 已同步 runs/client/mods

## Part 6 补丁 1（2026-08-29 会话 5 第十段 · ModList 界面 6 项反馈修复）

用户反馈 ModList 界面：不可滑动 / 太小两侧留白大 / 大缩放重复按钮 / 未按优先级排序 / can_ignore=false 应禁 later/never / 所有界面 \n 不换行。

| # | 根因 | 修复 |
|---|------|------|
| 1 不可滑动 | 列表视口过小（dialog 280 高+52 头部→仅 1-2 卡）且无任何滚动反馈；部分场景安装指南面板开着遮挡 | dialog 加大至 500×320；`fitToScreen` 支持**放大**（≤1.6×，大窗口不再过小/两侧大留白）；ListView 增加**可见滚动条+轨道点击跳转+拖拽**（与 ScrollElement 同风格） |
| 2 太小/两侧留白 | 对话框固定 420×280 只缩不放；大 GUI 时四周大片暗区 | 见 #1（500×320 + 放大到 1.6×） |
| 3 大缩放重复按钮 | `Screen.init()` 在 resize/缩放变化时重跑，而 vanilla init **不清除 widgets** → HellowModal 底部按钮/tabs 重复 add | `HellowModal.init()` 开头 `clearWidgets()`（子类在 super.init 之后重建，无副作用） |
| 4 未按优先级排序 | `BY_PRIORITY` 用了枚举 ordinal（FEATURED=0 < CRITICAL=1），与设计文档"critical→featured→normal→optional"相反 | `ModuleEntry.BY_PRIORITY` 改为显式顺序（critical 0/featured 1/normal 2/optional 3） |
| 5(=6) can_ignore=false | 未参与行为 | FirstPrompt/ModList 存在 `can_ignore=false` 缺失条目时：Later/Never Remind 按钮灰显（active=false）、ESC 的 later 捷径同样锁定 |
| 6 \n 不换行 | 配置 JSON 被写成 `\\n`（转义的反斜杠+n），Gson 解析为字面 `\n` 而非换行 | 显示层防御：guideFor/Disclaimer 渲染前 `replace("\\n","\n")`；修正运行目录 modules.json 为真正的 `\n` |

**验证**：`gradlew build` 全绿（14 tests）；最新 jar 已同步 runs/client/mods（8/29）。

## Part 6 补丁 2（2026-08-29 会话 5 第十一段 · 滚动体验/指南换行/链接/锁定出口/加宽）

用户反馈第二轮（ModList）：滚动不流畅无惯性、指南不自动换行且超出不滚、界面仍窄、链接弹窗 Cancel 返回标题而非列表、锁定态无出口、链接只显示小图标。

| # | 根因 | 修复 |
|---|------|------|
| 1-滑动流畅 | 滚轮每次仅 8px 跳变、无惯性 | ListView/ScrollElement 滚轮步进 8→24；新增 **fling 惯性**（`advanceFling()`，速度 0.82 衰减/tick，松手滑行）；ModListScreen `tick()` 驱动 |
| 2-指南换行+滚 | renderGuide 按 `\n` 硬分行、长行溢出、无滚动 | 指南改为 WRAP 段落 `TextElement`（自动换行）包进 `ScrollElement`：超出视口**自动出现滚动条**，滚轮/拖拽/惯性全可用（切标签重建） |
| 3-界面加宽 | ModList 500×320 / FirstPrompt 360×200 仍窄 | ModList 560×340、FirstPrompt 380×210（配合 fitToScreen 放大 ≤1.6×） |
| 4-链接返回错 | ExternalLinkConfirmScreen 的 parent 传的是 TitleScreen → Cancel/Open 回标题 | parent 改为 `this`（ModListScreen）→ 返回列表且**保留标签与滚动** |
| 5-锁定无出口 | can_ignore=false 时 ESC 无响应，玩家被困 | locked 时 ESC = 直接关闭弹窗回标题（不计数、下次再提示），同时首启弹窗新增 "⚠ 必须安装" 提示行（新键 `tisam.first_prompt.must_install`） |
| 6-链接可见 | 卡片只画 🔗 小图标 | 底部显示**完整 URL 文本**（TextRenderer CLAMPED 超长截断"…"），hover 高亮 + tooltip 显示完整链接，点击打开 |

**验证**：`gradlew build` 全绿（14 tests）；最新 jar 已同步（8/29）。

## Part 6 补丁 3（2026-08-29 会话 5 第十二段 · locked 升级为最高优先级 + 按钮累积根治）

用户反馈：① locked（can_ignore=false）应作为最高级、紫色；② 每次进入 View Mods 界面底部按钮越来越多。

| # | 根因 | 修复 |
|---|------|------|
| 1 locked=最高级紫色 | locked 只是小锁图标，priority 仍按原样 | `ModuleEntry.BY_PRIORITY`：`!canIgnore` 排最前（rank 0，其余顺延 critical→featured→normal→optional）；卡片 locked 紫卡（`CARD_BG_LOCKED 0xFF241A3A` / `CARD_BORDER_LOCKED 0xFFB08CFF`），右上标签显示 **REQUIRED**（紫色），图标也改紫 |
| 2 底部按钮累积 | 子类 init 每次执行 `addBottomButton`，HellowModal 的 `bottomButtons` 列表**从不清空**；resize/GUI 缩放/从链接返回触发多次 init → 旧按钮被反复重新注册 | 新增 `HellowModal.resetBottomButtons()`（清空列表），4 个子类（ModList/FirstPrompt/Disclaimer/ExternalLink）init 首行调用（注意时序：不能在 add 之后清） |

**验证**：`gradlew build` 全绿（14 tests）；最新 jar 已同步（8/29）。

---

# Part 7 — 排版能力全部归 hellow，TISAM 只声明内容（2026-08-29 会话 6）

用户要求：截断/自动换行/自动滚动/缩放/按钮排列等必须是 hellow 的能力，hellow 不能只给一个界面。现状核对：截断（TextRenderer CLAMPED）、换行（TextElement WRAP）、滚动（ListView/ScrollElement）、缩放（fitToScreen）、底部按钮排列（HellowModal）**已归 hellow**；但 3 个屏的 body 文案、ModList 卡片内容仍是 TISAM 手写固定坐标 `g.drawString`。目标：把这两块也迁移为"hellow 声明式组件"，TISAM 只给内容与颜色。

| 阶段 | 内容 |
|------|------|
| P7-A | hellow：`TextElement` 支持 `StringMode`（SINGLE/WRAP/CLAMPED/COLLAPSIBLE），新增 `Elements.clampedText` —— **截断成为元素能力** |
| P7-B | FirstPrompt body 改用 hellow 元素（column+text，锁提示条件行）+ mouse/tooltip 转发 |
| P7-C | Disclaimer body 改用 hellow（`fill(wrappedText)` 自动换行） |
| P7-D | ExternalLink body 改用 hellow（文本 + clamped URL + 警告） |
| P7-E | ModList 卡片内容 hellow 化：`Column`+`Row(flex/align)`+`LinkElement`（URL 行：CLAMPED 显示、hover、tooltip、点击弹窗）做内容，卡片保留最外层轻量壳（底色/描边/定高） |
| P7-F | `gradlew build` 全绿 + 文档同步 |

验证：每阶段 `gradlew :hellow:build` / `:tisam:build`，终局 `gradlew build` 全绿。

## Part 7 执行状态
| 阶段 | 状态 | 备注 |
|------|------|------|
| P7-A TextElement StringMode | ✅ | TextElement 支持 SINGLE(legacy)/WRAP/CLAMPED/COLLAPSIBLE（null=legacy 不截断兼容）；`TextElement.clamped`、`Elements.clampedText`、`Elements.wrappedText(Component)`；LinkElement 加可选 CLAMPED 渲染 |
| P7-B FirstPrompt body | ✅ | body 改为 hellow Column（组件树声明文案与颜色），mouse/tooltip 转发 |
| P7-C Disclaimer body | ✅ | body 改为 `fill(wrappedText)` 自动换行（不再手写 font.split 循环） |
| P7-D ExternalLink body | ✅ | body 改为 Column；URL 行用 `TextElement.clamped`（截断归 hellow） |
| P7-E ModList 卡片 | ✅ | 卡片 = 装饰外壳(底色/描边/定高) + hellow 内容树（Row+fillSpacer+clamped desc+LinkElement(CLAMPED,onOpen,tooltip)）；URL 整行可点/hover/tooltip |
| P7-F 验证 | ✅ | `gradlew build` 全绿（14 tests）；最新 jar 已同步（8/29） |

**结果**：TISAM 的 UI 现在 = hellow chrome/列表/滚动/换行/截断/hover/tooltip + 卡片外壳与业务内容声明；仅剩 3 处 vanill-like 装饰性直绘（Modal 底部 vanilla 按钮本体、卡片外壳填充描边、tab/guide vanilla 按钮）——均为"控件本体"而非"排版能力"。

## Part 7 补丁（2026-08-29 会话 6 · 完成 Disclaimer 底部按钮的 hellow 迁移）

上次会话遗留一个半迁移状态：`DisclaimerScreen` 的 `confirmButton` 字段类型已改为 `com.hellow.element.ButtonElement`，但 `init()`/`renderBody()` 仍用 vanilla `Button.builder`/`active`/`setMessage` → `:tisam:compileJava` 10 个编译错误，全量 build 红。

| # | 改动 | 说明 |
|---|------|------|
| 1 | Back/Confirm 按钮改用 `addButton(Elements.button(...))` / `new ButtonElement(...)` | 底部按钮全部 hellow 化（布局/渲染/点击转发由 HellowModal 管） |
| 2 | 冷却倒计时改用 `ButtonElement.setEnabled(false)` + `setLabel(剩余秒数)`，解锁用 `setEnabled(true)` + 最终文案 | 与 vanilla `active`/`setMessage` 等价；`ButtonElement.mouseClicked` 内置 enabled 门控，可去掉原来的 `if (!active) return` 冗余判断 |
| 3 | Confirm 构造传 `WARN_THEME`（按钮色=DEFAULT，视觉零变化）+ 初始 `enabled=false` | 冷却期间灰显（DISABLED_BG 等固定灰），与之前行为一致 |

**验证**：`gradlew build` 全绿（14 tasks）；最新 jar 已同步 runs/client/mods（8/29）。

**迁移后 vanilla 残留盘点**：仅剩 `TisamClientEvents.TitleScreenButton`（vanilla `AbstractWidget`，挂在 vanilla TitleScreen 上，hellow 不拥有 vanilla 标题屏 chrome——属"控件本体"，刻意保留）。
