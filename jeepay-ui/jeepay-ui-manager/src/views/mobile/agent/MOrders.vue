<template>
  <!-- 訂單：代收（GET /agentPortal/orders 或平台 /api/payOrder）與代付（transferOrders）。
       一般進入只顯示今天、每 30 秒自動更新；從主選單「查詢訂單」進入（?mode=query）時預設空白，選定日期按「查詢」才載入。
       員工最常做的事放最前面：今天收了多少、哪些還在等繳費、哪些商戶沒收到通知、貼上單號就查到 -->
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

  <!-- 摘要：點一下就篩選 -->
  <div v-if="kind === 'collect' && showData && vdata.sum" class="mo-sum">
    <button class="mo-hero" :class="{ on: vdata.filter === 'paid' }" @click="setFilter('paid')">
      <small>{{ periodLabel }}已收款</small>
      <strong class="num">{{ ntd(sumAmount(2)) }}</strong>
      <small>{{ sumCount(2) }} 筆・成功率 {{ successRate }}</small>
    </button>
    <button class="mo-mini" :class="{ on: vdata.filter === 'waiting' }" @click="setFilter('waiting')">
      <small>等待繳費</small><strong class="num">{{ sumCount(1) + sumCount(0) }}</strong>
    </button>
    <button class="mo-mini" :class="{ on: vdata.filter === 'expired' }" @click="setFilter('expired')">
      <small>逾期／失敗</small><strong class="num">{{ sumCount(6) + sumCount(3) }}</strong>
    </button>
    <button class="mo-mini" :class="{ on: vdata.filter === 'unnotified', alert: vdata.sum.unnotified > 0 }" @click="setFilter('unnotified')">
      <small>未通知商戶</small><strong class="num">{{ vdata.sum.unnotified }}</strong>
    </button>
  </div>

  <div class="m-search">
    <SearchOutlined />
    <input
      v-model="vdata.q"
      type="search"
      inputmode="search"
      :placeholder="kind === 'collect' ? '貼上任何單號：支付／商戶／渠道' : '代付單號或商戶單號'"
      @input="onSearchInput"
      @search="reload"
    />
    <button v-if="vdata.q" class="mo-clear" aria-label="清除" @click="clearSearch"><CloseCircleFilled /></button>
  </div>
  <div class="m-chips">
    <button v-for="f in filters" :key="f.key" :class="{ on: vdata.filter === f.key }" @click="setFilter(f.key)">
      {{ f.label }}<span v-if="f.count != null" class="mo-count">{{ f.count }}</span>
    </button>
  </div>
  <button v-if="queryMode" class="m-btn" :disabled="!rangeReady || vdata.loading" @click="submitQuery">查詢</button>
  <p v-if="showData" class="m-muted mo-status">
    <span>{{ vdata.q.trim() ? `搜尋「${vdata.q.trim()}」` : periodLabel }}・{{ vdata.total }} 筆</span>
    <span v-if="autoRefresh && vdata.updatedAt" class="mo-live"><i />{{ vdata.updatedAt }} 已更新</span>
  </p>

  <!-- 代收：依小時（今天）或日期（多日）分組 -->
  <template v-if="kind === 'collect' && showData">
    <section v-for="g in groups" :key="g.key" class="mo-group">
      <div class="mo-group-head"><b>{{ g.label }}</b><span>{{ g.items.length }} 筆<template v-if="g.paid">・已收 {{ ntd(g.paid) }}</template></span></div>
      <div class="m-list">
        <button v-for="o in g.items" :key="o.payOrderId" class="m-row tap" :class="{ 'mo-new': vdata.fresh[o.payOrderId] }" @click="openDetail(o)">
          <div class="main">
            <b>{{ o.mchName || o.mchNo }}</b>
            <small class="mono">{{ o.payOrderId }}</small>
            <small>
              {{ ago(o.createdAt) }}・{{ o.wayName || o.wayCode }}
              <template v-if="hint(o)">・<span :class="hint(o).cls">{{ hint(o).text }}</span></template>
            </small>
          </div>
          <div class="end">
            <b class="num">{{ ntd(o.amount) }}</b>
            <span :class="['m-pill', st(o.state)[1]]">{{ st(o.state)[0] }}</span>
          </div>
        </button>
      </div>
    </section>
  </template>

  <!-- 代付 -->
  <div v-if="kind === 'payout' && showData && vdata.records.length" class="m-list">
    <button v-for="o in vdata.records" :key="o.transferId" class="m-row tap" @click="openDetail(o)">
      <div class="main"><b>{{ o.mchName || o.mchNo }} → {{ o.accountName || '—' }}</b><small class="mono">{{ o.transferId }}</small><small>{{ o.bankName || '' }} {{ maskAcc(o.accountNo) }}・{{ ago(o.createdAt) }}</small></div>
      <div class="end"><b class="num">−{{ ntd(o.amount) }}</b><span :class="['m-pill', (TRANSFER_STATES[o.state] || [])[1]]">{{ (TRANSFER_STATES[o.state] || [o.state])[0] }}</span></div>
    </button>
  </div>

  <div v-if="queryMode && !vdata.searched" class="m-empty">選擇日期後按「查詢」</div>
  <div v-else-if="!vdata.records.length" class="m-empty">{{ emptyText }}</div>
  <div v-if="vdata.records.length && vdata.records.length < vdata.total" ref="sentinel" class="mo-more">{{ vdata.loading ? '載入中…' : '往下滑載入更多' }}</div>
  <p v-else-if="vdata.records.length > 10" class="m-muted" style="text-align: center">已經到底了</p>

  <!-- 明細 -->
  <MSheet :open="!!vdata.cur" :title="kind === 'collect' ? '代收明細' : '代付明細'" @update:open="(v) => !v && (vdata.cur = null)">
    <template v-if="vdata.cur && kind === 'collect'">
      <div class="mo-head">
        <div class="m-big num">{{ ntd(vdata.cur.amount) }}</div>
        <span :class="['m-pill', st(vdata.cur.state)[1]]">{{ st(vdata.cur.state)[0] }}</span>
        <p class="m-muted">{{ vdata.cur.mchName || vdata.cur.mchNo }}・{{ vdata.cur.wayName || vdata.cur.wayCode }}</p>
      </div>

      <ol class="mo-steps">
        <li class="done"><b>建立訂單</b><span class="num">{{ vdata.cur.createdAt }}</span></li>
        <li :class="payStep.cls"><b>{{ payStep.title }}</b><span>{{ payStep.text }}</span></li>
        <li :class="notifyStep.cls"><b>通知商戶</b><span>{{ notifyStep.text }}</span></li>
      </ol>

      <dl class="m-kv">
        <dt>支付單號</dt><dd><button class="mo-copy mono" @click="copy(vdata.cur.payOrderId)">{{ vdata.cur.payOrderId }} <CopyOutlined /></button></dd>
        <dt>商戶單號</dt><dd><button class="mo-copy mono" @click="copy(vdata.cur.mchOrderNo)">{{ vdata.cur.mchOrderNo }} <CopyOutlined /></button></dd>
        <dt>渠道單號</dt>
        <dd>
          <button v-if="vdata.cur.channelOrderNo" class="mo-copy mono" @click="copy(vdata.cur.channelOrderNo)">{{ vdata.cur.channelOrderNo }} <CopyOutlined /></button>
          <span v-else>—</span>
        </dd>
        <dt>商戶號</dt><dd class="mono">{{ vdata.cur.mchNo }}</dd>
        <dt>上游</dt><dd>{{ (vdata.cur.ifCode || '—').toUpperCase() }}</dd>
        <dt>商戶手續費</dt><dd class="num">{{ ntd(vdata.cur.mchFeeAmount, 2) }}</dd>
        <template v-if="vdata.cur.expiredTime"><dt>繳費期限</dt><dd class="num">{{ vdata.cur.expiredTime }}</dd></template>
      </dl>
      <div class="m-actions">
        <button v-if="canResend" class="m-btn ghost" :disabled="vdata.resending" @click="resend(vdata.cur)">{{ vdata.resending ? '送出中…' : '重送商戶通知' }}</button>
        <button class="m-btn" @click="copy(shareText(vdata.cur), '訂單資訊已複製，可直接貼給商戶')">複製訂單資訊</button>
      </div>
    </template>
    <template v-if="vdata.cur && kind === 'payout'">
      <div class="mo-head">
        <div class="m-big num">{{ ntd(vdata.cur.amount) }}</div>
        <span :class="['m-pill', (TRANSFER_STATES[vdata.cur.state] || [])[1]]">{{ (TRANSFER_STATES[vdata.cur.state] || [vdata.cur.state])[0] }}</span>
      </div>
      <dl class="m-kv">
        <dt>付款商戶</dt><dd>{{ vdata.cur.mchName || vdata.cur.mchNo }}</dd>
        <dt>收款人</dt><dd>{{ vdata.cur.accountName || '—' }}</dd>
        <dt>收款帳戶</dt><dd>{{ vdata.cur.bankName || '' }} {{ maskAcc(vdata.cur.accountNo) }}</dd>
        <dt>代付單號</dt><dd><button class="mo-copy mono" @click="copy(vdata.cur.transferId)">{{ vdata.cur.transferId }} <CopyOutlined /></button></dd>
        <dt>商戶單號</dt><dd><button class="mo-copy mono" @click="copy(vdata.cur.mchOrderNo)">{{ vdata.cur.mchOrderNo }} <CopyOutlined /></button></dd>
        <dt>上游</dt><dd>{{ vdata.cur.ifCode || '—' }}</dd>
        <dt>建立時間</dt><dd class="num">{{ vdata.cur.createdAt }}</dd>
        <template v-if="vdata.cur.errMsg"><dt>失敗原因</dt><dd style="color: var(--jp-bad)">{{ vdata.cur.errMsg }}</dd></template>
      </dl>
    </template>
  </MSheet>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, h, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { SearchOutlined, CloseCircleFilled, CopyOutlined } from '@ant-design/icons-vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { ntd, todayStr, TRANSFER_STATES } from '../mobileKit'
