<template>
  <!-- 主選單：依登入者實際擁有的選單（權限）產生，分組沿用左側選單 -->
  <div class="m-hello">
    <div class="m-avatar">
      <img v-if="userStore.userInfo.avatarUrl" :src="userStore.userInfo.avatarUrl" alt="" />
      <template v-else>{{ (userStore.userInfo.realname || '?').slice(0, 1) }}</template>
    </div>
    <div style="min-width: 0">
      <b>{{ userStore.userInfo.realname }}<span class="m-role">{{ roleLabel }}</span></b>
      <span v-if="vdata.me">{{ $t('代理編號') }} <span class="mono">{{ vdata.me.agentNo }}</span></span>
      <span v-else>{{ userStore.userInfo.loginUsername }}</span>
    </div>
  </div>

  <button v-if="vdata.stat" class="m-glance" @click="openByName('ENT_AGENT_PORTAL_DASH')">
    <div><small>{{ $t('今日成交') }}</small><strong class="num">{{ ntd(vdata.stat.todayAmount) }}</strong></div>
    <div><small>{{ $t('今日成功') }}</small><strong class="num">{{ $t('{0} 筆', [vdata.stat.todayPaid || 0]) }}</strong></div>
    <div><small>{{ $t('可提現') }}</small><strong class="num">{{ vdata.wallet ? ntd(vdata.wallet.balance) : '—' }}</strong></div>
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
import { t } from '@/i18n'
import { menuGroups } from '@/utils/menuGroups'

const props = defineProps({
  menuData: { type: Array as () => any[], default: () => [] },
  isAgentUser: { type: Boolean, default: false },
})
const emit = defineEmits(['open'])
const { $access } = getCurrentInstance()!.appContext.config.globalProperties
const userStore = useUserStore()
const vdata: any = reactive({ me: null, stat: null, wallet: null, pending: 0 })

// 分類規則與電腦版首頁共用（utils/menuGroups.ts）；手機版的訂單後面多一格「查詢訂單」
const groups = computed(() => menuGroups(props.menuData, { orderQuery: true }))
function openByName(name) {
  const hit = groups.value.flatMap((g) => g.items).find((i) => i.name === name)
  if (hit) emit('open', hit.path)
}

const roleLabel = computed(() => {
  if (!props.isAgentUser) return t('平台')
  if (!vdata.me) return t('代理')
  return vdata.me.agentLevel === 1 ? t('團長') : t('隊長')
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
