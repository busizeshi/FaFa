<template><view class="page"><PageHeader title="日常事件" /><scroll-view scroll-x class="filter-scroll"><view class="filter-row"><text v-for="item in filters" :key="item.value" class="pill" :class="active === item.value ? 'pill--primary' : 'pill--outline'" @click="active = item.value">{{ item.label }}</text></view></scroll-view><view class="section"><view v-for="item in filteredRecords" :key="item.id" class="card event-card"><view class="event-icon"><FaIcon name="event" tone="primary" :size="32" /></view><view class="event-copy"><view class="between"><text class="subheading">{{ item.title }}</text><text class="caption">{{ item.eventTime }}</text></view><text class="caption" v-if="item.tags?.length">{{ item.tags.join(' · ') }}</text></view></view><view v-if="!filteredRecords.length" class="card empty">还没有这个类型的事件。</view></view><view class="fab" @click="showForm = !showForm"><FaIcon name="plus" tone="default" :size="42" /></view><view v-if="showForm" class="modal-mask" @click="showForm = false"><view class="card event-form" @click.stop><text class="heading">记录事件</text><input v-model="form.title" class="input" placeholder="发生了什么？" /><textarea v-model="form.content" class="textarea" placeholder="补充细节（可选）" /><button class="button-primary" @click="save">保存事件</button></view></view></view></template>

<script>
import PageHeader from '../../components/PageHeader.vue'
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { currentPet } from '../../utils/store'
export default { components: { PageHeader, FaIcon }, data() { return { pet: currentPet(), records: [], active: 'all', showForm: false, form: { title: '', content: '' }, filters: [{ value: 'all', label: '全部' }, { value: 'bath', label: '洗澡' }, { value: 'vet', label: '就医' }, { value: 'deworming', label: '驱虫' }, { value: 'other', label: '其他' }] } }, onShow() { this.load() }, computed: { filteredRecords() { return this.active === 'all' ? this.records : this.records.filter(item => item.eventType === this.active) } }, methods: { load() { api.events.list({ petId: this.pet.id }).then(data => { this.records = data || [] }) }, save() { if (!this.form.title) return uni.showToast({ title: '请写下事件标题', icon: 'none' }); api.events.create({ petId: this.pet.id, eventType: 'other', eventTime: new Date().toISOString().slice(0, 10), ...this.form, tags: [] }).then(() => { this.form = { title: '', content: '' }; this.showForm = false; this.load(); uni.showToast({ title: '已保存', icon: 'success' }) }) } } }
</script>

<style scoped>
.filter-scroll { margin: 0 -32rpx; white-space: nowrap; }
.filter-row { display: inline-flex; gap: 16rpx; padding: 4rpx 32rpx; }
.event-card { display: flex; align-items: center; gap: 18rpx; padding: 24rpx; margin-bottom: 16rpx; }
.event-icon { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; border-radius: 20rpx; background: #FAF0EB; }
.event-copy { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.empty { padding: 48rpx 24rpx; text-align: center; color: #9A9185; }
.modal-mask { position: fixed; inset: 0; z-index: 60; display: flex; align-items: flex-end; background: rgba(31,36,33,.34); }
.event-form { width: 100%; padding: 36rpx 32rpx calc(36rpx + env(safe-area-inset-bottom)); border-radius: 32rpx 32rpx 0 0; }
.event-form .input, .event-form .textarea { margin-top: 20rpx; }
.event-form .button-primary { margin-top: 24rpx; }
</style>
