<template>
  <!-- 統計看板（GET /agentPortal/branch/dashboard、/wallet、/branch/loginLogs） -->
  <div class="m-stats">
    <div class="m-stat"><small>{{ $t('今日成交') }}</small><strong class="num">{{ ntd(s.todayAmount) }}</strong><small>{{ $t('昨日 {0}', [ntd(s.yesterdayAmount)]) }}</small></div>
    <div class="m-stat"><small>{{ $t('今日成功／失敗') }}</small><strong class="num">{{ s.todayPaid || 0 }}／{{ s.todayFailed || 0 }}</strong></div>
    <div class="m-stat"><small>{{ $t('今日商戶手續費') }}</small><strong class="num">{{ ntd(s.todayFee, 2) }}</strong></div>
    <div class="m-stat"><small>{{ $t('轄下商戶') }}</small><strong class="num">{{ $t('{0} 家', [s.mchCount || 0]) }}</strong></div>
    <div class="m-stat"><small>{{ $t('本月成交') }}</small><strong class="num">{{ ntd(s.monthAmount) }}</strong><small>{{ $t('上月 {0}', [ntd(s.lastMonthAmount)]) }}</small></div>
    <div class="m-stat"><small>{{ $t('可用餘額') }}</small><strong class="num">{{ ntd(w.balance, 2) }}</strong><small>{{ $t('凍結 {0}', [ntd(w.frozen, 2)]) }}</small></div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { ntd } from '../mobileKit'

const vdata: any = reactive({ stat: {}, wallet: {} })
const s = computed(() => vdata.stat)
const w = computed(() => vdata.wallet)
req.list(API_URL_AGENT_PORTAL + '/branch/dashboard', {}).then((r) => (vdata.stat = r || {}))
req.list(API_URL_AGENT_PORTAL + '/wallet', {}).then((r) => (vdata.wallet = r || {}))
</script>
