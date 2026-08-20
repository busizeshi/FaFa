<template><view class="page"><PageHeader title="饮水记录" /><view class="card quick-form"><text class="subheading">记录一次饮水</text><view class="inline-form"><input v-model="amount" type="number" class="input" placeholder="饮水量" /><text class="unit-label">ml</text><button class="pill pill--primary" @click="save">保存</button></view></view><view class="section"><view class="between mb-2"><text class="section-title no-margin">最近记录</text><text class="caption">{{ total }} ml · 近 7 天</text></view><view class="card record-list"><view v-for="item in records" :key="item.id" class="water-item"><view><text class="subheading">{{ time(item.recordTime) }}</text><text class="caption block">{{ date(item.recordTime) }}</text></view><text class="water-amount">{{ item.amount }}ml</text></view></view></view></view></template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader }, data() { return { pet: currentPet(), amount: '', records: [] } }, onShow() { this.load() }, computed: { total() { return this.records.reduce((sum, item) => sum + Number(item.amount || 0), 0) } }, methods: { load() { api.water.list({ petId: this.pet.id }).then(data => { this.records = data || [] }) }, save() { if (!this.amount) return uni.showToast({ title: '请输入饮水量', icon: 'none' }); api.water.create({ petId: this.pet.id, recordTime: new Date().toISOString(), amount: Number(this.amount) }).then(() => { this.amount = ''; this.load(); uni.showToast({ title: '已记录', icon: 'success' }) }) }, time(v) { return String(v).slice(11, 16) }, date(v) { return String(v).slice(0, 10).replaceAll('-', '/') } } }
</script>

<style scoped>
.quick-form { padding: 28rpx; }
.inline-form { position: relative; display: flex; align-items: center; gap: 16rpx; margin-top: 20rpx; }
.inline-form .input { flex: 1; padding-right: 70rpx; }
.unit-label { position: absolute; right: 210rpx; color: #9A9185; }
.no-margin { margin: 0; }
.record-list { padding: 0 28rpx; }
.water-item { min-height: 104rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 2rpx solid #F7F4EF; }
.water-item:last-child { border: 0; }
.block { display: block; margin-top: 4rpx; }
.water-amount { color: #5A8EAD; font-size: 34rpx; font-weight: 700; }
</style>
