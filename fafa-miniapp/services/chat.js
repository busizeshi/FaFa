const config = require('../config');
const { request } = require('../utils/request');
const base = `${config.endpoints.chat}/sessions`;

function createSession(petId) {
  return request({ url: base, method: 'POST', data: petId ? { petId } : {} });
}
function listSessions(petId, page = 1, size = config.request.pageSize) {
  return request({ url: base, params: { petId, page, size } });
}
function messages(sessionId, beforeId, limit = 50) {
  return request({ url: `${base}/${sessionId}/messages`, params: { beforeId, limit } });
}
function send(sessionId, data) {
  return request({ url: `${base}/${sessionId}/messages`, method: 'POST', data, timeout: config.request.aiTimeout });
}
function recommendations(petId, limit = 6) {
  return request({ url: `${config.endpoints.chat}/recommendations`, params: { petId, limit } });
}

module.exports = { createSession, listSessions, messages, send, recommendations };
