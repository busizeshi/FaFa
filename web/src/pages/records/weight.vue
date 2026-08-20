<template>
  <view class="page"><PageHeader title="体重记录与趋势" /><view class="period-row"><text v-for="item in periods" :key="item" class="pill" :class="period === item ? 'pill--primary' : 'pill--outline'" @click="period = item">{{ item }}</text></view><view class="card chart-card section"><view class="between"><view><text class="caption">当前体重</text><text class="chart-value">{{ latest }}<text class="chart-unit">kg</text></text></view><view class="trend-badge"><FaIcon name="timeline" tone="success" :size="24" /> 基本稳定</view></view><view class="bar-chart"><view v-for="item in chartData" :key="item.recordDate" class="bar-item"><view class="bar-wrap"><view class="bar" :style="{ height: `${item.height}%` }"></view></view><text>{{ item.label }}</text></view></view></view><view class="summary-row card"><view><text class="caption">起始体重</text><text>{{ analysis.startWeight || '—' }}kg</text></view><view><text class="caption">变化</text><text :class="analysis.change > 0 ? 'warning' : 'success'">{{ analysis.change > 0 ? '+' : '' }}{{ analysis.change || 0 }}kg</text></view><view><text class="caption">趋势</text><text class="success">稳定</text></view></view><view class="section"><view class="between mb-2"><text class="section-title no-margin">记录列表</text><text class="caption">最近 {{ records.length }} 条</text></view><view class="record-list card"><view v-for="record in records" :key="record.id" class="weight-item"><text class="caption">{{ record.recordDate }}</text><text class="weight-num">{{ record.weight }}kg</text><FaIcon name="check" tone="success" filled :size="24" /></view></view></view><view class="ai-card section ai-entry" @click="goAi"><FaIcon name="spark" tone="primary" filled :size="32" /><text>AI 分析最近体重变化</text><FaIcon name="next" tone="primary" :size="26" /></view><view class="fab" @click="add"><FaIcon name="plus" tone="default" :size="42" /></view></view>
</template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pet: currentPet(), periods: ['7天', '30天', '3个月', '1年'], period: '30天', records: [], analysis: {}, chartData: [] } }, onShow() { this.load() }, methods: { load() { Promise.all([api.weight.list({ petId: this.pet.id }), api.weight.trend({ petId: this.pet.id })]).then(([records, trend]) => { this.records = records || []; this.analysis = trend?.analysis || {}; const weights = this.records.map(item => Number(item.weight)); const min = Math.min(...weights, 0); const max = Math.max(...weights, 1); this.chartData = this.records.slice(-7).map(item => ({ ...item, label: String(item.recordDate).slice(5), height: 28 + ((item.weight - min) / Math.max(max - min, .1)) * 62 })) }) }, add() { uni.navigateTo({ url: '/pages/records/weight-edit' }) }, goAi() { uni.navigateTo({ url: '/pages/ai/chat?prompt=AI 分析最近体重变化' }) } }, computed: { latest() { return this.records[this.records.length - 1]?.weight || this.pet.weight || '—' } } }
</script>

<style scoped>
.period-row { display: flex; gap: 12rpx; overflow: hidden; }
.chart-card { padding: 28rpx; }
.chart-value { display: block; margin-top: 8rpx; color: #C4612F; font-size: 56rpx; font-weight: 700; line-height: 1; }
.chart-unit { margin-left: 6rpx; font-size: 26rpx; font-weight: 400; }
.trend-badge { padding: 10rpx 16rpx; display: flex; align-items: center; gap: 6rpx; border-radius: 999rpx; background: #F0FDF4; color: #5B8C5A; font-size: 24rpx; }
.bar-chart { height: 300rpx; display: flex; align-items: flex-end; gap: 16rpx; padding: 28rpx 8rpx 0; }
.bar-item { flex: 1; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; gap: 8rpx; color: #9A9185; font-size: 20rpx; }
.bar-wrap { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; border-bottom: 2rpx solid #EFEAE1; }
.bar { width: 32rpx; max-height: 100%; border-radius: 999rpx 999rpx 4rpx 4rpx; background: #D38057; }
.summary-row { display: flex; margin-top: 20rpx; padding: 24rpx 0; }
.summary-row > view { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6rpx; border-right: 2rpx solid #EFEAE1; font-size: 28rpx; }
.summary-row > view:last-child { border: 0; }
.no-margin { margin: 0; }
.record-list { padding: 0 28rpx; }
.weight-item { min-height: 88rpx; display: flex; align-items: center; gap: 20rpx; border-bottom: 2rpx solid #F7F4EF; }
.weight-item:last-child { border: 0; }
.weight-item .caption { flex: 1; }
.weight-num { font-size: 30rpx; font-weight: 600; color: #3D3D3A; }
.ai-entry { display: flex; align-items: center; gap: 14rpx; padding: 24rpx; color: #A85228; font-size: 28rpx; }
</style>
