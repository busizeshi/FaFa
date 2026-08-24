const { request } = require('../../utils/request');

Page({
  data: {
    petId: null,
    sessionId: null,
    recommendations: [],
    messages: [],
    input: '',
    loading: false
  },

  onLoad(options) {
    const petId = options.petId ? Number(options.petId) : null;
    this.setData({ petId });
    this.createSession(petId);
    this.loadRecommendations(petId);
  },

  createSession(petId) {
    request({
      url: '/api/chat/sessions',
      method: 'POST',
      data: petId ? { petId } : {}
    }).then((session) => {
      this.setData({ sessionId: session.sessionId, petId: session.petId || petId });
    }).catch(() => {});
  },

  loadRecommendations(petId) {
    const query = petId ? '?petId=' + petId + '&limit=6' : '?limit=6';
    request({ url: '/api/chat/recommendations' + query })
      .then((data) => this.setData({ recommendations: data.items || [] }))
      .catch(() => {});
  },

  onInput(event) {
    this.setData({ input: event.detail.value });
  },

  useRecommendation(event) {
    this.setData({ input: event.currentTarget.dataset.text });
    this.sendMessage(event.currentTarget.dataset.text);
  },

  sendByButton() {
    this.sendMessage(this.data.input);
  },

  sendMessage(message) {
    const text = (message || '').trim();
    if (!text || !this.data.sessionId || this.data.loading) return;
    const userMessage = { role: 'user', content: text };
    this.setData({
      messages: this.data.messages.concat(userMessage),
      input: '',
      loading: true
    });
    request({
      url: '/api/chat/sessions/' + this.data.sessionId + '/messages',
      method: 'POST',
      data: {
        petId: this.data.petId,
        message: text,
        clientMessageId: 'wx-' + Date.now()
      }
    }).then((reply) => {
      this.setData({
        messages: this.data.messages.concat({
          role: 'assistant',
          content: reply.content,
          blocks: reply.blocks || [],
          recordDraft: reply.recordDraft || null
        })
      });
    }).catch(() => {}).then(() => this.setData({ loading: false }));
  },

  openDraft(event) {
    const url = event.currentTarget.dataset.url;
    if (url) wx.navigateTo({ url });
  }
});
