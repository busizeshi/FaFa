<template>
  <view class="page"><PageHeader title="提醒中心" /><view class="view-switch card"><text :class="{ active: view === 'list' }" @click="view = 'list'">列表</text><text :class="{ active: view === 'calendar' }" @click="view = 'calendar'">日历</text></view><scroll-view scroll-x class="filter-scroll"><view class="filter-row"><text v-for="item in filters" :key="item.value" class="pill" :class="filter === item.value ? 'pill--primary' : 'pill--outline'" @click="filter = item.value">{{ item.label }}</text></view></scroll-view><view class="section"><view class="between mb-2"><text class="section-title no-margin">待办</text><text class="caption">{{ pending.length }} 件</text></view><view v-for="item in pending" :key="item.id" class="card reminder-item"><view class="reminder-icon"><FaIcon name="bell" tone="primary" :size="30" /></view><view class="reminder-copy"><text class="subheading">{{ item.title }}</text><text class="caption">{{ item.remindTime }} · {{ repeatText(item.repeatType) }}</text></view><button class="pill pill--primary complete" @click="complete(item)">已完成</button></view><view v-if="!pending.length" class="card empty">今天没有待办提醒，享受和 Ta 的轻松时光吧。</view></view><view class="section" v-if="done.length"><view class="between mb-2"><text class="section-title no-margin">已完成</text><text class="caption">{{ done.length }} 件</text></view><view v-for="item in done" :key="item.id" class="card reminder-item done"><view class="reminder-icon"><FaIcon name="check" tone="success" :size="30" /></view><view class="reminder-copy"><text class="subheading">{{ item.title }}</text><text class="caption">{{ item.remindTime }}</text></view></view></view><view class="fab" @click="add"><FaIcon name="plus" tone="default" :size="42" /></view></view>
</template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pet: currentPet(), reminders: [], view: 'list', filter: 'all', filters: [{ value: 'all', label: '全部' }, { value: 'vaccine', label: '疫苗' }, { value: 'deworming', label: '驱虫' }, { value: 'bath', label: '洗澡' }, { value: 'nail', label: '剪指甲' }, { value: 'checkup', label: '体检' }] } }, onShow() { this.load() }, computed: { filtered() { return this.filter === 'all' ? this.reminders : this.reminders.filter(item => item.reminderType === this.filter) }, pending() { return this.filtered.filter(item => item.status === 'pending') }, done() { return this.filtered.filter(item => item.status === 'completed') } }, methods: { load() { api.reminders.list({ petId: this.pet.id }).then(data => { this.reminders = data || [] }) }, complete(item) { api.reminders.complete(item.id).then(() => { item.status = 'completed'; uni.showToast({ title: '已完成', icon: 'success' }) }) }, add() { uni.showModal({ title: '新增提醒', editable: true, placeholderText: '例如：每月洗澡', success: (res) => { if (res.confirm && res.content) api.reminders.create({ petId: this.pet.id, title: res.content, reminderType: 'custom', remindTime: '待设置', repeatType: 'once' }).then(() => this.load()) } }) }, repeatText(v) { return { monthly: '每月', quarterly: '每3个月', yearly: '每年', once: '一次性' }[v] || '自定义' } } }
</script>

<style scoped>
.view-switch { display: flex; padding: 8rpx; margin-bottom: 20rpx; }
.view-switch text { flex: 1; padding: 14rpx; border-radius: 999rpx; text-align: center; color: #7C7367; font-size: 26rpx; }
.view-switch text.active { background: #FAF0EB; color: #C4612F; font-weight: 600; }
.filter-scroll { margin: 0 -32rpx; white-space: nowrap; }
.filter-row { display: inline-flex; gap: 16rpx; padding: 4rpx 32rpx; }
.no-margin { margin: 0; }
.reminder-item { display: flex; align-items: center; gap: 16rpx; padding: 24rpx; margin-bottom: 16rpx; }
.reminder-icon { width: 64rpx; height: 64rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; background: #FAF0EB; }
.reminder-copy { flex: 1; display: flex; flex-direction: column; gap: 4rpx; }
.complete { padding: 10rpx 18rpx; font-size: 23rpx; }
.done { opacity: .62; }
.done .reminder-icon { background: #F0FDF4; }
.empty { padding: 48rpx 24rpx; text-align: center; color: #9A9185; }
</style>
