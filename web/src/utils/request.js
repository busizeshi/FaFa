import { config } from './config'
import { getStore, saveStore, currentPet, nextId } from './store'

function unwrap(response) {
  if (response && typeof response === 'object' && 'data' in response && ('code' in response || 'message' in response)) return response.data
  return response
}

function mockRequest(path, method = 'GET', data = {}) {
  const store = getStore()
  const petId = Number(data.petId || currentPet()?.id || 1)
  if (path === '/auth/wechat/login' && method === 'POST') return { token: 'mock-token', userId: 1, nickname: 'FaFa 用户', avatar: '', isNewUser: false }
  if (path === '/auth/user/info') return store.user
  if (path === '/pet/list') return store.pets
  if (path === '/pet' && method === 'POST') { const item = { id: nextId(store.pets), avatar: '/static/assets/pet-avatar-doubao.jpg', speciesName: data.species === 'dog' ? '狗' : data.species === 'other' ? '其他' : '猫', genderName: data.gender === 'female' ? '母' : '公', ageInMonths: 1, status: 1, ...data }; store.pets.push(item); saveStore(store); return item }
  if (path.startsWith('/pet/') && method === 'PUT') { const item = store.pets.find(pet => pet.id === Number(path.split('/').pop())); if (item) Object.assign(item, data); saveStore(store); return item }
  if (path.startsWith('/pet/')) return store.pets.find(item => item.id === Number(path.split('/').pop()))
  if (path === '/records/feeding' && method === 'GET') return { records: store.feeding.filter(item => item.petId === petId), total: store.feeding.length, page: 1, size: 20 }
  if (path === '/records/feeding/quick' && method === 'POST') {
    const last = store.feeding.find(item => item.petId === petId) || { foodName: '默认猫粮', amount: 30, unit: 'g' }
    const item = { id: nextId(store.feeding), petId, feedTime: new Date().toISOString(), foodName: last.foodName, amount: last.amount, unit: last.unit, remarks: '' }
    store.feeding.unshift(item); saveStore(store); return item
  }
  if (path === '/records/feeding' && method === 'POST') { const item = { id: nextId(store.feeding), ...data, petId }; store.feeding.unshift(item); saveStore(store); return item }
  if (path === '/api/records/weight' && method === 'GET') return store.weight.filter(item => item.petId === petId)
  if (path === '/api/records/weight/trend' && method === 'GET') { const data = store.weight.filter(item => item.petId === petId); return { data, analysis: { startWeight: data[0]?.weight, endWeight: data[data.length - 1]?.weight, change: Number((data[data.length - 1]?.weight - data[0]?.weight).toFixed(2)), trend: 'stable' } } }
  if (path === '/api/records/weight' && method === 'POST') { const item = { id: nextId(store.weight), ...data, petId }; store.weight.push(item); saveStore(store); return item }
  if (path === '/api/records/water' && method === 'GET') return store.water.filter(item => item.petId === petId)
  if (path === '/api/records/water' && method === 'POST') { const item = { id: nextId(store.water), ...data, petId }; store.water.unshift(item); saveStore(store); return item }
  if (path === '/api/records/excretion' && method === 'GET') return store.excretion.filter(item => item.petId === petId)
  if (path === '/api/records/excretion' && method === 'POST') { const item = { id: nextId(store.excretion), ...data, petId }; store.excretion.unshift(item); saveStore(store); return item }
  if (path === '/api/records/event' && method === 'GET') return store.events.filter(item => item.petId === petId)
  if (path === '/api/records/event' && method === 'POST') { const item = { id: nextId(store.events), ...data, petId }; store.events.unshift(item); saveStore(store); return item }
  if (path === '/api/reminders' && method === 'GET') return store.reminders.filter(item => !data.status || item.status === data.status).filter(item => item.petId === petId)
  if (path === '/api/reminders' && method === 'POST') { const item = { id: nextId(store.reminders), status: 'pending', ...data, petId }; store.reminders.unshift(item); saveStore(store); return item }
  if (path.includes('/complete') && method === 'PUT') { const id = Number(path.split('/')[3]); const item = store.reminders.find(reminder => reminder.id === id); if (item) item.status = 'completed'; saveStore(store); return item }
  if (path === '/api/photos' && method === 'GET') return { records: store.photos.filter(item => item.petId === petId), total: store.photos.length }
  if (path === '/api/photos/search' && method === 'POST') { const words = String(data.query || '').split(/[，。！？、\s]+/).filter(word => word.length > 1); return store.photos.filter(item => item.petId === petId && (words.length === 0 || words.some(word => (item.description + item.tags.join('')).includes(word)))) }
  if (path === '/api/ai/chat' && method === 'POST') return { answer: `我结合豆包的历史记录看了一下：${data.message || '这件事'}。目前数据整体比较稳定，建议继续保持记录。`, toolsCalled: ['get_pet_profile'] }
  if (path === '/api/ai/suggestions' && method === 'POST') return ['豆包最近体重怎么样？', '这个月吃了多少猫粮？', '帮我找去年夏天在阳台的照片', '生成一份8月宠物月报']
  return null
}

export function request({ url, method = 'GET', data = {}, header = {}, filePath, name, formData } = {}) {
  if (config.mock) return Promise.resolve(mockRequest(url, method, data))
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('fafa-token')
    uni.request({ url: `${config.apiBaseUrl}${url}`, method, data, timeout: config.requestTimeout, header: { 'Content-Type': 'application/json', ...(token ? { satoken: token } : {}), ...header }, success: (res) => { const body = unwrap(res.data); if (res.statusCode >= 200 && res.statusCode < 300) resolve(body); else reject(new Error(body?.message || '请求失败')) }, fail: reject })
  })
}

export function upload({ filePath, formData = {} } = {}) {
  if (config.mock) { const store = getStore(); const item = { id: nextId(store.photos), petId: Number(formData.petId), url: filePath, takenAt: new Date().toISOString().slice(0, 10), description: '刚上传的宠物照片', tags: [] }; store.photos.unshift(item); saveStore(store); return Promise.resolve({ photoId: item.id, url: filePath }) }
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('fafa-token')
    uni.uploadFile({ url: `${config.apiBaseUrl}/api/photos/upload`, filePath, name: 'file', formData, header: token ? { satoken: token } : {}, success: (res) => { try { resolve(unwrap(JSON.parse(res.data))) } catch { resolve(res.data) } }, fail: reject })
  })
}
