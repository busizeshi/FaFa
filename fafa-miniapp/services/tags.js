const config = require('../config');
const { request } = require('../utils/request');

function list() { return request({ url: config.endpoints.tags }); }
function create(data) { return request({ url: config.endpoints.tags, method: 'POST', data }); }
function remove(id) { return request({ url: `${config.endpoints.tags}/${id}`, method: 'DELETE' }); }

module.exports = { list, create, remove };
