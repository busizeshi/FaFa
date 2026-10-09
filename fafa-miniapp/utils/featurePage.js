function create(pageKey) {
  return {
    data: { pageKey, routeOptions: {} },
    onLoad(options) { this._firstShow = true; this.setData({ routeOptions: options || {} }); },
    onShow() {
      if (this._firstShow) { this._firstShow = false; return; }
      const screen = this.selectComponent('#feature-screen');
      if (screen && screen.reload) screen.reload();
    }
  };
}

module.exports = { create };
