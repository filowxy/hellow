# TISAM — 研究发现

## R1：NeoForge 1.21.1 ModDevGradle MDK 结构

**来源**：GitHub 搜索结果

**关键发现**：
- 官方模板仓库：`https://github.com/NeoForgeMDKs/MDK-1.21.1-NeoGradle`（Gradle Groovy DSL）
- 备选：`https://github.com/mochi-753/MDK-1.21.1-ModDevGradle`（Gradle Kotlin DSL，功能更全，含 Parchment mappings、Modrinth/CurseForge 自动发布）
- 推荐使用 ModDevGradle（社区维护更活跃，文档更全）

**最小文件清单**：
- `build.gradle` 或 `build.gradle.kts`
- `settings.gradle` 或 `settings.gradle.kts`
- `gradle.properties`（mod_id, mod_name, mod_version, mod_group_id, mod_authors, mod_description, mod_license, minecraft_version, neo_version, loader_version_range）
- `gradle/wrapper/gradle-wrapper.properties` + `gradle-wrapper.jar`
- `gradlew` + `gradlew.bat`（Unix/Windows 启动脚本）
- `.gitignore`
- `src/main/java/...`（Java 源码）
- `src/main/resources/META-INF/neoforge.mods.toml`（模组元数据）
- `src/main/resources/pack.mcmeta`（资源包标识）
- `LICENSE.txt`（可选但推荐）

## R2：NeoForge 浏览器 URL API

**API 路径**：`net.neoforged.neoforge.client.ClientHooks#openUrl(String url)`

**行为**：
- 自动处理隐私提示（首次打开外部 URL 时弹出"Open URL in browser?"确认）
- 跨平台：Windows / macOS / Linux
- 替代品：`Minecraft.getInstance().setScreen(new ConfirmLinkScreen(...))`（更底层，需要手动实现隐私提示）

**推荐**：直接用 `ClientHooks.openUrl()`，设计文档明确"NeoForge 自动处理隐私提示和跳转到外部浏览器"。

## R3：Mod 文件夹路径

**客户端路径**（设计目标）：`<minecraft_dir>/mods/`
- Windows：`%appdata%/.minecraft/mods/`
- macOS：`~/Library/Application Support/minecraft/mods/`
- Linux：`~/.minecraft/mods/`

**获取方式**：
```java
Minecraft.getInstance().gameDirectory.toPath().resolve("mods")
```

**注意**：本模组仅用于**客户端**（不装到服务器），因为是给玩家在标题界面弹提示用的。

## R4：Gson 反序列化降级策略

**场景**：`modules.json` 字段缺失时不能让游戏崩溃。

**方案**：
- `ModuleEntry` 用 `@SerializedName` + 默认值（如 `canIgnore = true`）
- `reminder_frequency` 缺失时回退到默认 `[1, 1, 3, 3, 3, 7, 7, 7, 7, 15]`
- `install_guide_text` 缺失时回退到设计文档默认的 4 行说明
- 顶层 `modules` 缺失时 → 空列表，模组静默跳过（视为没有推荐）

## R5：用户偏好与约束

**来源**：`user_profile.md` + `project_memory.md`（vanilla-expense）

**关键约束**：
- 注释用英文（user rule: 注释要只有英文的版本）
- 使用常量代替魔法数字（user rule: Use constants instead of magic numbers, maintain bilingual comments）
- 验证用 `gradlew build`（user rule: Verify compilation with `gradlew build` after each task）
- 不引入新依赖（user rule: No new dependency if it can be avoided）→ 复用 MDK 自带的 Gson
- 询问用户必须用 `ask user question`（user rule: All questions to the user MUST go through the `ask user question` tool）

**Git 安全**：
- 禁止 `git checkout -f` / `git reset --hard` / `git clean -fd` / `git rm -f`
- 无 commit 的分支切换前必须先备份
- COMMIT EARLY, COMMIT OFTEN

## R6：用户新增需求（2026-07-26）

**永久忽略前必须弹免责声明**：
- 触发：用户在 FirstPromptScreen 点击 "Permanently Skip"
- 行为：弹出 DisclaimerScreen，显示 `modules.json` 的 `disclaimer_text` 字段
- 按钮：
  - 返回：回到 FirstPromptScreen
  - 确认（永久忽略）：**5 秒倒计时后才可点击**，倒计时期间按钮灰显且显示剩余秒数
- 倒计时实现：使用 `Minecraft.getInstance().level != null` 不合适（标题界面无 level），改用 `System.currentTimeMillis()` + 在 `render()` 中每帧检查 + 5 秒后更新按钮 active 状态

**配置 schema 更新**：`modules.json` 新增 `disclaimer_text` 字段（字符串，默认值见 task_plan.md Q4）

