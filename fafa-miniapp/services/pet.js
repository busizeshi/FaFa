const config = require('../config');
const { request } = require('../utils/request');
const { uploadFiles } = require('../utils/multipart');

function list() { return request({ url: config.endpoints.pets }); }
function get(id) { return request({ url: `${config.endpoints.pets}/${id}` }); }
function update(id, data) { return request({ url: `${config.endpoints.pets}/${id}`, method: 'PUT', data }); }
function remove(id) { return request({ url: `${config.endpoints.pets}/${id}`, method: 'DELETE' }); }
function access(id) { return request({ url: `${config.endpoints.pets}/${id}/access` }); }
function members(id) { return request({ url: `${config.endpoints.pets}/${id}/members` }); }
function invite(id, data) { return request({ url: `${config.endpoints.pets}/${id}/invitations`, method: 'POST', data }); }
function acceptInvitation(data) { return request({ url: `${config.endpoints.invitations}/accept`, method: 'POST', data }); }
function auditLogs(id, page = 1, size = config.request.pageSize) { return request({ url: `${config.endpoints.pets}/${id}/audit-logs`, params: { page, size } }); }

function create(data, photoPaths) {
  if (!photoPaths || photoPaths.length !== 3) {
    return Promise.reject({ code: 'VALIDATION_ERROR', message: '请按头像、正面照、侧面照选择三张照片' });
  }
  const files = photoPaths.map((path, index) => ({
    path,
    field: 'photos',
    name: `pet-photo-${index + 1}.jpg`
  }));
  return uploadFiles({ url: config.endpoints.pets, fields: data, files });
}

module.exports = { list, get, create, update, remove, access, members, invite, acceptInvitation, auditLogs };
