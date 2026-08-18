const app = getApp();
const { timelineApi } = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    petId: '',
    records: [],
    groupedRecords: [],
    filterType: 'all',
    loading: true,
    hasMore: true,
    page: 1,
    pageSize: 20,
    recordTypes: [
      { value: 'all', label: '全部', icon: '📋' },
      { value: 'feeding', label: '喂食', icon: '🍖' },
      { value: 'water', label: '饮水', icon: '💧' },
      { value: 'weight', label: '体重', icon: '⚖️' },
      { value: 'medical', label: '就医', icon: '🏥' },
      { value: 'vaccination', label: '疫苗', icon: '💉' },
      { value: 'grooming', label: '美容', icon: '✂️' },
      { value: 'event', label: '事件', icon: '📝' }
    ]
  },

  onLoad(options) {
    const { petId } = options;
    if (petId) {
      this.setData({ petId });
    } else {
      const currentPet = app.getCurrentPet();
      if (currentPet) {
        this.setData({ petId: currentPet });
      }
    }
    this.loadRecords();
  },

  onShow() {
    if (this.data.records.length > 0) {
      this.refreshRecords();
    }
  },

  async loadRecords() {
    if (!this.data.hasMore || this.data.loading) return;

    try {
      this.setData({ loading: true });
      
      const { petId, filterType, page, pageSize } = this.data;
      const query = {
        limit: pageSize,
        skip: (page - 1) * pageSize,
        sort: 'recordDate:desc'
      };

      if (filterType !== 'all') {
        query.type = filterType;
      }

      const newRecords = await timelineApi.getList(petId, query);
      
      const allRecords = page === 1 ? newRecords : [...this.data.records, ...newRecords];
      const grouped = this.groupRecordsByDate(allRecords);

      this.setData({
        records: allRecords,
        groupedRecords: grouped,
        hasMore: newRecords.length === pageSize,
        loading: false
      });
    } catch (error) {
      console.error('加载记录失败:', error);
      this.setData({ loading: false });
    }
  },

  groupRecordsByDate(records) {
    const groups = {};
    
    records.forEach(record => {
      const date = util.formatDate(record.recordDate);
      if (!groups[date]) {
        groups[date] = {
          date,
          dateLabel: this.getDateLabel(date),
          records: []
        };
      }
      groups[date].records.push(record);
    });

    return Object.values(groups);
  },

  getDateLabel(dateStr) {
    const today = util.formatDate(new Date());
    const yesterday = util.formatDate(new Date(Date.now() - 86400000));

    if (dateStr === today) return '今天';
    if (dateStr === yesterday) return '昨天';
    return dateStr;
  },

  async refreshRecords() {
    this.setData({
      records: [],
      groupedRecords: [],
      page: 1,
      hasMore: true
    });
    await this.loadRecords();
  },

  onFilterChange(e) {
    const { type } = e.currentTarget.dataset;
    if (type === this.data.filterType) return;

    this.setData({ filterType: type });
    this.refreshRecords();
  },

  onRecordTap(e) {
    const { id, type } = e.currentTarget.dataset;
    const typePages = {
      feeding: 'feeding',
      water: 'water',
      weight: 'weight',
      excretion: 'excretion',
      medication: 'medication',
      vaccination: 'vaccination',
      medical: 'medical',
      grooming: 'grooming',
      training: 'training',
      event: 'event'
    };

    const page = typePages[type] || 'event';
    wx.navigateTo({ url: `/pages/record/${page}/detail?id=${id}` });
  },

  onAddRecord() {
    wx.navigateTo({ url: '/pages/record/select' });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 });
      this.loadRecords();
    }
  },

  onPullDownRefresh() {
    this.refreshRecords().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  getRecordTypeIcon: util.getRecordTypeIcon,
  getRecordTypeText: util.getRecordTypeText,
  formatTime: util.formatTime
});
