<template><view class="page"><PageHeader title="宠物报告" /><view class="hero ai-card"><FaIcon name="report" tone="primary" :size="42" /><view><text class="heading">{{ pet.name }} · 8月月报</text><text class="caption block">数据会慢慢变成 Ta 的成长故事</text></view></view><view class="section report-list"><view v-for="item in reports" :key="item.title" class="card report-card"><view class="report-icon"><FaIcon :name="item.icon" tone="primary" :size="34" /></view><view class="report-copy"><text class="subheading">{{ item.title }}</text><text class="caption block">{{ item.period }} · {{ item.status }}</text></view><view class="report-link"><text>查看</text><FaIcon name="next" tone="muted" :size="26" /></view></view></view><view class="button-primary section" @click="generate">生成最新月报</view></view></template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pet: currentPet(), reports: [{ title: '2026年8月月报', period: '2026.08', status: '已生成', icon: 'report' }, { title: '2026年7月月报', period: '2026.07', status: '已生成', icon: 'report' }, { title: '豆包年度成长报告', period: '2025.06 - 2026.06', status: '已生成', icon: 'timeline' }] } }, methods: { generate() { uni.showToast({ title: '报告生成任务已提交', icon: 'success' }) } } }
</script>

<style scoped>
.block { display: block; margin-top: 8rpx; }
.hero { display: flex; align-items: center; gap: 18rpx; padding: 28rpx; }
.report-card { display: flex; align-items: center; gap: 18rpx; padding: 24rpx; margin-bottom: 16rpx; }
.report-icon { width: 64rpx; height: 64rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; background: #FAF0EB; }
.report-copy { flex: 1; }
.report-link { color: #C4612F; font-size: 26rpx; }
</style>
