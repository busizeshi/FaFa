<script setup lang="ts">
const emit = defineEmits<{ (event: 'tap'): void }>()
</script>
<template><view class="fab" @tap="emit('tap')"><uni-icons type="chat-filled" size="22" color="#FFFFFF" /></view></template>
<style scoped lang="scss">.fab { position: fixed; z-index: 21; right: 32rpx; bottom: 148rpx; display: flex; width: 100rpx; height: 100rpx; align-items: center; justify-content: center; border: 6rpx solid #fff; border-radius: 50%; background: #007AFF; box-shadow: 0 10rpx 24rpx rgba(0,122,255,.28); }</style>
