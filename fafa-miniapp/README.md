# FaFa 微信小程序

原生微信小程序前端，连接本仓库 `fafa-java` Spring Boot 服务。根目录的 `project.config.json` 将 `miniprogramRoot` 指向本目录，可直接用微信开发者工具打开仓库根目录。

## 本地启动

1. 启动 `fafa-java`。开发配置默认请求 `http://192.168.1.15:8080`，对应 Docker 部署服务器的局域网地址。
2. 使用微信开发者工具打开 `D:\dev\Java\FaFa` 或 `D:\dev\Java\FaFa\fafa-miniapp`。两处都包含项目配置：根目录配置将小程序根目录指向 `fafa-miniapp/`，子目录配置则直接以当前目录作为小程序根目录。AppID 应与 Java 开发环境的微信 AppID 一致；AppSecret 只保留在 Java 服务端配置中。
3. 开发者工具本地配置允许 `urlCheck: false`；发布版本仍需在微信公众平台配置合法的 HTTPS request/uploadFile 域名。

模拟器默认读取 `config.js` 中的 development 地址，开发者工具项目配置允许本地调试时跳过域名校验。真机联调和发布版本需要可访问的 HTTPS API 地址，并在微信公众平台配置合法 request/uploadFile 域名；如果运行环境提供 ExtConfig，也可注入 `apiBaseUrl` 覆盖。测试、生产地址目前留空，避免误连到未确认的服务环境。

## 目录

- `config.js`：API 环境、接口路径、路由、请求参数默认值、上传限制和存储键。
- `services/`：按业务域封装 Java API。
- `utils/request.js`：统一处理请求、`Result<T>` 响应、鉴权过期和文件上传。
- `utils/multipart.js`：宠物创建时提交三张同名 `photos` 图片字段。
- `features/`：套餐页面的数据加载、表单提交和操作定义。
- `pages/`：登录、首页、相册、问答、我的四个主 tab 及登录入口。
- `packages/`：宠物、日常、喂养、相册、医疗和业务页面分包。
- `components/feature-view/`：复用业务页面的表单、列表、错误和加载状态。

## Java 接口约定

服务端返回 `{ code, message, data }`，成功码为 `0`。登录成功后，token 统一放入 `Authorization` 请求头。接口路径和通用参数集中放在 `config.js`，业务请求放在 `services/`。

宠物创建按后端契约上传 `name/type/breed/gender/birthDate/isNeutered/adoptDate` 及三张 `photos` 文件；相册上传逐文件调用 `/api/photos/upload`。食品识别使用后端 `taskId/draft/status` DTO，确认时再调用确认接口创建食品档案。

## 平台与适配

- 页面以原生 WXML/WXSS/JavaScript 实现，不引入 WebView 或跨端运行时。
- 页面和 tab 底部留出安全区域；样式使用 `rpx` 适配不同屏宽。
- 相机/相册操作接入微信隐私授权 API，并保留隐私指引入口。
- 线上请求必须使用公众平台配置的 HTTPS 合法域名；请将已部署 Java 服务的真实域名配置到对应环境后再真机或线上联调。

## 暂未接通的后端能力

Java 当前没有微信支付下单与回调流程，因此会员页面只展示状态，不开放升级/续费按钮。微信订阅消息投递也尚无可调用服务端流程；提醒数据本身已接入提醒 API。
