<template>
  <!-- 主選單：依登入者實際擁有的選單（權限）產生，分組沿用左側選單 -->
  <div class="m-hello">
    <div class="m-avatar">
      <img v-if="userStore.userInfo.avatarUrl" :src="userStore.userInfo.avatarUrl" alt="" />
      <template v-else>{{ (userStore.userInfo.realname || '?').slice(0, 1) }}</template>
    </div>
    <div style="min-width: 0">
      <b>{{ userStore.userInfo.realname }}<span class="m-role">{{ roleLabel }}</span></b>
      <span v-if="vdata.me">代理編號 <span class="mono">{{ vdata.me.agentNo }}</span></span>
      <span v-else>{{ userStore.userInfo.loginUsername }}</span>
    </div>
  </div>

  <button v-if="vdata.stat" class="m-glance" @click="openByName('ENT_AGENT_PORTAL_DASH')">
    <div><small>今日成交</small><strong class="num">{{ ntd(vdata.stat.todayAmount) }}</strong></div>
    <div><small>今日成功</small><strong class="num">{{ vdata.stat.todayPaid || 0 }} 筆</strong></div>
    <div><small>可提現</small><strong class="num">{{ vdata.wallet ? ntd(vdata.wallet.balance) : '—' }}</strong></div>
  </button>

  <section v-for="g in groups" :key="g.key" class="m-group">
    <h3 v-if="g.title">{{ g.title }}</h3>
    <div class="m-tiles">
      <button v-for="item in g.items" :key="item.path" class="m-tile" @click="emit('open', item.path)">
        <span class="m-ic"><component :is="item.meta.icon" /></span>
        {{ item.meta.title }}
        <span v-if="item.name === 'ENT_AGENT_PORTAL_WITHDRAW_AUDIT' && vdata.pending" class="m-badge">{{ vdata.pending }}</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, watch, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { useUserStore } from '@/store/modules/user'
import { agentMe, ntd, todayDateRange } from './mobileKit'

const props = defineProps({
  menuData: { type: Array as () => any[], default: () => [] },
  isAgentUser: { type: Boolean, default: false },
})
const emit = defineEmits(['open'])
const { $access } = getCurrentInstance()!.appContext.config.globalProperties
const userStore = useUserStore()
const vdata: any = reactive({ me: null, stat: null, wallet: null, pending: 0 })

const visible = (r) => r && r.meta && !r.meta.hideInMenu && !r.hidden
function leaves(r) {
  if (!r.children || !r.children.length) return visible(r) ? withQuery(r) : []
  return r.children.filter(visible).flatMap(leaves)
}
// 「訂單」（代理）／「訂單管理」（平台）後面多一格「查詢訂單」：同一頁、可選日期區間（不需另開權限）
function withQuery(r) {
  if (r.name !== 'ENT_AGENT_PORTAL_ORDER' && r.name !== 'ENT_PAY_ORDER') return [r]
  return [r, { name: '_ORDER_QUERY', path: r.path + '?mode=query', meta: { title: '查詢訂單', icon: 'search-outlined' } }]
}
// 沒有子選單的直接放最上面一組；有子選單的依分組名稱各一組
const groups = computed(() => {
  const top = props.menuData.filter((r) => visible(r) && (!r.children || !r.children.length)).flatMap(withQuery)
  const result: any[] = top.length ? [{ key: '_top', title: '', items: top }] : []
  props.menuData
    .filter((r) => visible(r) && r.children && r.children.length)
    .forEach((r) => {
      const items = leaves(r)
      if (items.length) result.push({ key: r.path, title: r.meta.title, items })
    })
  return result
})
function openByName(name) {
  const hit = groups.value.flatMap((g) => g.items).find((i) => i.name === name)
  if (hit) emit('open', hit.path)
}

const roleLabel = computed(() => {
  if (!props.isAgentUser) return '平台'
  if (!vdata.me) return '代理'
  return vdata.me.agentLevel === 1 ? '團長' : '隊長'
})

// 選單載入後才知道是不是代理帳號，所以用 watch 而不是只在建立時判斷一次
let loaded = false
watch(() => props.isAgentUser, (agent) => {
  if (!agent || loaded) return
  loaded = true
  agentMe().then((me) => (vdata.me = me)).catch(() => {})
  if ($access('ENT_AGENT_PORTAL_DASH')) {
    req.list(API_URL_AGENT_PORTAL + '/branch/dashboard', {}).then((r) => (vdata.stat = r || {}))
    req.list(API_URL_AGENT_PORTAL + '/wallet', {}).then((r) => (vdata.wallet = r || {}))
  }
  if ($access('ENT_AGENT_PORTAL_WITHDRAW_AUDIT')) {
    req.list(API_URL_AGENT_PORTAL + '/branch/withdraws', { state: 0, pageSize: 1, ...todayDateRange() }).then((r) => (vdata.pending = (r && r.total) || 0))
  }
}, { immediate: true })
</script>
