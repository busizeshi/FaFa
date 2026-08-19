<template><view class="page"><PageHeader title="排便记录" /><view class="card quick-form"><view class="between"><text class="subheading">记录一次排便</text><text class="caption">简单记录，长期回看</text></view><view class="choice-row"><text v-for="item in typeOptions" :key="item.value" class="choice" :class="{ selected: form.type === item.value }" @click="form.type = item.value">{{ item.label }}</text></view><view class="status-row"><text v-for="item in statusOptions" :key="item.value" class="status-choice" :class="{ selected: form.status === item.value }" @click="form.status = item.value">{{ item.label }}</text></view><button class="button-primary save-button" @click="save">保存记录</button></view><view class="section"><text class="section-title">最近记录</text><view class="card record-list"><view v-for="item in records" :key="item.id" class="excretion-item"><view class="type-dot" :class="item.type"></view><view class="excretion-copy"><text class="subheading">{{ item.type === 'urine' ? '排尿' : '排便' }}</text><text class="caption">{{ time(item.recordTime) }} · {{ statusName(item.status) }}</text></view><FaIcon name="check" tone="success" :size="28" /></view></view></view></view></template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pet: currentPet(), records: [], form: { type: 'feces', status: 'normal' }, typeOptions: [{ value: 'feces', label: '排便' }, { value: 'urine', label: '排尿' }], statusOptions: [{ value: 'normal', label: '正常' }, { value: 'soft', label: '偏软' }, { value: 'hard', label: '偏硬' }, { value: 'abnormal', label: '异常' }] } }, onShow() { this.load() }, methods: { load() { api.excretion.list({ petId: this.pet.id }).then(data => { this.records = data || [] }) }, save() { api.excretion.create({ petId: this.pet.id, recordTime: new Date().toISOString(), ...this.form }).then(() => { this.load(); uni.showToast({ title: '已记录', icon: 'success' }) }) }, time(v) { return `${String(v).slice(0, 10)} ${String(v).slice(11, 16)}` }, statusName(v) { return { normal: '正常', soft: '偏软', hard: '偏硬', abnormal: '异常' }[v] || v } } }
</script>

<style scoped>
.quick-form { padding: 28rpx; }
.choice-row, .status-row { display: flex; gap: 16rpx; margin-top: 22rpx; }
.choice, .status-choice { flex: 1; height: 76rpx; display: flex; align-items: center; justify-content: center; border-radius: 16rpx; border: 2rpx solid #E7E1D7; color: #7C7367; }
.choice.selected, .status-choice.selected { border-color: #C4612F; background: #FAF0EB; color: #A85228; font-weight: 600; }
.status-choice { height: 64rpx; font-size: 25rpx; }
.save-button { height: 80rpx; margin-top: 24rpx; border-radius: 999rpx; font-size: 28rpx; }
.record-list { padding: 0 28rpx; }
.excretion-item { min-height: 104rpx; display: flex; align-items: center; gap: 18rpx; border-bottom: 2rpx solid #F7F4EF; }
.excretion-item:last-child { border: 0; }
.type-dot { width: 52rpx; height: 52rpx; border-radius: 999rpx; background: #FFF8E7; }
.type-dot.urine { background: #EFF6FF; }
.excretion-copy { flex: 1; display: flex; flex-direction: column; gap: 4rpx; }
</style>
