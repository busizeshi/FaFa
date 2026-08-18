import type { Log, Pet, Reminder } from '@/types/domain'

export const pet: Pet = { id: 'doubao', name: '豆包', breed: '英国短毛猫', gender: '公', age: '3岁2个月', weight: 5.2, avatar: '/static/images/doubao-avatar.png', arrivedDays: 873 }
export const feedingPlan = [
  { time: '07:30', title: '早餐', detail: '皇家英短成猫粮 · 25g', done: true },
  { time: '13:00', title: '午间加餐', detail: '鸡肉冻干 · 10g', done: true },
  { time: '19:00', title: '晚餐', detail: '皇家英短成猫粮 · 25g', done: false }
]
export const logs: Log[] = [
  { id: 'f1', type: 'feeding', title: '皇家英短成猫粮', value: '32 g', date: '今天', time: '08:15', note: '食欲很好', icon: 'compose', tint: 'orange' },
  { id: 'w1', type: 'water', title: '饮水记录', value: '120 ml', date: '今天', time: '09:40', icon: 'calendar', tint: 'blue' },
  { id: 'weight1', type: 'weight', title: '体重记录', value: '5.2 kg', date: '8月16日', time: '10:20', note: '与上次持平', icon: 'checkbox', tint: 'green' },
  { id: 'toilet1', type: 'toilet', title: '排便状态正常', value: '正常', date: '8月15日', time: '20:18', icon: 'list', tint: 'green' },
  { id: 'event1', type: 'event', title: '在阳台晒太阳', value: '日常事件', date: '8月15日', time: '14:30', note: '晒了一下午，心情很好。', icon: 'star', tint: 'blue' }
]
export const reminders: Reminder[] = [
  { id: 'nail', title: '剪指甲', date: '今天', repeat: '每 14 天', icon: 'compose', tint: 'orange' },
  { id: 'deworm', title: '体内驱虫', date: '11月14日', repeat: '每 3 个月', icon: 'calendar', tint: 'green' },
  { id: 'bath', title: '洗澡护理', date: '8月25日', repeat: '每 30 天', icon: 'notification', tint: 'blue' }
]
export const quickActions = [
  { type: 'feeding', icon: 'compose', label: '喂食', tint: 'orange' },
  { type: 'water', icon: 'calendar', label: '饮水', tint: 'blue' },
  { type: 'weight', icon: 'checkbox', label: '体重', tint: 'green' },
  { type: 'toilet', icon: 'list', label: '排便', tint: 'blue' },
  { type: 'photo', icon: 'camera', label: '照片', tint: 'orange' },
  { type: 'event', icon: 'star', label: '事件', tint: 'green' }
]
