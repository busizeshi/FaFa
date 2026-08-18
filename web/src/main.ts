import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import UniIcons from '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  app.component('uni-icons', UniIcons)
  return { app }
}
