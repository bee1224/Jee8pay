<template>
  <!-- 上帝：團長詳情（看板、商戶、隊長、訂單、渠道、登入裝置），可直接啟用／停用 -->
  <p v-if="vdata.error" class="m-empty">{{ vdata.error }}</p>
  <template v-else-if="vdata.agent">
    <div class="m-card" style="display: flex; align-items: center; gap: 10px">
      <div class="m-avatar">{{ (vdata.agent.agentName || '?').slice(0, 1) }}</div>
      <div style="flex: 1; min-width: 0">
        <b>{{ vdata.agent.agentName }}</b><span class="m-role">{{ $t('團長') }}</span><span v-if="vdata.agent.isHouse === 1" class="m-role">{{ $t('平台直屬') }}</span>
        <div class="m-muted" style="margin: 2px 0 0"><span class="mono">{{ vdata.agent.agentNo }}</span>{{ vdata.agent.contactName ? '・' + vdata.agent.contactName : '' }}</div>
      </div>
      <a-switch :checked="vdata.agent.state === 1" :disabled="!$access('ENT_AGENT_INFO_EDIT')" @change="setState" />
    </div>
    <div class="m-stats">
      <div class="m-stat"><small>{{ $t('今日成交') }}</small><strong class="num">{{ ntd(s.todayAmount) }}</strong><small>{{ $t('昨日 {0}', [ntd(s.yesterdayAmount)]) }}</small></div>
      <div class="m-stat"><small>{{ $t('今日成功／失敗') }}</small><strong class="num">{{ s.todayPaid || 0 }}／{{ s.todayFailed || 0 }}</strong></div>
      <div class="m-stat"><small>{{ $t('本月成交') }}</small><strong class="num">{{ ntd(s.monthAmount) }}</strong></div>
      <div class="m-stat"><small>{{ $t('轄下商戶') }}</small><strong class="num">{{ $t('{0} 家', [s.mchCount || 0]) }}</strong></div>
    </div>
    <div class="m-chips">
      <button v-for="t in TABS" :key="t.key" :class="{ on: vdata.tab === t.key }" @click="vdata.tab = t.key">{{ t.label }}</button>
    </div>

    <div v-if="vdata.tab === 'mch'" class="m-list">
      <div v-for="m in vdata.merchants" :key="m.mchNo" class="m-row">
        <div class="main"><b>{{ m.mchName || m.mchNo }}</b><small><span class="mono">{{ m.mchNo }}</span>・{{ ownerName(m.agentNo) }}</small></div>
        <span :class="['m-pill', m.mchState === 0 ? 'mute' : 'ok']">{{ m.mchState === 0 ? $t('停用') : $t('啟用') }}</span>
      </div>
      <div v-if="!vdata.merchants.length" class="m-empty">{{ $t('還沒有商戶') }}</div>
    </div>

    <div v-if="vdata.tab === 'sub'" class="m-list">
      <div v-for="a in vdata.subAgents" :key="a.agentNo" class="m-row">
        <div class="main"><b>{{ a.agentName }}</b><small><span class="mono">{{ a.agentNo }}</span>{{ $t('・商戶 {0}', [vdata.merchants.filter((m) => m.agentNo === a.agentNo).length]) }}</small></div>
        <span :class="['m-pill', a.state === 0 ? 'mute' : 'ok']">{{ a.state === 0 ? $t('停用') : $t('啟用') }}</span>
      </div>
      <div v-if="!vdata.subAgents.length" class="m-empty">{{ $t('還沒有隊長') }}</div>
    </div>

    <MOrders v-if="vdata.tab === 'order'" :agent-no="vdata.agent.agentNo" />

    <ChannelAccountPanel v-if="vdata.tab === 'channel'" :sr-agent-no="vdata.agent.agentNo" :agent-name="vdata.agent.agentName" />

    <template v-if="vdata.tab === 'device'">
      <div class="m-list">
        <div v-for="d in vdata.devices" :key="d.id" class="m-row" style="flex-direction: column; align-items: stretch; gap: 3px">
          <b class="num">{{ d.createdAt }}<span class="m-muted" style="margin-left: 8px">{{ d.userName }}</span></b>
          <small class="mono">{{ d.userIp }}</small>
          <small>{{ [d.deviceModel, d.deviceOs].filter(Boolean).join('・') || $t('未知裝置') }}</small>
          <small v-if="d.deviceId">{{ $t('裝置識別碼') }} <span class="mono">{{ d.deviceId }}</span></small>
          <small v-if="d.latitude != null">
            GPS <a :href="`https://www.google.com/maps?q=${d.latitude},${d.longitude}`" target="_blank" rel="noopener noreferrer">{{ Number(d.latitude).toFixed(5) }}, {{ Number(d.longitude).toFixed(5) }}</a>
            <template v-if="d.accuracy">{{ $t('（誤差約 {0} 公尺）', [d.accuracy]) }}</template>
          </small>
          <small v-else class="m-muted" style="margin: 0">{{ GEO_TEXT[d.geoState] || $t('未取得定位') }}</small>
        </div>
        <div v-if="!vdata.devices.length" class="m-empty">{{ $t('還沒有登入裝置紀錄') }}</div>
      </div>
      <p class="m-muted">{{ $t('只有上帝看得到裝置、裝置識別碼與 GPS；團長只看得到時間與 IP。') }}</p>
    </template>
  </template>
  <p v-else class="m-empty">{{ $t('載入中…') }}</p>
