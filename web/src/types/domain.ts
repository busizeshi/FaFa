export type RecordType = 'feeding' | 'weight' | 'water' | 'toilet' | 'event'
export interface Pet { id: string; name: string; breed: string; gender: string; age: string; weight: number; avatar: string; arrivedDays: number }
export interface Log { id: string; type: RecordType; title: string; value: string; date: string; time: string; note?: string; icon: string; tint: 'blue' | 'green' | 'orange' | 'gray' }
export interface Reminder { id: string; title: string; date: string; repeat: string; icon: string; done?: boolean; tint: 'blue' | 'green' | 'orange' }
