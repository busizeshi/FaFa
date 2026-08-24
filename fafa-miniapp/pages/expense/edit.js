const { request } = require('../../utils/request');

function today() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

Page({
  data: {
    petId: '',
    categories: [],
    categoryIndex: 0,
    amount: '',
    expenseDate: today(),
    title: '',
    notes: '',
    saving: false
  },

  onLoad(options) {
    this.setData({ petId: options.petId || '' });
    request({ url: '/api/expenses/categories' })
      .then((categories) => this.setData({ categories: categories || [] }))
      .catch(() => {});
  },

  onAmountInput(event) { this.setData({ amount: event.detail.value }); },
  onTitleInput(event) { this.setData({ title: event.detail.value }); },
  onNotesInput(event) { this.setData({ notes: event.detail.value }); },
  onDateChange(event) { this.setData({ expenseDate: event.detail.value }); },
  onCategoryChange(event) { this.setData({ categoryIndex: Number(event.detail.value) }); },

  save() {
    const category = this.data.categories[this.data.categoryIndex];
    if (!this.data.petId || !category || !this.data.amount || !this.data.title) {
      wx.showToast({ title: '请填写金额、分类和标题', icon: 'none' });
      return;
    }
    if (this.data.saving) return;
    this.setData({ saving: true });
    request({
      url: '/api/expenses',
      method: 'POST',
      data: {
        petId: Number(this.data.petId), category: category.code,
        amount: Number(this.data.amount), currency: 'CNY',
        expenseDate: this.data.expenseDate, title: this.data.title,
        notes: this.data.notes || undefined
      }
    }).then(() => {
      wx.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    }).catch(() => {}).then(() => this.setData({ saving: false }));
  }
});
