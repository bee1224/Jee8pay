<template>
  <!-- 訂單：代收（GET /agentPortal/orders）與代付（GET /agentPortal/transferOrders）。
       一般進入只顯示今天；從主選單「查詢訂單」進入（?mode=query）時預設空白，選定日期按「查詢」才載入 -->
  <div class="m-seg" role="tablist">
    <button :class="{ on: kind === 'collect' }" @click="setKind('collect')">代收訂單</button>
    <button :class="{ on: kind === 'payout' }" @click="setKind('payout')">代付訂單</button>
  </div>
  <template v-if="queryMode">
    <div class="m-chips">
      <button v-for="r in RANGES" :key="r.key" :class="{ on: vdata.range === r.key }" @click="setRange(r.key)">{{ r.label }}</button>
    </div>
    <a-range-picker v-if="vdata.range === 'custom'" v-model:value="vdata.custom" value-format="YYYY-MM-DD" size="large" :input-read-only="true" />
  </template>
  <div class="m-search">
    <SearchOutlined />
    <input v-model="vdata.q" type="search" :placeholder="kind === 'collect' ? '支付／商戶／渠道訂單號' : '代付／商戶訂單號'" @keyup.enter="reload" @search="reload" />
  </div>
  <div class="m-chips">
    <button v-for="f in filters" :key="String(f.value)" :class="{ on: vdata.state === f.value }" @click="setState(f.value)">{{ f.label }}</button>
  </div>
  <button v-if="queryMode" class="m-btn" :disabled="!rangeReady || vdata.loading" @click="submitQuery">查詢</button>
  <p v-if="!queryMode || vdata.searched" class="m-muted">{{ periodLabel }}・{{ vdata.total }} 筆{{ kind === 'collect' ? `・本頁成功 ${ntd(paidSum)}` : '' }}</p>

  <div class="m-list">
    <template v-if="kind === 'collect'">
      <button v-for="o in vdata.records" :key="o.payOrderId" class="m-row tap" @click="vdata.cur = o">
        <div class="main"><b>{{ o.mchName || o.mchNo }}</b><small class="mono">{{ o.payOrderId }}</small><small>{{ o.wayCode }}・{{ hhmm(o.createdAt) }}</small></div>
        <div class="end"><b class="num">{{ ntd(o.amount) }}</b><span :class="['m-pill', (PAY_STATES[o.state] || [])[1]]">{{ (PAY_STATES[o.state] || [o.state])[0] }}</span></div>
      </button>
    </template>
    <template v-else>
      <button v-for="o in vdata.records" :key="o.transferId" class="m-row tap" @click="vdata.cur = o">
        <div class="main"><b>{{ o.mchName || o.mchNo }} → {{ o.accountName || '—' }}</b><small class="mono">{{ o.transferId }}</small><small>{{ o.bankName || '' }} {{ maskAcc(o.accountNo) }}・{{ hhmm(o.createdAt) }}</small></div>
        <div class="end"><b class="num">−{{ ntd(o.amount) }}</b><span :class="['m-pill', (TRANSFER_STATES[o.state] || [])[1]]">{{ (TRANSFER_STATES[o.state] || [o.state])[0] }}</span></div>
      </button>
    </template>
    <div v-if="queryMode && !vdata.searched" class="m-empty">選擇日期後按「查詢」</div>
    <div v-else-if="!vdata.records.length" class="m-empty">
      {{ vdata.loading ? '載入中…' : kind === 'collect' ? `${periodLabel}沒有代收訂單` : `${periodLabel}沒有代付訂單（目前上游渠道未提供代付）` }}
    </div>
  </div>
  <button v-if="vdata.records.length < vdata.total" class="m-btn ghost" :disabled="vdata.loading" @click="more">載入更多</button>

  <MSheet :open="!!vdata.cur" :title="kind === 'collect' ? '代收明細' : '代付明細'" @update:open="(v) => !v && (vdata.cur = null)">
    <template v-if="vdata.cur && kind === 'collect'">
      <div style="text-align: center">
        <div class="m-big num">{{ ntd(vdata.cur.amount) }}</div>
        <span :class="['m-pill', (PAY_STATES[vdata.cur.state] || [])[1]]">{{ (PAY_STATES[vdata.cur.state] || [vdata.cur.state])[0] }}</span>
      </div>
      <dl class="m-kv">
        <dt>商戶</dt><dd>{{ vdata.cur.mchName || vdata.cur.mchNo }}</dd>
        <dt>支付訂單號</dt><dd class="mono">{{ vdata.cur.payOrderId }}</dd>
        <dt>商戶訂單號</dt><dd class="mono">{{ vdata.cur.mchOrderNo }}</dd>
        <dt>渠道訂單號</dt><dd class="mono">{{ vdata.cur.channelOrderNo || '—' }}</dd>
        <dt>支付方式</dt><dd>{{ vdata.cur.wayCode }}</dd>
        <dt>上游</dt><dd>{{ vdata.cur.ifCode || '—' }}</dd>
        <dt>商戶手續費</dt><dd class="num">{{ ntd(vdata.cur.mchFeeAmount, 2) }}</dd>
        <dt>建立時間</dt><dd class="num">{{ vdata.cur.createdAt }}</dd>
        <dt>成功時間</dt><dd class="num">{{ vdata.cur.successTime || '—' }}</dd>
        <dt>通知商戶</dt><dd>{{ vdata.cur.notifyState === 1 ? '已通知' : '未通知' }}</dd>
      </dl>
    </template>
    <template v-if="vdata.cur && kind === 'payout'">
      <div style="text-align: center">
        <div class="m-big num">{{ ntd(vdata.cur.amount) }}</div>
        <span :class="['m-pill', (TRANSFER_STATES[vdata.cur.state] || [])[1]]">{{ (TRANSFER_STATES[vdata.cur.state] || [vdata.cur.state])[0] }}</span>
      </div>
      <dl class="m-kv">
        <dt>付款商戶</dt><dd>{{ vdata.cur.mchName || vdata.cur.mchNo }}</dd>
        <dt>收款人</dt><dd>{{ vdata.cur.accountName || '—' }}</dd>
        <dt>收款帳戶</dt><dd>{{ vdata.cur.bankName || '' }} {{ maskAcc(vdata.cur.accountNo) }}</dd>
        <dt>代付單號</dt><dd class="mono">{{ vdata.cur.transferId }}</dd>
        <dt>商戶單號</dt><dd class="mono">{{ vdata.cur.mchOrderNo }}</dd>
        <dt>上游</dt><dd>{{ vdata.cur.ifCode || '—' }}</dd>
        <dt>建立時間</dt><dd class="num">{{ vdata.cur.createdAt }}</dd>
        <template v-if="vdata.cur.errMsg"><dt>失敗原因</dt><dd style="color: var(--jp-bad)">{{ vdata.cur.errMsg }}</dd></template>
      </dl>
    </template>
  </MSheet>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { SearchOutlined } from '@ant-design/icons-vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { ntd, hhmm, todayStr, PAY_STATES, TRANSFER_STATES } from '../mobileKit'
