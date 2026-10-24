<template>
  <!-- 操作紀錄（GET /agentPortal/opLogs）：我的代理帳號今天做過的操作 -->
  <p class="m-muted">{{ $t('今天・{0} 筆', [vdata.total]) }}</p>
  <div class="m-list">
    <div v-for="l in vdata.records" :key="l.sysLogId" class="m-row">
      <div class="main"><b>{{ l.methodRemark }}</b><small>{{ l.userName }}・<span class="num">{{ hhmm(l.createdAt) }}</span>・<span class="mono">{{ l.userIp }}</span></small></div>
    </div>
    <div v-if="!vdata.records.length" class="m-empty">{{ vdata.loaded ? $t('今天我的代理還沒有操作紀錄') : $t('載入中…') }}</div>
  </div>
  <button v-if="vdata.records.length < vdata.total" class="m-btn ghost" @click="more">{{ $t('載入更多') }}</button>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { hhmm, todayDateRange } from '../mobileKit'

const vdata: any = reactive({ records: [], total: 0, page: 1, loaded: false })
function load(append = false) {
  req.list(API_URL_AGENT_PORTAL + '/opLogs', { pageNumber: vdata.page, pageSize: 30, ...todayDateRange() }).then((res) => {
    vdata.records = append ? vdata.records.concat(res.records || []) : res.records || []
    vdata.total = res.total || 0
    vdata.loaded = true
  })
}
function more() {
  vdata.page += 1
  load(true)
}
load()
</script>
