<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>錢包帳戶</template>
      <template #extra>
        <a-range-picker v-if="$access('ENT_EXPORT_CENTER')" v-model:value="vdata.reportRange" value-format="YYYY-MM-DD" style="margin-right: 8px" />
        <a-button v-if="$access('ENT_EXPORT_CENTER')" style="margin-right: 8px" @click="exportDaily">匯出每日結算彙總</a-button>
        <a-button v-if="$access('ENT_WALLET_SETTLE_RUN')" :loading="vdata.settling" @click="settleNow">立即結算</a-button>
      </template>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
        message="訂單支付成功後，依下單當下的手續費快照，於結算日（T+N，見系統配置）自動入帳到商戶、代理、上游渠道與平台帳戶；全部帳戶的變動合計等於訂單金額。"
      />
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col v-for="t in ['PLATFORM', 'MCH', 'AGENT', 'CHANNEL']" :key="t" :xs="12" :md="6">
          <a-statistic :title="OWNER_TYPE_NAMES[t] + '可用（元）'" :value="yuan(vdata.summary[t]?.balance)" />
          <div style="color: #888; font-size: 12px">凍結 {{ yuan(vdata.summary[t]?.frozen) }}</div>
        </a-col>
      </a-row>

      <a-card v-if="vdata.pending.length" size="small" style="margin-bottom: 16px">
        <template #title>待覆核的人工調帳 <a-badge :count="vdata.pending.length" /></template>
        <a-table :columns="adjustColumns" :data-source="vdata.pending" :pagination="false" size="small" row-key="reqId">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'owner'">{{ OWNER_TYPE_NAMES[record.ownerType] }} {{ record.ownerId }}</template>
            <template v-if="column.key === 'amount'">{{ yuan(record.amount) }}</template>
            <template v-if="column.key === 'op'">
              <template v-if="$access('ENT_WALLET_ADJUST_REVIEW')">
                <a-button type="link" @click="reviewAdjust(record, 'approve')">核准</a-button>
                <a-button type="link" danger @click="reviewAdjust(record, 'reject')">駁回</a-button>
              </template>
              <span v-else>待覆核</span>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-form layout="inline" style="margin-bottom: 12px">
        <a-form-item>
          <a-select v-model:value="vdata.query.ownerType" style="width: 140px" placeholder="帳戶類型" allow-clear @change="loadAccounts">
            <a-select-option v-for="(n, k) in OWNER_TYPE_NAMES" :key="k" :value="k">{{ n }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item><a-input v-model:value="vdata.query.ownerId" placeholder="商戶號／代理號" allow-clear @pressEnter="loadAccounts" /></a-form-item>
        <a-form-item><a-button type="primary" @click="loadAccounts">搜尋</a-button></a-form-item>
      </a-form>
      <a-table :columns="columns" :data-source="vdata.accounts" size="small" row-key="accountId">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'owner'">
            <a-tag>{{ OWNER_TYPE_NAMES[record.ownerType] }}</a-tag>{{ record.ownerId }}
          </template>
          <template v-if="column.key === 'balance'"><b>{{ yuan(record.balance) }}</b></template>
          <template v-if="column.key === 'frozen'">{{ yuan(record.frozen) }}</template>
          <template v-if="column.key === 'totalIn'">{{ yuan(record.totalIn) }}</template>
          <template v-if="column.key === 'payout'">
            <span v-if="record.payoutAccountNo">{{ record.payoutBankCode }}-{{ record.payoutAccountNo }} {{ record.payoutAccountName }}</span>
            <span v-else style="color: #bbb">未設定</span>
          </template>
          <template v-if="column.key === 'op'">
            <a-button type="link" @click="showLedger(record)">流水</a-button>
            <a-button v-if="$access('ENT_WALLET_ADJUST')" type="link" @click="openAdjust(record)">調帳</a-button>
          </template>
        </template>
        <template #emptyText>尚無帳戶（第一筆訂單結算後自動建立）</template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="vdata.ledger.open" :title="'流水：' + vdata.ledger.title" width="70%">
      <a-table :columns="ledgerColumns" :data-source="vdata.ledger.records" size="small" row-key="ledgerId" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'bizType'">{{ BIZ_TYPE_NAMES[record.bizType] || record.bizType }}</template>
          <template v-if="column.key === 'amount'">{{ yuan(record.amount) }}</template>
          <template v-if="column.key === 'frozenChange'">{{ yuan(record.frozenChange) }}</template>
          <template v-if="column.key === 'balanceAfter'">{{ yuan(record.balanceAfter) }}</template>
        </template>
      </a-table>
    </a-drawer>

    <a-modal v-model:open="vdata.adjust.open" :title="'人工調帳：' + vdata.adjust.title" ok-text="送出覆核" @ok="submitAdjust">
      <a-alert type="warning" show-icon style="margin-bottom: 12px" message="調帳需由另一位具覆核權限的管理者核准後才會入帳。" />
      <a-form layout="vertical">
        <a-form-item label="調整金額（元，正數為加、負數為減）"><a-input-number v-model:value="vdata.adjust.amount" :precision="2" style="width: 100%" /></a-form-item>
        <a-form-item label="原因"><a-textarea v-model:value="vdata.adjust.reason" :maxlength="128" /></a-form-item>
      </a-form>
    </a-modal>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_WALLET, req } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'
