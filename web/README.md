# FaFa宠物管家微信小程序

一款功能完善的宠物管理微信小程序，帮助宠物主人记录和管理宠物的日常生活。

## 功能特性

### 核心功能
- **宠物档案管理**：创建和管理多个宠物的基本信息
- **首页仪表盘**：快速查看宠物状态和待办事项
- **多类型记录**：
  - 喂食记录
  - 饮水记录
  - 体重追踪
  - 就医记录
  - 疫苗记录
- **时光轴**：按时间线查看所有记录
- **提醒功能**：设置各类定时提醒
- **个人中心**：用户信息和数据管理

### 技术特点
- 采用微信小程序原生开发
- 响应式设计，适配各种屏幕尺寸
- 深色主题配色（#0F1117, #171A23, #1E222E）
- 现代化UI设计，流畅的交互体验
- 模块化代码结构，易于维护和扩展

## 项目结构

```
├── pages/                  # 页面目录
│   ├── index/             # 首页
│   ├── pet/               # 宠物管理
│   │   ├── list/          # 宠物列表
│   │   └── edit/          # 宠物编辑
│   ├── record/            # 记录管理
│   │   ├── select/        # 选择记录类型
│   │   ├── feeding/       # 喂食记录
│   │   ├── water/         # 饮水记录
│   │   ├── weight/        # 体重记录
│   │   └── medical/       # 就医记录
│   ├── timeline/          # 时光轴
│   └── profile/           # 个人中心
├── components/            # 自定义组件
├── utils/                 # 工具函数
│   ├── util.js           # 通用工具函数
│   ├── request.js        # 网络请求封装
│   ├── api.js            # API接口定义
│   └── config.js         # 配置文件
├── app.js                # 应用入口
├── app.json              # 全局配置
├── app.wxss              # 全局样式
└── project.config.json   # 项目配置
```

## 快速开始

### 环境要求
- 微信开发者工具
- 微信小程序开发账号

### 安装步骤

1. 克隆项目到本地
2. 使用微信开发者工具打开项目
3. 配置 AppID（在 project.config.json 中）
4. 修改 API 地址（在 utils/config.js 中）
5. 点击编译运行

### 配置说明

在 `utils/config.js` 中配置后端 API 地址：

```javascript
module.exports = {
  apiBaseURL: 'https://your-api-domain.com/v1',
  requestTimeout: 10000
};
```

## API 接口说明

项目需要后端提供以下 RESTful API 接口：

### 用户相关
- `POST /auth/login` - 用户登录
- `POST /auth/register` - 用户注册
- `GET /user/info` - 获取用户信息
- `PUT /user/info` - 更新用户信息

### 宠物管理
- `GET /pets` - 获取宠物列表
- `GET /pets/:id` - 获取宠物详情
- `POST /pets` - 创建宠物
- `PUT /pets/:id` - 更新宠物
- `DELETE /pets/:id` - 删除宠物

### 记录管理
- `GET /records/{type}` - 获取记录列表
- `GET /records/{type}/:id` - 获取记录详情
- `POST /records/{type}` - 创建记录
- `PUT /records/{type}/:id` - 更新记录
- `DELETE /records/{type}/:id` - 删除记录

type 包括：feeding, water, weight, medical, vaccination 等

### 提醒管理
- `GET /reminders` - 获取提醒列表
- `POST /reminders` - 创建提醒
- `PUT /reminders/:id` - 更新提醒
- `DELETE /reminders/:id` - 删除提醒

### 时光轴
- `GET /timeline` - 获取时光轴记录

## 设计规范

### 配色方案
- 主背景：#0F1117
- 卡片背景：#171A23
- 次级背景：#1E222E
- 主色调：#5B8CFF（蓝色）
- 辅助色：#22D3EE（青色）、#F87171（红色）、#FBBF24（黄色）
- 文字颜色：#E5E7EB（主文字）、#9CA3AF（次要文字）、#6B7280（提示文字）

### 组件规范
- 卡片圆角：12rpx
- 按钮圆角：12rpx
- 标准间距：32rpx
- 组件间距：16rpx
- 标准字体：28rpx
- 标题字体：32-36rpx

## 功能截图

（此处可添加小程序截图）

## 开发计划

- [x] 基础框架搭建
- [x] 首页功能
- [x] 宠物管理
- [x] 记录功能
- [x] 时光轴
- [x] 个人中心
- [ ] 提醒功能完善
- [ ] 数据统计图表
- [ ] 云端备份
- [ ] 数据导出

## 注意事项

1. 本项目仅包含前端代码，需要自行搭建后端服务
2. 请在正式使用前修改 API 地址
3. 图片资源需根据实际情况调整
4. 部分功能需要微信小程序权限（如相机、相册）

## 许可证

MIT License

## 联系方式

如有问题或建议，欢迎提交 Issue 或 Pull Request。
