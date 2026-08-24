const { request } = require('../../utils/request');

Page({
  data: { invitationId: '', token: '', submitting: false, result: null },

  onLoad(options) {
    this.setData({ invitationId: options.invitationId || '', token: options.token || '' });
    if (options.invitationId && options.token) this.accept();
  },

  inputId(event) { this.setData({ invitationId: event.detail.value }); },
  inputToken(event) { this.setData({ token: event.detail.value }); },

  accept() {
    if (this.data.submitting || !this.data.invitationId || !this.data.token) return;
    this.setData({ submitting: true });
    request({ url: '/api/pet-invitations/accept', method: 'POST', data: {
      invitationId: Number(this.data.invitationId), token: this.data.token
    }}).then(result => {
      this.setData({ result, submitting: false });
      wx.showToast({ title: '加入成功', icon: 'success' });
    }).catch(() => this.setData({ submitting: false }));
  }
});