</template>

<script setup lang="ts">
import { computed, reactive, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import { API_URL_AGENT_INFO, req } from '@/api/manage'
import ChannelAccountPanel from '@/views/agent/ChannelAccountPanel.vue'
import MOrders from '../agent/MOrders.vue'
import { ntd } from '../mobileKit'
import { t } from '@/i18n'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const route = useRoute()
const TABS = [
  { key: 'mch', label: t('商戶') },
  { key: 'sub', label: t('隊長') },
  { key: 'order', label: t('訂單') },
  { key: 'channel', label: t('渠道') },
  { key: 'device', label: t('登入裝置') },
]
const GEO_TEXT = { DENIED: t('使用者拒絕定位'), TIMEOUT: t('定位逾時'), UNAVAILABLE: t('無法定位'), UNSUPPORTED: t('裝置不支援定位') }
const vdata: any = reactive({ error: '', agent: null, stat: {}, merchants: [], subAgents: [], devices: [], tab: 'mch' })
const s = computed(() => vdata.stat || {})

function ownerName(agentNo) {
  if (vdata.agent && agentNo === vdata.agent.agentNo) return t('團長直屬')
  const sub = vdata.subAgents.find((a) => a.agentNo === agentNo)
  return sub ? t('隊長 ') + sub.agentName : agentNo
}
function setState(on) {
  const a = vdata.agent
  const doIt = () =>
    req.updateById(API_URL_AGENT_INFO, a.agentNo, { state: on ? 1 : 0 }).then(() => {
      a.state = on ? 1 : 0
      $infoBox.message.success(on ? t('已啟用') : t('已停用'))
    })
  if (on) doIt()
  else $infoBox.confirmDanger(t('停用「{0}」？', [a.agentName]), t('停用後這位代理與他的子帳號無法使用代理後台，也不能再綁定新商戶；旗下商戶照常收款。'), doIt)
}

const agentNo = String(route.query.agentNo || '')
if (!agentNo) {
  vdata.error = t('請從「團長列表」點選一位團長')
} else {
  req.getById(API_URL_AGENT_INFO, agentNo).then((a) => {
    if (a.agentLevel !== 1) {
      vdata.error = t('這不是團長，請從「團長列表」點選一位團長')
      return
    }
    vdata.agent = a
    req.list(`${API_URL_AGENT_INFO}/${agentNo}/dashboard`, {}).then((r) => (vdata.stat = r || {}))
    req.list(`${API_URL_AGENT_INFO}/${agentNo}/merchants`, {}).then((r) => (vdata.merchants = r || []))
    req.list(API_URL_AGENT_INFO, { parentAgentNo: agentNo, pageSize: -1 }).then((r) => (vdata.subAgents = r.records || []))
    req.list(`${API_URL_AGENT_INFO}/${agentNo}/loginDevices`, {}).then((r) => (vdata.devices = r || []))
  })
}
</script>
