const config = require('./config');

class Request {
  constructor() {
    this.baseURL = config.apiBaseURL;
    this.timeout = config.requestTimeout;
  }

  request(options) {
    return new Promise((resolve, reject) => {
      const { url, method = 'GET', data, header = {} } = options;

      wx.showLoading({ title: '加载中...', mask: true });

      wx.request({
        url: this.baseURL + url,
        method,
        data,
        header: {
          'Content-Type': 'application/json',
          'Authorization': wx.getStorageSync('token') || '',
          ...header
        },
        timeout: this.timeout,
        success: (res) => {
          wx.hideLoading();
          
          if (res.statusCode === 200) {
            if (res.data.code === 0) {
              resolve(res.data.data);
            } else {
              wx.showToast({
                title: res.data.message || '请求失败',
                icon: 'none'
              });
              reject(res.data);
            }
          } else if (res.statusCode === 401) {
            wx.showToast({
              title: '请先登录',
              icon: 'none'
            });
            setTimeout(() => {
              wx.reLaunch({ url: '/pages/login/index' });
            }, 1500);
            reject(res.data);
          } else {
            wx.showToast({
              title: '请求失败',
              icon: 'none'
            });
            reject(res.data);
          }
        },
        fail: (error) => {
          wx.hideLoading();
          wx.showToast({
            title: '网络错误',
            icon: 'none'
          });
          reject(error);
        }
      });
    });
  }

  get(url, data) {
    return this.request({ url, method: 'GET', data });
  }

  post(url, data) {
    return this.request({ url, method: 'POST', data });
  }

  put(url, data) {
    return this.request({ url, method: 'PUT', data });
  }

  delete(url, data) {
    return this.request({ url, method: 'DELETE', data });
  }
}

const request = new Request();

module.exports = request;
