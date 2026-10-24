<template>
  <!-- 電腦版首頁：問候＋（代理）今日概況，下面依分類一區一區排列，點圖示進入頁面 -->
  <section class="dk-hello">
    <div class="dk-avatar">
      <img v-if="userStore.userInfo.avatarUrl" :src="userStore.userInfo.avatarUrl" alt="" />
      <template v-else>{{ (userStore.userInfo.realname || '?').slice(0, 1) }}</template>
    </div>
    <div class="dk-hello-text">
      <b>{{ $t('{0}，{1}', [timeFix(), userStore.userInfo.realname]) }}</b>
      <span>
        <span class="dk-role">{{ roleLabel }}</span>
        <template v-if="vdata.me">{{ $t('代理編號') }} {{ vdata.me.agentNo }}</template>
        <template v-else>{{ userStore.userInfo.loginUsername }}</template>
      </span>
    </div>
  </section>

  <section v-if="vdata.stat" class="dk-glance">
    <button class="dk-stat main" @click="openByName('ENT_AGENT_PORTAL_DASH')">
      <small>{{ $t('今日成交') }}</small><strong>{{ ntd(vdata.stat.todayAmount) }}</strong>
    </button>
    <button class="dk-stat" @click="openByName('ENT_AGENT_PORTAL_DASH')">
      <small>{{ $t('今日成功') }}</small><strong>{{ $t('{0} 筆', [vdata.stat.todayPaid || 0]) }}</strong>
    </button>
    <button class="dk-stat" @click="openByName('ENT_AGENT_PORTAL_HOME')">
      <small>{{ $t('可提現') }}</small><strong>{{ vdata.wallet ? ntd(vdata.wallet.balance) : '—' }}</strong>
    </button>
  </section>

  <div class="dk-groups">
    <section v-for="g in groups" :key="g.key" class="dk-group">
      <!-- 沒有子選單的頁面（例如主頁）放在「常用」 -->
      <h3>
        <span class="dk-group-ic"><component :is="g.icon || 'star-outlined'" /></span>
        {{ g.title || $t('常用') }}
        <small>{{ g.items.length }}</small>
      </h3>
      <div class="dk-tiles">
        <button v-for="item in g.items" :key="item.path" class="dk-tile" @click="emit('open', item.path)">
          <span class="dk-ic"><component :is="item.meta.icon" /></span>
          <span class="dk-tile-name">{{ item.meta.title }}</span>
          <ShortcutKeys v-if="pageCombo(item.name)" class="dk-tile-key" :combo="pageCombo(item.name)" small />
          <span v-if="item.name === 'ENT_AGENT_PORTAL_WITHDRAW_AUDIT' && vdata.pending" class="dk-badge">{{ vdata.pending }}</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { useUserStore } from '@/store/modules/user'
import { agentMe, ntd, todayDateRange } from '@/views/mobile/mobileKit'
import { timeFix } from '@/utils/util'
import { t } from '@/i18n'
import type { MenuGroup } from '@/utils/menuGroups'
import { config } from '@/shortcuts'
import ShortcutKeys from '@/shortcuts/ShortcutKeys.vue'

const props = defineProps({
  groups: { type: Array as () => MenuGroup[], default: () => [] },
  isAgentUser: { type: Boolean, default: false },
})
const emit = defineEmits(['open'])
const { $access } = getCurrentInstance()!.appContext.config.globalProperties
const userStore = useUserStore()
const vdata: any = reactive({ me: null, stat: null, wallet: null, pending: 0 })

// 有設定頁面捷徑的圖示下方顯示按鍵（可在快捷鍵設定關閉）
const pageCombo = (name) => (config.value.showOnHome ? (config.value.pages.find((p) => p.name === name) || { combo: '' }).combo : '')

function openByName(name) {
  const hit = props.groups.flatMap((g) => g.items).find((i) => i.name === name)
  if (hit) emit('open', hit.path)
}

const roleLabel = computed(() => {
  if (!props.isAgentUser) return t('平台')
  if (!vdata.me) return t('代理')
  return vdata.me.agentLevel === 1 ? t('團長') : t('隊長')
})

// 代理帳號：與手機主選單相同的今日概況與待審核提現數
let loaded = false
watch(
  () => props.isAgentUser,
  (agent) => {
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
  },
  { immediate: true },
)
</script>
