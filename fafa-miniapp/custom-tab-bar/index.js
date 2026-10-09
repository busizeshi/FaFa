const config = require('../config');

Component({
  data: { selected: 0, tabs: config.tabs },
  lifetimes: {
    attached() {
      const pages = getCurrentPages();
      const route = pages.length ? pages[pages.length - 1].route : '';
      const selected = config.tabs.findIndex((tab) => tab.pagePath === route);
      this.setData({ selected: Math.max(0, selected) });
    }
  },
  methods: {
    onTap(event) {
      const index = Number(event.currentTarget.dataset.index);
      const tab = config.tabs[index];
      if (!tab || index === this.data.selected) return;
      wx.switchTab({ url: `/${tab.pagePath}` });
    }
  }
});
