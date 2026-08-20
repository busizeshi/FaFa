<script>
import { api } from './api'
import { config } from './utils/config'

export default {
  onLaunch() {
    const theme = uni.getStorageSync('fafa-theme') || 'light'
    uni.setStorageSync('fafa-theme', theme)
    if (!config.mock && !uni.getStorageSync('fafa-token')) {
      uni.login({ provider: 'weixin', success: ({ code }) => api.auth.login(code).then(result => { if (result?.token) uni.setStorageSync('fafa-token', result.token); if (result?.isNewUser) setTimeout(() => uni.navigateTo({ url: '/pages/pet/create' }), 300) }) })
    }
  }
}
</script>

<style>
/* FaFa 的视觉底座：页面、卡片与系统安全区只在这里定义一次。 */
page {
  --fa-bg: #F7F4EF;
  --fa-surface: rgba(255, 255, 255, .92);
  --fa-surface-soft: rgba(255, 255, 255, .64);
  --fa-ink: #1F2421;
  --fa-muted: #7C7367;
  --fa-faint: #9A9185;
  --fa-border: rgba(231, 225, 215, .9);
  --fa-primary: #C4612F;
  --fa-primary-soft: #FAF0EB;
  --fa-success: #5B8C5A;
  background: var(--fa-bg);
  color: var(--fa-ink);
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  font-size: 28rpx;
  line-height: 1.55;
  -webkit-font-smoothing: antialiased;
}

view, text, button, input, textarea, scroll-view, image {
  box-sizing: border-box;
}

button::after { border: 0; }
button { margin: 0; padding: 0; line-height: 1; }
image { max-width: 100%; }

.page { min-height: 100vh; background: var(--fa-bg); padding: calc(env(safe-area-inset-top) + 24rpx) 32rpx calc(env(safe-area-inset-bottom) + 168rpx); }
.page--compact { padding-top: calc(env(safe-area-inset-top) + 12rpx); }
.card { background: var(--fa-surface); border: 1rpx solid rgba(255,255,255,.9); border-radius: 32rpx; box-shadow: 0 10rpx 30rpx rgba(82,65,47,.06), 0 1rpx 2rpx rgba(82,65,47,.05); }
.glass-card { background: rgba(255,255,255,.54); border: 1rpx solid rgba(255,255,255,.78); box-shadow: 0 18rpx 48rpx rgba(63,42,24,.12); }
.ai-card { background: linear-gradient(135deg, #FBF3EE 0%, #F8E9DF 100%); border: 1rpx solid #EDC5B0; border-radius: 32rpx; box-shadow: 0 10rpx 28rpx rgba(196,97,47,.08); }
.title { font-size: 42rpx; line-height: 1.28; font-weight: 700; letter-spacing: -1rpx; color: var(--fa-ink); }
.heading { font-size: 34rpx; line-height: 1.4; font-weight: 700; color: var(--fa-ink); }
.subheading { font-size: 30rpx; line-height: 1.4; font-weight: 600; color: var(--fa-ink); }
.body { font-size: 30rpx; color: var(--fa-ink); }
.muted { color: var(--fa-muted); }
.caption { font-size: 24rpx; line-height: 1.45; color: var(--fa-faint); }
.primary { color: var(--fa-primary); }
.success { color: var(--fa-success); }
.warning { color: #D49B3A; }
.danger { color: #C44A3F; }
.row { display: flex; align-items: center; }
.between { display: flex; align-items: center; justify-content: space-between; }
.col { display: flex; flex-direction: column; }
.pill { display: inline-flex; align-items: center; justify-content: center; min-height: 60rpx; padding: 0 24rpx; border-radius: 999rpx; font-size: 25rpx; }
.pill--primary { background: var(--fa-primary); color: #fff; box-shadow: 0 6rpx 14rpx rgba(196,97,47,.16); }
.pill--soft { background: var(--fa-primary-soft); color: #A85228; }
.pill--outline { background: rgba(255,255,255,.7); color: var(--fa-muted); border: 1rpx solid var(--fa-border); }
.button-primary { height: 92rpx; border-radius: 999rpx; background: var(--fa-primary); color: #fff; font-size: 31rpx; font-weight: 600; display: flex; align-items: center; justify-content: center; box-shadow: 0 12rpx 24rpx rgba(196,97,47,.2); }
.button-primary:active { transform: scale(.985); opacity: .92; }
.button-ghost { height: 88rpx; border-radius: 999rpx; background: rgba(255,255,255,.72); border: 1rpx solid var(--fa-primary); color: #A85228; font-size: 30rpx; display: flex; align-items: center; justify-content: center; }
.section { margin-top: 24rpx; }
.section-title { margin-bottom: 20rpx; font-size: 32rpx; font-weight: 700; color: #1F2421; }
.gap-1 { gap: 12rpx; }
.gap-2 { gap: 20rpx; }
.gap-3 { gap: 32rpx; }
.mt-1 { margin-top: 12rpx; }
.mt-2 { margin-top: 20rpx; }
.mt-3 { margin-top: 32rpx; }
.mb-1 { margin-bottom: 12rpx; }
.mb-2 { margin-bottom: 20rpx; }
.mb-3 { margin-bottom: 32rpx; }
.safe-bottom { padding-bottom: env(safe-area-inset-bottom); }
.input { width: 100%; height: 88rpx; padding: 0 24rpx; background: rgba(251,249,245,.85); border: 1rpx solid var(--fa-border); border-radius: 18rpx; color: var(--fa-ink); font-size: 30rpx; }
.textarea { width: 100%; min-height: 180rpx; padding: 20rpx 24rpx; background: rgba(251,249,245,.85); border: 1rpx solid var(--fa-border); border-radius: 18rpx; color: var(--fa-ink); font-size: 30rpx; }
.form-label { display: block; margin: 24rpx 0 12rpx; font-size: 26rpx; font-weight: 600; color: #3D3D3A; }
.back { width: 72rpx; height: 72rpx; border-radius: 999rpx; background: rgba(255,255,255,.84); border: 1rpx solid rgba(255,255,255,.95); display: flex; align-items: center; justify-content: center; color: #3D3D3A; box-shadow: 0 6rpx 18rpx rgba(82,65,47,.06); }
.topbar { min-height: 72rpx; display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.topbar-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 700; color: var(--fa-ink); }
.topbar-spacer { width: 72rpx; }
.fab { position: fixed; right: 32rpx; bottom: calc(144rpx + env(safe-area-inset-bottom)); z-index: 10; width: 96rpx; height: 96rpx; border-radius: 999rpx; background: var(--fa-primary); color: #fff; display: flex; align-items: center; justify-content: center; border: 6rpx solid rgba(255,255,255,.72); box-shadow: 0 12rpx 28rpx rgba(196,97,47,.28); }
.fab:active { transform: scale(.94); }
.fab text { font-size: 38rpx; }
</style>
