const { request } = require('../../utils/request');

Page({
  data: { petId: '', access: null, members: [], roles: ['caregiver', 'member'], role: 'caregiver', expiresInDays: 7, invitation: null },

  onLoad(options) {
    this.setData({ petId: options.petId || '' });
    if (options.petId) this.load();
  },

  load() {
    const id = this.data.petId;
    Promise.all([
      request({ url: `/api/pets/${id}/access` }).then(access => this.setData({ access })),
      request({ url: `/api/pets/${id}/members` }).then(members => this.setData({ members: members || [] }))
    ]).catch(() => {});
  },

  selectRole(event) { this.setData({ role: this.data.roles[event.detail.value] }); },
  inputDays(event) { this.setData({ expiresInDays: Number(event.detail.value) || 1 }); },

  createInvitation() {
    request({
      url: `/api/pets/${this.data.petId}/invitations`, method: 'POST',
      data: { role: this.data.role, expiresInDays: this.data.expiresInDays }
    }).then(invitation => {
      this.setData({ invitation });
      wx.setClipboardData({ data: invitation.sharePath });
      wx.showModal({ title: '邀请已生成', content: '邀请参数已复制，可发送给家人或照护者。凭证只显示本次。', showCancel: false });
    });
  },

  revokeMember(event) {
    const memberId = event.currentTarget.dataset.id;
    wx.showModal({ title: '撤销成员？', content: '撤销后该成员立即失去访问权限。', success: result => {
      if (!result.confirm) return;
      request({ url: `/api/pets/${this.data.petId}/members/${memberId}`, method: 'DELETE' }).then(() => this.load());
    }});
  },

  acceptByPath() {
    if (this.data.invitation) wx.setClipboardData({ data: this.data.invitation.sharePath });
  }
});
