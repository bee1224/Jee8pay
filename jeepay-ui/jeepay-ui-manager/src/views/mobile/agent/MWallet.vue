<template>
  <!-- 錢包與提現（/agentPortal/wallet*）：餘額、申請提現、收款帳戶、今天的收支明細 -->
  <div class="m-card" style="display: grid; gap: 14px">
    <div><div class="m-muted" style="margin: 0">可用餘額</div><div class="m-big num">{{ ntd(a.balance, 2) }}</div></div>
    <dl class="m-kv">
      <dt>提現處理中</dt><dd class="num">{{ ntd(a.frozen, 2) }}</dd>
      <dt>累計入帳</dt><dd class="num">{{ ntd(a.totalIn, 2) }}</dd>
      <dt>收款帳戶</dt>
      <dd>
        <template v-if="a.payoutAccountNo">{{ a.payoutBankName }} ****{{ String(a.payoutAccountNo).slice(-4) }}</template>
        <span v-else style="color: var(--jp-warn)">尚未設定</span>
      </dd>
    </dl>
    <div class="m-actions">
      <button class="m-btn ghost" @click="openPayout">{{ a.payoutAccountNo ? '變更收款帳戶' : '設定收款帳戶' }}</button>
      <button class="m-btn" :disabled="!a.payoutAccountNo || !canApply" @click="vdata.apply.open = true">申請提現</button>
    </div>
  </div>

  <template v-if="pendingWithdraws.length">
    <div class="m-section-title"><h3>處理中的提現</h3></div>
    <div class="m-list">
      <div v-for="w in pendingWithdraws" :key="w.withdrawId" class="m-row">
        <div class="main"><b class="num">{{ ntd(w.amount, 2) }}</b><small class="num">{{ w.createdAt }}</small></div>
        <button class="m-btn sm danger" @click="cancel(w)">取消</button>
      </div>
    </div>
  </template>

  <div class="m-section-title"><h3>今天的收支明細</h3><span class="m-muted">{{ vdata.ledgerTotal }} 筆</span></div>
  <div class="m-list">
    <div v-for="l in vdata.ledger" :key="l.ledgerId" class="m-row">
      <div class="main"><b>{{ BIZ_TYPE_NAMES[l.bizType] || l.bizType }}</b><small>{{ hhmm(l.createdAt) }}{{ l.remark ? '・' + l.remark : '' }}</small></div>
      <div class="end">
        <b class="num" :style="{ color: l.amount > 0 ? 'var(--jp-ok)' : 'var(--jp-text)' }">{{ l.amount > 0 ? '+' : '' }}{{ ntd(l.amount, 2) }}</b>
        <small class="m-muted num" style="margin: 0">餘額 {{ ntd(l.balanceAfter, 2) }}</small>
      </div>
    </div>
    <div v-if="!vdata.ledger.length" class="m-empty">今天還沒有收支</div>
  </div>
  <button v-if="vdata.ledger.length < vdata.ledgerTotal" class="m-btn ghost" @click="moreLedger">載入更多</button>

  <MSheet v-model:open="vdata.apply.open" title="申請提現">
    <a-form layout="vertical">
      <a-form-item :label="`提現金額（可提 ${ntd(a.balance, 2)}）`">
        <a-input-number v-model:value="vdata.apply.amount" :min="1" :precision="2" addon-after="元" size="large" style="width: 100%" inputmode="decimal" />
      </a-form-item>
    </a-form>
    <dl class="m-kv"><dt>收款帳戶</dt><dd>{{ a.payoutBankName }} {{ a.payoutAccountNo }}</dd><dt>戶名</dt><dd>{{ a.payoutAccountName }}</dd></dl>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.apply.open = false">取消</button>
      <button class="m-btn" :disabled="vdata.apply.loading" @click="apply">送出申請</button>
    </div>
  </MSheet>

  <MSheet v-model:open="vdata.payout.open" title="收款帳戶">
    <a-form layout="vertical">
      <a-form-item label="銀行名稱"><a-input v-model:value="vdata.payout.form.payoutBankName" placeholder="例如 臺灣銀行" size="large" /></a-form-item>
      <a-form-item label="銀行代碼（3 碼）"><a-input v-model:value="vdata.payout.form.payoutBankCode" :maxlength="3" inputmode="numeric" size="large" /></a-form-item>
      <a-form-item label="分行（選填）"><a-input v-model:value="vdata.payout.form.payoutBranch" size="large" /></a-form-item>
      <a-form-item label="帳號"><a-input v-model:value="vdata.payout.form.payoutAccountNo" inputmode="numeric" size="large" /></a-form-item>
      <a-form-item label="戶名"><a-input v-model:value="vdata.payout.form.payoutAccountName" size="large" /></a-form-item>
    </a-form>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.payout.open = false">取消</button>
      <button class="m-btn" @click="savePayout">儲存</button>
    </div>
  </MSheet>