## R7：Hellow 能力上限审计（2026-08-28）

对当前 hellow 元素系统与自动布局能力的完整审计（grep 验证 + 逐文件阅读）。

**结论：布局"骨架"齐了，但"能力"停在能画出来；交互完整度约 60~70%。**
API 对"声明式页面 + 事件触发"已够用（TISAM 用法成立），对"复杂表单/可配置 UI"不够强。

### 完成不了的事情（按严重度）

**P0 · 正确性（现有功能被 bug 卡住）：**
1. `WrapRowElement` 只有 render，无 `mouseClicked`/`mouseScrolled` 转发 → 内部按钮/链接点不了
2. `AbsoluteElement` 只转发 `mouseClicked`，无 `mouseScrolled` → 内部 Scroll 失效
3. `ExpandableListElement` 有 `setExpanded` 但无点击交互 → 设计文档的 COLLAPSIBLE "展开按钮"未落地

**P1 · 自动布局（做不到的布局）：**
4. 无弹性/填充（flex/weight）→ 无法表达"占满剩余空间/拉伸填满容器宽"，跨 GUI 缩放的响应式布局做不了
5. `ContentAlign` 仅在测试中引用（grep 证实 0 处业务使用）→ Box/Row/Column 无垂直居中、无右对齐
6. 无多行自动换行文本元素 → `TextElement` 单行溢出；`TextRenderer.WRAP` 没有被任何元素包装
7. `GridElement` 单元格不拉伸、列宽不均衡分配；无百分比/比例列

**P1 · 交互：**
8. 无 hover 态、无 tooltip（可发现性差）
9. 无 `mouseDragged` → Slider 不能拖、Scroll 不能拖
10. 表单值无法从元素树读回（Toggle/Slider/TextInput/Enum 只经 onChange 回调出去）→ 设置页/表单做不了
11. **元素按下只有直接回调，无元素身份（pageId/elementId）、无 NeoForge 事件** → 第三方 mod 无法以 `@SubscribeEvent` 方式响应 hellow 里的按钮/控件（用户 2026-08-28 明确要求补齐：回调 API + NeoForge 事件）

**P1 · 列表/状态：**
12. 长列表必须一次性构造全部子元素（无按需构建/复用；500 条模组 = 500 个元素对象）
13. 滚动位置在元素重建后丢失（scroll 状态在元素实例里）

**P2 · 明确不做（能力上限，避免过度设计）：**
14. 键盘焦点/Tab 遍历、富文本内联样式、窗口拖拽/缩放、元素虚拟化（以按需构建替代）、点击外部关闭

### 已确认的强项
- `TextRenderer` 三种模式（WRAP / CLAMPED / COLLAPSIBLE）实现完整、无副作用
- 容器种类齐：Column / Row / Grid / WrapRow / Box / Absolute / Scroll
- 公共门面已可用：`Hellow`（when / whenEvent / whenApi + show / closeTop / closeAll / showDialog）+ `HellowModal` / `HellowDialog` / `Elements` / `PageBuilder`
- `ElementRegistry` 标签系统为半成品（`register` 从未被调用、无默认注册、无 args 约定）→ 死代码/误导，建议标记实验性而非补全

### 解决状态（2026-08-28 H1–H7 落地后）
- ✅ 1-3（P0 转发缺口）、4-6（弹性/对齐/多行文本）、8-11（hover/tooltip/拖拽/表单/事件回传）、12（ListView 按需构建）、13（滚动条）
- ⚠️ #7（Grid 单元格不拉伸/比例列）**未实现** —— 原标记过声明，见 R8 §2 与修复说明
- ⏭ 仍属能力上限（H8）：键盘焦点/Tab 遍历、富文本内联样式、窗口拖拽/缩放、元素虚拟化（以 ListView 按需构建替代）、点击外部关闭
- ⏭ 需 `runClient` 手动验证的视觉/事件行为未在本轮完成（见 progress.md 待办）

## R8：hellow 自动布局系统检查（2026-08-28 会话 3）

逐文件复读全部布局容器 + `gradlew :hellow:build`（✅ 通过，测试绿）。以下为与计划/先前审计**不一致**或**未覆盖**的点：

### 1. 高度测量 vs 渲染宽度不一致（新发现，P1 级潜在 bug）
- `RowElement.height(font, width)`（RowElement.java L39-43）把**整个可用宽度**传给子元素测高；但 `render()`（L52）传的是**per-child 布局宽度 `widths[i]`**。
- 后果：子元素高度依赖宽度时（如 `TextElement.wrapped` 多行文本、flex 子项），`height()` 比 `render()` 测得的高度小 → 外层容器（如包着这个 Row 的 Column）预留垂直空间不足，内容下溢重叠。
- 同类隐患：`GridElement`（render L50 等 6 处）与 `WrapRowElement`（render L46 等）的 rowH 都用了**容器全宽 `ctx.width()`**，而子元素实际以**单元格/自然宽度**渲染——含多行文本时行高同样被低估。
- **无任何测试覆盖此路径**（H2/H3 测试只测纯几何/State/ElementId）。

