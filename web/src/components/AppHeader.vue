<script setup lang="ts">
defineProps<{ title: string; subtitle?: string; action?: string }>()
const emit = defineEmits<{ action: [] }>()
</script>
<template>
  <view class="header">
    <view><text v-if="subtitle" class="subtitle">{{ subtitle }}</text><text class="title">{{ title }}</text></view>
    <view v-if="action" class="action" @tap="emit('action')"><uni-icons :type="action" size="23" color="#007AFF" /></view>
  </view>
</template>
<style scoped lang="scss">
@use '@/styles/tokens.scss' as *;
.header { display: flex; align-items: flex-end; justify-content: space-between; padding: 92rpx 32rpx 24rpx; }.subtitle { display: block; margin-bottom: 8rpx; color: $secondary; font-size: 24rpx; }.title { display: block; color: $ink; font-size: 56rpx; font-weight: 700; letter-spacing: -2rpx; }.action { display: flex; width: 72rpx; height: 72rpx; align-items: center; justify-content: center; border-radius: 50%; background: $blue-soft; }
</style>
