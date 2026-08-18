const app = getApp();
const { feedingApi } = require('../../../utils/api');
const util = require('../../../utils/util');

Page({
  data: {
    recordId: '',
    isEdit: false,
    formData: {
      foodType: 'main',
      foodName: '',
      amount: '',
      unit: 'g',
      feedTime: '',
      notes: ''
    },
    foodTypeOptions: [
      { value: 'main', label: '主食' },
      { value: 'snack', label: '零食' },
      { value: 'supplement', label: '营养品' }
    ],
    unitOptions: ['g', 'ml', '粒', '勺', '罐'],
    showUnitPicker: false
  },

  onLoad(options) {
    const { id } = options;
    const now = new Date();
    const timeStr = util.formatDateTime(now);

    this.setData({
      'formData.feedTime': timeStr
    });

    if (id) {
      this.setData({ 
        recordId: id,
        isEdit: true 
      });
      this.loadRecord();
    }
  },

  async loadRecord() {
    try {
      wx.showLoading({ title: '加载中...' });
      const record = await feedingApi.getDetail(this.data.recordId);
      
      this.setData({
        formData: {
          foodType: record.foodType,
          foodName: record.foodName,
          amount: record.amount.toString(),
          unit: record.unit,
          feedTime: record.feedTime,
          notes: record.notes || ''
        }
      });
      wx.hideLoading();
    } catch (error) {
      wx.hideLoading();
      util.showError('加载失败');
      setTimeout(() => wx.navigateBack(), 1500);
    }
  },

  onInputChange(e) {
    const { field } = e.currentTarget.dataset;
    const { value } = e.detail;
    this.setData({
      [`formData.${field}`]: value
    });
  },

  onFoodTypeChange(e) {
    const foodType = e.currentTarget.dataset.value;
    this.setData({ 'formData.foodType': foodType });
  },

  onTimeChange(e) {
    this.setData({ 'formData.feedTime': e.detail.value });
  },

  onShowUnitPicker() {
    this.setData({ showUnitPicker: true });
  },

  onUnitChange(e) {
    const unit = this.data.unitOptions[e.detail.value];
    this.setData({ 
      'formData.unit': unit,
      showUnitPicker: false 
    });
  },

  onCancelUnitPicker() {
    this.setData({ showUnitPicker: false });
  },

  validateForm() {
    const { foodName, amount, feedTime } = this.data.formData;

    if (!foodName.trim()) {
      util.showError('请输入食物名称');
      return false;
    }

    if (!amount || parseFloat(amount) <= 0) {
      util.showError('请输入正确的份量');
      return false;
    }

    if (!feedTime) {
      util.showError('请选择喂食时间');
      return false;
    }

    return true;
  },

  async onSubmit() {
    if (!this.validateForm()) return;

    const petId = app.getCurrentPet();
    if (!petId) {
      util.showError('请先选择宠物');
      return;
    }

    try {
      wx.showLoading({ title: '保存中...', mask: true });

      const data = {
        ...this.data.formData,
        amount: parseFloat(this.data.formData.amount),
        petId
      };

      if (this.data.isEdit) {
        await feedingApi.update(this.data.recordId, data);
      } else {
        await feedingApi.create(data);
      }

      wx.hideLoading();
      util.showSuccess('保存成功');
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1000);
    } catch (error) {
      wx.hideLoading();
      util.showError(error.message || '保存失败');
    }
  },

  async onDelete() {
    const confirm = await util.showModal('确认删除', '确定要删除这条记录吗？');
    if (!confirm) return;

    try {
      wx.showLoading({ title: '删除中...' });
      await feedingApi.delete(this.data.recordId);
      wx.hideLoading();
      util.showSuccess('删除成功');
      setTimeout(() => wx.navigateBack(), 1000);
    } catch (error) {
      wx.hideLoading();
      util.showError('删除失败');
    }
  }
});
