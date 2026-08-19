<template>
  <view class="page">
    <PageHeader title="喂食记录" />
    <view class="quick-feed ai-card" @click="quickFeed"><view class="quick-feed-icon"><FaIcon name="food" tone="primary" :size="38" /></view><view><text class="subheading">快捷喂食</text><text class="caption block">一键记录上次使用的食物和分量</text></view><FaIcon name="next" tone="primary" :size="38" /></view>
    <view class="section"><view class="between mb-2"><text class="section-title no-margin">最近记录</text><text class="caption">共 {{ records.length }} 条</text></view><view v-if="records.length" class="record-list card"><view v-for="record in records" :key="record.id" class="record-item"><view class="record-time"><text>{{ time(record.feedTime) }}</text><text class="caption">{{ date(record.feedTime) }}</text></view><view class="record-copy"><text class="subheading">{{ record.foodName || '默认猫粮' }}</text><text class="caption">{{ record.remarks || '日常喂食' }}</text></view><text class="record-amount">{{ record.amount }}{{ record.unit || 'g' }}</text></view></view><view v-else class="card empty">还没有喂食记录，先记下第一顿吧。</view></view>
    <view class="ai-card section ai-entry" @click="goAi"><FaIcon name="spark" tone="primary" :size="32" /><view><text class="subheading primary">AI 分析最近饮食</text><text class="caption block">看看平均每日喂食量有没有变化</text></view><FaIcon name="next" tone="primary" :size="34" /></view>
    <view class="fab" @click="add"><FaIcon name="plus" tone="default" :size="42" /></view>
  </view>
</template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pet: currentPet(), records: [] } }, onShow() { this.load() }, methods: { load() { api.feeding.list({ petId: this.pet.id }).then(data => { this.records = data?.records || [] }) }, quickFeed() { api.feeding.quick(this.pet.id).then(() => { uni.showToast({ title: '已记录', icon: 'success' }); this.load() }) }, add() { uni.navigateTo({ url: '/pages/records/feeding-edit' }) }, goAi() { uni.navigateTo({ url: '/pages/ai/chat?prompt=分析一下最近一个月吃得是不是变少了' }) }, time(value) { return value ? String(value).slice(11, 16) : '--:--' }, date(value) { return value ? String(value).slice(0, 10).replaceAll('-', '/') : '' } } }
</script>

<style scoped>
.block { display: block; margin-top: 6rpx; }
.quick-feed { display: flex; align-items: center; gap: 18rpx; padding: 26rpx; }
.quick-feed > view:nth-child(2) { flex: 1; }
.quick-feed-icon { width: 72rpx; height: 72rpx; border-radius: 24rpx; background: #F5E0D5; display: flex; align-items: center; justify-content: center; }
.no-margin { margin: 0; }
.record-list { padding: 0 28rpx; }
.record-item { min-height: 112rpx; display: flex; align-items: center; gap: 20rpx; border-bottom: 2rpx solid #F7F4EF; }
.record-item:last-child { border-bottom: 0; }
.record-time { width: 120rpx; display: flex; flex-direction: column; gap: 4rpx; color: #3D3D3A; font-size: 28rpx; }
.record-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4rpx; }
.record-copy .subheading { font-size: 28rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.record-amount { font-size: 32rpx; font-weight: 700; color: #C4612F; }
.empty { padding: 48rpx 24rpx; text-align: center; color: #9A9185; }
.ai-entry { display: flex; align-items: center; gap: 16rpx; padding: 26rpx; }
.ai-entry > view { flex: 1; }
</style>