### 2. findings R7「解决状态」标记过度声明
- R7 #7「Grid 单元格不拉伸、列宽不均衡、无比例列」被标为 ✅，但 `GridElement` 代码实际**仍不拉伸、无比例列**。
- 实际落地的是 Grid 的鼠标事件转发（任务执行期发现的新 P0 bug），不是单元格拉伸。H2 计划也确实只做了 Row flex + Column 对齐，Grid 拉伸从未列入范围 → 建议修正 R7 状态或补做。

### 3. D-H1 未落地
- 计划决策 D-H1：`ElementRegistry`（半成品，`register` 从未被调用）应标记 `@Deprecated`/实验性注释。实际代码**无任何标注**。

### 4. R7 #13 部分未解决
- 滚动条已加（可拖拽、可点击跳转），但**滚动位置仍在元素实例字段里**，页面重建后依然丢失——原 R7 #13 的「滚动位置重建后丢失」实际只做了一半（见 D-H8 的 ponytail 注释：行元素按索引缓存是升级路径）。属可接受上限，记录不修复。

### 确认无误的部分
- 6 个布局容器（Row/Column/Grid/WrapRow/Absolute/Box/Scroll/ListView/ExpandableList）鼠标事件转发齐全（H1 + 新发现的 Grid）
- `RowElement.layoutWidths`：flex 权重按比例分配剩余宽度 + 取整余数兜底到最后一个 flex 子项（L165-172），正确
- `ColumnElement` 子项水平对齐、`Elements.flex/fillSpacer/row(align)/column(align)/wrappedText/listView` 工厂齐全
- `TextElement.wrapped`：`TextRenderer.layout(WRAP)` 自动换行 + 高度随行数增长，正确
- `ScrollElement` 滚动条渲染/拖拽/点击跳转、`ListView` 按需构建 + 视口裁剪，正确

### 修复落地（2026-08-28 会话 3 · 用户要求修复布局问题）

**核心不变量**：`render()` 是真相源 —— 任何容器的 `height()` 度量必须用与 `render()` 相同的子元素宽度。以此修复全部 6 个不一致容器：
- **RowElement**：`height()` 改用 `layoutWidths()`（per-child 布局宽度，与 render 一致）；`layoutWidths` 改为委托新的纯函数 `FlexMath.rowWidths`
- **ColumnElement**：`ensureHeights` 按子元素自然宽度度量（render 以自然宽绘制并按 align 定位）
- **GridElement**：新增 `rowHeight(font, row)` 辅助，按单元格自然宽度度量；height/render/4 个鼠标方法统一使用
- **WrapRowElement**：5 处 `rowH` 改用子元素自然宽度 `cw`
- **AbsoluteElement**：`height()` 按条目自然宽度度量
- **ExpandableListElement**：5 处度量改用 `itemWidth = max(0, width - ITEM_INDENT)`（与渲染宽度一致）
- 确认无问题的：BoxElement（height/render 用同一 inner 宽度）、ScrollElement（子元素统一按 viewport 宽度量+渲染）、ListView（固定行高模型）、FlexElement（纯委托）

**可测性**：抽出 `com.hellow.layout.FlexMath.rowWidths(int[], int, int, int[])`（纯函数、无 MC 依赖），新增 `FlexMathTest`（7 项，覆盖比例分配/取整余数/无余量不缩小）。度量一致性本身因 MC 类型（Font）无法进测试 classpath，靠「height 与 render 共用同一 layoutWidths 辅助」在结构上保证 + `gradlew build` 验证。

**明确不做的（记录，非 bug）**：
- Grid 单元格拉伸/比例列（新功能，从未列入 H2 范围）——需要时单开阶段
- wrappedText 在 Column/Grid 里实际换行（当前自然宽度模型下只在显式限宽容器内换行；需要"填充/限宽子元素"概念，属新功能）
- 滚动位置重建后丢失（已记录上限，D-H8 ponytail 注明了升级路径）
- **D-H1 已落地**：ElementRegistry 加实验性 Javadoc 标注（不补默认注册）

## R9：未修复问题清点 + 调用方自定义空间评估（2026-08-28 会话 4）

### 1. findings 已知但尚未修复/未落地的问题（按现状复核）

