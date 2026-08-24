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
    form: { visitDate: today(), hospitalName: '', doctorName: '', chiefComplaint: '',
      diagnosis: '', prescription: '', treatment: '', totalCost: '', notes: '' },
    saving: false
  },

  onLoad(options) {
    this.setData({ petId: options.petId || '' });
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: event.detail.value });
  },

  save() {
    const form = this.data.form;
    if (!form.hospitalName) {
      wx.showToast({ title: '请填写医院名称', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    request({
      url: '/api/medical/visits',
      method: 'POST',
      data: { ...form, petId: Number(this.data.petId), totalCost: form.totalCost ? Number(form.totalCost) : null }
    }).then(() => {
      wx.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    }).catch(() => {}).then(() => this.setData({ saving: false }));
  }
});
