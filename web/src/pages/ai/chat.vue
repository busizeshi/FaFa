<template>
  <view class="chat-page"><view class="chat-topbar"><view class="back" @click="back"><FaIcon name="back" :size="38" /></view><view class="chat-title"><text>和 AI 聊聊</text><text class="caption">{{ pet.name }} 的生活助手</text></view><view class="chat-pet"><image :src="pet.avatar || '/static/assets/pet-avatar-doubao.jpg'" mode="aspectFill" /><view></view></view></view><scroll-view class="message-scroll" scroll-y :scroll-into-view="lastMessageId"><view v-for="item in messages" :key="item.id" :id="`message-${item.id}`" class="message-row" :class="item.role"><view class="message-bubble"><text>{{ item.content }}</text></view></view><view v-if="loading" class="message-row assistant"><view class="message-bubble typing"><text>正在思考…</text></view></view></scroll-view><view class="suggestion-row"><text v-for="item in suggestions" :key="item" class="suggestion" @click="sendSuggestion(item)">{{ item }}</text></view><view class="chat-input safe-bottom"><input v-model="input" confirm-type="send" @confirm="send" placeholder="问问关于豆包的事情..." /><button @click="send"><FaIcon name="paperplane" tone="default" filled :size="30" /></button></view></view>
</template>

<script>
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { FaIcon }, data() { return { pet: currentPet(), input: '', loading: false, messages: [{ id: 1, role: 'assistant', content: '你好呀，我已经准备好了解豆包的每一天。你可以问我体重、饮食、照片或成长记录。' }], suggestions: ['最近体重怎么样？', '这个月吃了多少？', '找阳台的照片'], lastMessageId: 'message-1' } }, onLoad(options) { if (options?.prompt) this.$nextTick(() => this.sendSuggestion(decodeURIComponent(options.prompt))) }, methods: { back() { uni.navigateBack({ delta: 1 }) }, sendSuggestion(text) { this.input = text; this.send() }, send() { const text = String(this.input || '').trim(); if (!text || this.loading) return; this.messages.push({ id: Date.now(), role: 'user', content: text }); this.input = ''; this.loading = true; this.scrollLast(); api.ai.chat({ petId: this.pet.id, message: text }).then(data => { this.messages.push({ id: Date.now() + 1, role: 'assistant', content: data?.answer || '我正在整理这部分记录，稍后再告诉你。' }) }).catch(() => { this.messages.push({ id: Date.now() + 1, role: 'assistant', content: '暂时连接不上 AI 服务，但你的问题已经保留。请稍后再试。' }) }).finally(() => { this.loading = false; this.scrollLast() }) }, scrollLast() { this.$nextTick(() => { const last = this.messages[this.messages.length - 1]; if (last) this.lastMessageId = `message-${last.id}` }) } } }
</script>

<style scoped>
.chat-page { min-height: 100vh; display: flex; flex-direction: column; background: #F7F4EF; padding-top: env(safe-area-inset-top); }
.chat-topbar { min-height: 112rpx; display: flex; align-items: center; gap: 18rpx; padding: 20rpx 32rpx; background: rgba(255,255,255,.88); border-bottom: 1rpx solid #EFEAE1; box-shadow: 0 8rpx 22rpx rgba(82,65,47,.06); }
.chat-title { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4rpx; font-size: 32rpx; font-weight: 700; color: #1F2421; }
.chat-pet { position: relative; }
.chat-pet image { width: 68rpx; height: 68rpx; border-radius: 999rpx; }
.chat-pet view { position: absolute; right: 0; bottom: 4rpx; width: 16rpx; height: 16rpx; border: 3rpx solid #fff; border-radius: 999rpx; background: #5B8C5A; }
.message-scroll { flex: 1; height: calc(100vh - 320rpx); padding: 28rpx 32rpx; }
.message-row { display: flex; margin-bottom: 24rpx; }
.message-row.user { justify-content: flex-end; }
.message-bubble { max-width: 78%; padding: 20rpx 24rpx; border-radius: 24rpx 24rpx 24rpx 6rpx; background: rgba(255,255,255,.92); color: #3D3D3A; font-size: 29rpx; line-height: 1.6; box-shadow: 0 8rpx 18rpx rgba(82,65,47,.06); }
.message-row.user .message-bubble { border-radius: 24rpx 24rpx 6rpx 24rpx; background: #C4612F; color: #fff; }
.typing { color: #A85228; border: 2rpx dashed #EDC5B0; background: #FAF0EB; }
.suggestion-row { display: flex; gap: 12rpx; padding: 12rpx 32rpx; overflow-x: auto; white-space: nowrap; }
.suggestion { flex-shrink: 0; padding: 10rpx 18rpx; border-radius: 999rpx; background: #fff; color: #A85228; font-size: 24rpx; }
.chat-input { display: flex; align-items: center; gap: 14rpx; padding: 14rpx 32rpx 24rpx; background: rgba(255,255,255,.9); border-top: 1rpx solid #EFEAE1; }
.chat-input input { flex: 1; height: 76rpx; padding: 0 24rpx; border-radius: 999rpx; background: #F7F4EF; color: #3D3D3A; font-size: 27rpx; }
.chat-input button { width: 76rpx; height: 76rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: #C4612F; }
</style>
