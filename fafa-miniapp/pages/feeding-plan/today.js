const { request } = require('../../utils/request');

Page({
  data: { petId: null, plan: null, checklist: null, date: '' },

  onLoad(options) {
    const date = this.formatDate(new Date());
    this.setData({ petId: Number(options.petId || 0), date });
    this.loadPlan();
  },

  loadPlan() {
    request({ url: '/api/feeding-plans', data: { petId: this.data.petId, status: 'active' } })
      .then((plans) => {
        const plan = plans && plans[0];
        this.setData({ plan });
        if (plan) this.loadToday(plan.id);
      }).catch(() => {});
  },

  loadToday(planId) {
    request({ url: '/api/feeding-plans/' + planId + '/today', data: { date: this.data.date } })
      .then((checklist) => this.setData({ checklist })).catch(() => {});
  },

  completeItem(event) {
    const item = event.currentTarget.dataset.item;
    if (!item || item.completionStatus === 'completed' || !this.data.plan) return;
    request({
      url: '/api/feeding-records',
      method: 'POST',
      data: {
        petId: this.data.petId,
        feedingTime: this.formatDateTime(new Date()),
        foodType: 'main_food',
        amount: item.amount,
        unit: item.unit,
        foodId: item.foodId,
        feedingPlanId: this.data.plan.id,
        feedingPlanItemId: item.planItemId,
        plannedAt: item.plannedAt,
        foodNameSnapshot: item.foodName,
        notes: '按今日计划完成'
      }
    }).then(() => this.loadToday(this.data.plan.id)).catch(() => {});
  },

  formatDate(date) {
    return [date.getFullYear(), this.pad(date.getMonth() + 1), this.pad(date.getDate())].join('-');
  },

  formatDateTime(date) {
    return this.formatDate(date) + 'T' + [this.pad(date.getHours()), this.pad(date.getMinutes()), this.pad(date.getSeconds())].join(':');
  },

  pad(value) { return String(value).padStart(2, '0'); }
});
