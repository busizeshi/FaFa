<template><view class="page"><PageHeader title="记录体重" /><view class="card form-card"><text class="form-label">记录日期</text><picker mode="date" :value="form.recordDate" @change="form.recordDate = $event.detail.value"><view class="input picker">{{ form.recordDate }}</view></picker><text class="form-label">当前体重</text><view class="weight-field"><input v-model="form.weight" type="digit" class="weight-input" placeholder="5.2" /><text>kg</text></view><text class="form-label">体况评分（可选）</text><view class="bcs-row"><text v-for="item in 9" :key="item" class="bcs" :class="{ selected: form.bcsScore === item }" @click="form.bcsScore = item">{{ item }}</text></view><text class="form-label">备注</text><textarea v-model="form.remarks" class="textarea" placeholder="补充说明..." /></view><view class="button-primary section" @click="save">保存记录</view></view></template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader }, data() { return { pet: currentPet(), form: { recordDate: '2026-08-18', weight: '', bcsScore: 5, remarks: '' } } }, methods: { save() { if (!this.form.weight) return uni.showToast({ title: '请输入体重', icon: 'none' }); api.weight.create({ petId: this.pet.id, recordDate: this.form.recordDate, weight: Number(this.form.weight), bcsScore: this.form.bcsScore, remarks: this.form.remarks }).then(() => { uni.showToast({ title: '已保存', icon: 'success' }); setTimeout(() => uni.navigateBack(), 500) }) } } }
</script>

<style scoped>
.form-card { padding: 28rpx; }
.picker { display: flex; align-items: center; }
.weight-field { position: relative; }
.weight-input { width: 100%; height: 148rpx; padding: 0 100rpx 0 32rpx; border-radius: 24rpx; background: #FAF0EB; color: #C4612F; font-size: 72rpx; font-weight: 700; text-align: center; }
.weight-field > text { position: absolute; right: 36rpx; top: 58rpx; color: #A85228; font-size: 28rpx; }
.bcs-row { display: flex; gap: 8rpx; }
.bcs { flex: 1; height: 64rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; background: #FBF9F5; border: 2rpx solid #E7E1D7; color: #7C7367; }
.bcs.selected { background: #C4612F; border-color: #C4612F; color: #fff; }
</style>
