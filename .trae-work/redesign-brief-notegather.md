# FaFa 小程序重设计方案 - NoteGather 春日自然主题

## 设计目标
将现有的温暖大地色系小程序重新设计为基于 NoteGather 设计系统的春日自然主题，保持原有功能和布局结构，重点改变色彩、视觉风格和图标设计。

## 原有设计系统 vs NoteGather
| 元素 | 原设计 | NoteGather 新设计 |
|------|--------|-------------------|
| 主色调 | 赤陶橙 #C4612F | 春日绿 #7CB342 |
| 背景色 | 奶油色 #F7F4EF | 春日渐变背景 gradient-spring |
| 卡片背景 | 白色 #FFFFFF | 毛玻璃卡片 glass + backdrop-filter |
| 辅助色 | 单一橙色系 | 春花粉 #FFCDD2 / 天空蓝 #81D4FA / 蒲公英黄 #FFF59D |
| 圆角 | 12-16px | 8-16px（md/lg/xl） |
| 阴影 | 简单阴影 | 5级阴影 + 3D立体阴影（春日绿色调）|
| 动效 | 简单过渡 | 自然呼吸动效（150ms/300ms/500ms + bounce缓动）|

## 设计代币映射

### 颜色系统
```css
/* 主色 - 春日绿色系 */
--color-brand-primary: var(--notegather-primary-500); /* #7CB342 */
--color-brand-primary-light: var(--notegather-primary-400); /* #9CCC65 */
--color-brand-primary-dark: var(--notegather-primary-700); /* #689F38 */
--color-brand-primary-alpha: rgba(124, 179, 66, 0.1);

/* 背景 */
--color-background-primary: var(--color-bg-canvas); /* 春日渐变 */
--color-surface-primary: var(--color-bg-surface); /* 白色表面 */
--color-surface-glass: var(--color-glass-bg); /* 毛玻璃 rgba(255,255,255,0.7) */

/* 辅助色 - 春天色彩 */
--color-accent-pink: var(--notegather-secondary-200); /* #FFCDD2 樱花粉 */
--color-accent-blue: var(--notegather-sky-300); /* #81D4FA 天空蓝 */
--color-accent-yellow: var(--notegather-secondary-500); /* #FFF59D 蒲公英黄 */
--color-accent-purple: var(--notegather-secondary-700); /* #CE93D8 紫花 */

/* 文字 */
--color-text-primary: var(--color-text-dark); /* #212121 */
--color-text-secondary: var(--color-text-normal); /* #616161 */
--color-text-tertiary: var(--color-text-light); /* #9E9E9E */
```

### 视觉特效
```css
/* 毛玻璃效果（NoteGather 签名特色）*/
.glass-card {
  background: var(--color-glass-bg);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--color-glass-border);
}

/* 渐变 */
--gradient-primary: linear-gradient(135deg, #7CB342 0%, #9CCC65 100%);
--gradient-spring: linear-gradient(135deg, #C8E6C9 0%, #FFF9C4 50%, #FFCDD2 100%);

/* 3D 立体阴影 */
box-shadow: var(--shadow-3d);
```

## 页面改造重点

### 1. 首页（home/index.html）
**原设计特征：**
- 赤陶橙主题色按钮和标签
- 奶油色背景
- 圆形头像
- 扁平卡片

**NoteGather 改造：**
- ✅ 春日绿渐变背景（gradient-spring）
- ✅ 毛玻璃宠物信息卡（backdrop-filter）
- ✅ 快速记录按钮改用春日绿渐变
- ✅ AI发现卡片使用虚线边框 + 春花粉背景
- ✅ 浮动AI按钮使用春日绿渐变 + 脉动动画
- ✅ 今日计划/提醒卡片使用轻微毛玻璃效果
- ✅ 图标改用春天自然主题（🌱种子、🌿叶子、🌸花朵）

### 2. 记录模块（record/index.html）
**改造要点：**
- ✅ 统计卡片使用3D立体阴影（shadow-3d）
- ✅ 喂食图标 🌱（种子）、体重 ⚖️、饮水 💧、排便 🍂（落叶）
- ✅ 卡片悬停使用 bounce 缓动效果
- ✅ 渐变卡片背景（gradient-card）

### 3. 喂食记录列表（record/feeding/list.html）
**改造要点：**
- ✅ 日期分组标签使用春日绿
- ✅ AI分析卡片使用虚线边框 + 森林渐变背景（gradient-forest）
- ✅ 列表项悬停效果使用轻微毛玻璃
- ✅ 时间筛选 tabs 使用春日绿选中态

### 4. 体重记录页（record/weight/list.html）
**改造要点：**
- ✅ 折线图改用春日绿渐变（#7CB342 → #9CCC65）
- ✅ 数据点使用樱花粉 #FFCDD2
- ✅ AI分析按钮使用 pill 样式 + 春日绿渐变
- ✅ 当前体重卡片使用 3D 立体阴影

