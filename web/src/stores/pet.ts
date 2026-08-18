import { defineStore } from 'pinia'
import { logs, pet, reminders } from '@/data/mock'
import type { Log, RecordType } from '@/types/domain'

export const usePetStore = defineStore('pet', {
  state: () => ({ pet, logs: [...logs] as Log[], reminders: [...reminders] }),
  getters: { todayCount: (state) => state.logs.filter((item) => item.date === '今天').length, activeReminders: (state) => state.reminders.filter((item) => !item.done) },
  actions: {
    addRecord(type: string, value: string, note: string) {
      const typeMap: Record<string, Pick<Log, 'icon' | 'title' | 'tint'>> = {
        feeding: { icon: 'compose', title: '皇家英短成猫粮', tint: 'orange' }, water: { icon: 'calendar', title: '饮水记录', tint: 'blue' }, weight: { icon: 'checkbox', title: '体重记录', tint: 'green' }, toilet: { icon: 'list', title: '排便状态正常', tint: 'green' }, event: { icon: 'star', title: note || '新的日常事件', tint: 'blue' }, photo: { icon: 'camera', title: '上传了 1 张照片', tint: 'orange' }
      }
      const current = typeMap[type] || typeMap.event
      const unit = type === 'feeding' ? ' g' : type === 'water' ? ' ml' : type === 'weight' ? ' kg' : ''
      this.logs.unshift({ id: `${type}-${Date.now()}`, type: (type === 'photo' ? 'event' : type) as RecordType, title: current.title, value: value ? `${value}${unit}` : '已记录', date: '今天', time: '刚刚', note, icon: current.icon, tint: current.tint })
    },
    completeReminder(id: string) { const target = this.reminders.find((item) => item.id === id); if (target) target.done = true }
  }
})
