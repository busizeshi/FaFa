const app = getApp();
const { weightApi } = require('../../../utils/api');
const util = require('../../../utils/util');

Page({
  data: {
    recordId: '',
    isEdit: false,
    formData: {
      weight: '',
      measureTime: '',
      notes: ''
    },
    lastWeight: null,
    weightChange: null
  },

  onLoad(options) {
    const { id } = options;
    const now = new Date();
    const timeStr = util.formatDateTime(now);

    this.setData({
      'formData.measureTime': timeStr
    });

    if (id) {
      this.setData({ 
        recordId: id,
        isEdit: true 
      });
      this.loadRecord();
    } else {
      this.loadLastWeight();
    }
  },

  async loadRecord() {
    try {
      wx.showLoading({ title: '加载中...' });
      const record = await weightApi.getDetail(this.data.recordId);
      
      this.setData({
        formData: {
          weight: record.weight.toString(),
          measureTime: record.measureTime,
          notes: record.notes || ''
        }
      });
      wx.hideLoading();
      this.loadLastWeight();
    } catch (error) {
      wx.hideLoading();
      util.showError('加载失败');
      setTimeout(() => wx.navigateBack(), 1500);
    }
  },

  async loadLastWeight() {
    try {
      const petId = app.getCurrentPet();
      const records = await weightApi.getList(petId, { limit: 2, sort: 'measureTime:desc' });
      
      if (records.length > 0) {
        const lastWeight = this.data.isEdit && records.length > 1 ? records[1].weight : records[0].weight;
        this.setData({ lastWeight });
        this.calculateWeightChange();
      }
    } catch (error) {
      console.error('加载上次体重失败:', error);
    }
  },

  onInputChange(e) {
    const { field } = e.currentTarget.dataset;
    const { value } = e.detail;
    this.setData({
      [`formData.${field}`]: value
    });
    
    if (field === 'weight') {
      this.calculateWeightChange();
    }
  },

  onTimeChange(e) {
    this.setData({ 'formData.measureTime': e.detail.value });
  },

  calculateWeightChange() {
    const { weight } = this.data.formData;
    const { lastWeight } = this.data;
    
    if (weight && lastWeight) {
      const currentWeight = parseFloat(weight);
      const change = currentWeight - lastWeight;
      this.setData({ 
        weightChange: {
          value: Math.abs(change),
          isGain: change > 0,
          percentage: ((change / lastWeight) * 100).toFixed(1)
        }
      });
    } else {
      this.setData({ weightChange: null });
    }
  },

  validateForm() {
    const { weight, measureTime } = this.data.formData;

    if (!weight || parseFloat(weight) <= 0) {
      util.showError('请输入正确的体重');
      return false;
    }

    if (parseFloat(weight) > 200) {
      util.showError('体重不能超过200kg');
      return false;
    }

    if (!measureTime) {
      util.showError('请选择测量时间');
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
        weight: parseFloat(this.data.formData.weight),
        petId
      };

      if (this.data.isEdit) {
        await weightApi.update(this.data.recordId, data);
      } else {
        await weightApi.create(data);
        await petApi.update(petId, { weight: data.weight });
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
      await weightApi.delete(this.data.recordId);
      wx.hideLoading();
      util.showSuccess('删除成功');
      setTimeout(() => wx.navigateBack(), 1000);
    } catch (error) {
      wx.hideLoading();
      util.showError('删除失败');
    }
  }
});