import { BIZ_TYPE_NAMES, OWNER_TYPE_NAMES, yuan } from '@/components/WalletPanel/walletText'
import { submitExport } from '@/utils/exportJob'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const columns = [
  { key: 'owner', title: '帳戶' },
  { key: 'balance', title: '可用（元）' },
  { key: 'frozen', title: '凍結（元）' },
  { key: 'totalIn', title: '累計入帳（元）' },
  { key: 'payout', title: '收款帳戶' },
  { key: 'op', title: '操作', width: '140px' },
]
const adjustColumns = [
  { title: '申請時間', dataIndex: 'createdAt' },
  { key: 'owner', title: '帳戶' },
  { key: 'amount', title: '金額（元）' },
  { title: '原因', dataIndex: 'reason' },
  { title: '申請人', dataIndex: 'requesterName' },
  { key: 'op', title: '操作', width: '140px' },
]
const ledgerColumns = [
  { title: '時間', dataIndex: 'createdAt' },
  { key: 'bizType', title: '類型' },
  { title: '單號', dataIndex: 'bizId' },
  { key: 'amount', title: '可用變動' },
  { key: 'frozenChange', title: '凍結變動' },
  { key: 'balanceAfter', title: '餘額' },
  { title: '說明', dataIndex: 'remark' },
  { title: '操作者', dataIndex: 'operatorName' },
]

const vdata: any = reactive({
  summary: {},
  accounts: [],
  pending: [],
  settling: false,
  reportRange: [],
  query: { ownerType: undefined, ownerId: '' },
  ledger: { open: false, title: '', records: [] },
  adjust: { open: false, title: '', accountId: null, amount: undefined, reason: '' },
})

function loadAccounts() {
  req.list(API_URL_WALLET + '/accounts', { ...vdata.query, pageSize: -1 }).then((res) => (vdata.accounts = res.records || []))
}
function loadAll() {
  req.list(API_URL_WALLET + '/summary', {}).then((res) => (vdata.summary = res || {}))
  req.list(API_URL_WALLET + '/adjusts', { state: 0, pageSize: -1 }).then((res) => (vdata.pending = res.records || []))
  loadAccounts()
}
loadAll()

function exportDaily() {
  const [startDate, endDate] = vdata.reportRange || []
  submitExport('SETTLE_DAILY', { startDate, endDate }).then(() => $infoBox.message.success('已建立匯出（未選日期為近 30 天），完成後請到「下載中心」下載'))
}
function settleNow() {
  vdata.settling = true
  req
    .add(API_URL_WALLET + '/settle/run', {})
    .then((r) => {
      $infoBox.message.success(`結算 ${r.settled} 筆、沖回 ${r.reversed} 筆、不結算 ${r.skipped} 筆`)
      loadAll()
    })
    .finally(() => (vdata.settling = false))
}

function showLedger(record) {
  vdata.ledger = { open: true, title: `${OWNER_TYPE_NAMES[record.ownerType]} ${record.ownerId}`, records: [] }
  req.list(API_URL_WALLET + '/ledger', { accountId: record.accountId, pageSize: 100 }).then((res) => (vdata.ledger.records = res.records || []))
}

function openAdjust(record) {
  vdata.adjust = { open: true, title: `${OWNER_TYPE_NAMES[record.ownerType]} ${record.ownerId}`, accountId: record.accountId, amount: undefined, reason: '' }
}
function submitAdjust() {
  const a = vdata.adjust
  if (!a.amount || !a.reason) {
    $infoBox.message.warning('請填寫金額與原因')
    return
  }
  req.add(API_URL_WALLET + '/adjusts', { accountId: a.accountId, amount: String(a.amount), reason: a.reason }).then(() => {
    $infoBox.message.info('已送出，需另一位管理者覆核')
    vdata.adjust.open = false
    loadAll()
  })
}
function reviewAdjust(record, action) {
  const verb = action === 'approve' ? '核准' : '駁回'
  $infoBox.confirmPrimary(`確認${verb}這筆調帳？`, action === 'approve' ? '核准後立即入帳並留下流水' : '', () => {
    req.add(`${API_URL_WALLET}/adjusts/${record.reqId}/${action}`, {}).then(() => {
      $infoBox.message.success('已' + verb)
      loadAll()
    })
  })
}
</script>
