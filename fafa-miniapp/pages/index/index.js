const config = require('../../utils/config');
const { normalizeBaseUrl, request } = require('../../utils/request');

function getConfiguredBaseUrl() {
  const savedBaseUrl = wx.getStorageSync(config.storageKeys.baseUrl);
  return normalizeBaseUrl(savedBaseUrl || config.defaultBaseUrl);
}

function formatResponse(value) {
  return JSON.stringify(value, null, 2);
}

function formatLoginRequest(code) {
  return formatResponse({ code });
}

Page({
  data: {
    baseUrl: getConfiguredBaseUrl(),
    loginPath: config.loginPath,
    code: '',
    requestBody: formatLoginRequest(''),
    token: wx.getStorageSync(config.storageKeys.token) || '',
    loginResult: null,
    loginResponseText: '',
    isLoadingCode: false,
    isLoggingIn: false,
    statusType: 'idle',
    statusText: '准备获取 code',
  },

  onLoad() {
    this.refreshCode();
  },

  onBaseUrlInput(event) {
    this.setData({ baseUrl: event.detail.value });
  },

  saveBaseUrl(event) {
    const shouldShowToast = typeof event !== 'boolean' || event;
    const baseUrl = normalizeBaseUrl(this.data.baseUrl);
    if (!/^https?:\/\//i.test(baseUrl)) {
      wx.showToast({ title: '请输入 http:// 或 https:// 地址', icon: 'none' });
      return false;
    }

    wx.setStorageSync(config.storageKeys.baseUrl, baseUrl);
    this.setData({ baseUrl });
    if (shouldShowToast) {
      wx.showToast({ title: '地址已保存', icon: 'success' });
    }
    return true;
  },

  refreshCode() {
    if (this.data.isLoadingCode) {
      return;
    }

    this.setData({
      code: '',
      requestBody: formatLoginRequest(''),
      loginResult: null,
      loginResponseText: '',
      isLoadingCode: true,
      statusType: 'loading',
      statusText: '正在获取新的 code',
    });

    wx.login({
      timeout: config.loginTimeoutMs,
      success: (result) => this.handleLoginCode(result),
      fail: (error) => this.handleCodeError(error),
      complete: () => this.setData({ isLoadingCode: false }),
    });
  },

  handleLoginCode(result) {
    if (!result || !result.code) {
      this.handleCodeError({ errMsg: '微信未返回有效 code' });
      return;
    }

    this.setData({
      code: result.code,
      requestBody: formatLoginRequest(result.code),
      statusType: 'success',
      statusText: 'code 已生成，可以复制或登录',
    });
  },

  handleCodeError(error) {
    const message = error && error.errMsg ? error.errMsg : '获取 code 失败';
    this.setData({ statusType: 'error', statusText: message });
    wx.showToast({ title: message, icon: 'none' });
  },

  login() {
    if (!this.data.code || this.data.isLoggingIn || !this.saveBaseUrl(false)) {
      return;
    }

    this.setData({
      isLoggingIn: true,
      statusType: 'loading',
      statusText: '正在调用登录接口',
      loginResponseText: '',
      loginResult: null,
    });

    request({
      url: config.loginPath,
      method: 'POST',
      data: { code: this.data.code },
      useToken: false,
    }).then((result) => this.handleLoginSuccess(result))
      .catch((error) => this.handleLoginError(error))
      .then(() => this.setData({ isLoggingIn: false }));
  },

  handleLoginSuccess(result) {
    const loginData = result && result.data;
    if (!result || result.code !== 0 || !loginData || !loginData.token) {
      throw new Error((result && result.message) || '登录响应格式异常');
    }

    wx.setStorageSync(config.storageKeys.token, loginData.token);
    this.setData({
      token: loginData.token,
      loginResult: loginData,
      loginResponseText: formatResponse(result),
      statusType: 'success',
      statusText: '登录成功，token 已保存',
    });
    wx.showToast({ title: '登录成功', icon: 'success' });
  },

  handleLoginError(error) {
    const message = error && error.message ? error.message : '登录请求失败';
    this.setData({
      loginResponseText: formatResponse({ error: message }),
      statusType: 'error',
      statusText: '登录失败',
    });
    wx.showToast({ title: message, icon: 'none' });
  },

  copyCode() {
    this.copyText(this.data.code, 'code 已复制');
  },

  copyLoginBody() {
    this.copyText(this.data.requestBody, '请求体已复制');
  },

  copyToken() {
    this.copyText(this.data.token, 'token 已复制');
  },

  copyAuthorization() {
    this.copyText(`Authorization: ${this.data.token}`, '请求头已复制');
  },

  copyText(text, successTitle) {
    if (!text) {
      wx.showToast({ title: '暂无可复制内容', icon: 'none' });
      return;
    }

    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: successTitle, icon: 'success' }),
    });
  },
});
