<template>
  <view class="page mine-page">
    <view class="profile-banner"><view class="avatar-frame"><image class="user-avatar" :src="user.avatar || '/static/assets/fafa-avatar.png'" mode="aspectFill" /><view class="avatar-glow"></view></view><view class="user-info"><text class="user-kicker">FAFA MEMBER</text><text class="user-name">{{ user.nickname }}</text><text class="user-sub">已陪伴 {{ pet.name }} {{ companionDays }} 天</text></view><view class="settings-btn" @click="go('/pages/settings/settings')"><FaIcon name="settings" tone="default" :size="30" /></view></view>
    <view class="card stat-card"><view v-for="item in stats" :key="item.label" class="stat-item"><text class="stat-value" :class="item.tone">{{ item.value }}</text><text class="caption">{{ item.label }}</text></view></view>
    <view class="section"><text class="section-label">我的空间</text><view class="card entry-list"><view v-for="item in entries" :key="item.key" class="entry" @click="go(item.path)"><view class="entry-icon" :style="{ background: item.bg }"><FaIcon :name="item.icon" :tone="item.tone" :filled="item.key === 'pets'" :size="32" /></view><text class="entry-name">{{ item.label }}</text><text v-if="item.badge" class="entry-badge">{{ item.badge }}</text><FaIcon name="next" tone="muted" :size="30" /></view></view></view>
    <BottomNav current="mine" />
  </view>
</template>

<script>
import BottomNav from '../../components/BottomNav.vue'
import FaIcon from '../../components/FaIcon.vue'
import { currentPet, getStore } from '../../utils/store'
export default { components: { BottomNav, FaIcon }, data() { const store = getStore(); return { user: store.user, pet: currentPet(), companionDays: 873, stats: [{ value: store.pets.length, label: '宠物', tone: '' }, { value: store.reminders.filter(item => item.status === 'pending').length, label: '待办提醒', tone: 'primary' }, { value: 5, label: 'AI发现', tone: '' }], entries: [{ key: 'pets', label: '我的宠物', icon: 'pet', tone: 'primary', bg: '#FAF0EB', path: '/pages/pet/list' }, { key: 'reminders', label: '提醒中心', icon: 'bell', tone: 'default', bg: '#FFF8E7', badge: '3', path: '/pages/reminders/list' }, { key: 'reports', label: '宠物报告', icon: 'report', tone: 'default', bg: '#EFF6FF', path: '/pages/reports/list' }, { key: 'photos', label: '照片墙', icon: 'photo', tone: 'default', bg: '#FAF5FF', path: '/pages/photos/wall' }, { key: 'feedback', label: '意见反馈', icon: 'message', tone: 'success', bg: '#F0FDF4', path: '/pages/settings/settings' }, { key: 'about', label: '关于 FaFa', icon: 'info', tone: 'muted', bg: '#F9FAFB', path: '/pages/settings/settings' }] } }, methods: { go(path) { uni.navigateTo({ url: path }) } } }
</script>

<style scoped>
.profile-banner { position: relative; display: flex; align-items: center; gap: 20rpx; padding: 30rpx 28rpx; overflow: hidden; border-radius: 34rpx; background: linear-gradient(135deg, #FFF9F4, #F4E6D8); border: 1rpx solid rgba(255,255,255,.82); box-shadow: 0 12rpx 30rpx rgba(82,65,47,.08); }
.profile-banner::after { content: ''; position: absolute; right: -42rpx; top: -56rpx; width: 190rpx; height: 190rpx; border-radius: 999rpx; background: rgba(196,97,47,.1); }
.avatar-frame { position: relative; z-index: 1; width: 96rpx; height: 96rpx; flex-shrink: 0; }
.user-avatar { position: relative; z-index: 1; width: 96rpx; height: 96rpx; display: block; border-radius: 999rpx; background: #F5E0D5; box-shadow: 0 8rpx 18rpx rgba(82,65,47,.12); }
.avatar-glow { position: absolute; inset: -8rpx; z-index: -1; border-radius: 999rpx; border: 1rpx solid rgba(196,97,47,.22); }
.user-info { flex: 1; }
.user-kicker { display: block; color: #C4612F; font-size: 18rpx; font-weight: 700; letter-spacing: 2rpx; }
.user-name { display: block; font-size: 34rpx; font-weight: 700; color: #1F2421; }
.user-sub { display: block; margin-top: 4rpx; font-size: 25rpx; color: #7C7367; }
.settings-btn { position: relative; z-index: 1; width: 58rpx; height: 58rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: rgba(255,255,255,.62); }
.stat-card { display: flex; align-items: center; margin-top: 20rpx; padding: 24rpx 0; }
.stat-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 8rpx; border-right: 2rpx solid #EFEAE1; }
.stat-item:last-child { border: 0; }
.stat-value { font-size: 48rpx; font-weight: 700; color: #1F2421; line-height: 1; }
.stat-value.primary { color: #C4612F; }
.section-label { display: block; margin: 28rpx 4rpx 14rpx; color: #7C7367; font-size: 24rpx; font-weight: 600; }
.entry-list { overflow: hidden; }
.entry { min-height: 104rpx; display: flex; align-items: center; gap: 18rpx; padding: 0 24rpx; border-bottom: 1rpx solid #F7F4EF; }
.entry:last-child { border-bottom: 0; }
.entry-icon { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; border-radius: 20rpx; }
.entry-name { flex: 1; font-size: 30rpx; color: #1F2421; }
.entry-badge { min-width: 40rpx; padding: 4rpx 10rpx; border-radius: 999rpx; background: #C44A3F; color: #fff; font-size: 22rpx; text-align: center; }
</style>
