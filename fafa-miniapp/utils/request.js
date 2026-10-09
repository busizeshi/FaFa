const config = require('../config');
const storage = require('./storage');

function queryString(params) {
  if (!params) return '';
  const pairs = [];
  Object.keys(params).forEach((key) => {
    const value = params[key];
    if (value === undefined || value === null || value === '') return;
    (Array.isArray(value) ? value : [value]).forEach((item) => {
      pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(item)}`);
    });
  });
  return pairs.length ? `?${pairs.join('&')}` : '';
}

function normalizeError(error) {
  if (error && error.errMsg) {
    return { code: 'NETWORK_ERROR', message: '网络连接失败，请检查网络后重试' };
  }
  return error || { code: 'UNKNOWN_ERROR', message: '请求失败，请稍后重试' };
}

function request(options) {
  const baseUrl = options.baseUrl || config.apiBaseUrl;
  if (!baseUrl) return Promise.reject({ code: 'CONFIG_ERROR', message: '请先配置 Java 服务地址' });
  const query = queryString(options.params);
  const token = storage.getToken();
  const header = Object.assign({ Accept: 'application/json' }, options.header || {});
  if (options.auth !== false && token) header[config.request.authHeader] = token;
  if (options.data && !(options.data instanceof ArrayBuffer) && !options.formData) {
    header['Content-Type'] = 'application/json';
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${baseUrl}${options.url}${query}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: options.timeout || config.request.timeout,
      success(response) {
        const body = response.data || {};
        if (response.statusCode === 401 || body.code === 401 || body.code === 1002) {
          if (options.auth !== false) getApp().handleAuthExpired();
          reject({ code: 'AUTH_EXPIRED', message: body.message || '登录状态已失效' });
          return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject({ code: body.code || response.statusCode, message: body.message || '服务暂时不可用' });
          return;
        }
        if (Object.prototype.hasOwnProperty.call(body, 'code')) {
          if (body.code !== 0) {
            reject({ code: body.code, message: body.message || '操作失败', data: body.data });
            return;
          }
          resolve(body.data);
          return;
        }
        resolve(body);
      },
      fail(error) { reject(normalizeError(error)); }
    });
  });
}

function upload(options) {
  const baseUrl = config.apiBaseUrl;
  if (!baseUrl) return Promise.reject({ code: 'CONFIG_ERROR', message: '请先配置 Java 服务地址' });
  const header = {};
  const token = storage.getToken();
  if (token) header[config.request.authHeader] = token;
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${baseUrl}${options.url}${queryString(options.params)}`,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData,
      header,
      timeout: options.timeout || config.request.timeout,
      success(response) {
        let body;
        try { body = JSON.parse(response.data || '{}'); }
        catch (_) { reject({ code: 'BAD_RESPONSE', message: '服务器返回格式异常' }); return; }
        if (response.statusCode === 401 || body.code === 1002) {
          getApp().handleAuthExpired();
          reject({ code: 'AUTH_EXPIRED', message: '登录状态已失效' });
          return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300 || (body.code !== undefined && body.code !== 0)) {
          reject({ code: body.code || response.statusCode, message: body.message || '上传失败' });
          return;
        }
        resolve(body.data);
      },
      fail(error) { reject(normalizeError(error)); }
    });
  });
}

module.exports = { request, upload, queryString };
