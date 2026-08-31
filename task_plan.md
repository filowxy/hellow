# Hellow 生态扩展实施计划(dbg + file-driven pages + everyconfig)

## 项目目标(Project Goal)

在已完成的 hellow UI 库 / TISAM 模组之上,交付三块能力:

1. **P0 — Git 推送**:把当前 monorepo(hellow + tisam)推送到 GitHub 仓库 `hellow`(作者 filowxy)。
2. **P1 — Hellow 调试机制**:界面错误报告等(捕获渲染/事件中的异常,汇总为可读报告,并提供游戏内查看入口,如 DebugScreen/日志导出/错误弹窗)。
3. **P2 — 文件驱动的页面系统**:玩家可在 `{游戏目录}/config/hellow/pages` 下放文件,以**与 Java API 一致的声明格式**描述页面(元素树/布局/控件/文本),并**必须声明触发机制**(何时打开:屏幕条件 / NeoForge 事件 / API 轮询),Hellow 启动时加载注册,使其像 `HellowPages.register(...)` 一样可被 `Hellow.show(...)` 调用。
4. **P3 — everyconfig 附属模组**(独立于 hellow,自身是一个新 NeoForge mod):
   - 兼容 cloth-config / YACL(YetAnotherConfigLib)/ Configured / FzzyConfig / ResourcefulConfig 等模组的配置界面;
   - 将上述任意模组的配置界面**嵌入 hellow 界面,作为一种新的界面元素**(如 `Elements.configScreen(...)`),从而可用 hellow 的声明式树来展示/导航第三方配置界面。

## 关键决策(Key Decisions)

| # | 决策点 | 选择 | 理由 |
|---|--------|------|------|
| P0-D1 | 远程仓库名 | `hellow` | 用户指定;owner = filowxy(GitHub 用户名待确认) |
| P0-D2 | 分支 | `master`(本地已提交 root-commit `91c64fa`) | 仓库无历史,直接推主分支 |
| P1-D1 | 错误收集范围 | hellow 的 render / event / 布局测量路径异常 | 覆盖调用方元素树中的运行时错误 |
| P1-D2 | 报告入口 | 游戏内 Debug 报告界面 + 日志写入(`config/hellow/debug/`) | 玩家可自查,作者可收集 |
| P2-D1 | 文件格式 | **JSON**(与 `Elements` 的工厂调用一一对应的模式化树) | 与 API 一致、Gson 友好、无新依赖 |
| P2-D2 | 位置 | `{游戏目录}/config/hellow/pages/*.json` | 用户指定;启动扫描 + 文件改动可重载 |
| P2-D3 | 触发 | 页面文件内 `trigger` 字段(屏幕/事件/API 三类,对齐 `Hellow.when*`)+ 可选 fireStrategy | 用户要求"必须包含触发机制" |
| P3-D1 | 语言 | Java(与 hellow/tisam 一致) | 兼容性优先 |
| P3-D2 | 依赖策略 | 各配置库**软依赖**(反射或 optional 依赖),缺库则对应元素禁用 | 避免硬性耦合多个第三方库 |

## 阶段划分(Phases)

### 阶段 0:Git 推送(P0)
- 目标:创建 GitHub 仓库 `hellow` 并推送本地提交(`91c64fa`)。
- 步骤:
  1. 重试 `mcp_GitHub.create_repository(name=hellow)`(上一轮 Bad credentials,用户称已修复)。
  2. `git remote add origin https://github.com/filowxy/hellow.git`
  3. `git push -u origin master`
- 验证:`git ls-remote origin` 有内容 / push exit 0。
- 阻塞:若 token 仍失效或网络 reset,记录并交给用户(给出命令)。

### 阶段 1:Hellow 调试机制(P1)
- 目标:界面运行时报错可被捕获、汇总、展示。
- 产物:
  - `com.hellow.debug` 包:
    - 错误收集器(全局 render/事件异常捕获,含调用方 Package 过滤以防误报 vanilla)；
    - `DebugReport`(时间/来源元素/堆栈摘要/当前 Screen)；
    - 游戏内查看 `DebugScreen`(HellowModal 子类,Dll 列表 + 复制/导出按钮)；
    - 磁盘导出(`config/hellow/debug/report-<ts>.log`)。
  - 接入点:`Element.render` / `Interactive` 事件派发的 catch 包装(Hellow 侧的 dispatch 而非逐个元素)。
- 验证:单测(错误收集/格式)+ 在测试页面抛错可看到报告条目。
- 决策:应用户"更多 debug 机制",除错误报告外再提供可选的 `Hellow.debug(owner, ...)` 调试日志通道。

