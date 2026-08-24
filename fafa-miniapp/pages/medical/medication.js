const { request } = require('../../utils/request');

function today() {
  const value = new Date();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${value.getFullYear()}-${month}-${day}`;
}

Page({
  data: {
    petId: '',
    frequencyOptions: ['daily', 'every_days', 'weekly', 'as_needed'],
    form: { medicationName: '', specification: '', purpose: '', dosageValue: '', dosageUnit: '粒',
      frequencyType: 'daily', frequencyValue: 1, mealRelation: 'any', startDate: today(), endDate: '', notes: '' },
    saving: false
  },

  onLoad(options) {
    this.setData({ petId: options.petId || '' });
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: event.detail.value });
  },

  onFrequencyChange(event) {
    const options = this.data.frequencyOptions;
    this.setData({ 'form.frequencyType': options[Number(event.detail.value)] });
  },

  save() {
    const form = this.data.form;
    if (!form.medicationName || !form.dosageValue) {
      wx.showToast({ title: '请填写药品和剂量', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    request({
      url: '/api/medical/medication-plans',
      method: 'POST',
      data: { ...form, petId: Number(this.data.petId), dosageValue: Number(form.dosageValue),
        administrationTimes: form.frequencyType === 'as_needed' ? [] : ['09:00:00'],
        endDate: form.endDate || null }
    }).then(() => {
      wx.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    }).catch(() => {}).then(() => this.setData({ saving: false }));
  }
});
