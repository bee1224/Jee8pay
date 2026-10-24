<template>
  <!-- 上帝主頁：全站今天的成交、近 7 天走勢、商戶與代理數；資料沿用電腦版主頁的 /api/mainChart -->
  <div class="m-stats">
    <div class="m-stat"><small>{{ $t('今日成交') }}</small><strong class="num">{{ money(w.todayAmount) }}</strong><small>{{ $t('昨日 {0}', [money(w.yesterdayAmount)]) }}</small></div>
    <div class="m-stat"><small>{{ $t('今日成功筆數') }}</small><strong class="num">{{ num(w.todayPayCount) }}</strong></div>
    <div class="m-stat"><small>{{ $t('近 7 天成交') }}</small><strong class="num">{{ money(w.payWeek) }}</strong></div>
    <div class="m-stat"><small>{{ $t('商戶總數') }}</small><strong class="num">{{ num(n.totalMch) }}</strong><small>{{ $t('累計成交 {0}', [money(n.totalAmount)]) }}</small></div>
  </div>
  <div class="m-section-title"><h3>{{ $t('近 7 天成交') }}</h3><span class="m-muted">{{ $t('新台幣') }}</span></div>
  <div class="m-card">
    <svg :viewBox="`0 -8 ${W} ${H + 30}`" width="100%" role="img" :aria-label="$t('近 7 天每日成交金額長條圖')">
      <defs><linearGradient id="jpBarGod" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7dd3fc" /><stop offset="0.55" stop-color="#2563eb" /><stop offset="1" stop-color="#1e40af" /></linearGradient></defs>
      <line v-for="t in ticks" :key="t" x1="0" :x2="W - 34" :y1="y(t)" :y2="y(t)" stroke="var(--jp-border)" />
      <text v-for="t in ticks" :key="'t' + t" :x="W - 30" :y="y(t) + 3" font-size="10" fill="var(--jp-text-3)">{{ short(t) }}</text>
      <g v-for="(v, i) in days" :key="i">
        <rect :x="i * slot + 6" :y="y(v)" :width="slot - 12" :height="H - y(v)" rx="6" fill="url(#jpBarGod)" :opacity="i === days.length - 1 ? 1 : 0.5" />
        <text :x="i * slot + slot / 2" :y="H + 16" text-anchor="middle" font-size="10" fill="var(--jp-text-3)">{{ labels[i] }}</text>
      </g>
    </svg>
  </div>
  <div class="m-section-title"><h3>{{ $t('常用') }}</h3></div>
  <div class="m-list">
    <button v-for="l in LINKS.filter((x) => $access(x.ent))" :key="l.path" class="m-row tap" @click="router.push(l.path)">
      <div class="main"><b>{{ l.title }}</b><small>{{ l.hint }}</small></div>
      <RightOutlined style="color: var(--jp-text-3)" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { RightOutlined } from '@ant-design/icons-vue'
import { getNumCount, getPayAmountWeek } from '@/api/manage'
import { t, intlLocale } from '@/i18n'

const router = useRouter()
const W = 340
const H = 120
const LINKS = [
  { ent: 'ENT_SYS_MONITOR', path: '/monitor', title: t('流量監控'), hint: t('每分鐘建單、容量上限、各上游成功率') },
  { ent: 'ENT_AGENT_INFO', path: '/agents', title: t('團長列表'), hint: t('家族樹、團長狀態') },
  { ent: 'ENT_WALLET_WITHDRAW', path: '/wallet/withdraw', title: t('提現審核'), hint: t('待撥款的提現單') },
  { ent: 'ENT_MCH_NOTIFY', path: '/notify', title: t('商戶通知'), hint: t('通知失敗可重新發送') },
]
const vdata: any = reactive({ w: {}, n: {} })
const w = computed(() => vdata.w || {})
const n = computed(() => vdata.n || {})
const days = computed(() => (w.value.dataArray || []).map((x) => Number(x) || 0))
const labels = computed(() => days.value.map((_, i) => {
  const d = new Date()
  d.setDate(d.getDate() - (days.value.length - 1 - i))
  return `${d.getMonth() + 1}/${d.getDate()}`
}))
const max = computed(() => {
  const m = Math.max(1, ...days.value)
  const step = Math.pow(10, Math.floor(Math.log10(m)))
  return Math.ceil(m / step) * step
})
const ticks = computed(() => [0, max.value / 2, max.value])
const slot = computed(() => (W - 34) / Math.max(1, days.value.length))
const y = (v) => H - (v / max.value) * H
const num = (v) => Number(v || 0).toLocaleString('zh-TW')
const money = (v) => 'NT$' + Number(v || 0).toLocaleString('zh-TW', { maximumFractionDigits: 0 })
// 大數字縮寫依介面語言：中文「1.2萬」、英文「12K」
const short = (v) => (v >= 10000 ? new Intl.NumberFormat(intlLocale(), { notation: 'compact', maximumFractionDigits: 1 }).format(v) : String(v))

getPayAmountWeek().then((r) => (vdata.w = r || {}))
getNumCount().then((r) => (vdata.n = r || {}))
</script>
