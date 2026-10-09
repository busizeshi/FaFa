const config = require('../../config');
const media = require('../../services/media');

Page({
  data: { loading: true, loadingMore: false, error: '', photos: [], total: 0, page: 0, pet: null },
  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    if (tab) tab.setData({ selected: 1 });
    this.setData({ pet: getApp().globalData.currentPet });
    this.loadPhotos(true);
  },
  loadPhotos(reset) {
    const isReset = reset !== false;
    if (!isReset && (this.data.loading || this.data.loadingMore || this.data.page * config.request.pageSize >= this.data.total)) return Promise.resolve();
    const page = isReset ? 1 : this.data.page + 1;
    this.setData(isReset ? { loading: true, error: '', page: 0 } : { loadingMore: true, error: '' });
    const pet = getApp().globalData.currentPet;
    return media.photos.search({ query: '', petId: pet && pet.id, page, size: config.request.pageSize })
      .then((result) => {
        const rows = result && Array.isArray(result.records) ? result.records : [];
        const photos = rows.map((item) => Object.assign({}, item, { displayUrl: item.fileUrl, dateLabel: (item.takenAt || item.createdAt || '').slice(0, 10) }));
        this.setData({ photos: isReset ? photos : this.data.photos.concat(photos), total: result && result.total || 0, page });
      })
      .catch((error) => this.setData({ error: error.message || '暂时无法加载相册' }))
      .finally(() => this.setData({ loading: false, loadingMore: false }));
  },
  goUpload() { wx.navigateTo({ url: config.routes.mediaUpload }); },
  goSearch() { wx.navigateTo({ url: config.routes.photoSearch }); },
  onPhotoTap(event) { wx.navigateTo({ url: `${config.routes.photoDetail}?id=${event.currentTarget.dataset.id}` }); },
  onRetry() { this.loadPhotos(true); },
  onReachBottom() { this.loadPhotos(false); },
  onPullDownRefresh() { this.loadPhotos(true).finally(() => wx.stopPullDownRefresh()); }
});