### 阶段 2:文件驱动页面系统(P2)
- 目标:玩家在 `config/hellow/pages/*.json` 声明页面并能被触发打开。
- 产物:
  - JSON Schema(与 `Elements` 工厂对齐):
    - `id`/`title(lang key)`/`layer(NORMAL|MODAL|TOP)`/`width,height`(可选)
    - `body` = 元素树(text/wrappedText/clampedText/button/link/toggle/slider/textInput/enumSelect/column/row/grid/scroll/listView/box/flex...)
    - `trigger`(必填):`screen`(className 匹配)/ `event`(NeoForge event class)/ `api`(保留)/ + `fireStrategy`
    - 交互字段:`button.onPress` → 内建动作(`close`/`closeTop`/`openPage:<id>`/`setScreen:<class>`/`url:<link>`)或事件 id(与 event bus 联动)
  - 解析器:`PageFileParser`(JSON → `Element` 树 + `PageSpec` + 触发规则),校验失败给出明确错误(并入 P1 报告)
  - 加载器:`PageFileLoader`:启动扫描 + (`FileWatcher` 或重载命令 `/hellow pages reload`)重载;注册进 `HellowRegistry`
  - 注册到 `Hellow` 触发系统(`when`/`whenEvent` 效果等价)
- 验证:样例 `config/hellow/pages/demo.json` 可被 `/hellow pages reload` 生效、`Hellow.show("demo")` 打开、触发条件满足时自动弹出。
- 安全:文件解析失败不清空已有注册;错误入 debug 报告。

### 阶段 3:everyconfig 附属模组(P3)
- 目标:把第三方配置界面作为 hellow 新元素嵌入。
- 产物:
  - 新子项目 `everyconfig`(独立 mod id=`everyconfig`,依赖 hellow):
    - `com.everyconfig.embed` 包:每库一个 adapter,统一输出为"可嵌入 hellow 的 Screen/元素":
      - cloth-config:`ModConfigScreen.create(...)`/`ConfigEntry` 包装
      - YACL:`YetAnotherConfigLib(...).generateScreen(...)`
      - Configured:动态 Screen 包装
      - FzzyConfig:配置界面 Screen
      - ResourcefulConfig:配置表单 Screen
    - hellow 侧新元素 `Elements.configEmbed(...)`(由 everyconfig 注册进 hellow 的扩展点,或 hellow 提供 `Element` 接口的通用 Screen 包装器):
      - `ScreenEmbedElement`:把第三方 Screen 渲染进一位矩形区域并转发鼠标/键盘
    - 各库**软依赖**:存在对应 mod 才启用该 adapter
  - 验证:任选 1-2 个已装库的配置界面展示在 hellow 测试页中;缺库时元素显示"未安装"占位且不崩溃。
- 范围控制:先做 cloth-config + YACL 两个引用实现,其余库 adapter 结构同构扩展;如网络受限,以本地 available libs 为准。

## 交付顺序与验证节奏

- 每阶段结束:`gradlew` 对应子项目 `compileJava/test` 通过;P2/P3 附带样例文件与可用性演示
- 每阶段结束更新 `progress.md`;研究结论进 `findings.md`

## 遇到的错误(追加记录)

| 错误 | 尝试次数 | 解决方案 |
|------|---------|---------|
| GitHub MCP Bad credentials(首轮) | 1 | MCP 服务器已被移除(server not found) |
| git https Connection reset(VPN) | 1 | 网络已恢复(ls-remote 可达,grep repo not found) |
| 仓库 filowxy/hellow 不存在 | 1 | **阻塞**——无 MCP/无 token/无 gh;等用户在 github.com/new 创建空仓库 `hellow` 后,执行 `git remote add origin https://github.com/filowxy/hellow.git; git push -u origin master` |

## 当前状态(2026-08-29)

- **P0**:已提交 `91c64fa`(根)+ `7f41c28`(P1)+ `5d1ae70`(P2)+ `7ba0fae`(ScreenEmbed)待推送 → **等待用户创建 GitHub 空仓库 `hellow`**(github.com/new,勿勾选 README/license),之后 `git remote add origin https://github.com/filowxy/hellow.git; git push -u origin master`。网络已达、token 无。
- **P1(complete)**:`com.hellow.debug`(ErrorCollector 限长收集 / DebugEntry / HellowDebug 导出 / DebugScreen 查看 / `/hellow debug`) + HellowModal 渲染/事件边界捕获并绘制 fallback。7 单测通过;commit `7f41c28`。
- **P2(complete)**:文件驱动页面 `config/hellow/pages/*.json`。JSON 与 Elements 工厂/触发 API 对齐;onPress 支持 closeTop / openPage:<id> / url:<url> / emit:<name>(→ `PageButtonActionEvent` 上 bus);trigger 支持 screen(按类名)与 event(按类),strategy once/everyTime;`/hellow pages reload` 重载页面不重挂触发;坏文件进 debug 报告且不影响其他。样例 `hellow_demo.json` 已放 runs 并演示。commit `5d1ae70`。
- **P3(in_progress)**:hellow 侧 `ScreenEmbedElement` 完成(任意第三方 Screen 以矩形区域嵌入 hellow 布局,scissor 裁剪、事件转发;commit `7ba0fae`)。**剩余**:everyconfig 新子项目 + cloth-config/YACL/Configured/FzzyConfig/ResourcefulConfig 的 adapter(软依赖),建议 subagent 调研各库 API 后实现。
- **范围说明(ponytail)**:PageSpec 尚无自定义 dialog 宽高;文件 trigger 热更需重启;everyconfig 各库适配以本地可用依赖为准。