<template>
  <view class="page">
    <PageHeader title="宠物档案" />
    <view class="card profile-head"><image class="profile-avatar" :src="pet.avatar || '/static/assets/pet-avatar-doubao.jpg'" mode="aspectFill" /><text class="title">{{ pet.name }}</text><text class="profile-sub">{{ pet.breed || pet.speciesName }} · {{ pet.genderName || '公' }} · {{ ageText }}</text><text class="profile-days">来到你身边 {{ companionDays }} 天</text></view>
    <view class="stats-grid section"><view v-for="item in stats" :key="item.label" class="card profile-stat"><text class="profile-stat-value">{{ item.value }}</text><text class="caption">{{ item.label }}</text></view></view>
    <view class="section"><view class="between mb-2"><text class="section-title no-margin">最近动态</text><view class="caption inline-action" @click="go('/pages/timeline/timeline')"><text>查看全部</text><FaIcon name="next" tone="muted" :size="24" /></view></view><view class="card activity-list"><view v-for="item in activities" :key="item.title" class="activity"><view class="activity-icon"><FaIcon :name="item.icon" tone="primary" :size="30" /></view><view class="activity-copy"><text>{{ item.title }}</text><text class="caption">{{ item.date }}</text></view></view></view></view>
    <view class="section ai-card insight"><view class="row gap-1"><FaIcon name="spark" tone="primary" :size="32" /><text class="subheading primary">AI 最近发现</text></view><text>最近 30 天体重基本稳定，平均每日喂食量约 73g。</text></view>
    <view class="section"><text class="section-title">常用入口</text><view class="entry-grid"><view v-for="item in quick" :key="item.label" class="card entry-tile" @click="go(item.path)"><FaIcon :name="item.icon" tone="primary" :size="36" /><text>{{ item.label }}</text></view></view></view>
  </view>
</template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pet: currentPet(), companionDays: 873, stats: [{ value: '5.2', label: '当前体重 kg' }, { value: '3岁2月', label: '年龄' }, { value: '2024.03', label: '到家时间' }, { value: '稳定', label: '近期状态' }], activities: [{ title: '体重 5.2kg', date: '8月16日', icon: 'weight' }, { title: '上传照片 3 张', date: '8月15日', icon: 'photo' }, { title: '完成驱虫', date: '8月14日', icon: 'check' }, { title: '日均进食 72g', date: '8月13日', icon: 'food' }], quick: [{ label: '喂食记录', icon: 'food', path: '/pages/records/feeding' }, { label: '体重趋势', icon: 'weight', path: '/pages/records/weight' }, { label: '照片墙', icon: 'photo', path: '/pages/photos/wall' }, { label: '成长时间轴', icon: 'timeline', path: '/pages/timeline/timeline' }] } }, onShow() { this.pet = currentPet() }, computed: { ageText() { return this.pet.ageInMonths ? `${Math.floor(this.pet.ageInMonths / 12)}岁${this.pet.ageInMonths % 12}个月` : '成长中' } }, methods: { go(path) { uni.navigateTo({ url: path }) } } }
</script>

<style scoped>
.profile-head { display: flex; flex-direction: column; align-items: center; padding: 36rpx 28rpx; }
.profile-avatar { width: 168rpx; height: 168rpx; border-radius: 999rpx; margin-bottom: 20rpx; }
.profile-sub { margin-top: 8rpx; font-size: 28rpx; color: #7C7367; }
.profile-days { margin-top: 8rpx; font-size: 25rpx; color: #9A9185; }
.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16rpx; }
.profile-stat { min-height: 142rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; }
.profile-stat-value { color: #C4612F; font-size: 36rpx; font-weight: 700; }
.no-margin { margin: 0; }
.activity-list { padding: 8rpx 28rpx; }
.activity { min-height: 96rpx; display: flex; align-items: center; gap: 18rpx; border-bottom: 2rpx solid #F7F4EF; }
.activity:last-child { border-bottom: 0; }
.activity-icon { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: #FAF0EB; }
.activity-copy { flex: 1; display: flex; justify-content: space-between; align-items: center; }
.activity-copy > text:first-child { font-size: 28rpx; color: #3D3D3A; }
.insight { padding: 26rpx; }
.insight > text { display: block; margin-top: 14rpx; color: #5C635D; font-size: 27rpx; }
.entry-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16rpx; }
.entry-tile { height: 120rpx; display: flex; align-items: center; justify-content: center; gap: 12rpx; font-size: 28rpx; color: #3D3D3A; }
</style>
