const config = require('../../config');
const petService = require('../../services/pet');
const care = require('../../services/care');

function localDate() {
  const date = new Date();
  const pad = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

Page({
  data: { loading: true, error: '', pet: null, pets: [], petNames: [], petIndex: 0, daily: null, reminders: [], canRecord: false, dateLabel: '' },

  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    if (tab) tab.setData({ selected: 0 });
    this.loadHome();
  },

  loadHome() {
    const now = new Date();
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    this.setData({ loading: true, error: '', dateLabel: `${now.getMonth() + 1}月${now.getDate()}日 · ${weekdays[now.getDay()]}` });
    return petService.list()
      .then((pets) => {
        const list = Array.isArray(pets) ? pets : [];
        const stored = getApp().globalData.currentPet;
        const current = list.find((pet) => String(pet.id) === String(stored && stored.id)) || list[0] || null;
        if (current) getApp().setCurrentPet(current);
        this.setData({ pets: list, petNames: list.map((item) => item.name), pet: current, canRecord: false, petIndex: Math.max(0, list.indexOf(current)) });
        if (!current) return Promise.resolve();
        return Promise.all([
          care.daily.get(current.id, localDate()).catch(() => null),
          care.reminders.today().catch(() => []),
          petService.access(current.id).catch(() => ({ permissions: [] }))
        ]).then(([daily, reminders, access]) => this.setData({
          daily,
          canRecord: (access.permissions || []).includes('CREATE_RECORD'),
          reminders: Array.isArray(reminders) ? reminders.filter((item) => String(item.reminder && item.reminder.petId) === String(current.id)).slice(0, 3).map((item) => Object.assign({}, item, { id: item.reminder && item.reminder.id })) : []
        }));
      })
      .catch((error) => this.setData({ error: error.message || '暂时无法加载首页' }))
      .finally(() => this.setData({ loading: false }));
  },

  onChoosePet(event) {
    const pet = this.data.pets[Number(event.detail.value)];
    if (!pet) return;
    getApp().setCurrentPet(pet);
    this.setData({ pet, petIndex: Number(event.detail.value), daily: null });
    this.loadHome();
  },

  goPets() { wx.navigateTo({ url: config.routes.pets }); },
  goCheckin() { wx.navigateTo({ url: `${config.routes.dailyCheckin}?petId=${this.data.pet.id}` }); },
  goFeeding() { wx.navigateTo({ url: `${config.routes.feedingToday}?petId=${this.data.pet.id}` }); },
  goReminders() { wx.navigateTo({ url: config.routes.reminders }); },
  goWeight() { wx.navigateTo({ url: `${config.routes.weightTrend}?petId=${this.data.pet.id}` }); },
  goAlbum() { wx.switchTab({ url: config.routes.album }); },
  onPullDownRefresh() { this.loadHome().finally(() => wx.stopPullDownRefresh()); }
});
