# FaFa 微信小程序前端

本项目依据 `../docs/小程序交互页面/` 的产品设计与 `../pages/` 的信息架构重新实现。它采用 **uni-app + Vue 3 + TypeScript + Pinia**，可构建为微信小程序，也可在浏览器中预览。

这版重做采用 Apple 式的分组列表信息架构：浅灰画布、纯白分组表面、系统蓝作为唯一高亮色、较大圆角和充分留白。重构时保留了原设计中的宠物档案、今日计划、快速记录、记录分类、成长、提醒、AI、报告和个人设置等核心路径，但没有复用其春日绿色、渐变或图形语言。

## 前端范围

- Mock 数据及交互状态均在 `src/data/mock.ts` 与 `src/stores/pet.ts`
- 快速记录、提醒完成、分组筛选、AI 模拟问答、报告详情均可交互
- 后端接入时用 Repository/API 层替换 Mock 数据，不影响页面结构

## 运行

```bash
npm install
npm run dev:h5
npm run build:mp-weixin
```

将 `dist/build/mp-weixin` 导入微信开发者工具预览。发布前在 `src/manifest.json` 填写微信小程序 AppID。
