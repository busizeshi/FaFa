const { request } = require('../../utils/request');

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function recentRange() {
  const end = new Date();
  const start = new Date(end);
  start.setDate(start.getDate() - 89);
  return { startDate: formatDate(start), endDate: formatDate(end) };
}

Page({
  data: {
    petId: '',
    range: recentRange(),
    visits: [],
    timeline: [],
    plans: [],
    alerts: [],
    summary: null,
    loading: false
  },

  onLoad(options) {
    this.setData({ petId: options.petId || '' });
    if (options.petId) this.loadAll(options.petId);
  },

  loadAll(petId) {
    if (this.data.loading) return;
    const range = this.data.range;
    this.setData({ loading: true });
    Promise.all([
      request({ url: '/api/medical/visits', data: { petId, ...range, page: 1, size: 20 } }),
      request({ url: '/api/medical/medication-plans', data: { petId, status: 'active' } }),
      request({ url: '/api/health-alerts', data: { petId, status: 'open', limit: 20 } }),
      request({ url: '/api/medical/timeline', data: { petId, ...range } })
    ]).then(([visitPage, plans, alerts, timeline]) => {
      this.setData({
        visits: (visitPage && visitPage.records) || [],
        plans: plans || [],
        alerts: alerts || [],
        timeline: timeline || []
      });
    }).catch(() => {}).then(() => this.setData({ loading: false }));
  },

  refresh() {
    if (this.data.petId) this.loadAll(this.data.petId);
  },

  evaluateAlerts() {
    request({
      url: '/api/health-alerts/evaluate',
      method: 'POST',
      data: { petId: this.data.petId }
    }).then((alerts) => {
      this.setData({ alerts: alerts || [] });
      wx.showToast({ title: '评估完成', icon: 'success' });
    }).catch(() => {});
  },

  generateSummary() {
    wx.showLoading({ title: '整理中' });
    request({
      url: '/api/medical/summaries',
      method: 'POST',
      data: { petId: this.data.petId, ...this.data.range, includeMedia: true }
    }).then((summary) => this.setData({ summary })).catch(() => {})
      .then(() => wx.hideLoading());
  },

  openVisitForm() {
    wx.navigateTo({ url: '/pages/medical/visit?petId=' + this.data.petId });
  },

  openMedicationForm() {
    wx.navigateTo({ url: '/pages/medical/medication?petId=' + this.data.petId });
  }
});