### 5. 宠物档案（pet/detail.html）
**改造要点：**
- ✅ 顶部信息卡使用渐变背景（白→薄荷绿）
- ✅ 功能入口图标使用春天主题（📊数据→📈、📷照片→🌸、📝日记→🍃）
- ✅ 最近动态使用毛玻璃卡片
- ✅ AI发现区域使用春花粉背景

### 6. 新建宠物表单（pet/create.html）
**改造要点：**
- ✅ 输入框焦点状态使用春日绿边框 + 绿色光晕
- ✅ 保存按钮使用春日绿渐变
- ✅ 单选按钮选中态使用春日绿
- ✅ 表单背景使用春日渐变

### 7. 弹窗组件（modals/feeding.html, ai-chat.html）
**改造要点：**
- ✅ 弹窗背景使用毛玻璃效果
- ✅ 标题栏使用春日绿强调色
- ✅ AI消息气泡使用春花粉背景
- ✅ 用户消息气泡使用天空蓝背景
- ✅ 弹窗入场动画使用 bounce 缓动

### 8. 照片网格（pet/photos.html）
**改造要点：**
- ✅ 照片卡片使用 3D 立体阴影
- ✅ 悬停效果使用 bounce 缩放
- ✅ 背景使用春日渐变

### 9. 空状态（components/empty.html）
**改造要点：**
- ✅ Icon 使用春天主题（🌱种子）
- ✅ 提示文字使用春日绿
- ✅ 按钮使用春日绿渐变

## 图标系统更新

### 原图标 → NoteGather 春天图标
| 功能 | 原图标 | 新图标 | 含义 |
|------|--------|--------|------|
| 喂食 | 🍚 | 🌱 | 种子（生命之源）|
| 饮水 | 💧 | 💧 | 保持水滴（清泉）|
| 体重 | ⚖️ | ⚖️ | 保持天平 |
| 排便 | 💩 | 🍂 | 落叶（自然循环）|
| 照片 | 📷 | 🌸 | 花朵（美好瞬间）|
| 事件 | 📝 | 🍃 | 叶子（成长记录）|
| AI助手 | ✨ | 🌿 | 新芽（智慧生长）|
| 数据 | 📊 | 📈 | 保持增长图 |
| 日记 | 📝 | 🍃 | 叶子（记忆）|
| 提醒 | ⏰ | 🔔 | 保持铃铛 |

## 动效系统

### 按钮交互
```css
.ng-btn-primary {
  transition: all var(--duration-normal) var(--ease-smooth);
}
.ng-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-3);
}
.ng-btn-primary:active {
  transform: translateY(0);
  animation: none;
}
```

### 卡片交互
```css
.ng-card-hover:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-3d);
  transition: all var(--duration-normal) var(--ease-bounce);
}
```

### 浮动AI按钮
```css
@keyframes pulse-green {
  0%, 100% { box-shadow: 0 0 0 0 rgba(124, 179, 66, 0.7); }
  50% { box-shadow: 0 0 0 12px rgba(124, 179, 66, 0); }
}
```

## 实现要求

1. **引入 NoteGather 设计系统**
   - 在所有页面 `<head>` 中引入：
   ```html
   <link rel="stylesheet" href="../.design_library/NoteGather/colors_and_type.css">
   <link rel="stylesheet" href="../.design_library/NoteGather/components.css">
   ```

2. **使用设计代币**
   - 所有颜色必须使用 CSS 变量：`var(--color-brand-primary)`
   - 所有间距使用：`var(--space-4)` 等
   - 所有圆角使用：`var(--radius-md)` 等
   - 所有阴影使用：`var(--shadow-3)` 等

3. **毛玻璃效果规范**
   - 顶部导航栏、宠物信息卡、重要卡片使用毛玻璃
   - 代码：`backdrop-filter: blur(20px) saturate(180%);`

4. **渐变使用规范**
   - 主按钮：`var(--gradient-primary)`
   - 背景：`var(--gradient-spring)`
   - 次级强调：`var(--gradient-forest)`

5. **动效规范**
   - 微交互：150ms（按钮悬停）
   - 常规动画：300ms（卡片展开）
   - 大型过渡：500ms（页面切换）
   - 使用 bounce 缓动：`var(--ease-bounce)`

## 交付清单
- ✅ 10个页面全部重新设计
- ✅ 统一使用 NoteGather 设计代币
- ✅ 所有交互动效更新为自然呼吸风格
- ✅ 图标系统更新为春天主题
- ✅ 毛玻璃效果应用到关键界面元素
- ✅ 春日绿色系完全替代赤陶橙色系
- ✅ 3D 立体阴影应用到重点卡片
- ✅ 保持原有功能和布局结构不变
