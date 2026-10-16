# UI / App 设计 Skills 使用指南

本指南说明项目中已下载的 UI 与产品设计 Skills 各自负责什么、适合何时使用，以及可直接复制的调用提示词。

## 文件位置与调用方式

- 下载原件：`D:\dev\Java\FaFa\skills\<skill-name>`
- Codex 项目自动发现目录：`D:\dev\Java\FaFa\.agents\skills\<skill-name>`
- Codex 扫描目录名是 `.agents/skills`（复数）。仓库中的 `.agent`（单数）不是自动发现目录。
- 在 Codex 提示中写 `$skill-name` 可显式调用；CLI/IDE 扩展也可用 `/skills` 查看列表。如果新 Skill 尚未显示，重启 Codex。

每个目录内的 `SKILL.md` 是完整规则，`references/`、`scripts/` 等文件是配套资料。Codex 需要时会读取它们。

## Skill 清单与选择时机

| Skill | 主要作用 | 适合什么时候用 |
| --- | --- | --- |
| `ui-ux-pro-max` | 为 Web、移动端和桌面界面提供结构、视觉、UX、无障碍和技术栈方面的设计建议；包含可搜索的样式、配色、字体、UX 规则和技术栈指导。 | 新产品或新页面开始设计时，用它建立整体方向和设计系统；遇到局部交互、可访问性或布局问题时，针对单一问题查询。支持 SwiftUI、Jetpack Compose、React Native、Flutter 等栈。 |
| `mobile-android-design` | Android 原生设计规范，重点是 Material 3、Jetpack Compose、Navigation Compose、动态颜色、无障碍和不同屏幕尺寸适配。 | 项目是原生 Android/Compose，或需要处理平板、折叠屏等 Android 形态时使用。它负责平台细节，不代替产品定位和品牌视觉规划。 |
| `apple-design` | 基于 Apple Human Interface Guidelines 评审和改进 iOS、macOS 及跨平台界面；涵盖导航、表单、深色模式、可访问性、onboarding 等。 | 评审现有截图、线框或代码，检查是否符合 Apple 平台习惯，或希望界面更精致、有辨识度时使用。偏体验评审和改进建议。 |
| `expo-native-ui` | Expo/React Native 界面的原生观感、平台样式、布局、颜色、控件、图标、媒体与响应式设计。 | 实现 Expo 屏幕、需要兼顾 iOS 与 Android 平台行为时使用。该 Skill 会引用 `expo-ui` companion skill；本次下载包没有包含它。 |
| `expo-design-system` | Expo 应用的设计 token、主题、颜色/间距/字体/圆角/阴影、共用组件及跨页面一致性检查。 | 开始实现多个页面前建立主题，或已有界面样式不一致、需要整理硬编码样式时使用。一般先建立 token 和组件，再逐页实现。 |
| `animate-expo` | Expo/React Native 动画、手势、底部抽屉、页面转场、按压反馈、触觉反馈及真机性能建议。 | 静态界面已经明确，需要增加有意义的交互动效，或修复动画卡顿、手势衔接问题时使用。注意减少动态效果偏好和无障碍体验。 |
| `frontend-design` | Web UI 的视觉方向、版式、字体和品牌表现，帮助设计出有辨识度、不像通用模板的页面。 | 目标是网站、Web App、dashboard 或落地页时使用。它不是原生 iOS/Android 规范。 |

## 按项目类型快速选择

- **Android 原生 App：**先用 `ui-ux-pro-max` 梳理产品目标、用户流程和页面，再用 `mobile-android-design` 处理 Material 3 与 Compose 细节。
- **iOS App：**用 `ui-ux-pro-max` 建立整体方向，再用 `apple-design` 检查 HIG、导航、触控目标和无障碍细节。
- **Expo 跨平台 App：**先用 `ui-ux-pro-max` 定方向；用 `expo-design-system` 建主题与共用组件；用 `expo-native-ui` 实现平台适配；需要动效时再用 `animate-expo`。
- **已有 App 或截图评审：**用 `apple-design` 做体验和平台规范审查；Android 原生项目可再参考 `mobile-android-design`。
- **网站或后台：**用 `frontend-design` 做视觉表达；需要结构化比较设计方向或查询 UX 规则时配合 `ui-ux-pro-max`。

