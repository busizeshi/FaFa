const config = require('../../config');
const auth = require('../../services/auth');
const userService = require('../../services/user');
const petService = require('../../services/pet');
const storage = require('../../utils/storage');
const privacy = require('../../utils/privacy');

Page({
  data: { loading: false, error: '' },

  onLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true, error: '' });
    wx.showLoading({ title: '正在登录' });
    privacy.authorizePrivacy()
      .then(() => auth.login())
      .then(() => userService.getMe())
      .then((profile) => {
        wx.setStorageSync(config.storage.user, profile);
        getApp().globalData.user = profile;
        return petService.list();
      })
      .then((pets) => {
        const list = Array.isArray(pets) ? pets : [];
        if (list.length) {
          const current = list[0];
          getApp().setCurrentPet(current);
          wx.switchTab({ url: config.routes.home });
          return;
        }
        getApp().setCurrentPet(null);
        wx.redirectTo({ url: config.routes.petCreate });
      })
      .catch((error) => this.setData({ error: error.message || (error.errMsg && error.errMsg.indexOf('cancel') >= 0 ? '请阅读并同意隐私保护指引后继续' : '登录失败，请稍后重试') }))
      .finally(() => {
        wx.hideLoading();
        this.setData({ loading: false });
      });
  },

  onPrivacyTap() { privacy.openPrivacyContract(); },

  onLoad() {
    if (storage.getToken()) {
      userService.getMe()
        .then((profile) => {
          wx.setStorageSync(config.storage.user, profile);
          getApp().globalData.user = profile;
          wx.switchTab({ url: config.routes.home });
        })
        .catch(() => storage.clearSession());
    }
  }
});