| # | 条目 | 现状 | 处置 |
|---|------|------|------|
| 1 | R8 §2：Grid 单元格不拉伸、列宽不均衡、无比例列（R7 #7 曾过度标记 ✅） | 未实现（`GridElement` L52/63 仍按自然宽度渲染） | 一直未列入范围；需要时单开阶段 |
| 2 | R8 §3 相关：wrappedText 在 Column/Grid 内实际换行 | 未做（缺"限宽子元素"概念）；只在显式限宽容器（如 dialog/Row flex 子项）内换行 | 记录为新功能，未排期 |
| 3 | R8 §4 / R7 #13：滚动位置重建后丢失 | 只做了一半：滚动条已加，但 `scrollOffset` 仍在元素实例字段里，页面重建后丢失 | 属可接受上限；D-H8 ponytail 已注明升级路径（行元素按索引缓存） |
| 4 | progress.md 待办：`runClient` 手动验证 | 未执行 | 弹性布局视觉、tooltip、表单 State 读回、滚动条/拖拽、`@SubscribeEvent` 事件投递（需测试 mod）均未在游戏内验证 |
| 5 | H8 P2 能力上限 | 明确不做 | 键盘焦点/Tab、富文本内联样式、窗口拖拽/缩放、元素虚拟化、点击外部关闭 |
| 6 | `ElementRegistry` 半成品 | 已加实验性 Javadoc（D-H1），但 `register` 无任何调用者，无激活路径 | 不补默认注册；第三方无法通过标签注册自定义元素（只能直接实现 `Element`） |

### 2. "调用方自定义空间"评估（hellow 是否取代调用方设计）

**结论：hellow 不取代调用方设计，但出现成的"接管面"和"开放面"不对等——自由度集中在元素接口与 HellowModal 子类化，chrome 与配色是封闭的。**

**开放面（调用方可自由定制）：**
- `Element` 是纯接口（width/height/render）+ `Interactive` 全部 default 方法 → 第三方可实现自定义元素，嵌入任意容器，交互钩子可选覆盖
- `HellowModal` 是设计给子类化的：protected `dialogWidth/Height/titleArea/buttonArea` 可调，`renderBody`/`init`/`onClose` 可覆盖 —— TISAM 的 `ExternalLinkConfirmScreen` 已实践（hellow 只管窗格骨架，内容与按钮用 vanilla widgets 自绘），是"hellow 提供自定义空间而非取代设计"的样板
- 触发层 `Hellow.when/whenEvent/whenApi` 的 factory 返回 `null` 即拒绝 → 门控权在调用方；H5 事件系统（`HellowElementPressedEvent`/`HellowValueChangedEvent`）允许第三方 `@SubscribeEvent` 响应，完全绕开 hellow 的 UI
- `HollowDialog`/`Hellow` 从不信任内容（设计原则：调用方定义内容，hellow 管理呈现）

**封闭面（目前无定制入口）：**
- `PageScreen` 是 `final` 类，chrome（DIM/PANEL 背景/边框/标题色）与几何全为 `private static final` → 走 page 路线的调用方改不了外观
- 元素级配色/尺寸是散落的 `private static final`（如 `ButtonElement` 的 BORDER_COLOR/TEXT_COLOR/HEIGHT），`Elements.button/link/toggle/slider` 无颜色/尺寸参数 → 无集中主题/配色入口
- `HollowDialog` 是 `final`，只能是固定尺寸的 320x200 元素树窗口；要定制尺寸/背景只能自己写 Screen

**缺口归纳（对应"自定义空间不足"的真实点）：**
1. 无主题/配色机制（颜色散落各元素类，`Elements` 不全收颜色参数）—— 最实际的缺口
2. `ElementRegistry` 未激活 → "注册自定义元素"的官方路径缺失（但实现 `Element` 接口本身就是绕过它的开放路径）
3. `PageScreen` 不可继承 → 页面级外观定制需要自定义 Screen，绕开 page 栈
4. 布局表达力缺口同 R8：Grid 无比例列、无限宽子元素、Column 无 flex（只有 Row 有 flex）

**评估结论**：对"调用方要保留自己的设计"（如 TISAM 的 ExternalLinkConfirmScreen 路线），hellow 已足够——`HellowModal` + 元素树提供了布局骨架而不剥夺自绘能力。对"调用方想直接用 hellow 的页面主题但改配色"则不够（无主题入口）。下一步如要继续，优先级建议：主题/配色参数化 > Grid 比例列 + 限宽子元素 > ElementRegistry 激活。

## R10：Part 3（F1–F6）执行状态审计（2026-08-28 会话 5）

逐项核验 task_plan.md Part 3 六阶段的代码落地情况（grep + 读源码 + `gradlew :hellow:build`，BUILD SUCCESSFUL）。

