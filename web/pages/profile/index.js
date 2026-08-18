const app = getApp();

Page({
  data: {
    userInfo: null,
    stats: {
      petsCount: 0,
      recordsCount: 0,
      daysCount: 0
    },
    menuItems: [
      {
        id: 'pets',
        icon: '🐾',
        title: '我的宠物',
        desc: '管理宠物信息',
        url: '/pages/pet/list'
      },
      {
        id: 'reminders',
        icon: '⏰',
        title: '提醒设置',
        desc: '管理各类提醒',
        url: '/pages/reminder/list'
      },
      {
        id: 'export',
        icon: '📤',
        title: '数据导出',
        desc: '导出记录数据',
        url: '/pages/profile/export'
      },
      {
        id: 'backup',
        icon: '☁️',
        title: '云端备份',
        desc: '备份与恢复',
        url: '/pages/profile/backup'
      },
      {
        id: 'about',
        icon: 'ℹ️',
        title: '关于我们',
        desc: '版本与帮助',
        url: '/pages/profile/about'
      }
    ]
  },

  onLoad() {
    this.loadUserInfo();
    this.loadStats();
  },

  onShow() {
    this.loadStats();
  },

  async loadUserInfo() {
    try {
      const userInfo = app.globalData.userInfo;
      this.setData({ userInfo });
    } catch (error) {
      console.error('加载用户信息失败:', error);
    }
  },

  async loadStats() {
    try {
      const userId = app.globalData.userId;
      if (!userId) return;

      // 获取宠物数量
      const pets = await app.getPets();
      
      // 获取记录数量和天数（这里简化处理，实际应该调用API）
      const petsCount = pets.length;
      const recordsCount = 156; // 示例数据
      const daysCount = 89; // 示例数据

      this.setData({
        stats: { petsCount, recordsCount, daysCount }
      });
    } catch (error) {
      console.error('加载统计信息失败:', error);
    }
  },

  onMenuTap(e) {
    const { url } = e.currentTarget.dataset;
    if (url) {
      wx.navigateTo({ url });
    }
  },

  onAvatarTap() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        this.uploadAvatar(res.tempFilePaths[0]);
      }
    });
  },

  async uploadAvatar(filePath) {
    try {
      wx.showLoading({ title: '上传中...' });
      // 这里应该调用上传API
      // const avatarUrl = await uploadFile(filePath);
      // await updateUserInfo({ avatar: avatarUrl });
      wx.hideLoading();
      wx.showToast({ title: '上传成功', icon: 'success' });
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: '上传失败', icon: 'none' });
    }
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout();
          wx.reLaunch({ url: '/pages/login/index' });
        }
      }
    });
  }
});
