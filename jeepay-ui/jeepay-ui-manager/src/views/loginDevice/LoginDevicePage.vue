<template>
  <!-- 上帝：登入裝置。自己的最近登入，以及每位團長、隊長（含其操作員）最近一次登入的 IP、裝置與 GPS -->
  <div class="m-page ld-page">
    <div class="m-section-title"><h3>我的登入</h3><span class="m-muted">最近 {{ vdata.mine.length }} 次</span></div>
    <div v-if="vdata.mine.length" class="m-card">
      <DeviceKv :d="vdata.mine[0]" />
    </div>
    <div v-else class="m-empty">{{ vdata.loaded ? '還沒有登入紀錄（下次登入後會出現）' : '載入中…' }}</div>
    <div v-if="vdata.mine.length > 1" class="m-list">
      <button v-for="d in vdata.mine.slice(1)" :key="d.id" class="m-row tap" @click="openOne('我的登入', d)">
        <div class="main"><b>{{ fmt(d.createdAt) }}</b><small>{{ d.userIp }}・{{ deviceText(d) }}</small></div>
        <span :class="['m-pill', d.latitude != null ? 'ok' : 'mute']">{{ d.latitude != null ? 'GPS' : GEO[d.geoState] || '無 GPS' }}</span>
      </button>
    </div>

    <div class="m-section-title"><h3>團長與隊長</h3><span class="m-muted">{{ rows.length }} 位</span></div>
    <div class="m-search"><SearchOutlined /><input v-model="vdata.q" type="search" placeholder="搜尋名稱、編號或 IP" /></div>
    <div class="m-chips">
      <button v-for="f in FILTERS" :key="f.key" :class="{ on: vdata.filter === f.key }" @click="vdata.filter = f.key">{{ f.label }}</button>
    </div>
    <div class="m-list">
      <button v-for="a in rows" :key="a.agentNo" class="m-row tap" @click="openAgent(a)">
        <div class="main">
          <b>{{ a.agentName }}<span class="m-role">{{ a.agentLevel === 1 ? '團長' : '隊長' }}</span><span v-if="a.state !== 1" class="m-pill mute" style="margin-left: 6px">停用</span></b>
          <small v-if="a.lastLogin">{{ fmt(a.lastLogin.createdAt) }}・{{ a.lastLogin.userIp }}・{{ deviceText(a.lastLogin) }}</small>
          <small v-else>沒有登入紀錄</small>
          <small v-if="a.agentLevel !== 1 && parentName(a)">上級團長 {{ parentName(a) }}</small>
        </div>
        <span v-if="a.lastLogin" :class="['m-pill', a.lastLogin.latitude != null ? 'ok' : 'mute']">{{ a.lastLogin.latitude != null ? 'GPS' : GEO[a.lastLogin.geoState] || '無 GPS' }}</span>
      </button>
      <div v-if="!rows.length" class="m-empty">{{ vdata.loaded ? '沒有符合的代理' : '載入中…' }}</div>
    </div>

    <MSheet v-model:open="vdata.sheet.open" :title="vdata.sheet.title">
      <div v-if="vdata.sheet.loading" class="m-empty">載入中…</div>
      <div v-else-if="!vdata.sheet.list.length" class="m-empty">沒有登入紀錄</div>
      <div v-for="d in vdata.sheet.list" :key="d.id" class="m-card" style="margin-bottom: 10px">
        <DeviceKv :d="d" />
      </div>
    </MSheet>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, reactive } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { req } from '@/api/manage'
import MSheet from '@/views/mobile/MSheet.vue'
import '@/views/mobile/mobile.less'

const GEO = { GRANTED: '已授權', DENIED: '拒絕定位', UNAVAILABLE: '無法定位', TIMEOUT: '定位逾時', UNSUPPORTED: '不支援定位' }
const FILTERS = [
  { key: 'all', label: '全部' },
  { key: 'leader', label: '團長' },
  { key: 'captain', label: '隊長' },
  { key: 'gps', label: '有 GPS' },
  { key: 'never', label: '沒登入過' },
]
const vdata: any = reactive({ mine: [], agents: [], loaded: false, q: '', filter: 'all', sheet: { open: false, title: '', list: [], loading: false } })

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')
const deviceText = (d) => [d.deviceModel, d.deviceOs].filter(Boolean).join(' ') || '未知裝置'
const parentName = (a) => (vdata.agents.find((x) => x.agentNo === a.parentAgentNo) || {}).agentName
const mapUrl = (d) => `https://www.google.com/maps?q=${d.latitude},${d.longitude}`

const rows = computed(() => {
  const q = vdata.q.trim()
  return vdata.agents.filter((a) => {
    const d = a.lastLogin
    if (vdata.filter === 'leader' && a.agentLevel !== 1) return false
    if (vdata.filter === 'captain' && a.agentLevel === 1) return false
    if (vdata.filter === 'gps' && !(d && d.latitude != null)) return false
    if (vdata.filter === 'never' && d) return false
    return !q || (a.agentName || '').includes(q) || a.agentNo.includes(q) || (d && (d.userIp || '').includes(q))
  })
})

// 一筆登入紀錄的完整欄位
const DeviceKv = defineComponent({
  props: { d: { type: Object, required: true } },
  setup(props: any) {
    const kv = (k, v) => [h('dt', k), h('dd', v)]
    return () => {
      const d = props.d
      const gps = d.latitude != null
        ? h('a', { href: mapUrl(d), target: '_blank', rel: 'noopener' }, `${d.latitude}, ${d.longitude}${d.accuracy != null ? `（±${d.accuracy} 公尺）` : ''}`)
        : GEO[d.geoState] || '—'
      return h('dl', { class: 'm-kv' }, [
        ...kv('時間', fmt(d.createdAt)),
        ...kv('帳號', d.userName || '—'),
        ...kv('IP', h('span', { class: 'mono' }, d.userIp || '—')),
        ...kv('裝置識別碼', h('span', { class: 'mono' }, d.deviceId || '—')),
        ...kv('型號', d.deviceModel || '—'),
        ...kv('系統', d.deviceOs || '—'),
        ...kv('GPS', gps),
        ...kv('瀏覽器', h('small', { class: 'ld-ua' }, d.userAgent || '—')),
      ])
    }
  },
})

function openOne(title, d) {
  vdata.sheet = { open: true, title, list: [d], loading: false }
}
function openAgent(a) {
  vdata.sheet = { open: true, title: `${a.agentName}・最近 20 次登入`, list: [], loading: true }
  req.list('/api/loginDevices/agent', { agentNo: a.agentNo }).then((r) => {
    vdata.sheet.list = r || []
    vdata.sheet.loading = false
  })
}

req.list('/api/loginDevices/overview', {}).then((r) => {
  vdata.mine = r.mine || []
  vdata.agents = r.agents || []
  vdata.loaded = true
})
</script>

<style scoped lang="less">
.ld-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
:deep(.ld-ua) {
  color: var(--jp-text-3);
  font-size: 12px;
}
</style>
