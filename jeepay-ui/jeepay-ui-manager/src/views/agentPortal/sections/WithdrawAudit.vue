<template>
  <!-- 提現審核：團長對旗下提現單表示同意或駁回；撥款仍由平台執行 -->
  <div>
    <a-alert type="info" show-icon style="margin-bottom: 12px"
      message="「同意」會在提現單上留下你的註記，平台看到後才撥款；「駁回」會直接結案，凍結的金額退回對方的可用餘額。" />
    <a-form layout="inline" style="margin-bottom: 12px">
      <a-form-item>
        <a-select v-model:value="vdata.state" style="width: 130px" @change="search">
          <a-select-option :value="0">待審核</a-select-option>
          <a-select-option :value="1">已撥款</a-select-option>
          <a-select-option :value="2">已駁回</a-select-option>
          <a-select-option :value="3">已取消</a-select-option>
          <a-select-option value="">全部</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
    <a-table :columns="columns" :data-source="vdata.records" size="small" row-key="withdrawId" :scroll="{ x: 1000 }"
      :pagination="{ current: vdata.page, pageSize: 20, total: vdata.total, showTotal: (t) => `共${t}筆`, onChange: (p) => { vdata.page = p; load() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'owner'"><a-tag>{{ OWNER_TYPE_NAMES[record.ownerType] }}</a-tag>{{ record.ownerId }}</template>
        <template v-if="column.key === 'amount'">{{ yuan(record.amount) }}</template>
        <template v-if="column.key === 'account'">{{ record.bankName }} {{ record.accountNo }}<br />{{ record.accountName }}</template>
        <template v-if="column.key === 'risk'">
          <a-tag v-for="f in (record.riskFlags || '').split(',').filter(Boolean)" :key="f" color="orange">{{ RISK_FLAG_NAMES[f] || f }}</a-tag>
        </template>
        <template v-if="column.key === 'state'">
          <a-tag :color="WITHDRAW_STATES[record.state].color">{{ WITHDRAW_STATES[record.state].text }}</a-tag>
          <div v-if="record.agentApproveBy" style="font-size: 12px; color: #389e0d">已同意：{{ record.agentApproveBy }}</div>
          <div v-if="record.reviewRemark" style="font-size: 12px; color: #888">{{ record.reviewRemark }}</div>
        </template>
        <template v-if="column.key === 'op'">
          <template v-if="record.state === 0 && me.agentLevel === 1">
            <a-button type="link" :disabled="!!record.agentApproveBy" @click="approve(record)">同意</a-button>
            <a-button type="link" danger @click="openReject(record)">駁回</a-button>
          </template>
        </template>
      </template>
      <template #emptyText>沒有符合條件的提現單</template>
    </a-table>

    <a-modal v-model:open="vdata.reject.open" title="駁回提現" ok-text="確定駁回" cancel-text="取消" :confirm-loading="vdata.reject.saving" @ok="submitReject">
      <a-form layout="vertical">
        <a-form-item label="駁回原因" required><a-input v-model:value="vdata.reject.remark" :maxlength="100" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { OWNER_TYPE_NAMES, RISK_FLAG_NAMES, WITHDRAW_STATES, yuan } from '@/components/WalletPanel/walletText'
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
defineProps({ me: { type: Object, required: true } })
const BASE = API_URL_AGENT_PORTAL + '/branch/withdraws'

const columns = [
  { title: '提現單號', dataIndex: 'withdrawId', width: 180 },
  { key: 'owner', title: '申請人' },
  { key: 'amount', title: '金額（元）', width: 110 },
  { key: 'account', title: '收款帳戶' },
  { key: 'risk', title: '風控提示' },
  { key: 'state', title: '狀態' },
  { title: '申請時間', dataIndex: 'createdAt', width: 170 },
  { key: 'op', title: '操作', width: 130 },
]
const vdata: any = reactive({ state: 0, records: [], total: 0, page: 1, reject: { open: false, saving: false, id: '', remark: '' } })

function load() {
  req.list(BASE, { pageNumber: vdata.page, pageSize: 20, state: vdata.state === '' ? undefined : vdata.state }).then((res) => {
    vdata.records = res.records || []
    vdata.total = res.total || 0
  })
}
function search() {
  vdata.page = 1
  load()
}
function approve(record) {
  req.add(`${BASE}/${record.withdrawId}/approve`, {}).then(() => {
    $infoBox.message.success('已註記同意，等待平台撥款')
    load()
  })
}
function openReject(record) {
  vdata.reject = { open: true, saving: false, id: record.withdrawId, remark: '' }
}
function submitReject() {
  if (!vdata.reject.remark.trim()) {
    $infoBox.message.warning('請填寫駁回原因')
    return
  }
  vdata.reject.saving = true
  req.add(`${BASE}/${vdata.reject.id}/reject`, { remark: vdata.reject.remark })
    .then(() => {
      $infoBox.message.success('已駁回')
      vdata.reject.open = false
      load()
    })
    .finally(() => (vdata.reject.saving = false))
}
load()
</script>
