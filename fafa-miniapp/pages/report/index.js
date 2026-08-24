const { request } = require('../../utils/request');

Page({
  data: { petId: '', reports: [], periodNames: ['周报', '月报', '季报', '年报'], periods: ['week', 'month', 'quarter', 'year'], periodIndex: 1, loading: false },
  onLoad(options) { this.setData({ petId: options.petId || '' }, () => this.load()); },
  onShow() { if (this.data.petId && this.data.reports.length) this.load(); },
  load() {
    if (!this.data.petId) return;
    request({ url: '/api/reports', data: { petId: this.data.petId, page: 1, size: 20 } })
      .then(data => this.setData({ reports: data || [] })).catch(() => {});
  },
  changePeriod(e) { this.setData({ periodIndex: Number(e.detail.value) }); },
  generate() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    request({ url: '/api/reports/generate', method: 'POST', data: { petId: this.data.petId, periodType: this.data.periods[this.data.periodIndex] } })
      .then(report => { this.load(); wx.navigateTo({ url: '/pages/report/detail?id=' + report.id }); })
      .catch(() => {}).then(() => this.setData({ loading: false }));
  },
  openDetail(e) { wx.navigateTo({ url: '/pages/report/detail?id=' + e.currentTarget.dataset.id }); },
  openContent() { wx.navigateTo({ url: '/pages/content/create?petId=' + this.data.petId }); },
  openSchedule() { wx.navigateTo({ url: '/pages/content/schedule?petId=' + this.data.petId }); }
});
