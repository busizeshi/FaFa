const { request } = require('./request');

function utf8Bytes(value) {
  const encoded = unescape(encodeURIComponent(value));
  const bytes = new Uint8Array(encoded.length);
  for (let i = 0; i < encoded.length; i += 1) bytes[i] = encoded.charCodeAt(i);
  return bytes;
}

function readFile(path) {
  return new Promise((resolve, reject) => {
    wx.getFileSystemManager().readFile({
      filePath: path,
      success: (result) => resolve(result.data),
      fail: () => reject({ code: 'FILE_READ_ERROR', message: '读取图片失败，请重新选择' })
    });
  });
}

function joinParts(parts) {
  const size = parts.reduce((total, part) => total + part.byteLength, 0);
  const output = new Uint8Array(size);
  let offset = 0;
  parts.forEach((part) => { output.set(part, offset); offset += part.byteLength; });
  return output.buffer;
}

async function uploadFiles(options) {
  const boundary = `----FaFaBoundary${Date.now().toString(16)}`;
  const parts = [];
  Object.keys(options.fields || {}).forEach((key) => {
    const value = options.fields[key];
    if (value === undefined || value === null || value === '') return;
    parts.push(utf8Bytes(`--${boundary}\r\nContent-Disposition: form-data; name="${key}"\r\n\r\n${value}\r\n`));
  });
  for (const file of options.files) {
    const bytes = await readFile(file.path);
    const type = file.type || 'image/jpeg';
    const name = file.name || 'upload.jpg';
    parts.push(utf8Bytes(`--${boundary}\r\nContent-Disposition: form-data; name="${file.field}"; filename="${name}"\r\nContent-Type: ${type}\r\n\r\n`));
    parts.push(new Uint8Array(bytes));
    parts.push(utf8Bytes('\r\n'));
  }
  parts.push(utf8Bytes(`--${boundary}--\r\n`));
  return request({
    url: options.url,
    method: options.method || 'POST',
    data: joinParts(parts),
    formData: true,
    header: { 'Content-Type': `multipart/form-data; boundary=${boundary}` },
    timeout: options.timeout || require('../config').request.uploadTimeout
  });
}

module.exports = { uploadFiles };
