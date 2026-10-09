# FaFa 视觉系统更新（晴日陪伴 × 鼠尾草）

本轮直接改造 pages/ 内现有原型，保留各页 DOM、布局、交互钩子、线性图标结构和新魏字体层级。页面主题变量统一来自 colors_and_type.css；静态独立页同时内嵌主题和 visual-enhancements.css，因此页面可直接预览。

## 色板

| 用途 | Token | 色值 |
|---|---|---|
| 品牌主色、选中态、描边图标 | --fafa-primary | #5C9E71 |
| 白字主按钮 | --fafa-primary-action | #3E7A52 |
| 页面底色 | --fafa-background | #FAFBF7 |
| 主文字 | --fafa-foreground | #293A31 |
| 卡片 | --fafa-card | #FFFFFF |
| 次级文字 | --fafa-muted-foreground | #65796B |
| 细线边框 | --fafa-border | #E2E9DF |
| 健康 / 日常薄荷 | --fafa-accent-mint / --fafa-accent-mint-soft | #468B79 / #E8F4EF |
| 影像 / 搜索雾蓝 | --fafa-accent-sky / --fafa-accent-sky-soft | #4E88A2 / #EAF4F8 |
| 提醒 / 活动杏橘 | --fafa-accent-coral / --fafa-accent-coral-soft | #D97B61 / #FFF0EA |
| 喂养 / 体重暖金 | --fafa-accent-amber / --fafa-accent-amber-soft | #B77B24 / #FFF4DE |
| AI / 助手雾紫 | --fafa-accent-lavender / --fafa-accent-lavender-soft | #7869A6 / #F1EDFA |

页面背景使用奶油白底上的低透明暖金与薄荷光晕。AI 卡片使用薄荷—雾青—雾紫的柔和渐变；主按钮使用深鼠尾草绿渐变，保证白色按钮文案清晰。辅助色主要用于线性图标、状态和浅色徽标，不用于大面积铺色。

## 圆角、阴影、插画与图标

- 保留现有圆角刻度：4 / 8 / 12 / 9999px。保留卡片细边框，不加粗描边。
- 卡片仅添加浅灰绿软阴影；图标容器加一层内高光。现有猫咪照片继续用作真实内容主视觉，不叠加装饰插画。
- 继续使用项目已有的 Lucide 描边图标。健康图标偏杏橘、素材与搜索图标偏雾蓝、AI 图标偏雾紫、喂养与体重图标偏暖金；形状与线宽保持统一。
- 玻璃效果仅用于顶部栏、底部导航、搜索/输入控件和 AI 输入条。表面底色不依赖模糊；运行环境不支持 backdrop-filter 时仍保持高不透明度和对比度。
- 字体名、字体回退顺序、字号、字重与行高全部沿用现有设计；本轮没有复制或嵌入字体文件。

## 核心页面应用

- 首页 home-sage.html：宠物照片卡和信息排列保持原样；三个健康指标以暖金、雾蓝、杏橘区分，四个快捷入口用薄荷、雾蓝、杏橘、雾紫；AI 卡片承担主要渐变焦点。
- 健康模块（宠物详情、体重、就医、用药与预警页）：数据卡结构不变；健康、提醒、记录类图标采用杏橘与鼠尾草语义色，主操作统一使用高对比度深绿。
- 素材页 album.html：保留三列照片网格和批量管理布局；搜索/上传控件更通透，素材、相机、搜索类图标采用雾蓝。
- AI 助手 chat.html：保留原消息流与建议问题布局；AI 头像、智能图标与置信度提示采用雾紫，用户消息气泡用深绿渐变，输入条呈毛玻璃效果。

## 登录页延展

- 登录页 login.html 改为全屏 9:16 动漫插画背景，顶部保留 FaFa 标识，底部使用半透明磨砂登录面板；微信一键登录按钮保留原协议勾选区。
- 背景素材 assets/login-cover-anime.jpg 为 936×1664 JPG，CSS 使用 cover 铺满移动端视口，手机比例不同时采用居中偏左裁切以尽量留住左侧熟睡猫咪。
- 素材由 ImageGen 生成。OpenAI 使用条款将输出归用户所有（在适用法律允许范围内），同时说明输出不保证唯一；用于商业投放前仍应进行常规素材审查：[OpenAI 使用条款](https://openai.com/policies/terms-of-use/)。

## 商用交付核对

- 原型现有图标来自 Lucide CDN；Lucide 官方仓库说明其可用于商业项目，许可为 ISC，并在许可文件中列明少量 Feather 衍生图标的 MIT 许可。正式打包时应保留所使用版本对应的第三方许可文本及版权声明：[Lucide README](https://github.com/lucide-icons/lucide) / [Lucide LICENSE](https://github.com/lucide-icons/lucide/blob/main/LICENSE)。
- 本轮保留 STXinwei / 华文新魏字体栈，没有嵌入字体文件。字体权利方页面说明将华文新魏用于微信、网站或 APP 等场景需取得相应授权；上线前请核实项目授权范围：[华文新魏授权说明](https://sinotype.vcg.com/font/10461)。
- 页面中的宠物照片沿用项目已有素材，投放前按各素材来源核对使用授权。
