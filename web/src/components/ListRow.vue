<script setup lang="ts">
withDefaults(defineProps<{ icon?: string; tint?: 'blue' | 'green' | 'orange' | 'gray'; title: string; note?: string; value?: string; chevron?: boolean }>(), { tint: 'blue', chevron: true })
</script>
<template>
  <view class="row"><view v-if="icon" class="icon" :class="tint"><uni-icons :type="icon" size="19" :color="tint === 'blue' ? '#007AFF' : tint === 'green' ? '#34C759' : tint === 'orange' ? '#FF9500' : '#8E8E93'" /></view><view class="copy"><text>{{ title }}</text><text v-if="note">{{ note }}</text></view><text v-if="value" class="value">{{ value }}</text><uni-icons v-if="chevron" type="arrow-right" size="16" color="#C7C7CC" /></view>
</template>
<style scoped lang="scss">
@use '@/styles/tokens.scss' as *;
.row { display: flex; min-height: 108rpx; align-items: center; padding: 16rpx 24rpx; border-bottom: 1rpx solid $separator; background: $surface; }.row:last-child { border-bottom: 0; }.icon { display: flex; width: 62rpx; height: 62rpx; align-items: center; justify-content: center; border-radius: 17rpx; }.icon.blue { background: #EAF2FF; }.icon.green { background: #EAF8EF; }.icon.orange { background: #FFF3E2; }.icon.gray { background: #EFEFF4; }.copy { display: flex; flex: 1; flex-direction: column; gap: 5rpx; margin-left: 18rpx; }.copy text:first-child { color: $ink; font-size: 28rpx; }.copy text:last-child { color: $tertiary; font-size: 22rpx; }.value { margin-right: 12rpx; color: $tertiary; font-size: 24rpx; }
</style>
