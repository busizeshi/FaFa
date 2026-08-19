<template>
  <view class="login-page">
    <image class="login-bg" src="/static/assets/login-bg-anime.jpg" mode="aspectFill" />
    <view class="login-shade"></view>
    <view class="login-content">
      <view class="brand-lockup">
        <view class="brand-mark glass-panel"><FaIcon name="pet" tone="primary" filled :size="48" /></view>
        <view class="brand-copy"><text class="brand">FaFa</text><text class="tagline">记录宠物一生的 AI 生活助手</text></view>
      </view>

      <view class="login-card glass-panel">
        <view class="login-eyebrow"><FaIcon name="spark" tone="primary" filled :size="28" /><text>把被爱的小日子，认真收藏</text></view>
        <view class="login-title"><text>和 Ta 一起，</text><text class="login-title-accent">把日子过得柔软</text></view>
        <text class="login-copy">从今天开始，记录每一个值得回看的小瞬间。</text>
        <button class="wechat-button" open-type="getUserInfo" @click="login"><FaIcon name="weixin" tone="default" :size="34" /><text>微信一键登录</text></button>
        <text class="agreement">登录即表示同意《用户协议》和《隐私政策》</text>
      </view>
    </view>
  </view>
</template>

<script>
import FaIcon from '../../components/FaIcon.vue'
import { api } from '../../api'
import { config } from '../../utils/config'

export default {
  components: { FaIcon },
  methods: {
    login() {
      const complete = (code) => api.auth.login(code).then(result => {
        if (result?.token) uni.setStorageSync('fafa-token', result.token)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: result?.isNewUser ? '/pages/pet/create' : '/pages/home/home' }), 500)
      })
      if (config.mock) return complete('mock-code')
      uni.login({ provider: 'weixin', success: ({ code }) => complete(code), fail: () => uni.showToast({ title: '微信登录失败', icon: 'none' }) })
    }
  }
}
</script>

<style scoped>
.login-page { position: relative; min-height: 100vh; overflow: hidden; background: #6C4B34; }
.login-bg, .login-shade { position: absolute; inset: 0; width: 100%; height: 100%; }
.login-bg { object-fit: cover; transform: scale(1.02); }
.login-shade { background: linear-gradient(180deg, rgba(46,29,19,.42) 0%, rgba(74,48,31,.10) 34%, rgba(46,29,19,.62) 100%); }
.login-content { position: relative; z-index: 1; min-height: 100vh; display: flex; flex-direction: column; justify-content: space-between; padding: calc(env(safe-area-inset-top) + 38rpx) 44rpx calc(env(safe-area-inset-bottom) + 42rpx); }
.brand-lockup { display: flex; align-items: center; gap: 18rpx; }
.glass-panel { background: rgba(255,255,255,.25); border: 1rpx solid rgba(255,255,255,.58); box-shadow: 0 18rpx 48rpx rgba(45,26,14,.18); backdrop-filter: blur(22px); }
.brand-mark { width: 82rpx; height: 82rpx; display: flex; align-items: center; justify-content: center; border-radius: 28rpx; }
.brand-copy { display: flex; flex-direction: column; }
.brand { color: #fff; font-size: 54rpx; line-height: 1; font-weight: 800; letter-spacing: 2rpx; text-shadow: 0 4rpx 18rpx rgba(45,26,14,.25); }
.tagline { margin-top: 10rpx; color: rgba(255,255,255,.84); font-size: 23rpx; }
.login-card { padding: 34rpx 32rpx 28rpx; border-radius: 34rpx; background: rgba(255,249,241,.72); border-color: rgba(255,255,255,.84); }
.login-eyebrow { display: flex; align-items: center; gap: 10rpx; color: #8B4221; font-size: 23rpx; }
.login-title { display: block; margin-top: 22rpx; color: #3B271C; font-size: 42rpx; line-height: 1.34; font-weight: 700; letter-spacing: -1rpx; }
.login-title-accent { color: #A85228; }
.login-copy { display: block; margin-top: 14rpx; color: #6E5A4B; font-size: 26rpx; line-height: 1.6; }
.wechat-button { height: 92rpx; display: flex; align-items: center; justify-content: center; gap: 12rpx; margin-top: 28rpx; border-radius: 999rpx; background: #fff; color: #8B4221; font-size: 30rpx; font-weight: 700; box-shadow: 0 10rpx 24rpx rgba(82,48,27,.14); }
.wechat-button:active { transform: scale(.985); background: #FFF8F0; }
.agreement { display: block; margin-top: 18rpx; text-align: center; color: #806C5C; font-size: 21rpx; }
</style>
