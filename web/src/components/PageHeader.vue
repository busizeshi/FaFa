<template>
  <view class="topbar">
    <view class="back" @click="back"><FaIcon name="back" tone="default" :size="38" /></view>
    <text class="topbar-title">{{ title }}</text>
    <view class="topbar-spacer"></view>
  </view>
</template>

<script>
import FaIcon from './FaIcon.vue'
export default { components: { FaIcon }, props: { title: { type: String, default: '' } }, methods: { back() { uni.navigateBack({ delta: 1 }) } } }
</script>
