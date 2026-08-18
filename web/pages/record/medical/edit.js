const app = getApp();
const { medicalApi } = require('../../../utils/api');
const util = require('../../../utils/util');

Page({
  data: {
    recordId: '',
    isEdit: false,
    formData: {
      medicalType: 'checkup',
      hospital: '',
      doctor: '',
      diagnosis: '',
      treatment: '',
      medication: '',
      cost: '',
      visitDate: '',
      nextVisit: '',
      notes: ''
    },
    medicalTypeOptions: [
      { value: 'checkup', label: '体检', icon: '🩺' },
      { value: 'illness', label: '就诊', icon: '🏥' },
      { value: 'surgery', label: '手术', icon: '💉' },
      { value: 'emergency', label: '急诊', icon: '🚑' }
    ],
    maxDate: ''
  },

  onLoad(options) {
    const { id } = options;
    const today = new Date();
    const dateStr = util.formatDate(today);

    this.setData({
      'formData.visitDate': dateStr,
      maxDate: dateStr
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
      const record = await medicalApi.getDetail(this.data.recordId);
      
      this.setData({
        formData: {
          medicalType: record.medicalType,
          hospital: record.hospital,
          doctor: record.doctor || '',
          diagnosis: record.diagnosis,
          treatment: record.treatment || '',
          medication: record.medication || '',
          cost: record.cost ? record.cost.toString() : '',
          visitDate: record.visitDate,
          nextVisit: record.nextVisit || '',
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

  onMedicalTypeChange(e) {
    const medicalType = e.currentTarget.dataset.value;
    this.setData({ 'formData.medicalType': medicalType });
  },

  onVisitDateChange(e) {
    this.setData({ 'formData.visitDate': e.detail.value });
  },

  onNextVisitChange(e) {
    this.setData({ 'formData.nextVisit': e.detail.value });
  },

  validateForm() {
    const { hospital, diagnosis, visitDate } = this.data.formData;

    if (!hospital.trim()) {
      util.showError('请输入医院名称');
      return false;
    }

    if (!diagnosis.trim()) {
      util.showError('请输入诊断结果');
      return false;
    }

    if (!visitDate) {
      util.showError('请选择就诊日期');
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
        cost: this.data.formData.cost ? parseFloat(this.data.formData.cost) : 0,
        petId
      };

      if (this.data.isEdit) {
        await medicalApi.update(this.data.recordId, data);
      } else {
        await medicalApi.create(data);
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
      await medicalApi.delete(this.data.recordId);
      wx.hideLoading();
      util.showSuccess('删除成功');
      setTimeout(() => wx.navigateBack(), 1000);
    } catch (error) {
      wx.hideLoading();
      util.showError('删除失败');
    }
  }
});
