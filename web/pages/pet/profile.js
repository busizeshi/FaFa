const app = getApp();
const { petApi, timelineApi, photoApi } = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    petId: '',
    pet: null,
    statistics: {
      photoCount: 0,
      recordCount: 0,
      daysSince: 0
    },
    recentPhotos: [],
    recentRecords: [],
    loading: true
  },

  onLoad(options) {
    const { id } = options;
    if (id) {
      this.setData({ petId: id });
      this.loadPetProfile();
    }
  },

  onShow() {
    if (this.data.petId) {
      this.loadPetProfile();
    }
  },

  async loadPetProfile() {
    try {
      this.setData({ loading: true });
      const { petId } = this.data;

      const [pet, photos, records] = await Promise.all([
        petApi.getDetail(petId),
        photoApi.getList(petId, { limit: 6, sort: 'createdAt:desc' }),
        timelineApi.getList(petId, { limit: 10, sort: 'recordDate:desc' })
      ]);

      const daysSince = util.getDaysSince(pet.adoptDate || pet.birthDate);

      this.setData({
        pet,
        recentPhotos: photos,
        recentRecords: records,
        statistics: {
          photoCount: pet.photoCount || 0,
          recordCount: pet.recordCount || 0,
          daysSince
        },
        loading: false
      });
    } catch (error) {
      console.error('加载宠物资料失败:', error);
      this.setData({ loading: false });
    }
  },

  onEdit() {
    wx.navigateTo({ url: `/pages/pet/edit?id=${this.data.petId}` });
  },

  onViewAllPhotos() {
    wx.navigateTo({ url: `/pages/archive/photo/list?petId=${this.data.petId}` });
  },

  onViewTimeline() {
    wx.navigateTo({ url: `/pages/timeline/index?petId=${this.data.petId}` });
  },

  onPhotoTap(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/archive/photo/detail?id=${id}` });
  },

  onRecordTap(e) {
    const { id, type } = e.currentTarget.dataset;
    const typePages = {
      feeding: 'feeding',
      water: 'water',
      weight: 'weight',
      excretion: 'excretion',
      medication: 'medication',
      vaccination: 'vaccination',
      medical: 'medical',
      grooming: 'grooming',
      training: 'training',
      event: 'event'
    };

    const page = typePages[type] || 'event';
    wx.navigateTo({ url: `/pages/record/${page}/detail?id=${id}` });
  },

  async onDelete() {
    const confirm = await util.showModal(
      '确认删除', 
      `确定要删除${this.data.pet.name}的全部档案吗？此操作不可恢复。`
    );
    
    if (!confirm) return;

    try {
      wx.showLoading({ title: '删除中...', mask: true });
      await petApi.delete(this.data.petId);
      wx.hideLoading();
      
      if (app.getCurrentPet() === this.data.petId) {
        app.setCurrentPet(null);
      }
      
      util.showSuccess('删除成功');
      setTimeout(() => {
        wx.navigateBack();
      }, 1000);
    } catch (error) {
      wx.hideLoading();
      util.showError('删除失败');
    }
  },

  onShare() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },

  onShareAppMessage() {
    const { pet } = this.data;
    return {
      title: `我的宠物 ${pet.name}`,
      path: `/pages/pet/profile?id=${this.data.petId}`,
      imageUrl: pet.avatar
    };
  },

  onShareTimeline() {
    const { pet } = this.data;
    return {
      title: `我的宠物 ${pet.name}`,
      query: `id=${this.data.petId}`,
      imageUrl: pet.avatar
    };
  },

  onPullDownRefresh() {
    this.loadPetProfile().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  getAge: util.getAge,
  getSpeciesText: util.getSpeciesText,
  getGenderText: util.getGenderText,
  formatDate: util.formatDate,
  getRecordTypeIcon: util.getRecordTypeIcon,
  getRecordTypeText: util.getRecordTypeText
});
