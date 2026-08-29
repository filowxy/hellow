# Hellow v3 声明式页面框架 — 进度

> **任务**: 将 hellow 从窗口生命周期薄封装升级为完整声明式页面管理框架
> **模式**: LOCAL_ONLY

## 已完成
- [x] PageSpec / PageId / PageLayer / PageStack（无 Screen 依赖，纯逻辑可测）
- [x] Condition + FireStrategy（修复 decaying 永不递增 bug，增加 onConditionMatch）
- [x] Element 系统：TextElement / ButtonElement / LinkElement / SpacerElement / ColumnElement / ExpandableListElement
- [x] RenderContext 携带 Font，声明式渲染
- [x] PageScreen 渲染适配器
- [x] HellowMod 事件监听：条件触发 -> PageScreen 渲染
- [x] HellowRegistry / HellowPages 注册入口
- [x] JUnit 测试：FireStrategyTest / PageStackTest
- [x] hellow 编译 + 测试全部通过

## 待办
- [ ] tisam 4 个 Screen 迁移到声明式 Page
- [ ] 顶层叠加（TOP layer）的 overlay Screen 渲染路径
- [ ] 元素交互事件分发（mouseClicked/scroll 到 Element）

## 验证命令
```powershell
cd 'D:\.trae-project\there is still a mod'
.\gradlew.bat :hellow:test --no-daemon --console=plain
```