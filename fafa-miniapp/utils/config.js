const config = Object.freeze({
  defaultBaseUrl: 'http://localhost:8080',
  loginPath: '/auth/wechat/login',
  loginTimeoutMs: 10000,
  requestTimeoutMs: 15000,
  storageKeys: Object.freeze({
    baseUrl: 'fafa_debug_base_url',
    token: 'fafa_debug_token',
  }),
});

module.exports = config;
