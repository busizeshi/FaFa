const config = require('../config');
const { request } = require('../utils/request');
const base = config.endpoints.medical;

const visits = {
  list: (params) => request({ url: `${base}/visits`, params }),
  get: (id) => request({ url: `${base}/visits/${id}` }),
  create: (data) => request({ url: `${base}/visits`, method: 'POST', data }),
  update: (id, data) => request({ url: `${base}/visits/${id}`, method: 'PUT', data }),
  remove: (id) => request({ url: `${base}/visits/${id}`, method: 'DELETE' }),
  attach: (id, data) => request({ url: `${base}/visits/${id}/attachments`, method: 'POST', data })
};

const medication = {
  list: (petId, status) => request({ url: `${base}/medication-plans`, params: { petId, status } }),
  get: (id) => request({ url: `${base}/medication-plans/${id}` }),
  create: (data) => request({ url: `${base}/medication-plans`, method: 'POST', data }),
  update: (id, data) => request({ url: `${base}/medication-plans/${id}`, method: 'PUT', data }),
  pause: (id) => request({ url: `${base}/medication-plans/${id}/pause`, method: 'POST' }),
  resume: (id) => request({ url: `${base}/medication-plans/${id}/resume`, method: 'POST' }),
  complete: (id) => request({ url: `${base}/medication-plans/${id}/complete`, method: 'POST' }),
  saveRecord: (data) => request({ url: `${base}/medication-records`, method: 'POST', data }),
  records: (params) => request({ url: `${base}/medication-records`, params })
};

const timeline = (params) => request({ url: `${base}/timeline`, params });
const summary = (data) => request({ url: `${base}/summaries`, method: 'POST', data, timeout: config.request.aiTimeout });
module.exports = { visits, medication, timeline, summary };
