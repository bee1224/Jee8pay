<template>
  <!-- 通道路由（/agentPortal/branch/wayRoutes）：別名代碼依金額／時段／權重分配到實際支付方式（ADR-0011） -->
  <p class="m-muted">新增或修改路由規則欄位較多，請使用電腦版；手機上可查看與停用。</p>
  <div class="m-list">
    <div v-for="r in vdata.list" :key="r.routeId" class="m-row">
      <div class="main">
        <b>{{ r.aliasWayCode }} → {{ r.targetWayCode }}</b>
        <small>商戶 <span class="mono">{{ r.mchNo }}</span>・權重 {{ r.weight }}</small>
        <small>{{ r.minAmount / 100 }}～{{ r.maxAmount ? r.maxAmount / 100 : '不限' }} 元・{{ r.timeStart ? `${r.timeStart}～${r.timeEnd}` : '全天' }}</small>
      </div>
      <div class="end">
        <a-switch :checked="r.state === 1" size="small" @change="(v) => toggle(r, v)" />
        <small class="m-muted" style="margin: 0">{{ r.state === 1 ? '使用中' : '停用' }}</small>
      </div>
    </div>
    <div v-if="!vdata.list.length" class="m-empty">{{ vdata.loaded ? '尚未設定路由規則' : '載入中…' }}</div>
  </div>
</template>

<script setup lang="ts">
import { reactive, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const BASE = API_URL_AGENT_PORTAL + '/branch/wayRoutes'
const vdata: any = reactive({ list: [], loaded: false })
function load() {
  req.list(BASE, {}).then((r) => {
    vdata.list = r || []
    vdata.loaded = true
  })
}
// 與電腦版相同：以完整規則 POST 回存（有 routeId 即為修改）
function toggle(r, on) {
  req.add(BASE, { ...r, state: on ? 1 : 0 }).then(() => {
    $infoBox.message.success(on ? '已啟用' : '已停用')
    load()
  })
}
load()
</script>
