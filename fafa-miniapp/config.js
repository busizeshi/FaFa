const extension = typeof wx !== 'undefined' && wx.getExtConfigSync
  ? wx.getExtConfigSync()
  : {};
const runtimeConfig = extension || {};

const API_ENV = runtimeConfig.apiEnv || 'development';
const API_BASE_URLS = {
  development: 'http://192.168.1.15:8081',
  test: '',
  production: ''
};
const API_BASE_URL = runtimeConfig.apiBaseUrl || API_BASE_URLS[API_ENV] || '';

const ENDPOINTS = {
  auth: { login: '/auth/wechat/login', logout: '/auth/logout' },
  user: { me: '/user/me', profile: '/user/profile', avatar: '/user/avatar', vip: '/user/vip' },
  pets: '/api/pets',
  tags: '/api/tags',
  daily: '/api/daily-records',
  weight: '/api/weight-records',
  feedingPlans: '/api/feeding-plans',
  feedingRecords: '/api/feeding-records',
  feedingAnalysis: '/api/feeding-analysis',
  foods: '/api/foods',
  foodRecognitions: '/api/foods/recognitions',
  foodTransitions: '/api/food-transitions',
  photos: '/api/photos',
  medical: '/api/medical',
  healthAlerts: '/api/health-alerts',
  reminders: '/api/reminders',
  events: '/api/event-records',
  chat: '/api/chat',
  reports: '/api/reports',
  contentDrafts: '/api/content-drafts',
  contentSchedules: '/api/content-schedules',
  expenses: '/api/expenses',
  invitations: '/api/pet-invitations',
  quota: '/api/quota'
};

module.exports = {
  appName: 'FaFa 宠物生活',
  apiEnv: API_ENV,
  apiBaseUrl: API_BASE_URL.replace(/\/+$/, ''),
  endpoints: ENDPOINTS,
  request: {
    timeout: 30000,
    uploadTimeout: 120000,
    aiTimeout: 60000,
    pageSize: 20,
    authHeader: 'Authorization'
  },
  storage: {
    token: 'fafa.session.token',
    user: 'fafa.session.user',
    currentPet: 'fafa.current.pet'
  },
  routes: {
    login: '/pages/login/index',
    home: '/pages/home/index',
    album: '/pages/album/index',
    chat: '/pages/chat/index',
    me: '/pages/me/index',
    pets: '/packages/pet/pages/list/index',
    petDetail: '/packages/pet/pages/detail/index',
    petTags: '/packages/pet/pages/tags/index',
    petCreate: '/packages/pet/pages/create/index',
    dailyCheckin: '/packages/daily/pages/checkin/index',
    dailyHistory: '/packages/daily/pages/history/index',
    weightRecord: '/packages/daily/pages/weight-record/index',
    weightTrend: '/packages/daily/pages/weight-trend/index',
    feedingToday: '/packages/feeding/pages/today/index',
    planList: '/packages/feeding/pages/plan-list/index',
    planCreate: '/packages/feeding/pages/plan-create/index',
    feedingAnalysis: '/packages/feeding/pages/analysis/index',
    foodLibrary: '/packages/feeding/pages/food-library/index',
    foodDetail: '/packages/feeding/pages/food-detail/index',
    foodRecognition: '/packages/feeding/pages/food-recognition/index',
    foodTransition: '/packages/feeding/pages/food-transition/index',
    mediaUpload: '/packages/media/pages/upload/index',
    photoDetail: '/packages/media/pages/photo-detail/index',
    photoSearch: '/packages/media/pages/search/index',
    medicalRecords: '/packages/medical/pages/records/index',
    medicalCreate: '/packages/medical/pages/create/index',
    medicationPlan: '/packages/medical/pages/medication/index',
    healthAlerts: '/packages/medical/pages/alerts/index',
    reminders: '/packages/medical/pages/reminder-center/index',
    reminderCreate: '/packages/medical/pages/reminder-create/index',
    timeline: '/packages/medical/pages/timeline/index',
    medicalSummary: '/packages/medical/pages/summary/index',
    reports: '/packages/biz/pages/reports/index',
    reportDetail: '/packages/biz/pages/report-detail/index',
    family: '/packages/biz/pages/family/index',
    expenseBook: '/packages/biz/pages/expense-book/index',
    expenseCreate: '/packages/biz/pages/expense-create/index',
    membership: '/packages/biz/pages/membership/index',
    profile: '/packages/biz/pages/profile/index',
    emergency: '/packages/biz/pages/emergency/index',
    invitationAccept: '/packages/biz/pages/invitation-accept/index',
    contentDrafts: '/packages/biz/pages/content-drafts/index',
    contentEdit: '/packages/biz/pages/content-edit/index',
    contentSchedule: '/packages/biz/pages/content-schedule/index'
  },
  uploads: {
    photosPath: '/api/photos/upload',
    petPhotos: ['photos'],
    photoLimit: 20,
    petPhotoMaxBytes: 2500 * 1024,
    imageMaxBytes: 10 * 1024 * 1024,
    videoMaxBytes: 50 * 1024 * 1024
  },
  tabs: [
    { pagePath: 'pages/home/index', text: '首页', icon: 'home' },
    { pagePath: 'pages/album/index', text: '相册', icon: 'album' },
    { pagePath: 'pages/chat/index', text: '问问', icon: 'chat' },
    { pagePath: 'pages/me/index', text: '我的', icon: 'me' }
  ]
};
