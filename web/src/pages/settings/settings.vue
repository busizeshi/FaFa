<template><view class="page"><PageHeader title="设置" /><view class="card settings-list"><view class="setting-row"><text>演示数据模式</text><switch :checked="mock" color="#C4612F" @change="toggleMock" /></view><view class="setting-row" @click="reset"><text>重置本地演示数据</text><FaIcon name="next" tone="muted" :size="34" /></view><view class="setting-row" @click="showAbout"><text>关于 FaFa</text><FaIcon name="next" tone="muted" :size="34" /></view></view><view class="setting-note">当前前端已按 Java 服务接口封装。关闭演示数据后，请在 utils/config.js 填入后端 HTTPS 地址。</view></view></template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { config } from '../../utils/config'
import { resetStore } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { mock: config.mock } }, methods: { toggleMock(e) { config.mock = e.detail.value; this.mock = config.mock }, reset() { resetStore(); uni.showToast({ title: '已重置', icon: 'success' }) }, showAbout() { uni.showModal({ title: 'FaFa 宠物生活助手', content: '记录宠物一生的 AI 生活助手。', showCancel: false }) } } }
</script>

<style scoped>
.settings-list { overflow: hidden; }
.setting-row { min-height: 112rpx; display: flex; align-items: center; justify-content: space-between; padding: 0 28rpx; border-bottom: 2rpx solid #F7F4EF; color: #3D3D3A; font-size: 30rpx; }
.setting-row:last-child { border: 0; }
.setting-note { margin: 24rpx 8rpx; color: #9A9185; font-size: 24rpx; line-height: 1.6; }
</style>
