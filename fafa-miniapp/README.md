# FaFa 微信登录调试小程序

这是一个面向本地联调的原生微信小程序，当前页面用于验证 Java 服务的：

```text
POST /auth/wechat/login
请求体：{"code":"wx.login() 返回值"}
```

## 使用方式

1. 使用微信开发者工具导入 `fafa-miniapp` 目录。
2. 默认 AppID 与 `fafa-java/src/main/resources/application-dev.yml` 中的开发配置一致；如果使用自己的 AppID，需要同时修改 `project.config.json` 和 Java 服务的 `WECHAT_APPID` / `WECHAT_SECRET`。
3. 启动 Java 服务后打开页面。开发者工具中默认后端地址是 `http://localhost:8080`，`project.config.json` 已关闭本地合法域名校验。
4. 点击“刷新 code”获取新凭证，再点击“复制 code”或“调用登录接口”。登录成功后，token 会保存在本机，可复制 `Authorization` 请求头到 Knife4j 或其他接口工具。

## 真机调试注意事项

- `wx.login()` 生成的 code 只能使用一次；刷新后必须使用最新值。
- 真机不能访问电脑的 `localhost`，请把页面中的 Base URL 改成电脑局域网 IP，例如 `http://192.168.1.10:8080`。
- 真机或正式环境还需要为小程序配置 HTTPS 合法域名；开发者工具的本地校验开关不等同于线上配置。
- Java 服务约定 `Authorization` 直接携带 token，不要添加 `Bearer ` 前缀。
