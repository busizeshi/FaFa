const app = getApp();
const { petApi, timelineApi, reminderApi, aiApi } = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    currentPet: null,
    todayFeedings: [],
    todayReminders: [],
    aiDiscovery: null,
    memoryPhoto: null,
    loading: true,
    quickActions: [
      { id: 'feeding', name: '喂食', icon: '🍚', url: '/pages/record/feeding/edit' },
      { id: 'water', name: '饮水', icon: '💧', url: '/pages/record/water/edit' },
      { id: 'weight', name: '体重', icon: '⚖️', url: '/pages/record/weight/edit' },
      { id: 'excretion', name: '排便', icon: '💩', url: '/pages/record/excretion/edit' },
      { id: 'photo', name: '照片', icon: '📷', url: '/pages/archive/photo/upload' },
      { id: 'event', name: '事件', icon: '📝', url: '/pages/record/event/edit' }
    ]
  },

  onLoad() {
    this.checkLogin();
  },

  onShow() {
    const petId = app.getCurrentPet();
    if (petId) {
      this.loadPageData();
    } else {
      this.navigateToCreatePet();
    }
  },

  checkLogin() {
    if (!app.globalData.token) {
      wx.reLaunch({ url: '/pages/login/index' });
    }
  },

  navigateToCreatePet() {
    wx.showModal({
      title: '提示',
      content: '还没有宠物档案，立即创建？',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/pet/create' });
        }
      }
    });
  },

  async loadPageData() {
    const petId = app.getCurrentPet();
    
    try {
      this.setData({ loading: true });

      const [pet, todayData, reminders, discoveries] = await Promise.all([
        petApi.getDetail(petId),
        timelineApi.getList(petId, { date: util.formatDate(new Date()), limit: 10 }),
        reminderApi.getList(petId, { status: 'pending', dueDate: util.formatDate(new Date()) }),
        aiApi.getDiscoveries(petId, { isRead: false, limit: 1 })
      ]);

      const todayFeedings = todayData.filter(item => item.type === 'feeding');
      const aiDiscovery = discoveries.length > 0 ? discoveries[0] : null;

      this.setData({
        currentPet: pet,
        todayFeedings,
        todayReminders: reminders,
        aiDiscovery,
        loading: false
      });

      this.loadMemoryPhoto(petId);
    } catch (error) {
      console.error('加载首页数据失败:', error);
      this.setData({ loading: false });
    }
  },

  async loadMemoryPhoto(petId) {
    try {
      const lastYear = new Date();
      lastYear.setFullYear(lastYear.getFullYear() - 1);
      const dateStr = util.formatDate(lastYear);
      
      const photos = await photoApi.getList(petId, { 
        date: dateStr, 
        limit: 1 
      });

      if (photos.length > 0) {
        this.setData({ memoryPhoto: photos[0] });
      }
    } catch (error) {
      console.error('加载历史照片失败:', error);
    }
  },

  onPetSwitch() {
    wx.navigateTo({ url: '/pages/pet/list' });
  },

  onQuickAction(e) {
    const { url } = e.currentTarget.dataset;
    if (url === '/pages/archive/photo/upload') {
      this.choosePhoto();
    } else {
      wx.navigateTo({ url });
    }
  },

  async choosePhoto() {
    try {
      const res = await wx.chooseMedia({
        count: 9,
        mediaType: ['image'],
        sizeType: ['compressed']
      });
      
      wx.navigateTo({
        url: '/pages/archive/photo/upload',
        success: (res) => {
          res.eventChannel.emit('photos', { tempFiles: res.tempFiles });
        }
      });
    } catch (error) {
      console.error('选择照片失败:', error);
    }
  },

  async onQuickFeeding() {
    try {
      wx.showLoading({ title: '记录中...' });
      const petId = app.getCurrentPet();
      await feedingApi.quickCreate(petId);
      wx.hideLoading();
      util.showSuccess('记录成功');
      this.loadPageData();
    } catch (error) {
      wx.hideLoading();
      util.showError('快捷喂食失败，请使用完整表单');
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/record/feeding/edit' });
      }, 1500);
    }
  },

  async onCompleteReminder(e) {
    const { id } = e.currentTarget.dataset;
    
    const confirm = await util.showModal('确认完成', '确认已完成此提醒事项？');
    if (!confirm) return;

    try {
      await reminderApi.complete(id, {});
      util.showSuccess('已完成');
      this.loadPageData();
    } catch (error) {
      util.showError('操作失败');
    }
  },

  onAiDiscovery() {
    const { aiDiscovery } = this.data;
    if (aiDiscovery) {
      wx.navigateTo({ 
        url: `/pages/ai/chat?context=discovery&discoveryId=${aiDiscovery.id}` 
      });
    }
  },

  onMemoryPhoto() {
    const { memoryPhoto } = this.data;
    if (memoryPhoto) {
      wx.navigateTo({ url: `/pages/archive/photo/detail?id=${memoryPhoto.id}` });
    }
  },

  onPullDownRefresh() {
    this.loadPageData().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  formatDate: util.formatDate,
  formatTime: util.formatTime,
  getAge: util.getAge,
  getDaysSince: util.getDaysSince
});
