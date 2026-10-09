const config = require('../../config');
const chatService = require('../../services/chat');

function messageId() {
  return `mini-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
}

Page({
  data: { messages: [], recommendations: [], input: '', sessionId: null, pet: null, loading: true, sending: false, error: '', scrollTarget: '', failedMessage: null },

  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    if (tab) tab.setData({ selected: 2 });
    this.setData({ pet: getApp().globalData.currentPet });
    this.loadChat();
  },

  loadChat() {
    const pet = getApp().globalData.currentPet;
    this.setData({ loading: true, error: '' });
    Promise.all([
      chatService.listSessions(pet && pet.id, 1, 20).catch(() => ({ records: [] })),
      chatService.recommendations(pet && pet.id).catch(() => null)
    ]).then(([result, suggestion]) => {
      const sessions = result && Array.isArray(result.records) ? result.records : [];
      const questions = suggestion && (suggestion.questions || suggestion.recommendations || suggestion.items) || [];
      if (!sessions.length) {
        this.setData({ sessionId: null, messages: [], recommendations: questions.map((item) => typeof item === 'string' ? item : item.text || item.question || '').filter(Boolean) });
        return;
      }
      const session = sessions[0];
      return chatService.messages(session.id || session.sessionId, null, 50).then((messages) => {
        const rows = (messages || []).map(this.toMessage);
        this.setData({ sessionId: session.id || session.sessionId, messages: rows, recommendations: questions.map((item) => typeof item === 'string' ? item : item.text || item.question || '').filter(Boolean) }, () => this.scrollToBottom());
      });
    }).catch((error) => this.setData({ error: error.message || '对话暂时无法加载' }))
      .finally(() => this.setData({ loading: false }));
  },

  toMessage(item) {
    return { id: `message-${item.messageId}`, text: item.content || '', role: item.role, time: (item.createdAt || '').slice(11, 16), failed: false };
  },

  onInput(event) { this.setData({ input: event.detail.value }); },

  onSend() {
    const text = (this.data.input || '').trim();
    if (!text || this.data.sending) return;
    const draft = { text, clientMessageId: messageId() };
    this.setData({ input: '', failedMessage: null });
    this.sendMessage(draft);
  },

  sendMessage(draft) {
    const userMessage = { id: `local-${draft.clientMessageId}`, text: draft.text, role: 'user', time: '', failed: false };
    this.setData({ sending: true, messages: this.data.messages.concat(userMessage), error: '' }, () => this.scrollToBottom());
    const ensureSession = this.data.sessionId
      ? Promise.resolve(this.data.sessionId)
      : chatService.createSession(this.data.pet && this.data.pet.id).then((session) => {
        const id = session.id || session.sessionId;
        this.setData({ sessionId: id });
        return id;
      });
    ensureSession.then((sessionId) => chatService.send(sessionId, {
      petId: this.data.pet && this.data.pet.id,
      message: draft.text,
      clientMessageId: draft.clientMessageId
    })).then((reply) => {
      const assistant = { id: `message-${reply.assistantMessageId}`, text: reply.content || '', role: 'assistant', time: '', failed: false };
      const messages = this.data.messages.concat(assistant);
      this.setData({ messages, failedMessage: null, recommendations: reply.suggestedQuestions || [] }, () => this.scrollToBottom());
    }).catch((error) => {
      const messages = this.data.messages.map((item) => item.id === userMessage.id ? Object.assign({}, item, { failed: true }) : item);
      this.setData({ messages, failedMessage: draft, error: error.message || '发送结果暂时无法确认。重试会沿用同一请求编号。' });
    }).finally(() => this.setData({ sending: false }));
  },

  onRetry() {
    if (!this.data.failedMessage || this.data.sending) return;
    this.setData({ messages: this.data.messages.filter((item) => !item.failed), error: '' });
    this.sendMessage(this.data.failedMessage);
  },

  onSuggestion(event) {
    const text = event.currentTarget.dataset.text;
    if (!text || this.data.sending) return;
    this.setData({ input: '' });
    this.sendMessage({ text, clientMessageId: messageId() });
  },
  scrollToBottom() { this.setData({ scrollTarget: `message-${this.data.messages.length}` }); },
  goPets() { wx.navigateTo({ url: config.routes.pets }); }
});
