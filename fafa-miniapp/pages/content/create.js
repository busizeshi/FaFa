const { request } = require('../../utils/request');
Page({
  data: { petId: '', types: ['分享笔记', '小红书文案'], values: ['share_note', 'xiaohongshu'], typeIndex: 0, customTopic: '', draft: null, loading: false },
  onLoad(options) { this.setData({ petId: options.petId || '' }); },
  changeType(e) { this.setData({ typeIndex: Number(e.detail.value) }); },
  inputTopic(e) { this.setData({ customTopic: e.detail.value }); },
  create() {
    this.setData({ loading: true });
    request({ url: '/api/content-drafts', method: 'POST', data: { petId: this.data.petId, contentType: this.data.values[this.data.typeIndex], customTopic: this.data.customTopic, periodType: 'month' } })
      .then(draft => { draft.draft = draft.draft || {}; this.setData({ draft }); }).catch(() => {}).then(() => this.setData({ loading: false }));
  },
  editBody(e) { const draft = this.data.draft; draft.draft.body = e.detail.value; this.setData({ draft }); },
  save() { request({ url: '/api/content-drafts/' + this.data.draft.id, method: 'PUT', data: { draft: this.data.draft.draft } }).then(draft => { draft.draft = draft.draft || {}; this.setData({ draft }); }); }
});
