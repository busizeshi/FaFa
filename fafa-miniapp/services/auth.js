const config = require('../config');
const { request } = require('../utils/request');
const storage = require('../utils/storage');

function login() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(result) {
        if (!result.code) { reject({ message: '微信登录凭证获取失败' }); return; }
        request({ url: config.endpoints.auth.login, method: 'POST', data: { code: result.code }, auth: false })
          .then((session) => {
            storage.saveSession(session.token, { id: session.userId, newUser: session.newUser });
            resolve(session);
          })
          .catch(reject);
      },
      fail() { reject({ message: '微信登录失败，请稍后重试' }); }
    });
  });
}

function logout() {
  return request({ url: config.endpoints.auth.logout, method: 'POST' })
    .finally(storage.clearSession);
}

module.exports = { login, logout };
