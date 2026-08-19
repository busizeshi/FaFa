<template>
  <view class="page home-page">
    <view class="home-welcome"><view><text class="welcome-date">2026年8月18日 · 星期二</text><text class="welcome-title">今天也要好好照顾豆包</text></view><view class="welcome-orb"><FaIcon name="spark" tone="primary" filled :size="28" /></view></view>
    <PetBar :pet="pet" />

    <view class="section card plan-card">
      <view class="between plan-heading"><view><text class="section-title no-margin">今日计划</text><text class="caption block">让每一顿、每一次陪伴都被记得</text></view><view class="plan-progress"><text>{{ plan.filter(item => item.done).length }}</text><text>/ {{ plan.length }}</text></view></view>
      <view v-for="item in plan" :key="item.time" class="plan-row">
        <view class="plan-check" :class="{ done: item.done }"><FaIcon v-if="item.done" name="check" tone="success" filled :size="22" /><view v-else class="plan-dot"></view></view>
        <text class="plan-time" :class="{ doneText: item.done }">{{ item.time }}</text>
        <text class="plan-name" :class="{ doneText: item.done }">{{ item.name }}</text>
        <text class="plan-amount" :class="{ doneText: item.done }">{{ item.amount }}</text>
      </view>
    </view>

    <view class="section card reminder-card" v-if="reminder">
      <view class="between"><view><text class="section-title no-margin">今日提醒</text><text class="caption block">{{ reminder.remindTime }}</text></view><view class="reminder-badge"><FaIcon name="bell" tone="primary" :size="28" /></view></view>
      <view class="between reminder-main"><text class="body">{{ reminder.title }}</text><button class="pill pill--soft" @click="completeReminder">完成</button></view>
    </view>

    <view class="section">
      <view class="between mb-2"><text class="section-title no-margin">快速记录</text><text class="caption">今天留下一个小片段</text></view>
      <view class="quick-grid card">
        <view v-for="item in quickItems" :key="item.key" class="quick-item" @click="quickRecord(item)">
          <view class="quick-icon" :style="{ background: item.bg, color: item.color }"><FaIcon :name="item.icon" :tone="item.tone" :size="36" /></view>
          <text>{{ item.label }}</text>
        </view>
      </view>
    </view>

    <view class="section ai-card discovery" @click="go('/pages/ai/home')">
      <view class="between"><view class="row gap-1"><view class="ai-dot"><FaIcon name="spark" tone="primary" filled :size="27" /></view><view><text class="subheading">AI 发现</text><text class="caption block">豆包的生活小洞察</text></view></view><view class="insight-chip">数据洞察</view></view>
      <text class="discovery-text">最近 14 天豆包体重基本稳定，平均每天 5.2kg。</text>
      <view class="discovery-link"><text>查看详情</text><FaIcon name="next" tone="primary" :size="26" /></view>
    </view>

    <view class="section card memory-card">
      <view class="between memory-heading"><text class="section-title no-margin">去年的今天</text><text class="caption">一段温柔回忆</text></view>
      <view class="memory-media"><image src="/static/assets/pet-photo-sleeping.jpg" mode="aspectFill" class="memory-image" /><view class="memory-overlay"><text>豆包在阳台晒了一下午太阳</text><FaIcon name="pet" tone="default" filled :size="26" /></view></view>
    </view>

    <view class="fab" @click="go('/pages/ai/chat')"><FaIcon name="spark" tone="default" filled :size="34" /></view>
    <BottomNav current="home" />
  </view>
</template>

<script>
import BottomNav from '../../components/BottomNav.vue'
import PetBar from '../../components/PetBar.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'

