const seed = {
  user: { userId: 1, nickname: 'FaFa 用户', avatar: '', registerDate: '2024-03-01' },
  pets: [
    { id: 1, name: '豆包', avatar: '/static/assets/pet-avatar-doubao.jpg', species: 'cat', speciesName: '猫', breed: '英国短毛猫', gender: 'male', genderName: '公', birthDate: '2023-06-18', adoptDate: '2024-03-01', ageInMonths: 38, weight: 5.2, isNeutered: true, coatColor: '蓝白', status: 1 },
    { id: 2, name: '团子', avatar: '/static/assets/pet-avatar-doubao.jpg', species: 'cat', speciesName: '猫', breed: '布偶猫', gender: 'female', genderName: '母', birthDate: '2025-03-03', adoptDate: '2025-05-10', ageInMonths: 17, weight: 3.8, isNeutered: false, coatColor: '重点色', status: 1 }
  ],
  feeding: [
    { id: 1, petId: 1, feedTime: '2026-08-18T07:30:00', foodName: '皇家英短成猫粮', amount: 25, unit: 'g', remarks: '' },
    { id: 2, petId: 1, feedTime: '2026-08-18T13:00:00', foodName: '鸡肉冻干', amount: 10, unit: 'g', remarks: '' },
    { id: 3, petId: 1, feedTime: '2026-08-17T19:00:00', foodName: '皇家英短成猫粮', amount: 25, unit: 'g', remarks: '' }
  ],
  weight: [
    { id: 1, petId: 1, recordDate: '2026-07-15', weight: 5.3 },
    { id: 2, petId: 1, recordDate: '2026-07-28', weight: 5.25 },
    { id: 3, petId: 1, recordDate: '2026-08-10', weight: 5.2 },
    { id: 4, petId: 1, recordDate: '2026-08-18', weight: 5.2 }
  ],
  water: [{ id: 1, petId: 1, recordTime: '2026-08-18T09:10:00', amount: 120 }, { id: 2, petId: 1, recordTime: '2026-08-17T16:20:00', amount: 180 }],
  excretion: [{ id: 1, petId: 1, recordTime: '2026-08-18T08:40:00', type: 'feces', status: 'normal', remarks: '' }, { id: 2, petId: 1, recordTime: '2026-08-17T20:10:00', type: 'urine', status: 'normal', remarks: '' }],
  events: [{ id: 1, petId: 1, eventTime: '2026-08-14', eventType: 'deworming', title: '完成本季度驱虫', content: '', tags: ['健康相关'] }, { id: 2, petId: 1, eventTime: '2026-08-03', eventType: 'food_change', title: '更换新的猫粮', content: '', tags: ['饮食变化'] }],
  reminders: [
    { id: 1, petId: 1, title: '剪指甲', reminderType: 'nail', remindTime: '2026-08-19 10:00', repeatType: 'monthly', status: 'pending' },
    { id: 2, petId: 1, title: '体内驱虫', reminderType: 'deworming', remindTime: '2026-08-21 09:00', repeatType: 'quarterly', status: 'pending' },
    { id: 3, petId: 1, title: '年度体检', reminderType: 'checkup', remindTime: '2026-08-30 14:00', repeatType: 'yearly', status: 'pending' },
    { id: 4, petId: 1, title: '洗澡', reminderType: 'bath', remindTime: '2026-08-05 15:00', repeatType: 'monthly', status: 'completed' }
  ],
  photos: [
    { id: 1, petId: 1, url: '/static/assets/pet-photo-sleeping.jpg', takenAt: '2026-08-17', description: '豆包趴在阳台蓝色猫窝里睡觉', tags: ['阳台', '睡觉'] },
    { id: 2, petId: 1, url: '/static/assets/pet-photo-playing.jpg', takenAt: '2026-08-15', description: '豆包在家里玩耍', tags: ['玩耍'] },
    { id: 3, petId: 1, url: '/static/assets/pet-photo-sleeping.jpg', takenAt: '2026-07-28', description: '午后晒太阳', tags: ['阳台', '睡觉'] },
    { id: 4, petId: 1, url: '/static/assets/pet-photo-playing.jpg', takenAt: '2026-07-21', description: '纸箱探险', tags: ['玩耍', '纸箱'] }
  ],
  conversations: [{ id: 1, title: '豆包体重趋势分析', preview: '最近30天体重从5.3kg降至5.2kg...', time: '2小时前' }, { id: 2, title: '8月饮食总结', preview: '平均每日主粮73g，比上月减少4%', time: '昨天' }]
}

function clone(value) { return JSON.parse(JSON.stringify(value)) }

export function getStore() {
  const saved = uni.getStorageSync('fafa-store')
  if (!saved) {
    uni.setStorageSync('fafa-store', seed)
    return clone(seed)
  }
  return saved
}

export function saveStore(next) {
  uni.setStorageSync('fafa-store', next)
  return next
}

export function resetStore() { uni.removeStorageSync('fafa-store'); return clone(seed) }

export function currentPet() {
  const store = getStore()
  const id = Number(uni.getStorageSync('fafa-pet-id')) || store.pets[0]?.id
  return store.pets.find(pet => Number(pet.id) === id) || store.pets[0]
}

export function setCurrentPet(id) { uni.setStorageSync('fafa-pet-id', Number(id)) }

export function nextId(list) { return list.reduce((max, item) => Math.max(max, Number(item.id) || 0), 0) + 1 }
