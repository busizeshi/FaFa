/**
 * 环境配置
 *
 * baseUrl：后端 fafa-java 服务地址。
 * - 本机启动后端：http://localhost:8080（微信开发者工具需在「详情-本地设置」勾选
 *   「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」）
 * - 真机调试：改为内网/公网可达的 IP，如 http://192.168.1.14:8080，并配置合法域名
 */
const config = {
  // 后端服务地址（默认本机 dev 环境）
  baseUrl: 'http://localhost:8080'
};

module.exports = config;
