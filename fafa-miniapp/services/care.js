const config = require('../config');
const { request } = require('../utils/request');
const base = config.endpoints;

const daily = {
  dimensions: (petType) => request({ url: `${base.daily}/dimensions`, params: { petType } }),
  get: (petId, date) => request({ url: base.daily, params: { petId, date } }),
  latest: (petId, before) => request({ url: `${base.daily}/latest`, params: { petId, before } }),
  save: (data) => request({ url: base.daily, method: 'POST', data }),
  recordNormal: (petId, date) => request({ url: `${base.daily}/record-normal`, method: 'POST', params: { petId, date } }),
  copyLast: (petId, date) => request({ url: `${base.daily}/copy-last`, method: 'POST', params: { petId, date } }),
  list: (petId, startDate, endDate) => request({ url: `${base.daily}/list`, params: { petId, startDate, endDate } }),
  remove: (petId, date) => request({ url: base.daily, method: 'DELETE', params: { petId, date } })
};

const weight = {
  create: (data) => request({ url: base.weight, method: 'POST', data }),
  latest: (petId, limit = 10) => request({ url: `${base.weight}/latest`, params: { petId, limit } }),
  trend: (petId, startDate, endDate) => request({ url: `${base.weight}/trend`, params: { petId, startDate, endDate } }),
  remove: (id) => request({ url: `${base.weight}/${id}`, method: 'DELETE' })
};

const reminders = {
  today: () => request({ url: `${base.reminders}/today` }),
  list: (petId, status) => request({ url: base.reminders, params: { petId, status } }),
  get: (id) => request({ url: `${base.reminders}/${id}` }),
  create: (data) => request({ url: base.reminders, method: 'POST', data }),
  update: (id, data) => request({ url: `${base.reminders}/${id}`, method: 'PUT', data }),
  complete: (id, data = {}) => request({ url: `${base.reminders}/${id}/complete`, method: 'POST', data }),
  pause: (id) => request({ url: `${base.reminders}/${id}/pause`, method: 'POST' }),
  resume: (id) => request({ url: `${base.reminders}/${id}/resume`, method: 'POST' }),
  remove: (id) => request({ url: `${base.reminders}/${id}`, method: 'DELETE' }),
  history: (id) => request({ url: `${base.reminders}/${id}/history` })
};

const alerts = {
  list: (petId, status, limit = 20) => request({ url: base.healthAlerts, params: { petId, status, limit } }),
  get: (id) => request({ url: `${base.healthAlerts}/${id}` }),
  evaluate: (data) => request({ url: `${base.healthAlerts}/evaluate`, method: 'POST', data }),
  acknowledge: (id) => request({ url: `${base.healthAlerts}/${id}/acknowledge`, method: 'POST' }),
  resolve: (id) => request({ url: `${base.healthAlerts}/${id}/resolve`, method: 'POST' })
};

module.exports = { daily, weight, reminders, alerts };