import MSheet from '../MSheet.vue'

const props = defineProps({
  // 上帝在團長詳情裡看某位團長的訂單：帶團長代理號，走平台 API 並依團長轄區篩選
  agentNo: { type: String, default: '' },
})
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const route = useRoute()
// 平台的「代收查詢／代付查詢」也是查詢模式（選日期後才載入）
const queryMode = computed(() => route.query.mode === 'query' || route.name === 'ENT_HISTORY_PAY' || route.name === 'ENT_HISTORY_PAYOUT')
// 代理看自己轄區（/agentPortal/*）；平台（上帝）的「訂單管理」看全站（/api/payOrder、/api/transferOrders）
const isPlatform = ['ENT_PAY_ORDER', 'ENT_HISTORY_PAY', 'ENT_HISTORY_PAYOUT'].includes(route.name as string) || !!props.agentNo
const URLS = isPlatform
  ? { collect: '/api/payOrder', payout: '/api/transferOrders', summary: '/api/payOrder/summary' }
  : { collect: API_URL_AGENT_PORTAL + '/orders', payout: API_URL_AGENT_PORTAL + '/transferOrders', summary: API_URL_AGENT_PORTAL + '/orders/summary' }
const canSeeNotify = isPlatform && $access('ENT_NOTIFY_LIST')
const RANGES = [
  { key: 'today', label: '今天' },
  { key: 'yesterday', label: '昨天' },
  { key: '7d', label: '近 7 天' },
  { key: 'month', label: '本月' },
  { key: 'custom', label: '自訂' },
]
// 員工看得懂的說法（ibon：支付中 = 已取號、等待顧客到超商繳費）
const COLLECT_STATES: Record<number, [string, string]> = {
  0: ['等待繳費', 'warn'],
  1: ['等待繳費', 'warn'],
  2: ['已收款', 'ok'],
  3: ['失敗', 'bad'],
  4: ['已撤銷', 'mute'],
  5: ['已退款', 'mute'],
  6: ['已逾期', 'mute'],
}
const st = (s) => COLLECT_STATES[s] || [String(s), 'mute']
// 篩選鍵 → 查詢條件（未通知 = 已收款但商戶還沒回覆成功，後端判斷）
const FILTER_PARAMS = {
  all: {},
  paid: { state: 2 },
  waiting: { state: 1 },
  expired: { state: 6 },
  failed: { state: 3 },
  unnotified: { notifyOk: 0 },
}
const PAYOUT_FILTERS = { all: {}, ok: { state: 2 }, ing: { state: 1 }, fail: { state: 3 }, closed: { state: 4 } }

