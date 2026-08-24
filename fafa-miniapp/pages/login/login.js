/**
 * 登录页：微信登录 code 获取与后端联调测试
 *
 * 两种用途：
 * 1. 获取 code：wx.login() 得到临时 code，一键复制到剪贴板 → 去 Apifox 调试
 * 2. 一键登录：wx.login() 取 code → POST /auth/wechat/login 换取 token
 */
const { request } = require('../../utils/request');
const config = require('../../utils/config');

Page({
  data: {
    baseUrl: config.baseUrl,
    code: '',
    getting: false,
    copying: false,
    logging: false,
    loginResult: ''
  },

  onLoad() {
    // 已登录则直接进入首页
    if (wx.getStorageSync('token')) {
      wx.reLaunch({ url: '/pages/index/index' });
      return;
    }
    // 进入页面自动获取一次 code（仅获取，不自动登录，便于复制到 Apifox）
    this.getCode();
  },

  // 仅获取 code：wx.login 拿到临时 code 展示在页面上，不登录
  getCode() {
    if (this.data.getting) return;
    this.setData({ getting: true, code: '', loginResult: '' });

    wx.login({
      success: (res) => {
        // res.code 为空通常是 AppID 未生效（游客模式/测试号），见页面提示
        if (!res.code) {
          this.setData({
            getting: false,
            loginResult: 'wx.login 未返回 code：请确认 project.config.json 中 appid 与后端 WECHAT_APPID 一致，并退出游客模式'
          });
          return;
        }
        this.setData({ code: res.code, getting: false });
      },
      fail: (err) => {
        this.setData({ getting: false, loginResult: 'wx.login 调用失败：' + JSON.stringify(err) });
      }
    });
  },

  // 一键复制 code 到剪贴板，供 Apifox 手动调试
  copyCode() {
    if (!this.data.code) {
      wx.showToast({ title: '暂无可复制的 code，请先获取', icon: 'none' });
      return;
    }
    this.setData({ copying: true });
    wx.setClipboardData({
      data: this.data.code,
      success: () => {
        wx.showToast({ title: '已复制，去 Apifox 调试吧', icon: 'success' });
      },
      fail: () => {
        wx.showToast({ title: '复制失败', icon: 'none' });
      },
      complete: () => {
        this.setData({ copying: false });
      }
    });
  },

  // 一键登录：wx.login 取 code 并交给后端
  autoLogin() {
    if (this.data.logging) return;
    this.setData({ logging: true, loginResult: '' });

    // 若尚未获取 code，先获取再登录
    const doLogin = (code) => {
      if (!code) {
        this.setData({ logging: false });
        return;
      }
      this.setData({ code });
      this.doLogin(code);
    };

    if (this.data.code) {
      doLogin(this.data.code);
      return;
    }

    wx.login({
      success: (res) => doLogin(res.code),
      fail: (err) => {
        this.setData({ logging: false, loginResult: 'wx.login 调用失败：' + JSON.stringify(err) });
      }
    });
  },

  // 将 code 发送给后端换取 token
  doLogin(code) {
    request({
      url: '/auth/wechat/login',
      method: 'POST',
      data: { code }
    })
      .then((data) => {
        wx.setStorageSync('token', data.token);
        wx.setStorageSync('userId', data.userId);
        this.setData({
          logging: false,
          loginResult: '登录成功！newUser=' + data.newUser
        });
        wx.showToast({ title: '登录成功', icon: 'success' });
        setTimeout(() => {
          wx.reLaunch({ url: '/pages/index/index' });
        }, 800);
      })
      .catch((err) => {
        this.setData({
          logging: false,
          loginResult: '登录失败：' + (err && err.message ? err.message : JSON.stringify(err))
        });
      });
  }
});
