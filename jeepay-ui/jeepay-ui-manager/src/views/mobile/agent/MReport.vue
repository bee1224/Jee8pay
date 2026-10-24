<template>
  <!-- 統計報表（GET /agentPortal/branch/report）：依日、代理、商戶彙總 -->
  <div class="m-chips">
    <button v-for="r in RANGES" :key="r.key" :class="{ on: vdata.range === r.key }" @click="setRange(r.key)">{{ r.label }}</button>
  </div>
  <div class="m-stats">
    <div class="m-stat"><small>{{ $t('成功金額') }}</small><strong class="num">{{ ntd(t.paidAmount) }}</strong></div>
    <div class="m-stat"><small>{{ $t('成功／訂單數') }}</small><strong class="num">{{ t.paid || 0 }}／{{ t.total || 0 }}</strong></div>
    <div class="m-stat"><small>{{ $t('商戶手續費') }}</small><strong class="num">{{ ntd(t.feeAmount, 2) }}</strong></div>
  </div>
  <div class="m-seg">
    <button v-for="d in DIMS" :key="d.key" :class="{ on: vdata.dim === d.key }" @click="vdata.dim = d.key">{{ d.label }}</button>
  </div>
  <div class="m-card m-table-wrap">
    <table class="num">
      <thead><tr><th>{{ dimLabel }}</th><th>{{ $t('訂單') }}</th><th>{{ $t('成功') }}</th><th>{{ $t('成功金額') }}</th><th>{{ $t('手續費') }}</th></tr></thead>
      <tbody>
        <tr v-for="r in rows" :key="r.id">
          <td>{{ r.name || r.id }}</td><td>{{ r.total }}</td><td>{{ r.paid }}</td><td>{{ ntd(r.paidAmount) }}</td><td>{{ ntd(r.feeAmount, 2) }}</td>
        </tr>
        <tr v-if="!rows.length"><td colspan="5" class="m-empty" style="text-align: center">{{ $t('這段期間沒有資料') }}</td></tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { ntd, todayStr } from '../mobileKit'
import { t } from '@/i18n'

const RANGES = [
  { key: 'today', label: t('今天') },
  { key: '7d', label: t('近 7 天') },
  { key: 'month', label: t('本月') },
]
const DIMS = [
  { key: 'byDay', label: t('依日期') },
  { key: 'byAgent', label: t('依代理') },
  { key: 'byMch', label: t('依商戶') },
]
const vdata: any = reactive({ range: 'today', dim: 'byDay', data: { total: {}, byDay: [], byAgent: [], byMch: [] } })
const t = computed(() => vdata.data.total || {})
const rows = computed(() => vdata.data[vdata.dim] || [])
const dimLabel = computed(() => ({ byDay: t('日期'), byAgent: t('代理'), byMch: t('商戶') })[vdata.dim])

function setRange(k) {
  vdata.range = k
  const today = todayStr()
  const d = new Date()
  if (k === '7d') d.setDate(d.getDate() - 6)
  else if (k === 'month') d.setDate(1)
  const p = (n) => String(n).padStart(2, '0')
  const start = k === 'today' ? today : `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
  req.list(API_URL_AGENT_PORTAL + '/branch/report', { startDate: start, endDate: today }).then((r) => (vdata.data = r || { total: {} }))
}
setRange('today')
</script>
