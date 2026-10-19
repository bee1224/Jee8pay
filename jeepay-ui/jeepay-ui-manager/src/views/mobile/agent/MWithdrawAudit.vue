<template>
  <!-- 提現審核（/agentPortal/branch/withdraws）：只列今天的申請；同意只是第一關註記，撥款由平台執行 -->
  <p class="m-muted">今天・待審核 {{ waiting.length }} 筆・已處理 {{ done.length }} 筆</p>
  <div v-for="w in waiting.concat(done)" :key="w.withdrawId" class="m-card" style="display: grid; gap: 12px">
    <div style="display: flex; justify-content: space-between; align-items: center; gap: 8px">
      <b>{{ OWNER_TYPE_NAMES[w.ownerType] }} <span class="mono">{{ w.ownerId }}</span></b>
      <span :class="['m-pill', STATE_CLASS[w.state]]">{{ (WITHDRAW_STATES[w.state] || {}).text }}</span>
    </div>
    <div class="m-big num" style="font-size: 24px">{{ ntd(w.amount, 2) }}</div>
    <dl class="m-kv">
      <dt>收款帳戶</dt><dd>{{ w.bankName }} {{ w.accountNo }}<br />{{ w.accountName }}</dd>
      <dt>申請時間</dt><dd class="num">{{ w.createdAt }}</dd>
      <dt>單號</dt><dd class="mono">{{ w.withdrawId }}</dd>
      <template v-if="w.riskFlags"><dt>風控提示</dt><dd><span v-for="f in w.riskFlags.split(',').filter(Boolean)" :key="f" class="m-pill warn" style="margin-left: 4px">{{ RISK_FLAG_NAMES[f] || f }}</span></dd></template>
      <template v-if="w.agentApproveBy"><dt>已同意</dt><dd>{{ w.agentApproveBy }}</dd></template>
      <template v-if="w.reviewRemark"><dt>說明</dt><dd>{{ w.reviewRemark }}</dd></template>
    </dl>
    <div v-if="w.state === 0 && vdata.me && vdata.me.agentLevel === 1" class="m-actions">
      <button class="m-btn danger" @click="openReject(w)">駁回</button>
      <button class="m-btn" :disabled="!!w.agentApproveBy" @click="approve(w)">{{ w.agentApproveBy ? '已同意' : '同意' }}</button>
    </div>
  </div>
  <div v-if="!vdata.records.length" class="m-empty">{{ vdata.loaded ? '今天沒有提現申請' : '載入中…' }}</div>
  <p class="m-muted">同意後由平台撥款，你這裡只做第一關確認。</p>

  <MSheet v-model:open="vdata.reject.open" title="駁回提現">
    <a-input v-model:value="vdata.reject.remark" :maxlength="100" size="large" placeholder="駁回原因（必填）" />
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.reject.open = false">取消</button>
      <button class="m-btn danger" :disabled="vdata.reject.saving" @click="submitReject">確定駁回</button>
    </div>
  </MSheet>
</template>

<script setup lang="ts">
import { computed, reactive, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { OWNER_TYPE_NAMES, WITHDRAW_STATES, RISK_FLAG_NAMES } from '@/components/WalletPanel/walletText'
import { agentMe, ntd, todayDateRange } from '../mobileKit'
import MSheet from '../MSheet.vue'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const BASE = API_URL_AGENT_PORTAL + '/branch/withdraws'
const STATE_CLASS = { 0: 'warn', 1: 'ok', 2: 'bad', 3: 'mute' }
const vdata: any = reactive({ me: null, records: [], loaded: false, reject: { open: false, saving: false, id: '', remark: '' } })
const waiting = computed(() => vdata.records.filter((w) => w.state === 0))
const done = computed(() => vdata.records.filter((w) => w.state !== 0))

function load() {
  req.list(BASE, { pageNumber: 1, pageSize: 100, ...todayDateRange() }).then((res) => {
    vdata.records = res.records || []
    vdata.loaded = true
  })
}
function approve(w) {
  req.add(`${BASE}/${w.withdrawId}/approve`, {}).then(() => {
    $infoBox.message.success('已同意，轉交平台撥款')
    load()
  })
}
function openReject(w) {
  vdata.reject = { open: true, saving: false, id: w.withdrawId, remark: '' }
}
function submitReject() {
  if (!vdata.reject.remark.trim()) {
    $infoBox.message.warning('請填寫駁回原因')
    return
  }
  vdata.reject.saving = true
  req
    .add(`${BASE}/${vdata.reject.id}/reject`, { remark: vdata.reject.remark })
    .then(() => {
      $infoBox.message.success('已駁回，金額退回對方錢包')
      vdata.reject.open = false
      load()
    })
    .finally(() => (vdata.reject.saving = false))
}
agentMe().then((me) => (vdata.me = me)).catch(() => {})
load()
</script>
