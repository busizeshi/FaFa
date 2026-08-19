<template>
  <view class="bottom-nav">
    <view class="nav-content">
      <view v-for="item in items" :key="item.key" class="nav-item" :class="{ active: current === item.key }" @click="go(item)">
        <view class="nav-icon-wrap"><FaIcon :name="item.icon" :tone="current === item.key ? 'primary' : 'muted'" :filled="current === item.key" :size="36" /></view>
        <text>{{ item.label }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import FaIcon from './FaIcon.vue'
export default { components: { FaIcon }, props: { current: { type: String, default: 'home' } }, data() { return { items: [{ key: 'home', label: '首页', icon: 'home', path: '/pages/home/home' }, { key: 'timeline', label: '档案', icon: 'timeline', path: '/pages/timeline/timeline' }, { key: 'ai', label: 'AI助手', icon: 'spark', path: '/pages/ai/home' }, { key: 'mine', label: '我的', icon: 'user', path: '/pages/mine/mine' }] } }, methods: { go(item) { if (this.current !== item.key) uni.switchTab({ url: item.path }) } } }
</script>

<style scoped>
.bottom-nav { position: fixed; left: 0; right: 0; bottom: 0; z-index: 30; height: calc(112rpx + env(safe-area-inset-bottom)); padding-bottom: env(safe-area-inset-bottom); background: rgba(255,255,255,.82); border-top: 1rpx solid rgba(255,255,255,.92); box-shadow: 0 -10rpx 30rpx rgba(82,65,47,.08); backdrop-filter: blur(24px); }
.nav-content { height: 112rpx; display: flex; align-items: stretch; }
.nav-item { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4rpx; color: #9A9185; font-size: 21rpx; }
.nav-item.active { color: #C4612F; font-weight: 600; }
.nav-icon-wrap { width: 56rpx; height: 50rpx; display: flex; align-items: center; justify-content: center; border-radius: 18rpx; }
.nav-item.active .nav-icon-wrap { background: #FAF0EB; }
</style>
