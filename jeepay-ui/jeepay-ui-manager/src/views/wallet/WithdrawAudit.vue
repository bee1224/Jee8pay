<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>{{ $t('提現審核') }}</template>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
        :message="$t('目前沒有代付通道，提現一律由平台人工匯款：確認收款帳戶與風控提示後完成匯款，再回來按「已撥款」並填寫匯款單號；駁回會把金額解凍回申請人。黑名單命中的申請系統已直接拒絕。')"
      />
      <a-form layout="inline" style="margin-bottom: 12px">
        <a-form-item>
          <a-radio-group v-model:value="vdata.query.state" @change="search">
            <a-radio-button :value="0">{{ $t('待審核') }}</a-radio-button>
            <a-radio-button :value="1">{{ $t('已撥款') }}</a-radio-button>
            <a-radio-button :value="2">{{ $t('已駁回') }}</a-radio-button>
            <a-radio-button :value="3">{{ $t('已取消') }}</a-radio-button>
            <a-radio-button :value="undefined">{{ $t('全部') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item><a-input v-model:value="vdata.query.ownerId" :placeholder="$t('商戶號／代理號')" allow-clear @pressEnter="search" /></a-form-item>
        <a-form-item><a-button type="primary" @click="search">{{ $t('搜尋') }}</a-button></a-form-item>
        <a-form-item v-if="$access('ENT_EXPORT_CENTER')"><a-button @click="exportFunc">{{ $t('匯出') }}</a-button></a-form-item>
        <a-form-item v-if="$access('ENT_EXPORT_CENTER')"><a-button @click="exportDrawer.open()">{{ $t('匯出紀錄') }}</a-button></a-form-item>
      </a-form>
      <a-table :columns="columns" :data-source="vdata.records" size="small" row-key="withdrawId"
        :pagination="{ current: vdata.page, pageSize: 20, total: vdata.total, onChange: (p) => { vdata.page = p; load() } }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'owner'"><a-tag>{{ OWNER_TYPE_NAMES[record.ownerType] }}</a-tag>{{ record.ownerId }}</template>
          <template v-if="column.key === 'amount'">
            <b>{{ yuan(record.actualAmount) }}</b>
            <div style="font-size: 12px; color: #888">{{ $t('申請 {0}，手續費 {1}', [yuan(record.amount), yuan(record.fee)]) }}</div>
          </template>
          <template v-if="column.key === 'bank'">
            {{ record.bankName }}（{{ record.bankCode }}）{{ record.branch || '' }}
            <div>{{ record.accountNo }} · {{ record.accountName }}</div>
          </template>
          <template v-if="column.key === 'risk'">
            <a-tag v-for="f in (record.riskFlags || '').split(',').filter(Boolean)" :key="f" :color="f === 'BLACKLIST' ? 'red' : 'orange'">
              {{ RISK_FLAG_NAMES[f] || f }}
            </a-tag>
          </template>
          <template v-if="column.key === 'state'">
            <a-tag :color="WITHDRAW_STATES[record.state].color">{{ WITHDRAW_STATES[record.state].text }}</a-tag>
            <div v-if="record.agentApproveBy" style="font-size: 12px; color: #389e0d">{{ $t('上級代理已同意：{0}', [record.agentApproveBy]) }}</div>
            <div v-if="record.paidRef" style="font-size: 12px">{{ $t('單號 {0}', [record.paidRef]) }}</div>
            <div v-if="record.reviewRemark" style="font-size: 12px; color: #888">{{ record.reviewRemark }}</div>
          </template>
          <template v-if="column.key === 'op'">
            <template v-if="record.state === 0 && $access('ENT_WALLET_WITHDRAW_REVIEW')">
              <a-button type="link" @click="openReview(record, 'paid')">{{ $t('已撥款') }}</a-button>
              <a-button type="link" danger @click="openReview(record, 'reject')">{{ $t('駁回') }}</a-button>
            </template>
          </template>
        </template>
        <template #emptyText>{{ $t('沒有符合條件的提現單') }}</template>
      </a-table>
      <ExportJobsDrawer ref="exportDrawer" :job-types="['WITHDRAW']" />
    </a-card>

    <a-modal v-model:open="vdata.review.open" :title="vdata.review.action === 'paid' ? $t('標記已撥款') : $t('駁回提現')" :ok-text="$t('確認')" @ok="submitReview">
      <p>
        {{ $t('{0} {1}，實付', [OWNER_TYPE_NAMES[vdata.review.record?.ownerType], vdata.review.record?.ownerId]) }}
        <b>{{ yuan(vdata.review.record?.actualAmount) }}</b> {{ $t('元') }}
      </p>
      <a-form layout="vertical">
        <a-form-item v-if="vdata.review.action === 'paid'" :label="$t('匯款單號／交易序號（必填）')"><a-input v-model:value="vdata.review.paidRef" /></a-form-item>
        <a-form-item :label="vdata.review.action === 'paid' ? $t('備註（選填）') : $t('駁回原因（必填，會顯示給申請人）')">
          <a-textarea v-model:value="vdata.review.remark" :maxlength="128" />
        </a-form-item>
      </a-form>
    </a-modal>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_WITHDRAWS, req } from '@/api/manage'
import { reactive, ref, getCurrentInstance } from 'vue'
import { OWNER_TYPE_NAMES, RISK_FLAG_NAMES, WITHDRAW_STATES, yuan } from '@/components/WalletPanel/walletText'
import { submitExport } from '@/utils/exportJob'
import ExportJobsDrawer from '@/components/ExportJobs/ExportJobsDrawer.vue'
import { t } from '@/i18n'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const columns = [
  { title: t('提現單號'), dataIndex: 'withdrawId' },
  { key: 'owner', title: t('申請人') },
  { key: 'amount', title: t('實付金額（元）') },
  { key: 'bank', title: t('收款帳戶') },
  { key: 'risk', title: t('風控提示') },
  { key: 'state', title: t('狀態') },
  { title: t('申請時間'), dataIndex: 'createdAt' },
  { key: 'op', title: t('操作'), width: '150px' },
]

const exportDrawer = ref()
const vdata: any = reactive({
  query: { state: 0, ownerId: '' },
  records: [],
  total: 0,
  page: 1,
  review: { open: false, action: '', record: null, paidRef: '', remark: '' },
})

function load() {
  req.list(API_URL_WITHDRAWS, { ...vdata.query, pageNumber: vdata.page, pageSize: 20 }).then((res) => {
    vdata.records = res.records || []
    vdata.total = res.total || 0
  })
}
function exportFunc() {
  submitExport('WITHDRAW', vdata.query).then(() => {
    $infoBox.message.success(t('已建立匯出，完成後可在匯出紀錄下載'))
    exportDrawer.value.open()
  })
}
function search() {
  vdata.page = 1
  load()
}
load()

function openReview(record, action) {
  vdata.review = { open: true, action, record, paidRef: '', remark: '' }
}
function submitReview() {
  const r = vdata.review
  req.add(`${API_URL_WITHDRAWS}/${r.record.withdrawId}/${r.action}`, { paidRef: r.paidRef, remark: r.remark }).then(() => {
    $infoBox.message.success(r.action === 'paid' ? t('已標記撥款') : t('已駁回並解凍'))
    vdata.review.open = false
    load()
  })
}
</script>
