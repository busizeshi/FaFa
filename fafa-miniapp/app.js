const config = require('./config');

App({
  globalData: {
    user: null,
    currentPet: null,
    authRedirecting: false
  },

  onLaunch() {
    this.globalData.user = wx.getStorageSync(config.storage.user) || null;
    this.globalData.currentPet = wx.getStorageSync(config.storage.currentPet) || null;
  },

  setCurrentPet(pet) {
    this.globalData.currentPet = pet || null;
    if (pet) wx.setStorageSync(config.storage.currentPet, pet);
    else wx.removeStorageSync(config.storage.currentPet);
  },

  handleAuthExpired() {
    if (this.globalData.authRedirecting) return;
    this.globalData.authRedirecting = true;
    wx.removeStorageSync(config.storage.token);
    wx.removeStorageSync(config.storage.user);
    wx.removeStorageSync(config.storage.currentPet);
    this.globalData.user = null;
    this.globalData.currentPet = null;
    wx.showToast({ title: '登录状态已失效，请重新登录', icon: 'none' });
    wx.reLaunch({
      url: config.routes.login,
      complete: () => { this.globalData.authRedirecting = false; }
    });
  }
});
