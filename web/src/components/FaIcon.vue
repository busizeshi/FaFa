<template>
  <view class="fa-icon" :class="[`fa-icon--${name}`, `fa-icon--${tone}`]">
    <uni-icons :type="iconType" :size="iconPixelSize" :color="iconColor" />
  </view>
</template>

<script>
import UniIcons from '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue'

export default {
  name: 'FaIcon',
  components: { UniIcons },
  props: { name: { type: String, default: 'dot' }, size: { type: Number, default: 32 }, tone: { type: String, default: 'default' }, filled: { type: Boolean, default: false } },
  computed: {
    iconType() {
      const map = { back: 'arrow-left', next: 'arrow-right', plus: 'plus', more: 'more', home: 'home', timeline: 'list', spark: 'star', user: 'person', pet: 'heart', settings: 'gear', food: 'cart', water: 'cloud-download', weight: 'tune', excretion: 'circle', photo: 'images', event: 'calendar', bell: 'notification', report: 'list', message: 'chat', calendar: 'calendar', camera: 'camera', check: 'checkmarkempty', search: 'search', info: 'info', weixin: 'weixin', heart: 'heart', paperplane: 'paperplane', dot: 'smallcircle' }
      const filled = { home: 'home-filled', timeline: 'list', spark: 'star-filled', user: 'person-filled', pet: 'heart-filled', settings: 'gear-filled', food: 'cart-filled', water: 'cloud-download-filled', photo: 'images-filled', event: 'calendar-filled', bell: 'notification', report: 'list', message: 'chat-filled', calendar: 'calendar-filled', camera: 'camera-filled', check: 'checkbox-filled', heart: 'heart-filled', paperplane: 'paperplane-filled', info: 'info' }
      return this.filled && filled[this.name] ? filled[this.name] : (map[this.name] || map.dot)
    },
    iconPixelSize() { return Math.max(12, Math.round(this.size / 2)) },
    iconColor() { return { primary: '#C4612F', success: '#5B8C5A', muted: '#9A9185', danger: '#C44A3F', default: '#5C635D' }[this.tone] || '#5C635D' }
  }
}
</script>

<style scoped>
.fa-icon { width: 44rpx; height: 44rpx; display: inline-flex; align-items: center; justify-content: center; line-height: 1; flex-shrink: 0; }
</style>
