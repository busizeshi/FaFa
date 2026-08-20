export const config = {
  // 真机联调时改为可被微信小程序信任的 HTTPS 域名。
  apiBaseUrl: '',
  // 后端未启动时保持 true，界面使用本地演示数据；联调时改为 false。
  mock: true,  // 改回 true，避免调用接口超时
  requestTimeout: 12000
}
