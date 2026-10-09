const config = require('../config');
const { request, upload } = require('../utils/request');

function getMe() { return request({ url: config.endpoints.user.me }); }
function updateProfile(data) { return request({ url: config.endpoints.user.profile, method: 'PUT', data }); }
function uploadAvatar(filePath) {
  return upload({ url: config.endpoints.user.avatar, filePath, name: 'file', timeout: config.request.uploadTimeout });
}
function getVip() { return request({ url: config.endpoints.user.vip }); }
function getQuota() { return request({ url: config.endpoints.quota }); }

module.exports = { getMe, updateProfile, uploadAvatar, getVip, getQuota };