| 阶段 | 计划状态 | 代码实际 | 证据 |
|------|---------|---------|------|
| F1 交互转发缺口 | ⏳ | ✅ 已落地 | FlexElement L39/44/49 有 mouseScrolled/mouseDragged/tooltipAt；SliderElement L66 有 mouseDragged；ExpandableListElement L94/103 有 mouseScrolled |
| F2 滚动位置 State 化 | ⏳ | ✅ 已落地 | ScrollElement L26/39 State 构造 + L141-148 scroll()/setScroll 读写 State；ListView 同理；Elements L204/218 重载 |
| F3 Grid 比例列 | ⏳ | ❌ 未实现 | GridElement 仅 `(children, columns, gap)` 构造；FlexMath 仅 rowWidths |
| F4 FillElement（限宽子元素） | ⏳ | ❌ 未实现 | 全库无 FillElement |
| F5 HellowTheme 主题化 | ⏳ | ❌ 未实现 | 无 com.hellow.style 包；配色仍为各元素 private static final |
| F6 ElementRegistry 复核 | ⏳ | ✅ 已落地 | D-H1 实验性 Javadoc 已在 R8 落地 |

**结论**：
1. task_plan.md Part 3 执行状态表已全部误标 ⏳（F1/F2/F6 实际已落地）——本次审计已更正为「已落地 vs 未实现」。
2. 真正未完成的 hellow 缺口仍是 F3（Grid 比例列 + `FlexMath.gridColumnWidths`）、F4（`FillElement`）、F5（`HellowTheme` 主题/配色参数化）——与 R9 缺口 #1/#2/#7（主题）一致。
3. 非 hellow 范围：`:tisam:compileJava` 被 [ModListScreen.java](file:///d:/.trae-project/there%20is%20still%20a%20mod/tisam/src/main/java/com/tisam/client/screen/ModListScreen.java) 的坏重构阻断（`int this.dialogLeft = ...` 等 25 个错误，compile_errors.txt），全量 `gradlew build` 门禁当前无法达成。
4. 根目录 hs_err_pid8300.log / replay_pid8300.log 是会话 1 已知的 Gradle daemon OOM 崩溃（2026-07-26，-Xmx1G 时 15G RAM 机器内存不足），与本轮审计无关。

## R11：F3/F4/F5 落地记录 + R10 更正（2026-08-28 会话 5 第二段）

用户确认"完成这些所有"。G1–G5 全部落地，`gradlew build` 14 tasks 全绿。

### 实施
- **F3（Grid 比例列）**：`FlexMath.gridColumnWidths(naturals, available, gap, weights)` 纯函数（委托 `rowWidths`，语义等价共享实现）；GridElement 4 参构造 + 全网格按列宽渲染（render 是真相源）；`Elements.grid(columns, gap, weights, …)`。FlexMathTest +4（退化/比例/余数兜底/无余量）。
- **F4（FillElement）**：`FillElement` record 薄包装（instanceof 由 Column/Grid 识别）；Column `ensureMeasures` 统一"测量==渲染宽度"；Grid `rowRenderWidths` 将行剩余宽度分给 fill 单元格；`Elements.fill`。语义：wrappedText 包 fill 后在 Column/Grid 内可换行，`fill(wrappedText(...))`。
- **F5（HellowTheme）**：26 字段 record（全部旧硬编码色，DEFAULT 零视觉变化）；9 个元素类（Button/ExpandableList/Scroll/Toggle/Slider/TextInput/Enum + PageScreen/HellowModal chrome）主题参数化；Elements 8 个 theme 重载（button/link/toggle/slider/textInput/enumSelect/enumSelectIndex/scroll）；HellowThemeTest 2 项（默认色回归 + 自定义读回）。
- **G4 更正**：**R10 结论 #3 是误报** —— compile_errors.txt 为过期日志，磁盘上的 ModListScreen.java 已是干净版本（无 `this.dialogLeft` 垃圾代码）。`:tisam:compileJava` 实际通过。已将 task_plan/progress 相应记录更正。

### 新增小问题（记录，非 bug）
1. 多个 FillElement 在 Column/Grid 内均分剩余宽度（注释已声明；单填充为常见场景）。
2. Grid `cellWidthIn` 每单元格重算行宽（O(cells²)，列数小，可接受；升级路径：每行缓存）。

### R7/R8/R9 关闭状态更新
- R7 #7（Grid 拉伸/比例列）→ **已实现**（F3）
- R8「明确不做 #1」（Grid 比例列）→ **已实现**，不再是"单开阶段"
- R8「明确不做 #2」（wrappedText 在 Column/Grid 换行）→ **已实现**（F4, `Elements.fill(Elements.wrappedText(...))`）
- R9 缺口 #1/#2 → **已关闭**；R9 §2「无主题入口」→ **已关闭**（F5）
- 仍待办：`runClient` 手动视觉验证（R9 #4）

## R12：TISAM UI 迁移至最新 hellow API（2026-08-28 会话 5 第三段）

用户要求"补全 tisam 使用最新 hellow API 并设计良好界面（尽量与原画风一致）"。Part 5（P1–P5）全部落地，`gradlew build` 14 tasks 全绿。

### 迁移后架构
| 屏幕 | 基类 | 内容 |
|------|------|------|
| FirstPromptScreen | HellowModal（320×160） | body 自绘 3 行文案 + ESC 提示；3 按钮 addBottomButton |
| ModListScreen | HellowModal（420×280） | vanilla tabs/guide 按钮 + hellow `ListView`（70px 行高按需构建/视口裁剪/滚轮）；卡片=私有 `ModuleCardElement implements Element`（像素级复刻旧卡片、URL 悬停高亮 + tooltip、点击→ExternalLinkConfirmScreen） |
| DisclaimerScreen | HellowModal + `WARN_THEME` | 仅 border/title 换橙 0xFFFF8866；5s 冷却 Confirm 原逻辑保留 |
| ExternalLinkConfirmScreen | HellowModal（既有样板，未动） | — |

### 关键点
- **画风保持**：HellowModal chrome = HellowTheme.DEFAULT = 旧手绘 chrome 的精确颜色（dim 0xCC000000 / dialog 0xFF2A2A2A / border 白 / title 0xFFFFFF55）；卡片与警示橙以 TISAM 色常量保留在 TisamConstants（content 色不属于框架 chrome）
- **新增能力**：ModList 卡片 URL 悬停 tooltip + 高亮、ListView 虚拟化（长列表不建 N 个对象）、滚轮滚动
- **改进**：安装指南现按活动标签类型显示（`DEFAULT_INSTALL_GUIDE_BY_TYPE`，全局 install_guide_text 覆盖优先）——原先硬编码 MOD 指南
- **鼠标事件**：Screen 层 PageScreen 式转发（null-g RenderContext）到 ListView，与 render 共用同一 layout 坐标（bodyTop+HEADER_OFFSET）

### 过程中发现（记录）
1. DisclaimerScreen confirmTarget 原为 TitleScreen 参数与 HellowModal.parent(Screen) 冲突 → 放宽为 Screen
2. FirstPrompt 由缩放尺寸改为固定 320×160（与 ExternalLinkConfirmScreen 固定 420×180 先例一致；小屏不再缩放，可接受）

### 待办（不变）
- `runClient` 游戏内手动视觉/交互验证（含新 tooltip、ListView 滚轮、3 屏跳转闭环）仍为环境外待办

## R13：窗口模式溢出 / 文本按钮重合 — 根因与修复（2026-08-28 会话 5 第四段）

用户报告窗口模式溢出 + 文本/按钮重合。逐层排查 → 3 个根因（2 个 hellow 库级 + 1 个 TISAM 细点）：

### 根因 1（hellow，主因）：HellowModal 无 body scissor
- `render()` 直接调 `renderBody()`，不裁剪 → 超出 `bodyHeight()` 的内容直绘进底部按钮条
- 具体触发：`ModListScreen` 的 ListView 只对"行起点"做了 y 循环裁剪，**最后一个可见卡片的下半段（含 🔗 图标区）越过视口下缘**；超长免责声明多行文本同样可越界
- 修复：`g.enableScissor(bodyLeft(), bodyTop(), +bodyWidth(), +bodyHeight())` 包裹 renderBody（bodyHeight≤0 时跳过）

### 根因 2（hellow）：HellowModal 无窗口钳制
- 固定 dialogWidth/Height（420×280 / 320×160 / 360×200 / 420×180）在窗口模式小窗口下超出屏幕，底部按钮行被推出屏幕
- 修复：`init()` 顶部 `Math.min(dialogWidth, width-20)` / 同高；底部按钮行宽改为按 `(dialogWidth - PADDING*2)` 自适应（`buttonW = min(100, avail/count)`），行永远在对话框内

### 根因 3（TISAM）：FirstPrompt ESC 提示撞行
- 迁移时「ESC = Later」画在 body 顶部右对齐，与 body 第一行**同一 y** → 长译文时提示与文案重叠
- 修复：提示并入标题 Component（灰色后缀，回到原标题栏右缘位置），body 不再画该提示

### 记录（非本次修复）
- ListView/ScrollElement 的"局部可见行绘制"本身不内建 scissor，由 modal body scissor 兜底；若未来用在别处容器内，需自行对视口裁剪（升级路径）
- vanilla Button 不裁剪标签文本 → tab 文字在极端窄窗下溢出为外观小瑕疵

### 验证
- `gradlew build` 全绿（14 tasks）

## R14：最大化仍部分超出屏幕 — 结构级修复（2026-08-28 会话 5 第五段）

用户反馈最大化窗口下仍部分超出屏幕。第一轮（R13）修的是"body 无 scissor + 逐轴 clamp + ESC 撞行"，本轮找到更深的结构问题：

### 根因 1（hellow）：ListView/ScrollElement 只按行起点裁剪
- `render()` 的循环条件是「行起点 y < 视口底」，因此**起点在视口内、底部越出视口的行仍被完整绘制** → 列表实际绘制高度大于视口高度，越出的部分会画到视口下方（对话内任何位置，与窗口大小、是否最大化无关）
- 修复：`ListView.render` / `ScrollElement.render` 对自身视口加 scissor（`(ctx.x, ctx.y, ctx.x+width, ctx.y+viewportHeight)`，GUI 坐标，与 HellowModal 的 body scissor 嵌套求交）；`viewportHeight==0` 时跳过
- 补丁 2 修正 R13 中的记录项"ListView 无内部 scissor、由 body 兜底"——现在列表自身在视口边界闭合

### 根因 2（hellow）：HellowModal 逐轴 clamp 不保宽高比
- 只做 `min(dialogWidth, width-20)` 逐轴钳制会扭曲布局比例；且未复现迁移前的"响应式缩放"
- 修复：`fitToScreen()` —— 能放下用设计尺寸；放不下按同一比例 `floor(dialog*scale)` 缩到窗口内（margin 20），与迁移前（DESIGN_WIDTH*scale 方案）行为一致

### 验证
- `gradlew build` 全绿（14 tasks）
- 视觉复核仍待 `runClient`（环境外）

## R17：按钮根治 + 全量审计（2026-08-28 会话 5 第九段，4 个子 agent）

用户要求"最大思考力度 + 子 agent"。四份并行审计与修复（详见 task_plan Part 6）：

1. **按钮根治**：反射修好后仍无按钮 → 根因是时序/生命周期：① Opening 抢占使提示在 TitleScreen init 前弹出、嵌套 setScreen 争用；② Later 返回触发 re-init `clearWidgets()` 清按钮且 `containsKey` 阻止重加。改为 Init.Post 每次加按钮 + 下一 tick 弹提示。
2. **配置消费审计**：字段 → 使用点全表在 task_plan Part 6.B。未消费项（version/license/can_ignore/条目级 install_guide_text/只写不读 last_reminder_date）全部转为"有消费或明确 informational"。
3. **GUI scale 审计**：hellow 全在 vanilla 缩放坐标（Screen.width/height、enableScissor 绝对坐标、font.width/split）→ 一致 ✅；修复 fitToScreen 累积缩小（保存 design 尺寸）。
4. **hellow 布局/事件全面审计**：修 P0（WrapRow 测量≠渲染）、P1×3（PageScreen 生命周期幂等 removed()、PageScheduler 引用身份、DISPATCHING volatile 等）、P2×4（clearRules 清理、Column fill 超宽、TextRenderer maxWidth、PageStack 标注）。其余 P2（嵌套 scissor 无栈、ScrollElement O(n)、CLAMPED 丢格式等）记录于 task_plan Part 6.D，附升级路径。

**验证**：`gradlew build` 全绿（14 tests）；最新 jar 已同步 runs/client/mods；按钮行为待用户游戏内复核。

## R18：排版能力归 hellow 的完整现状（2026-08-29 会话 6）

用户要求截断/换行/滚动/缩放/按钮排列等必须是 hellow 能力。迁移后最终分工（详见 task_plan Part 7）：

| 能力 | 归属 | TISAM 用法 |
|------|------|-----------|
| 窗口 chrome/缩放/按钮排列/防重复 init | hellow（HellowModal） | 4 屏继承 |
| 截断（省略号） | hellow（TextElement CLAMPED / LinkElement CLAMPED / TextRenderer） | 卡片 URL、描述、外链 URL |
| 自动换行 | hellow（TextElement WRAP + fill） | 指南、免责声明 |
| 自动滚动 | hellow（ListView/ScrollElement 滚动条+惯性） | 卡片列表、指南 |
| hover/tooltip | hellow（LinkElement/Interactive） | 卡片 URL 整行 |
| 行内对齐/填充 | hellow（Row/Column/fillSpacer/fill） | 卡片内容、body |
| 控件本体（vanilla Button） | TISAM（刻意取舍，hellow 不生产 MC widget） | 底部/标签/指南按钮 |
| 业务（config/扫描/提醒/排序/锁定） | TISAM | — |

新增 hellow API：`TextElement.clamped`、`Elements.clampedText`、`Elements.wrappedText(Component)`、`LinkElement(…, StringMode)`。

## R19：Disclaimer 底部按钮 hellow 化完成（2026-08-29 会话 6 第二段）

用户要求"继续迁移 tisam 的 hellow API"。核查发现一个**半迁移编译错误**：`DisclaimerScreen` 的 `confirmButton` 字段已被上次会话改成 `com.hellow.element.ButtonElement`，但 `init()`/`renderBody()` 仍使用 vanilla `Button.builder`/`active`/`setMessage`（无 `Button` import）→ `:tisam:compileJava` 10 个错误，全量 build 红。

修复要点：
- Back/Confirm 改用 `addButton(Elements.button(...))` / `new ButtonElement(..., WARN_THEME, false)` —— 底部按钮行完全走 hellow 声明式路径（HellowModal 负责布局/渲染/点击转发/tooltip）。
- 冷却倒计时：`ButtonElement.setEnabled(false)` + `setLabel("Confirm (Ns)")`，解锁用 `setEnabled(true)` + 最终文案，与旧 `active`/`setMessage` 行为等价；`ButtonElement.mouseClicked` 内置 enabled 门控，原 `if (!active) return` 冗余判断删除。
- 视觉零变化：`WARN_THEME` 的按钮色全部继承 `HellowTheme.DEFAULT`；禁用态用 ButtonElement 固定灰（DISABLED_BG/BORDER/TEXT）。

**迁移后 vanilla 残留盘点**：TISAM 4 个模态屏的 chrome/body/底部按钮/卡片内容已全部 hellow 化；唯一保留的 vanilla 控件是 `TisamClientEvents.TitleScreenButton`（`AbstractWidget`，挂在 vanilla TitleScreen 上——hellow 不拥有 vanilla 标题屏 chrome，属"控件本体"，刻意保留）。

**验证**：`gradlew build` 全绿（14 tasks）；最新 jar 已同步 runs/client/mods（8/29）。

## R20：P3 everyconfig 实现 + ScreenEmbedElement 集成缺口（2026-08-31 会话 8）

### 1. 依赖坐标（实测验证）
- **Cloth Config**：`me.shedaniel.cloth:cloth-config-neoforge:15.0.140`（MC 1.21.1 线）@ `https://maven.shedaniel.me/`（可达）。
- **YACL**：`dev.isxander:yet-another-config-lib:3.7.1+1.21.1-neoforge` @ `https://maven.isxander.dev/releases`（可达，pom 200）。
  - **坑**：group 是 `dev.isxander`（不是 `dev.isxander.yacl`）；版本串含 `+`（pom 需 `%2B` 编码访问）；该 maven 仓库不提供目录列表（dir 404 正常）。
  - 库 mod id：cloth=`cloth_config`，YACL=`yet_another_config_lib_v3`。
- API 要点（subagent 调研）：Cloth `ConfigBuilder.create().setParentScreen/setTitle/setSavingRunnable + getOrCreateCategory + build()`→Screen；YACL `YetAnotherConfigLib.createBuilder().title/category(...).save(...).build().generateScreen(parent)`→Screen，且每个 Screen 需全新实例。

### 2. ScreenEmbedElement 集成缺口（hellow 已修复）
- 原实现（commit `7ba0fae`）从不调用嵌入 Screen 的 `init()` → 嵌入屏 widgets 为空，且把**绝对屏幕坐标**直传给嵌入屏（其布局是元素局部坐标）。
- 修复：`ensureInit(ctx)` 按矩形尺寸 `init(mc,w,h)`（once per size，尺寸变化时重 init）；render/鼠标事件改用 `ctx.mouseX()-ctx.x()` 局部坐标。
- 限制（记录）：嵌入屏内容超矩形被 scissor 裁剪；`init` 在尺寸抖动时可能重建 widgets。

### 3. 软依赖 + 可测性设计
- **compileOnly + ModList 门控**：adapter 类直接引用库类型，但 `createScreen` 只在 `isInstalled()` 为真后被调用（JVM 方法级惰性加载，缺库不崩）。
- **注册表惰性化**：`ConfigAdapterRegistry` 用 `ensureRegistered()` 而非 static 块——否则在无 MC 的测试 JVM 里加载注册表类即触发 adapter 实例化 → NoClassDefFoundError。
- **纯逻辑可测**：把"选第一个满足条件"抽成泛型 `firstWhere(List<T>, Predicate<T>)`，`firstInstalled()` 委托它 → 无需 MC 即可 JUnit 覆盖（4 项）。

### 4. 范围说明
- 只实现 cloth + YACL 两个引用 adapter；Configured/FzzyConfig/ResourcefulConfig 按计划"以本地 available libs 为准"暂缓（网络仅可及前两者）。
- everyconfig 的 demo 配置屏（DemoConfig）仅演示嵌入机制；真实嵌入具体 mod 的配置需接 `IConfigScreenFactory`（后续方向）。
