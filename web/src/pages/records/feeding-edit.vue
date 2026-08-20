<template>
  <view class="page"><PageHeader title="记录喂食" /><view class="card form-card"><text class="form-label">时间</text><picker mode="time" :value="form.time" @change="form.time = $event.detail.value"><view class="input picker">{{ form.time }}</view></picker><text class="form-label">食物名称</text><input v-model="form.foodName" class="input" placeholder="如：皇家英短成猫粮" /><view class="amount-row"><view><text class="form-label">数量</text><input v-model="form.amount" class="input" type="digit" placeholder="30" /></view><view class="unit"><text class="form-label">单位</text><picker :range="units" @change="form.unit = units[$event.detail.value]"><view class="input picker">{{ form.unit }}</view></picker></view></view><text class="form-label">备注（可选）</text><textarea v-model="form.remarks" class="textarea" placeholder="例如：今天换了新口味" /></view><view class="button-primary section" @click="save">保存记录</view></view>
</template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader }, data() { const now = new Date(); return { pet: currentPet(), units: ['g', 'ml', '份'], form: { time: `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`, foodName: '皇家英短成猫粮', amount: '30', unit: 'g', remarks: '' } } }, methods: { save() { if (!this.form.foodName || !this.form.amount) return uni.showToast({ title: '请填写食物和数量', icon: 'none' }); api.feeding.create({ petId: this.pet.id, feedTime: `${new Date().toISOString().slice(0, 10)}T${this.form.time}:00`, foodName: this.form.foodName, amount: Number(this.form.amount), unit: this.form.unit, remarks: this.form.remarks }).then(() => { uni.showToast({ title: '已保存', icon: 'success' }); setTimeout(() => uni.navigateBack(), 500) }) } } }
</script>

<style scoped>
.form-card { padding: 28rpx; }
.picker { display: flex; align-items: center; }
.amount-row { display: flex; gap: 20rpx; }
.amount-row > view:first-child { flex: 1; }
.unit { width: 180rpx; }
</style>
