<template>
  <!-- 通道路由：只能設定指定給轄下商戶的規則 -->
  <div>
    <a-alert type="info" show-icon style="margin-bottom: 12px"
      message="商戶用「別名代碼」下單時，系統依金額區間、時段與權重，從符合的規則裡挑一個實際支付方式。這裡只能設定指定給自己旗下商戶的規則。" />
    <a-button v-if="canEdit" type="primary" style="margin-bottom: 12px" @click="openForm(null)">新增規則</a-button>
    <a-table :columns="columns" :data-source="vdata.records" size="small" row-key="routeId" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount'">{{ record.minAmount / 100 }} ～ {{ record.maxAmount ? record.maxAmount / 100 : '不限' }}</template>
        <template v-if="column.key === 'time'">{{ record.timeStart ? `${record.timeStart}～${record.timeEnd}` : '全天' }}</template>
        <template v-if="column.key === 'state'"><a-badge :status="record.state === 1 ? 'processing' : 'error'" :text="record.state === 1 ? '啟用' : '停用'" /></template>
        <template v-if="column.key === 'op'">
          <template v-if="canEdit">
            <a-button type="link" @click="openForm(record)">修改</a-button>
            <a-button type="link" danger @click="remove(record)">刪除</a-button>
          </template>
        </template>
      </template>
      <template #emptyText>尚未設定路由規則</template>
    </a-table>

    <a-modal v-model:open="vdata.form.open" :title="vdata.form.data.routeId ? '修改規則' : '新增規則'" ok-text="儲存" cancel-text="取消" :confirm-loading="vdata.form.saving" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="商戶" required><a-select v-model:value="vdata.form.data.mchNo" :options="mchOptions" show-search option-filter-prop="label" /></a-form-item>
        <a-form-item label="別名代碼" required extra="2～20 碼大寫英數或底線，例如 IBON"><a-input v-model:value="vdata.form.data.aliasWayCode" /></a-form-item>
        <a-form-item label="實際支付方式" required><a-select v-model:value="vdata.form.data.targetWayCode" :options="wayOptions" /></a-form-item>
        <a-form-item label="金額區間（元，上限 0 表示不限）">
          <a-input-number v-model:value="vdata.form.minYuan" :min="0" :precision="0" /> ～
          <a-input-number v-model:value="vdata.form.maxYuan" :min="0" :precision="0" />
        </a-form-item>
        <a-form-item label="權重（1～9）"><a-input-number v-model:value="vdata.form.data.weight" :min="1" :max="9" :precision="0" /></a-form-item>
        <a-form-item label="可用時段（HH:mm，留空為全天）">
          <a-input v-model:value="vdata.form.data.timeStart" placeholder="09:00" style="width: 100px" /> ～
          <a-input v-model:value="vdata.form.data.timeEnd" placeholder="18:00" style="width: 100px" />
        </a-form-item>
        <a-form-item label="狀態"><a-radio-group v-model:value="vdata.form.data.state"><a-radio :value="1">啟用</a-radio><a-radio :value="0">停用</a-radio></a-radio-group></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { computed, reactive, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const props = defineProps({ me: { type: Object, required: true }, merchants: { type: Array, default: () => [] } })
const BASE = API_URL_AGENT_PORTAL + '/branch/wayRoutes'

const canEdit = computed(() => props.me.agentLevel === 1 && $access('ENT_AGENT_PORTAL_ROUTE'))
const mchOptions = computed(() => (props.merchants as any[]).filter((m) => m.relation !== '推薦').map((m) => ({ value: m.mchNo, label: `${m.mchName || ''}（${m.mchNo}）` })))
const wayOptions = computed(() => (props.me.payWays || []).map((w) => ({ value: w.wayCode, label: `${w.wayName}（${w.wayCode}）` })))
const columns = [
  { title: '別名', dataIndex: 'aliasWayCode' },
  { title: '實際支付方式', dataIndex: 'targetWayCode' },
  { title: '商戶號', dataIndex: 'mchNo' },
  { key: 'amount', title: '金額區間（元）' },
  { title: '權重', dataIndex: 'weight' },
  { key: 'time', title: '時段' },
  { key: 'state', title: '狀態' },
  { key: 'op', title: '操作', width: 130 },
]
const vdata: any = reactive({ records: [], form: { open: false, saving: false, data: {}, minYuan: 0, maxYuan: 0 } })

function load() {
  req.list(BASE, {}).then((res) => (vdata.records = res || []))
}
function openForm(record) {
  vdata.form = record
    ? { open: true, saving: false, data: { ...record }, minYuan: record.minAmount / 100, maxYuan: record.maxAmount / 100 }
    : { open: true, saving: false, data: { weight: 1, state: 1 }, minYuan: 0, maxYuan: 0 }
}
function save() {
  const f = vdata.form
  if (!f.data.mchNo || !f.data.aliasWayCode || !f.data.targetWayCode) {
    $infoBox.message.warning('請選擇商戶並填寫別名代碼與實際支付方式')
    return
  }
  f.saving = true
  req.add(BASE, { ...f.data, minAmount: Math.round((f.minYuan || 0) * 100), maxAmount: Math.round((f.maxYuan || 0) * 100) })
    .then(() => {
      $infoBox.message.success('已儲存')
      f.open = false
      load()
    })
    .finally(() => (f.saving = false))
}
function remove(record) {
  $infoBox.confirmDanger('確認刪除？', `別名 ${record.aliasWayCode} → ${record.targetWayCode}（商戶 ${record.mchNo}）`, () => {
    req.delById(BASE, record.routeId).then(() => {
      $infoBox.message.success('已刪除')
      load()
    })
  })
}
load()
</script>
