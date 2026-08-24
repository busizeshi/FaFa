/**
 * 首页：后端联调测试
 *
 * 调用受保护接口验证登录态：
 * - GET /user/me     当前用户信息
 * - GET /api/pets    宠物列表
 * - POST /auth/logout 登出
 */
const { request } = require('../../utils/request');

Page({
  data: {
    userId: '',
    userInfo: null,
    pets: [],
    loading: false
  },

  onShow() {
    this.setData({ userId: wx.getStorageSync('userId') || '' });
    this.loadAll();
  },

  loadAll() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    const p1 = request({ url: '/user/me' })
      .then((data) => this.setData({ userInfo: data }))
      .catch(() => {});

    const p2 = request({ url: '/api/pets' })
      .then((data) => this.setData({ pets: data || [] }))
      .catch(() => {});

    Promise.all([p1, p2]).then(() => this.setData({ loading: false }));
  },

  refresh() {
    this.loadAll();
  },

  openAssistant() {
    const petId = this.data.pets.length ? this.data.pets[0].id : '';
    wx.navigateTo({ url: '/pages/chat/chat?petId=' + petId });
  },

  openMedical() {
    const petId = this.data.pets.length ? this.data.pets[0].id : '';
    wx.navigateTo({ url: '/pages/medical/medical?petId=' + petId });
  },

  openFood() {
    const petId = this.data.pets.length ? this.data.pets[0].id : '';
    wx.navigateTo({ url: '/pages/food/index?petId=' + petId });
  },

  openFeedingPlan() {
    const petId = this.data.pets.length ? this.data.pets[0].id : '';
    wx.navigateTo({ url: '/pages/feeding-plan/today?petId=' + petId });
  },

  openExpense() {
    const petId = this.data.pets.length ? this.data.pets[0].id : '';
    wx.navigateTo({ url: '/pages/expense/index?petId=' + petId });
  },

  openReport() {
    const petId = this.data.pets.length ? this.data.pets[0].id : '';
    wx.navigateTo({ url: '/pages/report/index?petId=' + petId });
  },

  openCollaboration() {
    const petId = this.data.pets.length ? this.data.pets[0].id : '';
    wx.navigateTo({ url: '/pages/collaboration/index?petId=' + petId });
  },

  openEmergencyCard() {
    const petId = this.data.pets.length ? this.data.pets[0].id : '';
    wx.navigateTo({ url: '/pages/emergency-card/index?petId=' + petId });
  },

  logout() {
    request({ url: '/auth/logout', method: 'POST' })
      .catch(() => {})
      .then(() => {
        wx.removeStorageSync('token');
        wx.removeStorageSync('userId');
        wx.reLaunch({ url: '/pages/login/login' });
      });
  }
});
