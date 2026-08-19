<template>
  <view class="page">
    <PageHeader title="我的宠物" />
    <view class="between page-intro"><view><text class="heading">共 {{ pets.length }} 只宠物</text><text class="caption block">长按卡片可拖拽排序</text></view><FaIcon name="pet" tone="primary" :size="42" /></view>
    <view v-for="petItem in pets" :key="petItem.id" class="card pet-card" @click="openPet(petItem)"><image class="pet-image" :src="petItem.avatar || '/static/assets/pet-avatar-doubao.jpg'" mode="aspectFill" /><view class="pet-card-main"><view class="between"><view class="row gap-1"><text class="subheading">{{ petItem.name }}</text><text class="status-pill">正常</text></view><text class="edit-link" @click.stop="editPet(petItem)">编辑</text></view><text class="pet-description">{{ petItem.breed || petItem.speciesName }} · {{ petItem.genderName || '公' }} · {{ ageText(petItem) }}</text><text class="pet-weight">当前体重 {{ petItem.weight || '—' }}kg</text></view></view>
    <view class="ai-card ai-tip"><view class="row gap-1"><FaIcon name="spark" tone="primary" :size="30" /><text class="subheading primary">AI 小助手</text></view><text>豆包已满3岁，建议定期进行口腔检查和体重管理哦。</text></view>
    <view class="button-primary add-button" @click="createPet"><FaIcon name="plus" tone="default" :size="34" /><text>添加宠物</text></view>
  </view>
</template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { setCurrentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pets: [] } }, onShow() { api.pets.list().then(data => { this.pets = data || [] }) }, methods: { ageText(pet) { return pet.ageInMonths ? `${Math.floor(pet.ageInMonths / 12)}岁${pet.ageInMonths % 12}个月` : '成长中' }, openPet(pet) { setCurrentPet(pet.id); uni.navigateTo({ url: `/pages/pet/profile?id=${pet.id}` }) }, editPet(pet) { setCurrentPet(pet.id); uni.navigateTo({ url: `/pages/pet/create?id=${pet.id}` }) }, createPet() { uni.navigateTo({ url: '/pages/pet/create' }) } } }
</script>

<style scoped>
.block { display: block; margin-top: 4rpx; }
.page-intro { margin-bottom: 24rpx; }
.pet-card { display: flex; align-items: center; gap: 24rpx; padding: 28rpx; margin-bottom: 20rpx; }
.pet-image { width: 136rpx; height: 136rpx; border-radius: 24rpx; background: #EFEAE1; }
.pet-card-main { flex: 1; min-width: 0; }
.status-pill { padding: 5rpx 12rpx; border-radius: 999rpx; background: #F0FDF4; color: #5B8C5A; font-size: 22rpx; }
.edit-link { color: #C4612F; font-size: 26rpx; }
.pet-description, .pet-weight { display: block; margin-top: 12rpx; color: #7C7367; font-size: 25rpx; }
.pet-weight { color: #5C635D; font-weight: 600; }
.ai-tip { padding: 24rpx; margin-top: 12rpx; }
.ai-tip > text { display: block; margin-top: 12rpx; font-size: 26rpx; color: #5C635D; }
.add-button { margin-top: 28rpx; gap: 8rpx; }
</style>
