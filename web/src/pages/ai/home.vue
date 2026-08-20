<template>
  <view class="page ai-page">
    <view class="ai-header"><view><text class="eyebrow">FAFA INTELLIGENCE</text><text class="title">AI 助手</text><text class="header-sub">问问关于{{ pet.name }}的任何事情</text></view><view class="ai-avatar-wrap"><image class="ai-avatar" :src="pet.avatar || '/static/assets/pet-avatar-doubao.jpg'" mode="aspectFill" /><view class="ai-status"></view></view></view>
    <view class="pet-context ai-card"><view class="context-icon"><FaIcon name="spark" tone="primary" filled :size="27" /></view><view><text class="context-title">正在和 {{ pet.name }} 的档案对话</text><text class="caption block">我会结合 Ta 的日常记录回答</text></view><text class="context-count">3 条新发现</text></view>
    <view class="section"><view class="section-lead"><view><text class="section-title no-margin">试试问这些</text><text class="caption block">从一个小问题开始了解 Ta</text></view><FaIcon name="chat" tone="primary" :size="30" /></view><view class="question-list"><view v-for="question in questions" :key="question" class="question-card card" @click="ask(question)"><view class="question-mark"><FaIcon name="spark" tone="primary" :size="24" /></view><text>{{ question }}</text><FaIcon name="next" tone="muted" :size="30" /></view></view></view>
    <view class="section"><view class="between mb-2"><view><text class="section-title no-margin">历史会话</text><text class="caption block">你和 FaFa 的每次对话</text></view><view class="caption view-all"><text>查看全部</text><FaIcon name="next" tone="muted" :size="24" /></view></view><view class="history-list"><view v-for="item in conversations" :key="item.id" class="history-card card" @click="openConversation(item)"><view class="history-icon"><FaIcon name="chat" tone="primary" :size="26" /></view><view class="history-copy"><view class="between"><text class="subheading">{{ item.title }}</text><text class="caption">{{ item.time }}</text></view><text class="history-preview">{{ item.preview }}</text></view></view></view></view>
    <view class="chat-entry card" @click="openChat"><text>问问关于{{ pet.name }}的事情...</text><view class="send-btn"><FaIcon name="next" tone="default" :size="34" /></view></view>
    <BottomNav current="ai" />
  </view>
</template>

<script>
import BottomNav from '../../components/BottomNav.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet, getStore } from '../../utils/store'
export default { components: { BottomNav, FaIcon }, data() { const store = getStore(); return { pet: currentPet(), questions: ['豆包最近体重怎么样？', '这个月吃了多少猫粮？', '帮我找去年夏天在阳台的照片', '生成一份8月宠物月报'], conversations: store.conversations || [] } }, onShow() { this.pet = currentPet() }, methods: { ask(question) { uni.navigateTo({ url: `/pages/ai/chat?prompt=${encodeURIComponent(question)}` }) }, openChat() { uni.navigateTo({ url: '/pages/ai/chat' }) }, openConversation(item) { uni.navigateTo({ url: `/pages/ai/chat?conversationId=${item.id}` }) }, loadSuggestions() { api.ai.suggestions({ petId: this.pet.id }).then(data => { if (Array.isArray(data)) this.questions = data }) } } }
</script>

<style scoped>
.ai-header { display: flex; align-items: center; justify-content: space-between; padding: 8rpx 4rpx 24rpx; }
.eyebrow { display: block; color: #C4612F; font-size: 19rpx; font-weight: 700; letter-spacing: 2rpx; }
.header-sub { display: block; margin-top: 8rpx; font-size: 28rpx; color: #7C7367; }
.ai-avatar-wrap { position: relative; }
.ai-avatar { width: 84rpx; height: 84rpx; border-radius: 999rpx; border: 6rpx solid #fff; box-shadow: 0 8rpx 18rpx rgba(82,65,47,.12); }
.ai-status { position: absolute; right: 2rpx; bottom: 6rpx; width: 18rpx; height: 18rpx; border: 4rpx solid #fff; border-radius: 999rpx; background: #5B8C5A; }
.pet-context { display: flex; align-items: center; gap: 16rpx; padding: 24rpx; }
.pet-context image { width: 52rpx; height: 52rpx; border-radius: 999rpx; }
.pet-context text { font-size: 25rpx; color: #5C635D; }
.context-icon { width: 58rpx; height: 58rpx; display: flex; align-items: center; justify-content: center; border-radius: 20rpx; background: rgba(255,255,255,.66); }
.context-title { display: block; color: #3D3D3A; font-size: 27rpx; font-weight: 600; }
.context-count { margin-left: auto; padding: 8rpx 14rpx; border-radius: 999rpx; background: #FAF0EB; color: #A85228 !important; }
.no-margin { margin: 0; }
.section-lead { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.question-list, .history-list { display: flex; flex-direction: column; gap: 16rpx; }
.question-card, .history-card { padding: 20rpx 22rpx; display: flex; align-items: center; justify-content: space-between; }
.question-card text { font-size: 30rpx; color: #3D3D3A; }
.question-mark, .history-icon { width: 54rpx; height: 54rpx; display: flex; align-items: center; justify-content: center; border-radius: 18rpx; background: #FAF0EB; margin-right: 14rpx; }
.history-card { justify-content: flex-start; }
.history-copy { flex: 1; min-width: 0; }
.history-preview { display: block; margin-top: 8rpx; font-size: 25rpx; color: #7C7367; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.view-all { display: inline-flex; align-items: center; gap: 2rpx; color: #A85228; }
.chat-entry { position: fixed; left: 32rpx; right: 32rpx; bottom: 136rpx; z-index: 20; min-height: 88rpx; padding: 14rpx 16rpx 14rpx 28rpx; display: flex; align-items: center; justify-content: space-between; }
.chat-entry > text { color: #9A9185; font-size: 28rpx; }
.send-btn { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; background: #C4612F; border-radius: 999rpx; }
</style>
