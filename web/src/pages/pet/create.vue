<template>
  <view class="page">
    <PageHeader :title="editing ? '编辑宠物' : '创建宠物'" />
    <view class="intro"><text class="heading">{{ editing ? '更新 Ta 的档案' : '几十秒完成建档' }}</text><text class="caption block">基础信息保存后也可以随时补充</text></view>
    <view class="card form-card">
      <text class="form-title">必填信息</text>
      <text class="form-label">昵称 <text class="danger">*</text></text><input v-model="form.name" class="input" placeholder="给Ta起个名字吧" />
      <text class="form-label">物种 <text class="danger">*</text></text><view class="choice-row"><text v-for="item in speciesOptions" :key="item.value" class="choice" :class="{ selected: form.species === item.value }" @click="form.species = item.value">{{ item.label }}</text></view>
      <text class="form-label">性别 <text class="danger">*</text></text><view class="choice-row"><text v-for="item in genderOptions" :key="item.value" class="choice" :class="{ selected: form.gender === item.value }" @click="form.gender = item.value">{{ item.label }}</text></view>
      <text class="form-label">出生日期 <text class="danger">*</text></text><picker mode="date" :value="form.birthDate" @change="form.birthDate = $event.detail.value"><view class="picker input">{{ form.birthDate || '选择日期' }}<FaIcon name="calendar" tone="muted" :size="30" /></view></picker>
      <text class="form-label">当前体重 <text class="danger">*</text></text><view class="weight-input row"><input v-model="form.weight" type="digit" class="input" placeholder="0.0" /><text>kg</text></view>
    </view>
    <view class="card form-card section"><view class="between"><text class="form-title">可选信息</text><text class="caption">可跳过</text></view><text class="form-label">品种</text><input v-model="form.breed" class="input" placeholder="如：英国短毛猫" /><text class="form-label">到家日期</text><picker mode="date" :value="form.adoptDate" @change="form.adoptDate = $event.detail.value"><view class="picker input">{{ form.adoptDate || '选择日期' }}<FaIcon name="calendar" tone="muted" :size="30" /></view></picker><text class="form-label">毛色</text><input v-model="form.coatColor" class="input" placeholder="如：蓝色、白色" /><text class="form-label">备注</text><textarea v-model="form.remarks" class="textarea" placeholder="性格特点、过敏史、其他需要记录的信息..." /></view>
    <view class="button-primary section" @click="save">{{ editing ? '保存修改' : '创建档案' }}</view>
  </view>
</template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { editing: false, form: { name: '', species: 'cat', gender: 'male', birthDate: '', adoptDate: '', weight: '', breed: '', coatColor: '', remarks: '' }, speciesOptions: [{ value: 'cat', label: '猫' }, { value: 'dog', label: '狗' }, { value: 'other', label: '其他' }], genderOptions: [{ value: 'male', label: '公' }, { value: 'female', label: '母' }] } }, onLoad(options) { if (options.id) { this.editing = true; api.pets.detail(options.id).then(pet => { this.form = { ...this.form, ...pet } }) } }, methods: { save() { if (!this.form.name || !this.form.species || !this.form.gender) return uni.showToast({ title: '请先完善必填信息', icon: 'none' }); const action = this.editing ? api.pets.update(currentPet().id, this.form) : api.pets.create(this.form); action.then(() => { uni.showToast({ title: '保存成功', icon: 'success' }); setTimeout(() => uni.navigateBack(), 500) }) } } }
</script>

<style scoped>
.intro { margin: 0 4rpx 24rpx; }
.block { display: block; margin-top: 6rpx; }
.form-card { padding: 28rpx; }
.form-title { font-size: 32rpx; font-weight: 700; color: #1F2421; }
.choice-row { display: flex; gap: 16rpx; }
.choice { flex: 1; height: 80rpx; display: flex; align-items: center; justify-content: center; border-radius: 16rpx; border: 2rpx solid #E7E1D7; background: #FBF9F5; color: #7C7367; font-size: 28rpx; }
.choice.selected { border-color: #C4612F; background: #FAF0EB; color: #A85228; font-weight: 600; }
.picker { display: flex; align-items: center; justify-content: space-between; }
.weight-input { position: relative; }
.weight-input .input { padding-right: 72rpx; }
.weight-input > text { position: absolute; right: 26rpx; color: #9A9185; }
</style>
