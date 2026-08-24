const { request } = require('../../utils/request');
const config = require('../../utils/config');

Page({
  data: {
    petId: null,
    foods: [],
    task: null,
    loading: false,
    recognizing: false
  },

  onLoad(options) {
    this.setData({ petId: Number(options.petId || 0) });
    this.loadFoods();
  },

  loadFoods() {
    if (!this.data.petId) return;
    request({ url: '/api/foods', data: { petId: this.data.petId, status: 'active' } })
      .then((foods) => this.setData({ foods: foods || [] }))
      .catch(() => {});
  },

  choosePackageImage() {
    if (this.data.recognizing) return;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (result) => {
        const file = result.tempFiles && result.tempFiles[0];
        if (file && file.tempFilePath) this.uploadPackage(file.tempFilePath);
      }
    });
  },

  uploadPackage(filePath) {
    this.setData({ recognizing: true });
    const token = wx.getStorageSync('token');
    wx.uploadFile({
      url: config.baseUrl + '/api/photos/upload',
      filePath,
      name: 'files',
      formData: { petId: String(this.data.petId) },
      header: { Authorization: token || '' },
      success: (res) => {
        try {
          const body = JSON.parse(res.data);
          if (body.code !== 0 || !body.data || !body.data.length) throw new Error(body.message || '上传失败');
          this.createRecognition(body.data[0].id);
        } catch (error) {
          this.setData({ recognizing: false });
          wx.showToast({ title: error.message, icon: 'none' });
        }
      },
      fail: () => {
        this.setData({ recognizing: false });
        wx.showToast({ title: '上传失败', icon: 'none' });
      }
    });
  },

  createRecognition(mediaId) {
    request({
      url: '/api/foods/recognitions',
      method: 'POST',
      data: { petId: this.data.petId, mediaId, idempotencyKey: 'miniapp-' + mediaId }
    }).then((task) => {
      this.setData({ task });
      this.pollRecognition(task.taskId);
    }).catch(() => this.setData({ recognizing: false }));
  },

  pollRecognition(taskId, attempt = 0) {
    request({ url: '/api/foods/recognitions/' + taskId })
      .then((task) => {
        this.setData({ task });
        if (task.status === 'completed' || task.status === 'failed' || task.status === 'rejected' || task.status === 'expired') {
          this.setData({ recognizing: false });
          return;
        }
        if (attempt < 30) setTimeout(() => this.pollRecognition(taskId, attempt + 1), 2000);
      }).catch(() => this.setData({ recognizing: false }));
  },

  confirmRecognition() {
    const task = this.data.task;
    const draft = task && task.draft;
    if (!task || task.status !== 'completed' || !draft || !draft.name || !draft.foodType) {
      wx.showToast({ title: '识别字段不足，请手工录入', icon: 'none' });
      return;
    }
    const payload = Object.assign({}, draft, { name: draft.name, foodType: draft.foodType });
    delete payload.confidence;
    request({
      url: '/api/foods/recognitions/' + task.taskId + '/confirm',
      method: 'POST',
      data: payload
    }).then(() => {
      wx.showToast({ title: '食品卡已保存', icon: 'success' });
      this.setData({ task: null });
      this.loadFoods();
    }).catch(() => {});
  },

  rejectRecognition() {
    const task = this.data.task;
    if (!task) return;
    request({ url: '/api/foods/recognitions/' + task.taskId + '/reject', method: 'POST', data: {} })
      .then(() => this.setData({ task: null, recognizing: false }))
      .catch(() => {});
  },

  openPlan() {
    wx.navigateTo({ url: '/pages/feeding-plan/today?petId=' + this.data.petId });
  }
});