import MSheet from '../MSheet.vue'

const props = defineProps({
  // 上帝在團長詳情裡看某位團長的訂單：帶團長代理號，走平台 API 並依團長轄區篩選
  agentNo: { type: String, default: '' },
})
const route = useRoute()
// 平台的「代收查詢／代付查詢」也是查詢模式（選日期後才載入）
const queryMode = computed(() => route.query.mode === 'query' || route.name === 'ENT_HISTORY_PAY' || route.name === 'ENT_HISTORY_PAYOUT')
// 代理看自己轄區（/agentPortal/*）；平台（上帝）的「訂單管理」看全站（/api/payOrder、/api/transferOrders）
const isPlatform = ['ENT_PAY_ORDER', 'ENT_HISTORY_PAY', 'ENT_HISTORY_PAYOUT'].includes(route.name as string) || !!props.agentNo
const URLS = isPlatform
  ? { collect: '/api/payOrder', payout: '/api/transferOrders' }
  : { collect: API_URL_AGENT_PORTAL + '/orders', payout: API_URL_AGENT_PORTAL + '/transferOrders' }
const RANGES = [
  { key: 'today', label: '今天' },
  { key: 'yesterday', label: '昨天' },
  { key: '7d', label: '近 7 天' },
  { key: 'month', label: '本月' },
  { key: 'custom', label: '自訂' },
]
const kind = ref<'collect' | 'payout'>(route.name === 'ENT_HISTORY_PAYOUT' ? 'payout' : 'collect')
const vdata: any = reactive({ q: '', state: undefined, range: queryMode.value ? '' : 'today', custom: [], searched: false, records: [], total: 0, page: 1, loading: false, cur: null })
const rangeReady = computed(() => (vdata.range === 'custom' ? vdata.custom && vdata.custom.length === 2 : !!vdata.range))

