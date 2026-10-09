const config = require('../../config');
const userService = require('../../services/user');
const petService = require('../../services/pet');

Page({
  data: { user: null, petCount: 0, quota: null, error: '' },
  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    if (tab) tab.setData({ selected: 3 });
    this.loadProfile();
  },
  loadProfile() {
    this.setData({ error: '' });
    Promise.all([userService.getMe(), petService.list(), userService.getQuota().catch(() => null)])
      .then(([user, pets, quota]) => {
        wx.setStorageSync(config.storage.user, user);
        getApp().globalData.user = user;
        const avatarLetter = (user.displayName || user.nickname || '我').slice(0, 1);
        this.setData({ user, avatarLetter, petCount: Array.isArray(pets) ? pets.length : 0, quota });
      })
      .catch((error) => this.setData({ error: error.message || '暂时无法加载个人信息' }));
  },
  goPets() { wx.navigateTo({ url: config.routes.pets }); },
  goProfile() { wx.navigateTo({ url: config.routes.profile }); },
  goFamily() { wx.navigateTo({ url: config.routes.family }); },
  goReports() { wx.navigateTo({ url: config.routes.reports }); },
  goExpenses() { wx.navigateTo({ url: config.routes.expenseBook }); },
  goMembership() { wx.navigateTo({ url: config.routes.membership }); },
  goReminders() { wx.navigateTo({ url: config.routes.reminders }); },
  onRetry() { this.loadProfile(); }
});
