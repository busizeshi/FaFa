<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import AppTabbar from '@/components/AppTabbar.vue'
import AiFab from '@/components/AiFab.vue'
import { usePetStore } from '@/stores/pet'
const store = usePetStore()
const entries = [
  { type: 'feeding', title: '喂食', detail: '92 条记录', icon: 'compose', tint: 'orange' },
  { type: 'weight', title: '体重', detail: '最近 5.2 kg', icon: 'checkbox', tint: 'green' },
  { type: 'water', title: '饮水', detail: '今日 120 ml', icon: 'calendar', tint: 'blue' },
  { type: 'toilet', title: '排便', detail: '状态正常', icon: 'list', tint: 'green' },
  { type: 'event', title: '事件', detail: '留住日常瞬间', icon: 'star', tint: 'blue' },
  { type: 'photo', title: '照片', detail: '156 张回忆', icon: 'camera', tint: 'orange' }
]
function go(type: string) { uni.navigateTo({ url: `/pages/record/list?type=${type}` }) }
function openAi() { uni.navigateTo({ url: '/pages/ai/index' }) }
</script>
<template><view class="screen"><AppHeader title="记录" subtitle="用一条记录，记住这一刻" action="plus" @action="go('event')" /><view class="body"><view class="summary"><view><text>{{ store.todayCount }}</text><text>今日记录</text></view><view><text>5.2 <small>kg</small></text><text>最近体重</text></view><view><text>2</text><text>待完成提醒</text></view></view><text class="section-label">所有记录</text><view class="entry-grid"><view v-for="entry in entries" :key="entry.type" class="entry" @tap="go(entry.type)"><view class="entry-icon" :class="entry.tint"><uni-icons :type="entry.icon" size="23" :color="entry.tint === 'orange' ? '#FF9500' : entry.tint === 'green' ? '#34C759' : '#007AFF'" /></view><view><text>{{ entry.title }}</text><text>{{ entry.detail }}</text></view><uni-icons type="arrow-right" size="15" color="#C7C7CC" /></view></view><view class="ai-note" @tap="openAi"><uni-icons type="chat-filled" size="18" color="#007AFF" /><text>问问 AI：这个月有什么值得注意的变化？</text><uni-icons type="arrow-right" size="15" color="#8E8E93" /></view><view class="spacer" /></view><AiFab @tap="openAi" /><AppTabbar active="record" /></view></template>
<style scoped lang="scss">
@use '@/styles/tokens.scss' as *;
.screen { min-height: 100vh; background: $canvas; }.body { padding: 0 32rpx; }.summary { display: flex; padding: 22rpx 0; border-radius: 28rpx; background: $surface; }.summary view { display: flex; width: 33%; flex-direction: column; align-items: center; gap: 8rpx; border-right: 1rpx solid $separator; }.summary view:last-child { border: 0; }.summary text:first-child { color: $ink; font-size: 33rpx; font-weight: 700; }.summary small { color: $secondary; font-size: 20rpx; font-weight: 400; }.summary text:last-child { color: $tertiary; font-size: 20rpx; }.section-label { display: block; margin: 42rpx 0 14rpx 24rpx; color: $secondary; font-size: 24rpx; font-weight: 500; }.entry-grid { display: grid; overflow: hidden; grid-template-columns: repeat(2, 1fr); border-radius: 28rpx; background: $surface; }.entry { display: flex; min-height: 134rpx; align-items: center; padding: 20rpx; }.entry:nth-child(odd) { border-right: 1rpx solid $separator; }.entry:nth-child(-n+4) { border-bottom: 1rpx solid $separator; }.entry-icon { display: flex; width: 60rpx; height: 60rpx; align-items: center; justify-content: center; border-radius: 18rpx; }.entry-icon.orange { background: #FFF3E2; }.entry-icon.green { background: #EAF8EF; }.entry-icon.blue { background: #EAF2FF; }.entry > view:nth-child(2) { display: flex; flex: 1; flex-direction: column; gap: 7rpx; margin-left: 14rpx; }.entry > view:nth-child(2) text:first-child { color: $ink; font-size: 27rpx; font-weight: 600; }.entry > view:nth-child(2) text:last-child { color: $tertiary; font-size: 19rpx; }.ai-note { display: flex; align-items: center; gap: 13rpx; margin-top: 28rpx; padding: 20rpx 22rpx; border-radius: 22rpx; background: $blue-soft; }.ai-note text { flex: 1; color: $secondary; font-size: 23rpx; }.spacer { height: 160rpx; }
</style>
