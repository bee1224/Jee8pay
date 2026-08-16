import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import wayCode from './utils/wayCode'
import config from './config'
import 'amfe-flexible'

/**
 * 路由守卫
 */
router.beforeEach((to, from, next) => {
    // 免守卫路由名单（使用 passGuardRouteList 数组判断，而非字符串 includes）
    if (config.passGuardRouteList.includes(to.name)) {
      next()
      return
    }

    // 从路由参数或 query 中恢复 token（支持 /hub/:jeepayToken 和刷新场景）
    const token = to.params[config.urlTokenName] || to.query[config.urlTokenName]
    if (token) {
        config.cacheToken = token
    }

    if (!config.cacheToken) {
        next({ name: config.errorPageRouteName, params: { errInfo: '請透過 QR Code 進入支付頁面！' } })
        return
    }

    if (!wayCode.getPayWay()) {
        next({ name: config.errorPageRouteName, params: { errInfo: '不支援的支付方式！ 請在微信/支付寶/銀聯應用內掃碼進入！' } })
        return
    }

    next()
})

const app = createApp(App)
app.config.globalProperties.$config = config
app.use(router)
app.mount('#app')
