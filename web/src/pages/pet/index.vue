<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import AppTabbar from '@/components/AppTabbar.vue'
import ListRow from '@/components/ListRow.vue'
import { usePetStore } from '@/stores/pet'
const store = usePetStore()
function go(path: string) { uni.navigateTo({ url: path }) }
function newPet() { uni.showToast({ title: '新建宠物档案将在接入账户后开放', icon: 'none' }) }
</script>
<template><view class="screen"><AppHeader title="宠物" subtitle="陪伴每一只小伙伴" action="plus" @action="newPet" /><view class="body"><view class="pet-card" @tap="go('/pages/pet/detail')"><image :src="store.pet.avatar" mode="aspectFill" /><view><text>{{ store.pet.name }}</text><text>{{ store.pet.breed }} · {{ store.pet.gender }} · {{ store.pet.age }}</text><view class="status"><view /><text>生活状态良好</text></view></view><uni-icons type="arrow-right" size="17" color="#C7C7CC" /></view><text class="section-label">宠物空间</text><view class="group"><ListRow icon="camera" tint="blue" title="成长档案" note="照片、日记和时间轴" @tap="go('/pages/growth/index')" /><ListRow icon="notification" tint="orange" title="提醒" note="照顾好每一件小事" @tap="go('/pages/reminder/index')" /><ListRow icon="list" tint="green" title="健康报告" note="周期数据总结" @tap="go('/pages/report/index')" /></view><text class="section-label">账户</text><view class="group"><ListRow icon="plus" tint="gray" title="添加宠物" :chevron="false" @tap="newPet" /></view><view class="spacer" /></view><AppTabbar active="pet" /></view></template>
<style scoped lang="scss">
@use '@/styles/tokens.scss' as *;
.screen { min-height: 100vh; background: $canvas; }.body { padding: 0 32rpx; }.pet-card { display: flex; align-items: center; padding: 24rpx; border-radius: 30rpx; background: $surface; }.pet-card image { width: 136rpx; height: 136rpx; border-radius: 50%; }.pet-card > view { display: flex; flex: 1; flex-direction: column; gap: 8rpx; margin-left: 20rpx; }.pet-card > view > text:first-child { color: $ink; font-size: 38rpx; font-weight: 700; }.pet-card > view > text:nth-child(2) { color: $secondary; font-size: 22rpx; }.status { display: flex; align-items: center; gap: 8rpx; margin-top: 5rpx; }.status view { width: 12rpx; height: 12rpx; border-radius: 50%; background: $green; }.status text { color: $tertiary; font-size: 20rpx; }.section-label { display: block; margin: 42rpx 0 14rpx 24rpx; color: $secondary; font-size: 24rpx; font-weight: 500; }.group { @include grouped-surface; }.spacer { height: 155rpx; }
</style>
