const config = require('../config');

function getToken() {
  return wx.getStorageSync(config.storage.token) || '';
}

function saveSession(token, user) {
  wx.setStorageSync(config.storage.token, token);
  wx.setStorageSync(config.storage.user, user || null);
  const app = getApp();
  app.globalData.user = user || null;
}

function clearSession() {
  wx.removeStorageSync(config.storage.token);
  wx.removeStorageSync(config.storage.user);
  wx.removeStorageSync(config.storage.currentPet);
  const app = getApp();
  app.globalData.user = null;
  app.globalData.currentPet = null;
}

module.exports = { getToken, saveSession, clearSession };
