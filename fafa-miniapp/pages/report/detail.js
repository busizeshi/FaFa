const { request } = require('../../utils/request');
Page({
  data: { id: '', report: null, loading: false },
  onLoad(options) { this.setData({ id: options.id || '' }, () => this.load()); },
  load() { request({ url: '/api/reports/' + this.data.id }).then(report => { report.content = report.content || {}; this.setData({ report }); }).catch(() => {}); },
  regenerate() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    request({ url: '/api/reports/' + this.data.id + '/regenerate', method: 'POST' }).then(report => { report.content = report.content || {}; this.setData({ report }); }).catch(() => {}).then(() => this.setData({ loading: false }));
  }
});
