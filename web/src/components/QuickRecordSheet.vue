<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { usePetStore } from '@/stores/pet'
const props = defineProps<{ modelValue: boolean; type?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const store = usePetStore()
const value = ref('32')
const note = ref('')
const configuration: Record<string, { title: string; unit: string; placeholder: string; value: string; icon: string }> = {
  feeding: { title: '记录喂食', unit: 'g', placeholder: '输入克数', value: '32', icon: 'compose' }, water: { title: '记录饮水', unit: 'ml', placeholder: '输入饮水量', value: '120', icon: 'calendar' }, weight: { title: '记录体重', unit: 'kg', placeholder: '输入体重', value: '5.2', icon: 'checkbox' }, toilet: { title: '记录排便', unit: '', placeholder: '正常 / 偏软 / 偏硬', value: '正常', icon: 'list' }, event: { title: '记录事件', unit: '', placeholder: '今天发生了什么？', value: '', icon: 'star' }, photo: { title: '添加照片', unit: '', placeholder: '给照片写个描述', value: '', icon: 'camera' }
}
const current = computed(() => configuration[props.type || 'feeding'] || configuration.event)
watch(() => props.type, () => { value.value = current.value.value; note.value = '' }, { immediate: true })
function close() { emit('update:modelValue', false) }
function save() { store.addRecord(props.type || 'feeding', value.value, note.value); uni.showToast({ title: '已保存', icon: 'success' }); close() }
</script>
<template><view v-if="modelValue" class="mask" @tap="close"><view class="sheet" @tap.stop><view class="bar" /><view class="head"><view><text>{{ current.title }}</text><text>为 {{ store.pet.name }} 添加一条记录</text></view><view class="close" @tap="close"><uni-icons type="closeempty" size="20" color="#636366" /></view></view><view class="pet"><image :src="store.pet.avatar" mode="aspectFill" /><text>{{ store.pet.name }}</text><text>刚刚</text></view><view class="field"><text>数量</text><view><input v-model="value" :placeholder="current.placeholder" /><text>{{ current.unit }}</text></view></view><view class="field"><text>备注（选填）</text><view><input v-model="note" placeholder="写一点小细节" /></view></view><button class="save" @tap="save">保存记录</button></view></view></template>
<style scoped lang="scss">
@use '@/styles/tokens.scss' as *;
.mask { position: fixed; z-index: 80; inset: 0; display: flex; align-items: flex-end; background: rgba(0,0,0,.34); }.sheet { width: 100%; padding: 18rpx 32rpx calc(38rpx + env(safe-area-inset-bottom)); border-radius: 38rpx 38rpx 0 0; background: #F9F9FB; }.bar { width: 64rpx; height: 8rpx; margin: 0 auto 28rpx; border-radius: 99rpx; background: #C7C7CC; }.head { display: flex; align-items: center; justify-content: space-between; }.head view:first-child { display: flex; flex-direction: column; gap: 8rpx; }.head text:first-child { font-size: 36rpx; font-weight: 700; }.head text:last-child { color: $tertiary; font-size: 22rpx; }.close { display: flex; width: 60rpx; height: 60rpx; align-items: center; justify-content: center; border-radius: 50%; background: #E9E9ED; }.pet { display: flex; align-items: center; gap: 11rpx; margin: 30rpx 0; color: $secondary; font-size: 24rpx; }.pet image { width: 48rpx; height: 48rpx; border-radius: 50%; }.pet text:last-child { margin-left: auto; color: $tertiary; }.field { margin: 20rpx 0; }.field > text { display: block; margin: 0 0 12rpx 8rpx; color: $secondary; font-size: 24rpx; }.field > view { display: flex; height: 92rpx; align-items: center; padding: 0 22rpx; border-radius: 18rpx; background: #fff; }.field input { flex: 1; font-size: 30rpx; }.field view text { color: $secondary; }.save { margin-top: 32rpx; border-radius: 999rpx; background: $blue; color: #fff; font-size: 30rpx; line-height: 92rpx; }
</style>
