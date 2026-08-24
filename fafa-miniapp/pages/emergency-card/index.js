const { request } = require('../../utils/request');

Page({
  data: { petId: '', card: null, canvasWidth: 650, canvasHeight: 900 },

  onLoad(options) {
    this.setData({ petId: options.petId || '' });
    if (options.petId) this.load();
  },

  onShow() { if (this.data.petId) this.load(); },

  load() {
    request({ url: `/api/pets/${this.data.petId}/emergency-card` }).then(card => {
      this.setData({ card });
      setTimeout(() => this.drawCard(), 80);
    });
  },

  edit() { wx.navigateTo({ url: `/pages/emergency-card/edit?petId=${this.data.petId}` }); },

  drawCard() {
    const card = this.data.card;
    if (!card) return;
    const ctx = wx.createCanvasContext('emergencyCard', this);
    ctx.setFillStyle('#ffffff'); ctx.fillRect(0, 0, 650, 900);
    ctx.setFillStyle('#2f7d4f'); ctx.fillRect(0, 0, 650, 130);
    ctx.setFillStyle('#ffffff'); ctx.setFontSize(34); ctx.fillText('宠物应急信息卡', 36, 62);
    ctx.setFontSize(26); ctx.fillText(card.pet.name || '未命名宠物', 36, 104);
    ctx.setFillStyle('#222222'); ctx.setFontSize(24);
    const rows = [
      `品种：${card.pet.breed || '未填写'}`,
      `血型：${(card.profile && card.profile.bloodType) || '未填写'}`,
      `过敏史：${(card.profile && card.profile.allergies) || '未填写'}`,
      `慢性病：${(card.profile && card.profile.chronicConditions) || '未填写'}`,
      `联系人：${(card.emergencyContact && card.emergencyContact.name) || '未填写'}`,
      `电话：${(card.emergencyContact && card.emergencyContact.phone) || '未填写'}`,
      `医院：${(card.primaryHospital && card.primaryHospital.name) || '未填写'}`,
      `医院电话：${(card.primaryHospital && card.primaryHospital.phone) || '未填写'}`
    ];
    rows.forEach((text, index) => ctx.fillText(text, 36, 190 + index * 62));
    ctx.setFillStyle('#8a918c'); ctx.setFontSize(20);
    ctx.fillText('资料由用户维护，请在使用前确认有效', 36, 760);
    ctx.fillText(`状态：${card.sourceStatus === 'user_confirmed' ? '用户已确认' : '尚未填写'}`, 36, 800);
    ctx.draw();
  },

  saveImage() {
    wx.canvasToTempFilePath({ canvasId: 'emergencyCard', success: result => {
      wx.saveImageToPhotosAlbum({ filePath: result.tempFilePath, success: () => wx.showToast({ title: '已保存', icon: 'success' }),
        fail: () => wx.showToast({ title: '请允许保存图片权限', icon: 'none' }) });
    }, fail: () => wx.showToast({ title: '卡片生成失败', icon: 'none' }) }, this);
  },

  callContact() {
    const phone = this.data.card && this.data.card.emergencyContact && this.data.card.emergencyContact.phone;
    if (phone) wx.makePhoneCall({ phoneNumber: phone });
  }
});
