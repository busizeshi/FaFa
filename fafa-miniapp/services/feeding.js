const config = require('../config');
const { request } = require('../utils/request');
const base = config.endpoints;

const plans = {
  recommend: (data) => request({ url: `${base.feedingPlans}/recommendations`, method: 'POST', data, timeout: config.request.aiTimeout }),
  list: (petId, status) => request({ url: base.feedingPlans, params: { petId, status } }),
  get: (id) => request({ url: `${base.feedingPlans}/${id}` }),
  today: (id, date) => request({ url: `${base.feedingPlans}/${id}/today`, params: { date } }),
  create: (data) => request({ url: base.feedingPlans, method: 'POST', data }),
  pause: (id) => request({ url: `${base.feedingPlans}/${id}/pause`, method: 'POST' }),
  resume: (id) => request({ url: `${base.feedingPlans}/${id}/resume`, method: 'POST' }),
  complete: (id) => request({ url: `${base.feedingPlans}/${id}/complete`, method: 'POST' })
};

const records = {
  list: (petId, date) => request({ url: base.feedingRecords, params: { petId, date } }),
  range: (petId, startDate, endDate) => request({ url: `${base.feedingRecords}/range`, params: { petId, startDate, endDate } }),
  create: (data) => request({ url: base.feedingRecords, method: 'POST', data }),
  update: (id, data) => request({ url: `${base.feedingRecords}/${id}`, method: 'PUT', data }),
  remove: (id) => request({ url: `${base.feedingRecords}/${id}`, method: 'DELETE' })
};

const foods = {
  list: (petId, status) => request({ url: base.foods, params: { petId, status } }),
  get: (id) => request({ url: `${base.foods}/${id}` }),
  create: (data) => request({ url: base.foods, method: 'POST', data }),
  update: (id, data) => request({ url: `${base.foods}/${id}`, method: 'PUT', data }),
  safety: (id, petId) => request({ url: `${base.foods}/${id}/safety-score`, params: { petId } })
};

const recognition = {
  create: (data) => request({ url: base.foodRecognitions, method: 'POST', data }),
  get: (id) => request({ url: `${base.foodRecognitions}/${id}` }),
  confirm: (id, data) => request({ url: `${base.foodRecognitions}/${id}/confirm`, method: 'POST', data }),
  reject: (id) => request({ url: `${base.foodRecognitions}/${id}/reject`, method: 'POST' })
};

const transitions = {
  list: (petId) => request({ url: base.foodTransitions, params: { petId } }),
  get: (id) => request({ url: `${base.foodTransitions}/${id}` }),
  create: (data) => request({ url: base.foodTransitions, method: 'POST', data }),
  reaction: (id, data) => request({ url: `${base.foodTransitions}/${id}/reaction`, method: 'PUT', data }),
  complete: (id) => request({ url: `${base.foodTransitions}/${id}/complete`, method: 'POST' }),
  cancel: (id) => request({ url: `${base.foodTransitions}/${id}/cancel`, method: 'POST' })
};

const analysis = (petId, startDate, endDate) => request({ url: base.feedingAnalysis, params: { petId, startDate, endDate } });
module.exports = { plans, records, foods, recognition, transitions, analysis };
