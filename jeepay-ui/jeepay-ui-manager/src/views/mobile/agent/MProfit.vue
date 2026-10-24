<template>
  <!-- 分潤（/agentPortal/profits、/profits/summary）：依期間彙總與逐筆 -->
  <div class="m-chips">
    <button v-for="r in RANGES" :key="r.key" :class="{ on: vdata.range === r.key }" @click="setRange(r.key)">{{ r.label }}</button>
  </div>
  <div class="m-stats">
    <div class="m-stat"><small>{{ $t('分潤') }}</small><strong class="num" style="color: var(--jp-ok)">{{ ntd(vdata.summary.profit, 2) }}</strong></div>
    <div class="m-stat"><small>{{ $t('成功訂單') }}</small><strong class="num">{{ $t('{0} 筆', [vdata.summary.orderCount || 0]) }}</strong><small>{{ $t('交易 {0}', [ntd(vdata.summary.amount)]) }}</small></div>
  </div>
  <div class="m-list">
    <div v-for="p in vdata.records" :key="p.payOrderId" class="m-row">
      <div class="main"><b>{{ p.mchName }}</b><small class="mono">{{ p.payOrderId }}</small><small>{{ p.wayCode }}・{{ p.successTime }}</small></div>
      <div class="end"><b class="num" style="color: var(--jp-ok)">+{{ ntd(p.profit, 2) }}</b><small class="m-muted num" style="margin: 0">{{ $t('交易 {0}', [ntd(p.amount)]) }}</small></div>
    </div>
    <div v-if="!vdata.records.length" class="m-empty">{{ $t('這段期間沒有分潤') }}</div>
  </div>
  <button v-if="vdata.records.length < vdata.total" class="m-btn ghost" @click="more">{{ $t('載入更多') }}</button>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { ntd, todayStr } from '../mobileKit'
import { t } from '@/i18n'

const RANGES = [
  { key: 'today', label: t('今天') },
  { key: '7d', label: t('近 7 天') },
  { key: 'month', label: t('本月') },
]
const vdata: any = reactive({ range: 'today', summary: {}, records: [], total: 0, page: 1 })

function params() {
  const t = todayStr()
  if (vdata.range === 'today') return { startDate: t, endDate: t }
  const d = new Date()
  if (vdata.range === '7d') d.setDate(d.getDate() - 6)
  else d.setDate(1)
  const p = (n) => String(n).padStart(2, '0')
  return { startDate: `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`, endDate: t }
}
function load(append = false) {
  req.list(API_URL_AGENT_PORTAL + '/profits', { ...params(), pageNumber: vdata.page, pageSize: 30 }).then((res) => {
    vdata.records = append ? vdata.records.concat(res.records || []) : res.records || []
    vdata.total = res.total || 0
  })
}
function setRange(k) {
  vdata.range = k
  vdata.page = 1
  req.list(API_URL_AGENT_PORTAL + '/profits/summary', params()).then((r) => (vdata.summary = r || {}))
  load()
}
function more() {
  vdata.page += 1
  load(true)
}
setRange('today')
</script>
