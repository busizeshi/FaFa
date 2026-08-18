<script setup lang="ts">
defineProps<{ title: string; more?: string }>()
const emit = defineEmits<{ more: [] }>()
</script>
<template><view class="section"><text>{{ title }}</text><text v-if="more" class="more" @tap="emit('more')">{{ more }}</text></view></template>
<style scoped lang="scss">
@use '@/styles/tokens.scss' as *;
.section { display: flex; align-items: center; justify-content: space-between; margin: 44rpx 8rpx 16rpx 24rpx; color: $secondary; font-size: 24rpx; font-weight: 500; }.more { color: $blue; font-size: 26rpx; }
</style>
