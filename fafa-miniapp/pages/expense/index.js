const { request } = require('../../utils/request');

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function monthStart(anchor) {
  const date = new Date(anchor);
  date.setDate(1);
  return formatDate(date);
}

Page({
  data: {
    petId: '',
    anchorDate: formatDate(new Date()),
    period: 'month',
    statistics: null,
    expenses: [],
    loading: false
  },

  onLoad(options) {
    this.setData({ petId: options.petId || '' });
    if (options.petId) this.loadAll();
  },

  onShow() {
    if (this.data.petId && this.data.statistics) this.loadAll();
  },

  loadAll() {
    if (this.data.loading || !this.data.petId) return;
    const endDate = this.data.anchorDate;
    const startDate = this.data.period === 'year'
      ? `${endDate.slice(0, 4)}-01-01` : monthStart(endDate);
    this.setData({ loading: true });
    Promise.all([
      request({ url: '/api/expenses/statistics', data: {
        petId: this.data.petId, period: this.data.period, anchorDate: endDate
      }}),
      request({ url: '/api/expenses', data: {
        petId: this.data.petId, startDate, endDate, page: 1, size: 50
      }})
    ]).then(([statistics, page]) => {
      this.setData({ statistics, expenses: (page && page.records) || [] });
      this.drawCharts(statistics);
    }).catch(() => {}).then(() => this.setData({ loading: false }));
  },

  switchPeriod(event) {
    this.setData({ period: event.currentTarget.dataset.period }, () => this.loadAll());
  },

  openEdit() {
    wx.navigateTo({ url: '/pages/expense/edit?petId=' + this.data.petId });
  },

  drawCharts(statistics) {
    if (!statistics) return;
    const pie = wx.createCanvasContext('expensePie', this);
    const categories = statistics.categoryBreakdown || [];
    const colors = ['#4caf50', '#ff8a65', '#42a5f5', '#ab47bc', '#ffa726', '#78909c'];
    const centerX = 100;
    const centerY = 100;
    const radius = 72;
    let start = -Math.PI / 2;
    categories.forEach((item, index) => {
      const angle = Number(item.percentage || 0) / 100 * Math.PI * 2;
      pie.beginPath();
      pie.moveTo(centerX, centerY);
      pie.arc(centerX, centerY, radius, start, start + angle);
      pie.setFillStyle(colors[index % colors.length]);
      pie.fill();
      start += angle;
    });
    pie.draw();
  }
});
