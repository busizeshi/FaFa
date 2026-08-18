const app = getApp();
const { petApi } = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    step: 1,
    formData: {
      name: '',
      species: 'cat',
      gender: 'male',
      birthDate: '',
      weight: '',
      avatar: '',
      breed: '',
      adoptDate: '',
      isNeutered: false,
      furColor: '',
      notes: ''
    },
    speciesOptions: [
      { value: 'cat', label: '猫' },
      { value: 'dog', label: '狗' },
      { value: 'other', label: '其他' }
    ],
    genderOptions: [
      { value: 'male', label: '公' },
      { value: 'female', label: '母' }
    ],
    maxDate: '',
    uploading: false
  },

  onLoad() {
    const today = new Date();
    this.setData({
      maxDate: util.formatDate(today)
    });
  },

  onInputChange(e) {
    const { field } = e.currentTarget.dataset;
    const { value } = e.detail;
    this.setData({
      [`formData.${field}`]: value
    });
  },

  onSpeciesChange(e) {
    const species = e.currentTarget.dataset.value;
    this.setData({ 'formData.species': species });
  },

  onGenderChange(e) {
    const gender = e.currentTarget.dataset.value;
    this.setData({ 'formData.gender': gender });
  },

  onBirthDateChange(e) {
    this.setData({ 'formData.birthDate': e.detail.value });
  },

  onAdoptDateChange(e) {
    this.setData({ 'formData.adoptDate': e.detail.value });
  },

  onNeuteredChange(e) {
    this.setData({ 'formData.isNeutered': e.detail.value });
  },

  async onChooseAvatar() {
    try {
      const res = await wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sizeType: ['compressed']
      });

      if (res.tempFiles.length > 0) {
        this.uploadAvatar(res.tempFiles[0].tempFilePath);
      }
    } catch (error) {
      console.error('选择头像失败:', error);
    }
  },

  async uploadAvatar(filePath) {
    try {
      this.setData({ uploading: true });
      const compressedPath = await util.compressImage(filePath);
      const result = await petApi.uploadFile(compressedPath, {
        name: 'avatar',
        formData: { type: 'pet_avatar' }
      });
      
      this.setData({ 
        'formData.avatar': result.url,
        uploading: false 
      });
      util.showSuccess('头像上传成功');
    } catch (error) {
      this.setData({ uploading: false });
      util.showError('头像上传失败');
    }
  },

  validateStep1() {
    const { name, birthDate, weight } = this.data.formData;
    
    if (!name.trim()) {
      util.showError('请输入宠物昵称');
      return false;
    }
    
    if (!birthDate) {
      util.showError('请选择出生日期');
      return false;
    }
    
    if (!weight || weight <= 0) {
      util.showError('请输入正确的体重');
      return false;
    }
    
    if (weight > 200) {
      util.showError('体重不能超过200kg');
      return false;
    }
    
    return true;
  },

  onNextStep() {
    if (!this.validateStep1()) return;
    this.setData({ step: 2 });
  },

  onPrevStep() {
    this.setData({ step: 1 });
  },

  onSkipStep2() {
    this.submitForm();
  },

  async submitForm() {
    const { formData } = this.data;
    
    if (!this.validateStep1()) return;

    try {
      wx.showLoading({ title: '创建中...', mask: true });
      
      const petData = {
        ...formData,
        weight: parseFloat(formData.weight)
      };
      
      const newPet = await petApi.create(petData);
      
      wx.hideLoading();
      util.showSuccess('创建成功');
      
      app.setCurrentPet(newPet.id);
      
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/index/index' });
      }, 1000);
    } catch (error) {
      wx.hideLoading();
      util.showError(error.message || '创建失败');
    }
  }
});