</template>

<script setup lang="ts">
import { computed, reactive, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { BIZ_TYPE_NAMES, newReqNo } from '@/components/WalletPanel/walletText'
import { ntd, hhmm, todayDateRange } from '../mobileKit'
import MSheet from '../MSheet.vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const BASE = API_URL_AGENT_PORTAL + '/wallet'
const canApply = $access('ENT_AGENT_PORTAL_HOME')
const vdata: any = reactive({
  account: {},
  withdraws: [],
  ledger: [],
  ledgerTotal: 0,
  ledgerPage: 1,
  apply: { open: false, amount: undefined, loading: false, reqNo: newReqNo() },
  payout: { open: false, form: {} },
})
const a = computed(() => vdata.account)
const pendingWithdraws = computed(() => vdata.withdraws.filter((w) => w.state === 0))

function loadLedger(append = false) {
  req.list(BASE + '/ledger', { pageNumber: vdata.ledgerPage, pageSize: 30, ...todayDateRange() }).then((res) => {
    vdata.ledger = append ? vdata.ledger.concat(res.records || []) : res.records || []
    vdata.ledgerTotal = res.total || 0
  })
}
function moreLedger() {
  vdata.ledgerPage += 1
  loadLedger(true)
}
function load() {
  req.list(BASE, {}).then((r) => (vdata.account = r || {}))
  req.list(BASE + '/withdraws', { pageSize: 20 }).then((r) => (vdata.withdraws = r.records || []))
  vdata.ledgerPage = 1
  loadLedger()
}
function openPayout() {
  const x = vdata.account
  vdata.payout = {
    open: true,
    form: { payoutBankName: x.payoutBankName, payoutBankCode: x.payoutBankCode, payoutBranch: x.payoutBranch, payoutAccountNo: x.payoutAccountNo, payoutAccountName: x.payoutAccountName },
  }
}
function savePayout() {
  req.updateById(BASE, 'payoutAccount', vdata.payout.form).then(() => {
    $infoBox.message.success('收款帳戶已更新')
    vdata.payout.open = false
    load()
  })
}
function apply() {
  const ap = vdata.apply
  if (!ap.amount) {
    $infoBox.message.warning('請輸入提現金額')
    return
  }
  ap.loading = true
  // reqNo 在成功前保持不變：網路重送不會重複凍結
  req
    .add(BASE + '/withdraws', { amount: String(ap.amount), reqNo: ap.reqNo })
    .then((res) => {
      if (res.state === 2) $infoBox.message.error('申請未通過：' + (res.reviewRemark || '風控拒絕'))
      else $infoBox.message.success('已送出，金額已凍結，等待平台撥款')
      vdata.apply = { open: false, amount: undefined, loading: false, reqNo: newReqNo() }
      load()
    })
    .finally(() => (ap.loading = false))
}
function cancel(w) {
  $infoBox.confirmDanger('確認取消這筆提現？', '取消後金額會解凍回可用餘額', () => {
    req.add(`${BASE}/withdraws/${w.withdrawId}/cancel`, {}).then(() => {
      $infoBox.message.success('已取消')
      load()
    })
  })
}
load()
</script>