export default {
  components: { BottomNav, PetBar, FaIcon },
  data() { return { pet: currentPet(), today: '2026年8月18日', plan: [{ time: '07:30', name: '早餐', amount: '25g', done: true }, { time: '13:00', name: '午间加餐', amount: '10g', done: true }, { time: '19:00', name: '晚餐', amount: '25g', done: false }, { time: '22:30', name: '夜宵', amount: '10g', done: false }], reminder: null, quickItems: [{ key: 'feeding', label: '喂食', icon: 'food', bg: '#FAF0EB', color: '#C4612F', tone: 'primary', path: '/pages/records/feeding-edit' }, { key: 'water', label: '饮水', icon: 'water', bg: '#EFF6FF', color: '#5A8EAD', tone: 'default', path: '/pages/records/water' }, { key: 'weight', label: '体重', icon: 'weight', bg: '#F0FDF4', color: '#5B8C5A', tone: 'success', path: '/pages/records/weight-edit' }, { key: 'excretion', label: '排便', icon: 'excretion', bg: '#FFF8E7', color: '#D49B3A', tone: 'default', path: '/pages/records/excretion' }, { key: 'photo', label: '照片', icon: 'photo', bg: '#FAF5FF', color: '#9333EA', tone: 'default', path: '/pages/photos/wall' }, { key: 'event', label: '事件', icon: 'event', bg: '#F1F5F9', color: '#5C635D', tone: 'default', path: '/pages/records/event' }] } },
  onShow() { this.pet = currentPet(); this.loadReminders() },
  onPullDownRefresh() { this.loadReminders().finally(() => uni.stopPullDownRefresh()) },
  methods: {
    loadReminders() { return api.reminders.list({ petId: this.pet.id, status: 'pending' }).then(list => { this.reminder = Array.isArray(list) ? list[0] : null }) },
    completeReminder() { if (!this.reminder) return; api.reminders.complete(this.reminder.id).then(() => { uni.showToast({ title: '已完成', icon: 'success' }); this.reminder = null }) },
    quickRecord(item) { uni.navigateTo({ url: item.path }) },
    go(path) { uni.navigateTo({ url: path }) }
  }
}
</script>

<style scoped>
.no-margin { margin: 0; }
.block { display: block; margin-top: 4rpx; }
.home-welcome { display: flex; align-items: center; justify-content: space-between; margin: 0 4rpx 24rpx; }
.welcome-date { display: block; color: #9A9185; font-size: 23rpx; }
.welcome-title { display: block; margin-top: 6rpx; color: #1F2421; font-size: 38rpx; line-height: 1.25; font-weight: 700; letter-spacing: -1rpx; }
.welcome-orb { width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center; border-radius: 22rpx; background: #FAF0EB; }
.plan-card, .reminder-card, .memory-card { padding: 28rpx; }
.plan-heading { align-items: flex-start; margin-bottom: 16rpx; }
.plan-progress { display: flex; align-items: baseline; gap: 2rpx; color: #A85228; font-size: 25rpx; }
.plan-progress text:first-child { font-size: 40rpx; line-height: 1; font-weight: 700; }
.plan-row { display: flex; align-items: center; min-height: 76rpx; border-top: 2rpx solid #F7F4EF; }
.plan-check { width: 36rpx; height: 36rpx; margin-right: 20rpx; display: flex; align-items: center; justify-content: center; border: 2rpx solid #D4CDC0; border-radius: 999rpx; }
.plan-check.done { border-color: #5B8C5A; background: #F0FDF4; }
.plan-dot { width: 12rpx; height: 12rpx; border-radius: 999rpx; background: #D4CDC0; }
.plan-time { width: 108rpx; font-size: 28rpx; color: #7C7367; }
.plan-name { flex: 1; font-size: 28rpx; color: #1F2421; }
.plan-amount { font-size: 28rpx; color: #1F2421; font-weight: 600; }
.doneText { color: #B8B0A3 !important; text-decoration: line-through; }
.reminder-badge, .ai-dot { width: 56rpx; height: 56rpx; border-radius: 999rpx; display: flex; align-items: center; justify-content: center; background: #FAF0EB; }
.reminder-main { margin-top: 24rpx; padding-top: 24rpx; border-top: 2rpx solid #F7F4EF; }
.quick-grid { display: grid; grid-template-columns: repeat(3, 1fr); padding: 16rpx 10rpx; }
.quick-item { min-height: 132rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10rpx; color: #5C635D; font-size: 25rpx; }
.quick-icon { width: 68rpx; height: 68rpx; border-radius: 22rpx; display: flex; align-items: center; justify-content: center; }
.discovery { padding: 28rpx; }
.insight-chip { padding: 8rpx 14rpx; border-radius: 999rpx; background: rgba(255,255,255,.58); color: #A85228; font-size: 21rpx; }
.discovery-text { display: block; margin-top: 22rpx; font-size: 30rpx; line-height: 1.55; color: #3D3D3A; }
.discovery-link { display: flex; align-items: center; gap: 4rpx; margin-top: 18rpx; color: #A85228; font-size: 26rpx; font-weight: 600; }
.memory-heading { margin-bottom: 18rpx; }
.memory-media { position: relative; overflow: hidden; border-radius: 24rpx; }
.memory-image { width: 100%; height: 300rpx; display: block; }
.memory-overlay { position: absolute; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; justify-content: space-between; padding: 54rpx 22rpx 20rpx; background: linear-gradient(180deg, transparent, rgba(31,24,18,.62)); color: #fff; font-size: 28rpx; }
</style>
