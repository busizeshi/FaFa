<template>
  <view class="page"><PageHeader title="照片墙" /><view class="photo-search card"><FaIcon name="search" tone="muted" :size="32" /><input v-model="query" @confirm="search" class="search-input" placeholder="找一下去年夏天在阳台睡觉的照片..." /><text class="ai-badge">AI</text></view><scroll-view scroll-x class="filter-scroll"><view class="filter-row"><text v-for="item in tags" :key="item" class="pill" :class="activeTag === item ? 'pill--primary' : 'pill--outline'" @click="activeTag = item">{{ item }}</text></view></scroll-view><view class="section"><view class="between mb-2"><text class="section-title no-margin">{{ query ? '搜索结果' : '2026年8月' }}</text><text class="caption">{{ visiblePhotos.length }} 张</text></view><view class="photo-grid"><view v-for="photo in visiblePhotos" :key="photo.id" class="photo-item" @click="preview(photo)"><image :src="photo.url" mode="aspectFill" /><view v-if="photo.identifying" class="photo-status">AI识别中</view></view></view><view v-if="!visiblePhotos.length" class="card empty">没有找到相关照片，试试“阳台”或“睡觉”。</view></view><view class="fab" @click="uploadPhoto"><FaIcon name="plus" tone="default" :size="42" /></view></view>
</template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pet: currentPet(), photos: [], query: '', activeTag: '全部', tags: ['全部', '阳台', '睡觉', '玩耍', '吃饭', '户外', '纸箱'] } }, onShow() { this.load() }, computed: { visiblePhotos() { let list = this.photos; if (this.activeTag !== '全部') list = list.filter(item => item.tags?.includes(this.activeTag)); return list } }, methods: { load() { api.photos.list({ petId: this.pet.id }).then(data => { this.photos = data?.records || [] }) }, search() { if (!this.query) return this.load(); api.photos.search({ petId: this.pet.id, query: this.query, limit: 20 }).then(data => { this.photos = data || [] }) }, preview(photo) { uni.previewImage({ urls: this.visiblePhotos.map(item => item.url), current: photo.url }) }, uploadPhoto() { uni.chooseImage({ count: 9, success: (res) => { Promise.all(res.tempFilePaths.map(file => api.photos.upload(file, this.pet.id))).then(() => { this.load(); uni.showToast({ title: '上传成功', icon: 'success' }) }) } }) } } }
</script>

<style scoped>
.photo-search { display: flex; align-items: center; gap: 12rpx; padding: 14rpx 20rpx; }
.search-input { flex: 1; height: 60rpx; color: #3D3D3A; font-size: 26rpx; }
.ai-badge { padding: 4rpx 12rpx; border-radius: 999rpx; background: #C4612F; color: #fff; font-size: 20rpx; }
.filter-scroll { margin: 24rpx -32rpx 0; white-space: nowrap; }
.filter-row { display: inline-flex; gap: 16rpx; padding: 0 32rpx; }
.no-margin { margin: 0; }
.photo-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12rpx; }
.photo-item { position: relative; height: 216rpx; overflow: hidden; border-radius: 16rpx; background: #EFEAE1; }
.photo-item image { width: 100%; height: 100%; display: block; }
.photo-status { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(31,36,33,.34); color: #fff; font-size: 22rpx; }
.empty { grid-column: 1 / 4; padding: 48rpx 24rpx; text-align: center; color: #9A9185; }
</style>
