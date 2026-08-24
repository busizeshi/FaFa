/**
 * 请求封装
 *
 * 统一处理：
 * 1. 自动携带登录 token（Authorization 头，与 sa-token 约定一致）
 * 2. 解析后端统一响应结构 Result { code, message, data }：code=0 成功
 * 3. 401 会话失效 → 清理本地登录态并跳转登录页
 * 4. 错误统一 toast 提示
 */
const config = require('./config');

/**
 * 发起请求
 * @param {Object} options { url, method, data, header }
 * @returns {Promise<any>} resolve 出后端 data 字段
 */
function request(options) {
  const token = wx.getStorageSync('token');

  return new Promise((resolve, reject) => {
    wx.request({
      url: config.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data,
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: token } : {}),
        ...(options.header || {})
      },
      success(res) {
        if (res.statusCode === 200) {
          const body = res.data;
          if (body && body.code === 0) {
            resolve(body.data);
          } else {
            const msg = (body && body.message) || '请求失败';
            wx.showToast({ title: msg, icon: 'none' });
            reject(new Error(msg));
          }
        } else if (res.statusCode === 401) {
          // 会话失效：清理登录态并回到登录页
          wx.removeStorageSync('token');
          wx.removeStorageSync('userId');
          wx.reLaunch({ url: '/pages/login/login' });
          const msg = '登录已过期，请重新登录';
          wx.showToast({ title: msg, icon: 'none' });
          reject(new Error(msg));
        } else {
          const msg = 'HTTP ' + res.statusCode;
          wx.showToast({ title: msg, icon: 'none' });
          reject(new Error(msg));
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请确认后端服务已启动', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = { request };
