function authorizePrivacy() {
  if (!wx.requirePrivacyAuthorize) return Promise.resolve();
  return new Promise((resolve, reject) => {
    wx.requirePrivacyAuthorize({ success: resolve, fail: reject });
  });
}

function openPrivacyContract() {
  if (!wx.openPrivacyContract) {
    wx.showToast({ title: '请在微信设置中查看隐私指引', icon: 'none' });
    return;
  }
  wx.openPrivacyContract({ fail: () => wx.showToast({ title: '暂时无法打开隐私指引', icon: 'none' }) });
}

module.exports = { authorizePrivacy, openPrivacyContract };
