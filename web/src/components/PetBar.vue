<template>
  <view class="pet-bar card" @click="openSwitcher">
    <view class="avatar-wrap"><image class="avatar" :src="pet.avatar || '/static/assets/pet-avatar-doubao.jpg'" mode="aspectFill" /><view class="presence-dot"></view></view>
    <view class="pet-info">
      <view class="row gap-1"><text class="pet-name">{{ pet.name }}</text><text class="pet-meta">{{ pet.breed || pet.speciesName || '宠物' }}</text></view>
      <text class="pet-sub">{{ pet.genderName || '公' }} · {{ pet.weight || '—' }} kg · {{ pet.ageInMonths ? Math.floor(pet.ageInMonths / 12) + '岁' : '成长中' }}</text>
    </view>
    <view class="pet-next"><FaIcon name="next" tone="muted" :size="34" /></view>
  </view>
</template>

<script>
import FaIcon from './FaIcon.vue'
import { setCurrentPet } from '../utils/store'
export default { components: { FaIcon }, props: { pet: { type: Object, required: true } }, methods: { openSwitcher() { setCurrentPet(this.pet.id); uni.navigateTo({ url: '/pages/pet/list' }) } } }
</script>

<style scoped>
.pet-bar { padding: 22rpx 24rpx; display: flex; align-items: center; gap: 20rpx; }
.avatar-wrap { position: relative; flex-shrink: 0; }
.avatar { width: 96rpx; height: 96rpx; border-radius: 999rpx; background: #EFEAE1; }
.presence-dot { position: absolute; right: 2rpx; bottom: 4rpx; width: 18rpx; height: 18rpx; border: 4rpx solid #fff; border-radius: 999rpx; background: #5B8C5A; }
.pet-info { flex: 1; min-width: 0; }
.pet-name { font-size: 34rpx; font-weight: 700; color: #1F2421; }
.pet-meta { max-width: 300rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 24rpx; color: #9A9185; }
.pet-sub { display: block; margin-top: 4rpx; font-size: 24rpx; color: #7C7367; }
.pet-next { width: 52rpx; height: 52rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: #FBF9F5; }
</style>
