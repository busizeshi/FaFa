const { request } = require('../../utils/request');

Page({
  data: { petId: '', form: { version: 0, bloodType: '', allergies: '', chronicConditions: '', emergencyContactName: '', emergencyContactPhone: '', primaryHospitalName: '', primaryHospitalPhone: '', primaryHospitalAddress: '', notes: '' } },

  onLoad(options) {
    this.setData({ petId: options.petId || '' });
    request({ url: `/api/pets/${options.petId}/emergency-profile` }).then(profile => this.setData({ form: profile }));
  },

  input(event) {
    this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail.value });
  },

  save() {
    request({ url: `/api/pets/${this.data.petId}/emergency-profile`, method: 'PUT', data: this.data.form }).then(profile => {
      this.setData({ form: profile });
      wx.showToast({ title: '保存成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    });
  }
});
