const { request } = require('../../utils/request');
Page({
  data: { petId: '', types: ['分享笔记', '小红书文案'], values: ['share_note', 'xiaohongshu'], typeIndex: 0, intervalNames: ['每7天', '每15天', '每30天'], intervals: [7, 15, 30], intervalIndex: 1, schedules: [] },
  onLoad(options) { this.setData({ petId: options.petId || '' }, () => this.load()); },
  load() { request({ url: '/api/content-schedules', data: { petId: this.data.petId } }).then(schedules => this.setData({ schedules: schedules || [] })).catch(() => {}); },
  changeType(e) { this.setData({ typeIndex: Number(e.detail.value) }); },
  changeInterval(e) { this.setData({ intervalIndex: Number(e.detail.value) }); },
  save() { request({ url: '/api/content-schedules', method: 'PUT', data: { petId: this.data.petId, contentType: this.data.values[this.data.typeIndex], intervalDays: this.data.intervals[this.data.intervalIndex] } }).then(() => this.load()); },
  toggle(e) { const item = this.data.schedules.find(value => value.id === e.currentTarget.dataset.id); if (!item) return; request({ url: '/api/content-schedules/' + item.id + '/' + (item.enabled ? 'disable' : 'enable'), method: 'POST' }).then(() => this.load()); }
});
