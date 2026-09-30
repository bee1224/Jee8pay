<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>我的錢包</template>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
        :message="`訂單支付成功後，扣除手續費的實收金額於 T+${vdata.account.settleDelayDays ?? 1} 結算入帳。申請提現後金額先凍結，由平台人工匯款至收款帳戶；單筆 ${vdata.account.withdrawMin ?? '-'}～${vdata.account.withdrawMax ?? '-'} 元，每筆手續費 ${vdata.account.withdrawFee ?? 0} 元。`"
      />
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :xs="24" :sm="8"><a-statistic title="可用餘額（新臺幣元）" :value="yuan(vdata.account.balance)" /></a-col>
        <a-col :xs="24" :sm="8"><a-statistic title="提現處理中（新臺幣元）" :value="yuan(vdata.account.frozen)" /></a-col>
        <a-col :xs="24" :sm="8"><a-statistic title="累計入帳（新臺幣元）" :value="yuan(vdata.account.totalIn)" /></a-col>
      </a-row>

      <a-card size="small" title="收款帳戶" style="margin-bottom: 16px">
        <template #extra>
          <a-button v-if="$access('ENT_MCH_WALLET_PAYOUT_EDIT')" type="link" @click="vdata.payout.open = true">
            {{ vdata.account.payoutAccountNo ? '變更' : '設定' }}
          </a-button>
        </template>
        <span v-if="vdata.account.payoutAccountNo">
          {{ vdata.account.payoutBankName }}（{{ vdata.account.payoutBankCode }}）{{ vdata.account.payoutBranch || '' }}
          · {{ vdata.account.payoutAccountNo }} · {{ vdata.account.payoutAccountName }}
        </span>
        <span v-else style="color: #999">尚未設定，設定後才能申請提現</span>
      </a-card>

      <a-card v-if="$access('ENT_MCH_WALLET_WITHDRAW')" size="small" title="申請提現" style="margin-bottom: 16px">
        <a-form layout="inline">
          <a-form-item label="金額"><a-input-number v-model:value="vdata.apply.amount" :min="1" :precision="2" addon-after="元" /></a-form-item>
          <a-form-item>
            <a-button type="primary" :loading="vdata.apply.loading" :disabled="!vdata.account.payoutAccountNo" @click="applyFunc">送出申請</a-button>
          </a-form-item>
        </a-form>
      </a-card>

      <a-tabs>
        <a-tab-pane key="w" tab="提現紀錄">
          <a-table :columns="withdrawColumns" :data-source="vdata.withdraws" size="small" row-key="withdrawId" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{ yuan(record.amount) }}</template>
              <template v-if="column.key === 'actualAmount'">{{ yuan(record.actualAmount) }}</template>
              <template v-if="column.key === 'state'"><a-tag :color="STATES[record.state].color">{{ STATES[record.state].text }}</a-tag></template>
              <template v-if="column.key === 'op'">
                <a-button v-if="record.state === 0 && $access('ENT_MCH_WALLET_WITHDRAW')" type="link" danger @click="cancelFunc(record)">取消</a-button>
              </template>
            </template>
            <template #emptyText>尚無提現紀錄</template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="l" tab="錢包流水">
          <a-table :columns="ledgerColumns" :data-source="vdata.ledger" size="small" row-key="ledgerId" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'bizType'">{{ BIZ_TYPES[record.bizType] || record.bizType }}</template>
              <template v-if="column.key === 'amount'">
                <span :style="{ color: record.amount < 0 ? '#cf1322' : record.amount > 0 ? '#389e0d' : 'inherit' }">{{ yuan(record.amount) }}</span>
              </template>
              <template v-if="column.key === 'balanceAfter'">{{ yuan(record.balanceAfter) }}</template>
            </template>
            <template #emptyText>尚無流水</template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="vdata.payout.open" title="收款帳戶" ok-text="儲存" @ok="savePayout">
      <a-alert type="warning" show-icon style="margin-bottom: 12px" message="變更後 24 小時內的提現，平台會加強審核。" />
      <a-form layout="vertical">
        <a-form-item label="銀行名稱"><a-input v-model:value="vdata.payout.form.payoutBankName" placeholder="例如 臺灣銀行" /></a-form-item>
        <a-form-item label="銀行代碼（3 碼）"><a-input v-model:value="vdata.payout.form.payoutBankCode" :maxlength="3" /></a-form-item>
        <a-form-item label="分行（選填）"><a-input v-model:value="vdata.payout.form.payoutBranch" /></a-form-item>
        <a-form-item label="帳號"><a-input v-model:value="vdata.payout.form.payoutAccountNo" /></a-form-item>
        <a-form-item label="戶名"><a-input v-model:value="vdata.payout.form.payoutAccountName" /></a-form-item>
      </a-form>
    </a-modal>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_MCH_WALLET, req } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const BIZ_TYPES = {
  ORDER_SETTLE: '訂單結算',
  ORDER_REVERSE: '退款沖回',
  WITHDRAW_APPLY: '提現凍結',
  WITHDRAW_RELEASE: '提現解凍',
  WITHDRAW_PAID: '提現撥款',
  ADJUST: '人工調帳',
}
const STATES = {
  0: { text: '待審核', color: 'orange' },
  1: { text: '已撥款', color: 'green' },
  2: { text: '已駁回', color: 'red' },
  3: { text: '已取消', color: 'default' },
}
const withdrawColumns = [
  { title: '提現單號', dataIndex: 'withdrawId' },
  { key: 'amount', title: '申請（元）' },
  { key: 'actualAmount', title: '實付（元）' },
  { key: 'state', title: '狀態' },
  { title: '說明', dataIndex: 'reviewRemark' },
  { title: '申請時間', dataIndex: 'createdAt' },
  { key: 'op', title: '', width: '70px' },
]
const ledgerColumns = [
  { title: '時間', dataIndex: 'createdAt' },
  { key: 'bizType', title: '類型' },
  { title: '訂單／提現單號', dataIndex: 'bizId' },
  { key: 'amount', title: '變動（元）' },
  { key: 'balanceAfter', title: '餘額（元）' },
  { title: '說明', dataIndex: 'remark' },
]