const pad = (n) => String(n).padStart(2, '0')
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
function period(): [string, string] {
  const t = todayStr()
  const d = new Date()
  switch (vdata.range) {
    case 'yesterday':
      d.setDate(d.getDate() - 1)
      return [fmt(d), fmt(d)]
    case '7d':
      d.setDate(d.getDate() - 6)
      return [fmt(d), t]
    case 'month':
      d.setDate(1)
      return [fmt(d), t]
    case 'custom':
      return vdata.custom && vdata.custom.length === 2 ? vdata.custom : [t, t]
    default:
      return [t, t]
  }
}
const periodLabel = computed(() => {
  if (vdata.range === 'today') return '今天'
  const [a, b] = period()
  return a === b ? a : `${a}～${b}`
})
// 查詢模式：選日期不自動載入，按「查詢」才送出
function setRange(k) {
  vdata.range = k
}
function submitQuery() {
  vdata.searched = true
  reload()
}
const filters = computed(() =>
  kind.value === 'collect'
    ? [{ value: undefined, label: '全部' }, { value: 2, label: '支付成功' }, { value: 1, label: '支付中' }, { value: 6, label: '已關閉' }, { value: 3, label: '支付失敗' }]
    : [{ value: undefined, label: '全部' }, { value: 2, label: '代付成功' }, { value: 1, label: '處理中' }, { value: 3, label: '代付失敗' }, { value: 4, label: '已關閉' }]
)
const paidSum = computed(() => vdata.records.filter((o) => o.state === 2).reduce((a, o) => a + Number(o.amount || 0), 0))
function maskAcc(no) {
  const s = String(no || '')
  return s.length > 4 ? '****' + s.slice(-4) : s
}

function load(append = false) {
  vdata.loading = true
  const q = vdata.q.trim() || undefined
  const [start, end] = period()
  const params: any = { pageNumber: vdata.page, pageSize: 30, state: vdata.state, createdStart: `${start} 00:00:00`, createdEnd: `${end} 23:59:59`, agentNo: props.agentNo || undefined }
  if (kind.value === 'collect') params.unionOrderId = q
  else if (q) params[q.startsWith('T') ? 'transferId' : 'mchOrderNo'] = q
  req
    .list(URLS[kind.value], params)
    .then((res) => {
      vdata.records = append ? vdata.records.concat(res.records || []) : res.records || []
      vdata.total = res.total || 0
    })
    .finally(() => (vdata.loading = false))
}
function reload() {
  if (queryMode.value && !vdata.searched) return
  vdata.page = 1
  load()
}
function more() {
  vdata.page += 1
  load(true)
}
function setKind(k) {
  kind.value = k
  vdata.q = ''
  vdata.state = undefined
  vdata.records = []
  vdata.total = 0
  reload()
}
function setState(s) {
  vdata.state = s
  reload()
}
reload()
</script>
