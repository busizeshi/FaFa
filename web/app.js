App({
  globalData: {
    userInfo: null,
    token: null,
    currentPetId: null,
    baseUrl: 'https://your-api-domain.com/api',
    ossUrl: 'https://your-oss-domain.com'
  },

  onLaunch(options) {
    console.log('小程序启动', options);
    this.checkLoginStatus();
    this.initSystemInfo();
  },

  onShow(options) {
    console.log('小程序显示', options);
  },

  onHide() {
    console.log('小程序隐藏');
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    const currentPetId = wx.getStorageSync('currentPetId');

    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.currentPetId = currentPetId;
    }
  },

  initSystemInfo() {
    const systemInfo = wx.getSystemInfoSync();
    this.globalData.systemInfo = systemInfo;
    this.globalData.statusBarHeight = systemInfo.statusBarHeight;
    this.globalData.navBarHeight = systemInfo.statusBarHeight + 44;
  },

  login() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            this.getUserToken(res.code).then(resolve).catch(reject);
          } else {
            reject(new Error('获取code失败'));
          }
        },
        fail: reject
      });
    });
  },

  getUserToken(code) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${this.globalData.baseUrl}/auth/login`,
        method: 'POST',
        data: { code },
        success: (res) => {
          if (res.data.code === 200) {
            const { token, userInfo } = res.data.data;
            this.globalData.token = token;
            this.globalData.userInfo = userInfo;
            wx.setStorageSync('token', token);
            wx.setStorageSync('userInfo', userInfo);
            resolve(userInfo);
          } else {
            reject(new Error(res.data.message));
          }
        },
        fail: reject
      });
    });
  },

  setCurrentPet(petId) {
    this.globalData.currentPetId = petId;
    wx.setStorageSync('currentPetId', petId);
  },

  getCurrentPet() {
    return this.globalData.currentPetId || wx.getStorageSync('currentPetId');
  },

  logout() {
    this.globalData.token = null;
    this.globalData.userInfo = null;
    this.globalData.currentPetId = null;
    wx.clearStorageSync();
    wx.reLaunch({ url: '/pages/login/index' });
  }
});