const kind = ref<'collect' | 'payout'>(route.name === 'ENT_HISTORY_PAYOUT' ? 'payout' : 'collect')
const vdata: any = reactive({
  q: '', filter: 'all', range: queryMode.value ? '' : 'today', custom: [], searched: false,
  records: [], total: 0, page: 1, loading: false, cur: null, sum: null, notify: null, resending: false,
  updatedAt: '', fresh: {}, now: Date.now(),
})
const rangeReady = computed(() => (vdata.range === 'custom' ? vdata.custom && vdata.custom.length === 2 : !!vdata.range))
const showData = computed(() => !queryMode.value || vdata.searched)
// 只有「今天」且沒有在搜尋時自動更新（不需要重新整理按鈕）
const autoRefresh = computed(() => !queryMode.value && vdata.range === 'today' && !vdata.q.trim())

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
  if (vdata.range === 'yesterday') return '昨天'
  const [a, b] = period()
  return a === b ? a : `${a.slice(5)}～${b.slice(5)} `
})
const isMultiDay = computed(() => {
  const [a, b] = period()
  return a !== b
})

// 時間：接受 "2026-10-19 10:28:00" 或毫秒數
function toDate(v): Date | null {
  if (!v) return null
  if (typeof v === 'number') return new Date(v)
  const d = new Date(String(v).replace(' ', 'T'))
  return isNaN(d.getTime()) ? null : d
}
function ago(v) {
  const d = toDate(v)
  if (!d) return ''
  const mins = Math.floor((vdata.now - d.getTime()) / 60000)
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (isMultiDay.value || mins >= 60 * 12) return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${hm}`
  if (mins < 1) return '剛剛'
  if (mins < 60) return `${mins} 分鐘前`
  return hm
}
function span(ms) {
  const m = Math.max(0, Math.round(ms / 60000))
  if (m < 60) return `${m} 分鐘`
  const h = Math.floor(m / 60)
  return m % 60 ? `${h} 小時 ${m % 60} 分` : `${h} 小時`
}
// 列表上的提醒：等待繳費顯示剩餘時間；已收款的顯示通知結果（商戶回覆成功才算已通知，其餘要醒目）
const NOTIFY_HINTS = { 1: { text: '通知中，等商戶回覆', cls: 'mo-warn' }, 2: { text: '已通知商戶', cls: 'mo-ok' }, 3: { text: '通知商戶失敗', cls: 'mo-warn' } }
function hint(o) {
  if (o.state === 2) return NOTIFY_HINTS[o.notifyResult] || { text: '尚未通知商戶', cls: 'mo-warn' }
  if (o.state === 1 || o.state === 0) {
    const exp = toDate(o.expiredTime)
    if (exp) {
      const left = exp.getTime() - vdata.now
      return left > 0 ? { text: `剩 ${span(left)}`, cls: left < 30 * 60000 ? 'mo-warn' : '' } : { text: '即將關閉', cls: 'mo-warn' }
    }
  }
  return null
}

// 分組：今天依小時，多日依日期
const groups = computed(() => {
  const map = new Map<string, any>()
  for (const o of vdata.records) {
    const d = toDate(o.createdAt)
    const key = d ? (isMultiDay.value ? fmt(d) : `${fmt(d)} ${pad(d.getHours())}`) : '?'
    let label = '其他'
    if (d) {
      if (isMultiDay.value) label = `${d.getMonth() + 1} 月 ${d.getDate()} 日（週${'日一二三四五六'[d.getDay()]}）`
      else label = `${pad(d.getHours())}:00 – ${pad(d.getHours())}:59`
    }
    if (!map.has(key)) map.set(key, { key, label, items: [], paid: 0 })
    const g = map.get(key)
    g.items.push(o)
    if (o.state === 2) g.paid += Number(o.amount || 0)
  }
  return [...map.values()]
})

const sumCount = (s) => Number((vdata.sum && vdata.sum.counts[s]) || 0)
const sumAmount = (s) => Number((vdata.sum && vdata.sum.amounts[s]) || 0)
const successRate = computed(() => {
  const done = sumCount(2) + sumCount(3) + sumCount(6)
  return done ? `${Math.round((sumCount(2) / done) * 100)}%` : '—'
})
const filters = computed(() =>
  kind.value === 'collect'
    ? [
        { key: 'all', label: '全部', count: vdata.sum ? vdata.sum.total : null },
        { key: 'paid', label: '已收款', count: vdata.sum ? sumCount(2) : null },
        { key: 'waiting', label: '等待繳費', count: vdata.sum ? sumCount(1) + sumCount(0) : null },
        { key: 'unnotified', label: '未通知商戶', count: vdata.sum ? vdata.sum.unnotified : null },
        { key: 'expired', label: '已逾期', count: vdata.sum ? sumCount(6) : null },
        { key: 'failed', label: '失敗', count: vdata.sum ? sumCount(3) : null },
      ]
    : [
        { key: 'all', label: '全部' },
        { key: 'ok', label: '代付成功' },
        { key: 'ing', label: '處理中' },
        { key: 'fail', label: '代付失敗' },
        { key: 'closed', label: '已關閉' },
      ]
)
const emptyText = computed(() => {
  if (vdata.loading) return '載入中…'
  if (vdata.q.trim()) return `找不到「${vdata.q.trim()}」，請確認單號是否完整`
  if (kind.value === 'payout') return `${periodLabel.value}沒有代付訂單（目前上游渠道未提供代付）`
  if (vdata.filter === 'unnotified') return '很好，已收款的訂單都通知商戶了'
  return `${periodLabel.value}沒有符合的代收訂單`
})
function maskAcc(no) {
  const s = String(no || '')
  return s.length > 4 ? '****' + s.slice(-4) : s
}

function baseParams() {
  const [start, end] = period()
  const p: any = { createdStart: `${start} 00:00:00`, createdEnd: `${end} 23:59:59`, agentNo: props.agentNo || undefined }
  // 搜尋單號時不限日期，貼上任何一天的單號都找得到
  if (vdata.q.trim()) {
    delete p.createdStart
    delete p.createdEnd
  }
  return p
}
function load(append = false, silent = false) {
  if (!silent) vdata.loading = true
  const q = vdata.q.trim() || undefined
  const params: any = { ...baseParams(), pageNumber: vdata.page, pageSize: 30 }
  if (kind.value === 'collect') Object.assign(params, FILTER_PARAMS[vdata.filter] || {}, { unionOrderId: q })
  else {
    Object.assign(params, PAYOUT_FILTERS[vdata.filter] || {})
    if (q) params[q.startsWith('T') ? 'transferId' : 'mchOrderNo'] = q
  }
  return req
    .list(URLS[kind.value], params)
    .then((res) => {
      const rows = res.records || []
      if (silent && !append) {
        // 自動更新：新進的訂單短暫標亮
        const known = new Set(vdata.records.map((o) => o.payOrderId || o.transferId))
        const fresh = {}
        rows.forEach((o) => !known.has(o.payOrderId || o.transferId) && (fresh[o.payOrderId || o.transferId] = true))
        vdata.fresh = fresh
        setTimeout(() => (vdata.fresh = {}), 4000)
        // 已往下載入的頁數保留，只替換第一頁
        vdata.records = rows.concat(vdata.records.slice(rows.length))
      } else {
        vdata.records = append ? vdata.records.concat(rows) : rows
      }
      vdata.total = res.total || 0
      const n = new Date()
      vdata.updatedAt = `${pad(n.getHours())}:${pad(n.getMinutes())}:${pad(n.getSeconds())}`
    })
    .finally(() => (vdata.loading = false))
}
function loadSummary() {
  if (kind.value !== 'collect' || vdata.q.trim()) return
  const p = baseParams()
  req.list(URLS.summary, p).then((r) => (vdata.sum = r))
}
function reload() {
  if (queryMode.value && !vdata.searched) return
  vdata.page = 1
  load()
  loadSummary()
}
function more() {
  if (vdata.loading || vdata.records.length >= vdata.total) return
  vdata.page += 1
  load(true)
}
function setRange(k) {
  vdata.range = k
}
function submitQuery() {
  vdata.searched = true
  reload()
}
function setKind(k) {
  kind.value = k
  vdata.q = ''
  vdata.filter = 'all'
  vdata.records = []
  vdata.total = 0
  vdata.sum = null
  reload()
}
function setFilter(k) {
  vdata.filter = vdata.filter === k && k !== 'all' ? 'all' : k
  vdata.page = 1
  load()
}
// 貼上或輸入單號後自動搜尋（停頓 0.4 秒）
let searchTimer: any = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    const q = vdata.q.trim()
    if (!q || q.length >= 6) reload()
  }, 400)
}
function clearSearch() {
  vdata.q = ''
  reload()
}

// 明細：通知紀錄（平台才有權限看）
function openDetail(o) {
  vdata.cur = o
  vdata.notify = null
  if (kind.value === 'collect' && canSeeNotify && o.state === 2) {
    req.list('/api/mchNotify', { orderId: o.payOrderId, pageSize: 1 }).then((r) => {
      const n = (r.records || [])[0] || false
      if (vdata.cur === o) vdata.notify = n
      // 商戶已回覆成功（例如剛重送）：列表與「未通知商戶」數字跟著更新
      if (n && n.state !== o.notifyResult) {
        o.notifyResult = n.state
        loadSummary()
      }
    })
  }
}
const payStep = computed(() => {
  const o = vdata.cur
  if (!o) return {}
  if (o.state === 2) {
    const a = toDate(o.createdAt)
    const b = toDate(o.successTime)
    return { cls: 'done', title: '顧客已繳費', text: `${o.successTime || ''}${a && b ? `（${span(b.getTime() - a.getTime())}後繳費）` : ''}` }
  }
  if (o.state === 1 || o.state === 0) {
    const exp = toDate(o.expiredTime)
    const left = exp ? exp.getTime() - vdata.now : 0
    return { cls: 'now', title: '等待顧客繳費', text: exp ? (left > 0 ? `還有 ${span(left)}，${o.expiredTime} 截止` : '已過期限，等待系統關閉') : '尚未繳費' }
  }
  if (o.state === 6) return { cls: 'stop', title: '未繳費，已逾期', text: o.expiredTime ? `${o.expiredTime} 截止` : '' }
  if (o.state === 3) return { cls: 'fail', title: '繳費失敗', text: o.errMsg || o.errCode || '' }
  return { cls: 'stop', title: st(o.state)[0], text: '' }
})
const NOTIFY_STATES = { 1: '通知中', 2: '已通知商戶（商戶回覆成功）', 3: '通知失敗' }
const notifyStep = computed(() => {
  const o = vdata.cur
  if (!o || o.state !== 2) return { cls: 'todo', text: '繳費後自動通知' }
  const n = vdata.notify
  if (n) {
    const cls = n.state === 2 ? 'done' : n.state === 3 ? 'fail' : 'now'
    const reply = n.state === 3 && n.resResult ? `・商戶回覆「${String(n.resResult).slice(0, 40)}」` : ''
    return { cls, text: `${NOTIFY_STATES[n.state] || n.state}・共送 ${n.notifyCount || 0} 次${n.lastNotifyTime ? `・最後 ${n.lastNotifyTime}` : ''}${reply}` }
  }
  // 團長沒有通知紀錄權限，改用列表帶回的通知結果
  if (n === null && o.notifyResult) return { cls: o.notifyResult === 2 ? 'done' : o.notifyResult === 3 ? 'fail' : 'now', text: NOTIFY_STATES[o.notifyResult] }
  return { cls: 'fail', text: '尚未通知商戶' }
})
const canResend = computed(() => kind.value === 'collect' && isPlatform && vdata.cur && vdata.cur.state === 2 && $access('ENT_MCH_NOTIFY_RESEND'))
function resend(o) {
  // 二次確認：列出商戶、金額與單號，避免在明細裡誤觸
  const content = h('div', [
    h('div', `商戶：${o.mchName || o.mchNo}`),
    h('div', `金額：${ntd(o.amount)}`),
    h('div', { class: 'mono' }, `支付單號：${o.payOrderId}`),
    h('p', { style: 'margin: 10px 0 0' }, '會把這筆已收款訂單的結果再送一次給商戶，不會改變訂單狀態。'),
  ])
  $infoBox.confirmPrimary('確定要重送商戶通知？', content, () => {
    vdata.resending = true
    req
      .add(`/api/mchNotify/send/${o.payOrderId}`, {})
      .then(() => {
        $infoBox.message.success('已送出，稍後可在這裡看到商戶回覆')
        setTimeout(() => vdata.cur === o && openDetail(o), 3000)
      })
      .finally(() => (vdata.resending = false))
  }, () => {}, { okText: '確認重送', cancelText: '先不要', autoFocusButton: 'cancel' })
}
function shareText(o) {
  return [
    `商戶：${o.mchName || o.mchNo}`,
    `金額：${ntd(o.amount)}`,
    `狀態：${st(o.state)[0]}`,
    `支付單號：${o.payOrderId}`,
    `商戶單號：${o.mchOrderNo}`,
    `建立時間：${o.createdAt}`,
    o.successTime ? `繳費時間：${o.successTime}` : '',
  ]
    .filter(Boolean)
    .join('\n')
}
function copy(text, msg = '已複製') {
  const done = () => $infoBox.message.success(msg)
  if (navigator.clipboard && window.isSecureContext) return navigator.clipboard.writeText(text).then(done)
  const ta = document.createElement('textarea')
  ta.value = text
  document.body.appendChild(ta)
  ta.select()
  document.execCommand('copy')
  ta.remove()
  done()
}

// 往下滑自動載入更多
const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
watch(sentinel, (el) => {
  observer && observer.disconnect()
  if (el) {
    observer = new IntersectionObserver((entries) => entries.some((e) => e.isIntersecting) && more(), { rootMargin: '200px' })
    observer.observe(el)
  }
})

// 每 30 秒自動更新（頁面在前景、沒有打開明細時）；每 30 秒也更新「幾分鐘前」
let timer: any = null
onMounted(() => {
  timer = setInterval(() => {
    vdata.now = Date.now()
    if (autoRefresh.value && !vdata.cur && !document.hidden && !vdata.loading) {
      load(false, true)
      loadSummary()
    }
  }, 30000)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  clearTimeout(searchTimer)
  observer && observer.disconnect()
})

reload()
</script>

<style scoped lang="less">
.mo-sum {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  > button {
    text-align: left;
    display: grid;
    gap: 2px;
    padding: 12px 14px;
    border-radius: 16px;
    border: 1px solid var(--jp-card-edge);
    background: var(--jp-card-bg);
    box-shadow: var(--jp-card-gloss), var(--jp-shadow-1);
    -webkit-backdrop-filter: blur(14px);
    backdrop-filter: blur(14px);
    min-width: 0;
    transition: transform 0.15s ease;
    &:active {
      transform: scale(0.97);
    }
    small {
      color: var(--jp-text-3);
      font-size: 12px;
      font-weight: 500;
    }
    strong {
      font-size: 20px;
      font-weight: 700;
    }
    &.on {
      box-shadow: inset 0 0 0 2px var(--jp-primary), var(--jp-shadow-1);
    }
    &.alert {
      background: linear-gradient(180deg, var(--jp-warn-soft), var(--jp-card-bg));
      strong {
        color: var(--jp-warn);
      }
    }
  }
  .mo-hero {
    grid-column: 1 / -1;
    background: var(--jp-brand-grad);
    border-color: transparent;
    box-shadow: var(--jp-brand-glow), inset 0 1px 0 rgba(255, 255, 255, 0.3);
    color: #fff;
    small {
      color: rgba(255, 255, 255, 0.8);
    }
    strong {
      font-size: 28px;
      letter-spacing: -0.01em;
    }
    &.on {
      box-shadow: var(--jp-brand-glow), inset 0 0 0 2px rgba(255, 255, 255, 0.7);
    }
  }
}
.mo-clear {
  color: var(--jp-text-3);
  font-size: 16px;
  display: grid;
  place-items: center;
  padding: 4px !important;
}
.mo-count {
  margin-left: 6px;
  font-size: 11.5px;
  font-weight: 700;
  opacity: 0.75;
  font-variant-numeric: tabular-nums;
}
.mo-status {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.mo-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--jp-ok);
    box-shadow: 0 0 0 0 var(--jp-ok);
    animation: mo-pulse 2s infinite;
  }
}
@keyframes mo-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(19, 138, 82, 0.5);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(19, 138, 82, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(19, 138, 82, 0);
  }
}
.mo-group {
  display: grid;
  gap: 8px;
}
.mo-group-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 0 4px;
  b {
    font-size: 13.5px;
    font-weight: 650;
  }
  span {
    font-size: 12.5px;
    color: var(--jp-text-3);
    font-variant-numeric: tabular-nums;
  }
}
.mo-warn {
  color: var(--jp-warn);
  font-weight: 600;
}
.mo-ok {
  color: var(--jp-ok);
}
.m-row.mo-new {
  animation: mo-flash 4s ease-out;
}
@keyframes mo-flash {
  0%,
  40% {
    background: var(--jp-primary-soft);
  }
  100% {
    background: transparent;
  }
}
.mo-more {
  text-align: center;
  color: var(--jp-text-3);
  font-size: 13px;
  padding: 8px 0 16px;
}
.mo-head {
  text-align: center;
  display: grid;
  gap: 6px;
  justify-items: center;
  margin-bottom: 14px;
  p {
    margin: 0;
  }
}
.mo-steps {
  list-style: none;
  margin: 0 0 16px;
  padding: 0 0 0 4px;
  display: grid;
  gap: 0;
  li {
    position: relative;
    padding: 0 0 14px 26px;
    display: grid;
    gap: 2px;
    b {
      font-size: 14px;
      font-weight: 600;
    }
    span {
      font-size: 12.5px;
      color: var(--jp-text-3);
    }
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 3px;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: var(--jp-surface-2);
      border: 2px solid var(--jp-border);
    }
    &:not(:last-child)::after {
      content: '';
      position: absolute;
      left: 6px;
      top: 19px;
      bottom: 2px;
      width: 2px;
      background: var(--jp-border);
    }
    &.done::before {
      background: var(--jp-ok);
      border-color: var(--jp-ok);
    }
    &.done::after {
      background: var(--jp-ok);
    }
    &.now::before {
      background: var(--jp-warn);
      border-color: var(--jp-warn);
      box-shadow: 0 0 0 4px var(--jp-warn-soft);
    }
    &.fail::before {
      background: var(--jp-bad);
      border-color: var(--jp-bad);
    }
    &.fail b {
      color: var(--jp-bad);
    }
  }
}
.mo-copy {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--jp-primary) !important;
  text-align: right;
  overflow-wrap: anywhere;
  .anticon {
    font-size: 12px;
    opacity: 0.7;
  }
}
</style>
