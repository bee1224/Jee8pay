<template>
  <!-- 渠道列表（GET /agentPortal/channels）：平台派發給這位代理的第三方支付渠道 -->
  <p class="m-muted">要新增渠道或更換金鑰，請把資料交給平台處理。</p>
  <div class="m-list">
    <div v-for="c in vdata.list" :key="c.accountId" class="m-row">
      <div class="main">
        <b>{{ c.accountName }}</b>
        <small>{{ c.ifName || c.ifCode }}・{{ (c.scopes || []).length ? '只限：' + c.scopes.map((s) => s.agentName || s.agentNo).join('、') : '全部可用' }}</small>
      </div>
      <div class="end">
        <span :class="['m-pill', c.state === 0 ? 'mute' : 'ok']">{{ c.state === 0 ? '停用' : '啟用' }}</span>
        <span :class="['m-pill', c.owned ? 'ok' : 'warn']">{{ c.owned ? '自己的' : '共用' }}</span>
      </div>
    </div>
    <div v-if="!vdata.list.length" class="m-empty">{{ vdata.loaded ? '平台尚未派發渠道給你' : '載入中…' }}</div>
  </div>
  <p v-if="$access('ENT_AGENT_PORTAL_CHANNEL_TEST')" class="m-muted">要測試渠道，請到「我的商戶」點選商戶後按「渠道測試」。</p>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'

const vdata: any = reactive({ list: [], loaded: false })
req.list(API_URL_AGENT_PORTAL + '/channels', {}).then((r) => {
  vdata.list = r || []
  vdata.loaded = true
})
</script>
