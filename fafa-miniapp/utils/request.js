const config = require('./config');

function normalizeBaseUrl(value) {
  return String(value || '').trim().replace(/\/+$/, '');
}

function getBaseUrl() {
  const savedBaseUrl = wx.getStorageSync(config.storageKeys.baseUrl);
  return normalizeBaseUrl(savedBaseUrl || config.defaultBaseUrl);
}

function request({ url, method = 'GET', data, useToken = true }) {
  const token = wx.getStorageSync(config.storageKeys.token);
  const header = { 'Content-Type': 'application/json' };

  if (useToken && token) {
    header.Authorization = token;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${getBaseUrl()}${url}`,
      method,
      data,
      timeout: config.requestTimeoutMs,
      header,
      success(response) {
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`HTTP ${response.statusCode}`));
          return;
        }
        resolve(response.data);
      },
      fail(error) {
        reject(new Error(error.errMsg || '网络请求失败'));
      },
    });
  });
}

module.exports = { normalizeBaseUrl, request };
