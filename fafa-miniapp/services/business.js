const config = require('../config');
const { request } = require('../utils/request');
const base = config.endpoints;

const reports = {
  list: (petId, page = 1, size = config.request.pageSize) => request({ url: base.reports, params: { petId, page, size } }),
  get: (id) => request({ url: `${base.reports}/${id}` }),
  generate: (data) => request({ url: `${base.reports}/generate`, method: 'POST', data, timeout: config.request.aiTimeout }),
  regenerate: (id) => request({ url: `${base.reports}/${id}/regenerate`, method: 'POST', timeout: config.request.aiTimeout }),
  remove: (id) => request({ url: `${base.reports}/${id}`, method: 'DELETE' })
};

const expenses = {
  list: (params) => request({ url: base.expenses, params }),
  categories: () => request({ url: `${base.expenses}/categories` }),
  statistics: (params) => request({ url: `${base.expenses}/statistics`, params }),
  create: (data) => request({ url: base.expenses, method: 'POST', data }),
  update: (id, data) => request({ url: `${base.expenses}/${id}`, method: 'PUT', data }),
  remove: (id) => request({ url: `${base.expenses}/${id}`, method: 'DELETE' })
};

const content = {
  list: (petId, page = 1, size = config.request.pageSize) => request({ url: base.contentDrafts, params: { petId, page, size } }),
  get: (id) => request({ url: `${base.contentDrafts}/${id}` }),
  create: (data) => request({ url: base.contentDrafts, method: 'POST', data, timeout: config.request.aiTimeout }),
  update: (id, data) => request({ url: `${base.contentDrafts}/${id}`, method: 'PUT', data }),
  regenerate: (id) => request({ url: `${base.contentDrafts}/${id}/regenerate`, method: 'POST', timeout: config.request.aiTimeout }),
  remove: (id) => request({ url: `${base.contentDrafts}/${id}`, method: 'DELETE' }),
  schedules: (petId) => request({ url: base.contentSchedules, params: { petId } }),
  schedule: (data) => request({ url: base.contentSchedules, method: 'PUT', data }),
  enableSchedule: (id) => request({ url: `${base.contentSchedules}/${id}/enable`, method: 'POST' }),
  disableSchedule: (id) => request({ url: `${base.contentSchedules}/${id}/disable`, method: 'POST' }),
  removeSchedule: (id) => request({ url: `${base.contentSchedules}/${id}`, method: 'DELETE' })
};

const emergency = {
  getProfile: (petId) => request({ url: `${base.pets}/${petId}/emergency-profile` }),
  updateProfile: (petId, data) => request({ url: `${base.pets}/${petId}/emergency-profile`, method: 'PUT', data }),
  card: (petId) => request({ url: `${base.pets}/${petId}/emergency-card` })
};

module.exports = { reports, expenses, content, emergency };
