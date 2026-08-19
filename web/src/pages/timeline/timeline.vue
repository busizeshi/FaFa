<template>
  <view class="page timeline-page">
    <PetBar :pet="pet" />
    <scroll-view scroll-x class="filter-scroll"><view class="filter-row"><text v-for="item in filters" :key="item" class="pill" :class="activeFilter === item ? 'pill--primary' : 'pill--outline'" @click="activeFilter = item">{{ item }}</text></view></scroll-view>
    <view class="month-group section"><view class="between month-heading"><view><text class="heading">2026年8月</text><text class="caption block">豆包的近期开销与日常</text></view><text class="caption">12 条记录</text></view>
      <view class="timeline-list card"><view v-for="item in augustItems" :key="item.id" class="timeline-item"><view class="timeline-dot" :class="item.tone"></view><view class="timeline-content"><view class="between"><text class="subheading">{{ item.title }}</text><text class="caption">{{ item.date }}</text></view><text v-if="item.detail" class="timeline-detail">{{ item.detail }}</text><view v-if="item.images" class="thumb-row"><image v-for="src in item.images" :key="src" :src="src" mode="aspectFill" class="thumb" /></view><text v-if="item.tag" class="tag-pill">{{ item.tag }}</text></view></view></view>
    </view>
    <view class="month-group section"><view class="between month-heading"><view><text class="heading">2026年7月</text><text class="caption block">那些刚刚发生的好日子</text></view><text class="caption">8 条记录</text></view><view class="timeline-list card"><view v-for="item in julyItems" :key="item.id" class="timeline-item"><view class="timeline-dot" :class="item.tone"></view><view class="timeline-content"><view class="between"><text class="subheading">{{ item.title }}</text><text class="caption">{{ item.date }}</text></view><text v-if="item.detail" class="timeline-detail">{{ item.detail }}</text></view></view></view></view>
    <view class="fab timeline-fab" @click="goAi"><FaIcon name="spark" tone="default" :size="30" /><text>生成本月回忆</text></view>
    <BottomNav current="timeline" />
  </view>
</template>

<script>
import BottomNav from '../../components/BottomNav.vue'
import PetBar from '../../components/PetBar.vue'
import FaIcon from '../../components/FaIcon.vue'
import { currentPet } from '../../utils/store'
export default { components: { BottomNav, PetBar, FaIcon }, data() { return { pet: currentPet(), activeFilter: '全部', filters: ['全部', '照片', '日记', '事件', '记录'], augustItems: [{ id: 1, title: '上传了 4 张照片', date: '8月17日', detail: '阳台的午后，豆包睡得很香。', images: ['/static/assets/pet-photo-sleeping.jpg', '/static/assets/pet-photo-playing.jpg'], tone: 'photo' }, { id: 2, title: '完成本季度驱虫', date: '8月14日', tag: '健康相关', tone: 'health' }, { id: 3, title: '体重 5.2kg', date: '8月10日', detail: '比上次记录下降 0.05kg', tone: 'record' }, { id: 4, title: '更换新的猫粮', date: '8月3日', tag: '饮食变化', tone: 'event' }], julyItems: [{ id: 5, title: '日记：豆包今天特别黏人', date: '7月28日', detail: '今天一直跟在脚边，像一个小尾巴。', tone: 'diary' }, { id: 6, title: '体重 5.3kg', date: '7月15日', tone: 'record' }] } }, methods: { goAi() { uni.navigateTo({ url: '/pages/ai/chat?prompt=帮我生成本月回忆' }) } } }
</script>

<style scoped>
.filter-scroll { margin: 24rpx -32rpx 0; white-space: nowrap; }
.filter-row { display: inline-flex; gap: 16rpx; padding: 0 32rpx 4rpx; }
.month-heading { align-items: flex-start; padding: 0 4rpx 18rpx; }
.timeline-list { padding: 8rpx 28rpx; }
.timeline-item { position: relative; display: flex; gap: 20rpx; padding: 24rpx 0; border-bottom: 2rpx solid #F7F4EF; }
.timeline-item:last-child { border-bottom: 0; }
.timeline-dot { width: 24rpx; height: 24rpx; margin-top: 8rpx; border-radius: 999rpx; background: #C4612F; box-shadow: 0 0 0 8rpx #FAF0EB; flex-shrink: 0; }
.timeline-dot.health { background: #5B8C5A; box-shadow: 0 0 0 8rpx #F0FDF4; }
.timeline-dot.record { background: #5A8EAD; box-shadow: 0 0 0 8rpx #EFF6FF; }
.timeline-dot.photo { background: #9333EA; box-shadow: 0 0 0 8rpx #FAF5FF; }
.timeline-dot.diary { background: #D49B3A; box-shadow: 0 0 0 8rpx #FFF8E7; }
.timeline-content { flex: 1; min-width: 0; }
.timeline-content .subheading { font-size: 28rpx; }
.timeline-detail { display: block; margin-top: 10rpx; font-size: 26rpx; color: #7C7367; }
.thumb-row { display: flex; gap: 12rpx; margin-top: 14rpx; }
.thumb { width: 80rpx; height: 80rpx; border-radius: 16rpx; }
.tag-pill { display: inline-block; margin-top: 12rpx; padding: 6rpx 16rpx; border-radius: 999rpx; background: #FAF0EB; color: #A85228; font-size: 22rpx; }
.timeline-fab { width: auto; height: 76rpx; right: 32rpx; bottom: 152rpx; padding: 0 28rpx; gap: 8rpx; border-radius: 999rpx; font-size: 26rpx; font-weight: 600; }
</style>