一次任务通常选一个主要 Skill。跨平台 App 可以按阶段串联多个 Skill，并在提示中说明当前希望它负责的部分，避免不同规则互相覆盖。

## 可直接复制的提示词

最好提供产品类型、目标用户、核心任务、平台、技术栈和期望交付物。示例：

### 新 App 的整体设计

```text
$ui-ux-pro-max 为一个面向通勤人群的习惯追踪 App 做前期设计。技术栈是 Flutter。先给出用户流程、信息架构、核心屏幕清单、设计 token 和无障碍要点；不要假设项目已有的技术栈或视觉规范。
```

### Android 原生界面

```text
$mobile-android-design 检查并实现这个 Jetpack Compose 个人记账首页。遵循 Material 3，说明手机和平板布局、导航、动态颜色与无障碍行为，并沿用仓库已有架构。
```

### Apple 平台设计评审

```text
$apple-design 评审我附上的 iPhone 截图和对应 SwiftUI 页面。按 Apple HIG 检查导航、层级、触控目标、表单、深色模式与 VoiceOver，并按严重度列出具体改法。
```

### Expo 设计系统

```text
$expo-design-system 为当前 Expo App 建立颜色、间距、字体、圆角、阴影 token 和按钮/卡片组件。先检查现有样式与依赖，再给出适合当前项目的文件结构。
```

### Expo 屏幕实现

```text
$expo-native-ui 按 iOS 与 Android 的平台习惯实现这个 Expo 设置页。优先使用符合平台的控件，处理 safe area、键盘、响应式布局、语义颜色和无障碍标签。
```

### Expo 交互动效

```text
$animate-expo 给这个 Expo App 的底部抽屉添加可打断的手势动画和触觉反馈。使用项目已有依赖，保证动画留在 UI runtime，并处理 reduced motion。
```

### Web 首页

```text
$frontend-design 为独立咖啡烘焙品牌设计一个有明确视觉个性的 Web 首页。先确定受众和内容，再选择字体、色彩、版式和真实的页面内容；避免套用通用 SaaS 模板。
```

## UI/UX Pro Max 查询脚本

该 Skill 自带搜索脚本，原始说明中的 `${CLAUDE_PLUGIN_ROOT}` 是 Claude 插件路径变量。在当前项目可使用实际 Windows 路径：

```powershell
python "D:\dev\Java\FaFa\skills\ui-ux-pro-max\scripts\search.py" "fitness habit tracking mobile app" --design-system
python "D:\dev\Java\FaFa\skills\ui-ux-pro-max\scripts\search.py" "navigation accessibility" --domain ux
python "D:\dev\Java\FaFa\skills\ui-ux-pro-max\scripts\search.py" "dynamic color adaptive layout" --stack jetpack-compose
```

- 新项目或新页面：用 `--design-system`。
- 单个 UX 问题：选一个明确领域，例如 `--domain ux`、`--domain color` 或 `--domain typography`。
- 已确定技术栈：用 `--stack`，例如 `swiftui`、`jetpack-compose`、`react-native` 或 `flutter`。
- 需要保存设计系统时，可加 `--persist --output-dir "<项目根目录>"`；先查看已有输出文件，避免覆盖已有设计决策。

## 上游仓库

- [UI/UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)
- [Android Mobile Design](https://github.com/wshobson/agents/tree/main/plugins/ui-design/skills/mobile-android-design)
- [Apple Design Skill](https://github.com/dickwu/apple-design-skill)
- [Expo Native UI](https://github.com/expo/skills/tree/main/plugins/expo/skills/expo-native-ui)
- [Expo Design System](https://github.com/expo/skills/tree/main/plugins/expo/skills/expo-design-system)
- [Animate Expo](https://github.com/emilkowalski/skills/tree/main/skills/animate-expo)
- [Frontend Design](https://github.com/anthropics/skills/tree/main/skills/frontend-design)
