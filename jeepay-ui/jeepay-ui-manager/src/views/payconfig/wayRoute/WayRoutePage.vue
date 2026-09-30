<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>通道路由</template>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
        message="商戶以「別名代碼」下單時，系統在商戶已開通的實際通道中，依金額區間、時段（台北時間）與權重（1–9）挑選一個；有商戶專屬規則時只用專屬規則。以實際代碼（例如 RYO_IBON）下單不受影響。"
      />
      <a-tabs>
        <a-tab-pane key="rules" tab="路由規則">
          <a-button v-if="$access('ENT_WAY_ROUTE_EDIT')" type="primary" style="margin-bottom: 12px" @click="openEdit(null)">新增規則</a-button>
          <a-table :columns="columns" :data-source="vdata.routes" size="small" row-key="routeId" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'mchNo'">{{ record.mchNo || '全部商戶' }}</template>
              <template v-if="column.key === 'amount'">{{ amountText(record) }}</template>
              <template v-if="column.key === 'time'">{{ record.timeStart ? `${record.timeStart}–${record.timeEnd}` : '全天' }}</template>
              <template v-if="column.key === 'state'">
                <a-badge :status="record.state === 1 ? 'processing' : 'error'" :text="record.state === 1 ? '啟用' : '停用'" />
              </template>
              <template v-if="column.key === 'op'">
                <template v-if="$access('ENT_WAY_ROUTE_EDIT')">
                  <a-button type="link" @click="openEdit(record)">修改</a-button>
                  <a-button type="link" danger @click="delFunc(record)">刪除</a-button>
                </template>
              </template>
            </template>
            <template #emptyText>尚無路由規則</template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="logs" tab="決策紀錄">
          <a-form layout="inline" style="margin-bottom: 12px">
            <a-form-item><a-input v-model:value="vdata.logQuery.mchNo" placeholder="商戶號" allow-clear /></a-form-item>
            <a-form-item><a-input v-model:value="vdata.logQuery.mchOrderNo" placeholder="商戶訂單號" allow-clear /></a-form-item>
            <a-form-item><a-button type="primary" @click="loadLogs">搜尋</a-button></a-form-item>
          </a-form>
          <a-table :columns="logColumns" :data-source="vdata.logs" size="small" row-key="logId" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'chosen'">
                <a-tag v-if="record.chosenWayCode" color="green">{{ record.chosenWayCode }}</a-tag>
                <a-tag v-else color="red">無可用通道</a-tag>
              </template>
              <template v-if="column.key === 'amount'">{{ (record.amount / 100).toFixed(2) }}</template>
              <template v-if="column.key === 'candidates'">
                <span v-for="c in parse(record.candidates)" :key="c.wayCode" style="margin-right: 8px">{{ c.wayCode }}×{{ c.weight }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="vdata.edit.open" :title="vdata.edit.form.routeId ? '修改路由規則' : '新增路由規則'" ok-text="儲存" @ok="submit">
      <a-form layout="vertical">
        <a-form-item label="別名代碼（商戶下單用，例如 IBON）"><a-input v-model:value="vdata.edit.form.aliasWayCode" /></a-form-item>
        <a-form-item label="實際支付方式">
          <a-select v-model:value="vdata.edit.form.targetWayCode" :options="vdata.wayOptions" show-search option-filter-prop="label" />
        </a-form-item>
        <a-form-item label="商戶號（留空表示全部商戶）"><a-input v-model:value="vdata.edit.form.mchNo" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="金額下限（元，0 不限）"><a-input-number v-model:value="vdata.edit.minYuan" :min="0" :precision="0" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="金額上限（元，0 不限）"><a-input-number v-model:value="vdata.edit.maxYuan" :min="0" :precision="0" style="width: 100%" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="8"><a-form-item label="權重"><a-input-number v-model:value="vdata.edit.form.weight" :min="1" :max="9" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="時段起（HH:mm）"><a-input v-model:value="vdata.edit.form.timeStart" placeholder="全天留空" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="時段迄（HH:mm）"><a-input v-model:value="vdata.edit.form.timeEnd" placeholder="可跨午夜" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="狀態">
          <a-radio-group v-model:value="vdata.edit.form.state"><a-radio :value="1">啟用</a-radio><a-radio :value="0">停用</a-radio></a-radio-group>
        </a-form-item>
        <a-form-item label="備註"><a-input v-model:value="vdata.edit.form.remark" /></a-form-item>
      </a-form>
    </a-modal>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_PAYWAYS_LIST, API_URL_WAY_ROUTES, req } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const columns = [
  { title: '別名', dataIndex: 'aliasWayCode' },
  { title: '實際通道', dataIndex: 'targetWayCode' },
  { key: 'mchNo', title: '適用商戶' },
  { key: 'amount', title: '金額（元）' },
  { key: 'time', title: '時段' },
  { title: '權重', dataIndex: 'weight' },
  { key: 'state', title: '狀態' },
  { title: '備註', dataIndex: 'remark' },
  { key: 'op', title: '操作', width: '140px' },
]
const logColumns = [
  { title: '時間', dataIndex: 'createdAt' },
  { title: '商戶號', dataIndex: 'mchNo' },
  { title: '商戶訂單號', dataIndex: 'mchOrderNo' },
  { title: '別名', dataIndex: 'aliasWayCode' },
  { key: 'amount', title: '金額（元）' },
  { key: 'candidates', title: '候選（代碼×權重）' },
  { key: 'chosen', title: '結果' },
]

const vdata: any = reactive({
  routes: [],
  logs: [],
  logQuery: {},
  wayOptions: [],
  edit: { open: false, form: {}, minYuan: 0, maxYuan: 0 },
})

function amountText(r) {
  const min = r.minAmount ? r.minAmount / 100 : 0
  const max = r.maxAmount ? r.maxAmount / 100 : 0
  if (!min && !max) return '不限'
  return `${min || 0} – ${max || '∞'}`
}
function parse(v) {
  try {
    return JSON.parse(v || '[]')
  } catch (e) {
    return []
  }
}
function load() {
  req.list(API_URL_WAY_ROUTES, { pageSize: -1 }).then((res) => (vdata.routes = res.records || []))
}
function loadLogs() {
  req.list(API_URL_WAY_ROUTES + '/logs', { ...vdata.logQuery, pageSize: 50 }).then((res) => (vdata.logs = res.records || []))
}
load()
loadLogs()
req.list(API_URL_PAYWAYS_LIST, { pageSize: -1 }).then((res) => {
  vdata.wayOptions = (res.records || []).map((w) => ({ value: w.wayCode, label: `${w.wayName}（${w.wayCode}）` }))
})

function openEdit(record) {
  const f = record ? { ...record } : { aliasWayCode: 'IBON', weight: 1, state: 1, mchNo: '' }
  vdata.edit = { open: true, form: f, minYuan: record ? (record.minAmount || 0) / 100 : 0, maxYuan: record ? (record.maxAmount || 0) / 100 : 0 }
}
function submit() {
  const e = vdata.edit
  req
    .add(API_URL_WAY_ROUTES, { ...e.form, minAmount: Math.round((e.minYuan || 0) * 100), maxAmount: Math.round((e.maxYuan || 0) * 100) })
    .then(() => {
      $infoBox.message.success('已儲存')
      vdata.edit.open = false
      load()
    })
}
function delFunc(record) {
  $infoBox.confirmDanger('確認刪除這條路由規則？', '', () => {
    req.delById(API_URL_WAY_ROUTES, record.routeId).then(() => {
      $infoBox.message.success('已刪除')
      load()
    })
  })
}
</script>
