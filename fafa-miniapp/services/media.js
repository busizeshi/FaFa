const config = require('../config');
const { request, upload } = require('../utils/request');
const base = config.endpoints.photos;

function uploadOne(file, fields) {
  return upload({ url: `${base}/upload`, filePath: file.path, name: 'files', formData: fields, timeout: config.request.uploadTimeout })
    .then((data) => Array.isArray(data) ? data : [data]);
}

function uploadBatch(files, fields) {
  if (files.length > config.uploads.photoLimit) return Promise.reject({ message: `单次最多选择 ${config.uploads.photoLimit} 项` });
  let completed = [];
  return files.reduce((promise, file) => promise.then(() => uploadOne(file, fields).then((uploaded) => { completed = completed.concat(uploaded); })), Promise.resolve())
    .then(() => completed)
    .catch((error) => {
      error.uploadedCount = completed.length;
      error.message = `${completed.length ? `已有 ${completed.length} 项上传成功；` : ''}${error.message || '上传失败'}`;
      throw error;
    });
}

const photos = {
  search: (data) => request({ url: `${base}/search`, method: 'POST', data }),
  get: (id) => request({ url: `${base}/${id}` }),
  upload: (files, fields = {}) => uploadBatch(files, fields),
  update: (id, data) => request({ url: `${base}/${id}`, method: 'PATCH', data }),
  confirmPet: (id, petId) => request({ url: `${base}/${id}/confirm-pet`, method: 'POST', data: { petId } }),
  remove: (id) => request({ url: `${base}/${id}`, method: 'DELETE' })
};

module.exports = { photos };
