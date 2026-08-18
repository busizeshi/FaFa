<script setup lang="ts">
const props = defineProps<{ active: 'home' | 'record' | 'pet' | 'mine' }>()
const tabs = [
  { key: 'home', label: '首页', icon: 'home', activeIcon: 'home-filled', path: '/pages/home/index' },
  { key: 'record', label: '记录', icon: 'compose', activeIcon: 'compose-filled', path: '/pages/record/index' },
  { key: 'pet', label: '宠物', icon: 'contact', activeIcon: 'contact-filled', path: '/pages/pet/index' },
  { key: 'mine', label: '我的', icon: 'person', activeIcon: 'person-filled', path: '/pages/mine/index' }
]
function go(path: string) { uni.reLaunch({ url: path }) }
</script>
<template>
  <view class="tabbar safe"><view v-for="tab in tabs" :key="tab.key" class="tab" :class="{ active: props.active === tab.key }" @tap="go(tab.path)"><uni-icons :type="props.active === tab.key ? tab.activeIcon : tab.icon" size="23" :color="props.active === tab.key ? '#007AFF' : '#8E8E93'" /><text>{{ tab.label }}</text></view></view>
</template>
<style scoped lang="scss">
@use '@/styles/tokens.scss' as *;
.tabbar { position: fixed; z-index: 30; right: 0; bottom: 0; left: 0; display: flex; height: 112rpx; border-top: 1rpx solid rgba(60,60,67,.18); background: rgba(249,249,249,.95); }.safe { height: calc(112rpx + env(safe-area-inset-bottom)); padding-bottom: env(safe-area-inset-bottom); }.tab { display: flex; width: 25%; flex-direction: column; align-items: center; justify-content: center; gap: 5rpx; color: $tertiary; font-size: 20rpx; }.tab.active { color: $blue; font-weight: 600; }
</style>
