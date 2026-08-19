import { request, upload } from '../utils/request'

export const api = {
  auth: {
    login(code) { return request({ url: '/auth/wechat/login', method: 'POST', data: { code } }) },
    userInfo() { return request({ url: '/auth/user/info' }) }
  },
  pets: {
    list() { return request({ url: '/pet/list' }) },
    detail(id) { return request({ url: `/pet/${id}` }) },
    create(data) { return request({ url: '/pet', method: 'POST', data }) },
    update(id, data) { return request({ url: `/pet/${id}`, method: 'PUT', data }) }
  },
  feeding: {
    list(params) { return request({ url: '/records/feeding', data: params }) },
    quick(petId) { return request({ url: '/records/feeding/quick', method: 'POST', data: { petId } }) },
    create(data) { return request({ url: '/records/feeding', method: 'POST', data }) }
  },
  weight: {
    list(params) { return request({ url: '/api/records/weight', data: params }) },
    trend(params) { return request({ url: '/api/records/weight/trend', data: params }) },
    create(data) { return request({ url: '/api/records/weight', method: 'POST', data }) }
  },
  water: {
    list(params) { return request({ url: '/api/records/water', data: params }) },
    create(data) { return request({ url: '/api/records/water', method: 'POST', data }) }
  },
  excretion: {
    list(params) { return request({ url: '/api/records/excretion', data: params }) },
    create(data) { return request({ url: '/api/records/excretion', method: 'POST', data }) }
  },
  events: {
    list(params) { return request({ url: '/api/records/event', data: params }) },
    create(data) { return request({ url: '/api/records/event', method: 'POST', data }) }
  },
  reminders: {
    list(params) { return request({ url: '/api/reminders', data: params }) },
    create(data) { return request({ url: '/api/reminders', method: 'POST', data }) },
    complete(id) { return request({ url: `/api/reminders/${id}/complete`, method: 'PUT', data: {} }) }
  },
  photos: {
    list(params) { return request({ url: '/api/photos', data: params }) },
    search(data) { return request({ url: '/api/photos/search', method: 'POST', data }) },
    upload(filePath, petId) { return upload({ filePath, formData: { petId } }) }
  },
  ai: {
    chat(data) { return request({ url: '/api/ai/chat', method: 'POST', data }) },
    suggestions(data) { return request({ url: '/api/ai/suggestions', method: 'POST', data }) }
  }
}