function yuan(fen) {
  return (Number(fen || 0) / 100).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function newReqNo() {
  return 'R' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
}

const vdata: any = reactive({
  account: {},
  withdraws: [],
  ledger: [],
  apply: { amount: undefined, loading: false, reqNo: newReqNo() },
  payout: { open: false, form: {} },
})

function load() {
  req.list(API_URL_MCH_WALLET, {}).then((res) => {
    vdata.account = res || {}
    vdata.payout.form = {
      payoutBankName: res.payoutBankName,
      payoutBankCode: res.payoutBankCode,
      payoutBranch: res.payoutBranch,
      payoutAccountNo: res.payoutAccountNo,
      payoutAccountName: res.payoutAccountName,
    }
  })
  req.list(API_URL_MCH_WALLET + '/withdraws', { pageSize: 20 }).then((res) => (vdata.withdraws = res.records || []))
  req.list(API_URL_MCH_WALLET + '/ledger', { pageSize: 30 }).then((res) => (vdata.ledger = res.records || []))
}
load()

function applyFunc() {
  if (!vdata.apply.amount) {
    $infoBox.message.warning('請輸入提現金額')
    return
  }
  vdata.apply.loading = true
  // reqNo 在成功前保持不變：網路重送不會重複凍結
  req
    .add(API_URL_MCH_WALLET + '/withdraws', { amount: String(vdata.apply.amount), reqNo: vdata.apply.reqNo })
    .then((res) => {
      if (res.state === 2) {
        $infoBox.message.error('申請未通過：' + (res.reviewRemark || '風控拒絕'))
      } else {
        $infoBox.message.success('已送出，金額已凍結，等待平台撥款')
      }
      vdata.apply.amount = undefined
      vdata.apply.reqNo = newReqNo()
      load()
    })
    .finally(() => (vdata.apply.loading = false))
}

function cancelFunc(record) {
  $infoBox.confirmDanger('確認取消這筆提現？', '取消後金額會解凍回可用餘額', () => {
    req.add(`${API_URL_MCH_WALLET}/withdraws/${record.withdrawId}/cancel`, {}).then(() => {
      $infoBox.message.success('已取消')
      load()
    })
  })
}

function savePayout() {
  req.updateById(API_URL_MCH_WALLET, 'payoutAccount', vdata.payout.form).then(() => {
    $infoBox.message.success('收款帳戶已更新')
    vdata.payout.open = false
    load()
  })
}
</script>
