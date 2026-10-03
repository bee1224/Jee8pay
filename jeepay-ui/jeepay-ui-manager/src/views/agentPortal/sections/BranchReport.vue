<template>
  <!-- 統計報表：轄下商戶的代收統計，依代理、商戶、日期 -->
  <div>
    <a-form layout="inline" style="margin-bottom: 16px">
      <a-form-item><a-range-picker v-model:value="vdata.range" value-format="YYYY-MM-DD" /></a-form-item>
      <a-form-item><a-button type="primary" @click="load">查詢</a-button></a-form-item>
      <a-form-item><span style="color: #888">未選日期為近 7 天，最長 93 天</span></a-form-item>
    </a-form>
    <template v-if="vdata.data">
      <a-row :gutter="16" style="margin-bottom: 20px">
        <a-col :xs="12" :md="6"><a-statistic title="訂單數" :value="vdata.data.total.total" /></a-col>
        <a-col :xs="12" :md="6"><a-statistic title="成功筆數" :value="vdata.data.total.paid" /></a-col>
        <a-col :xs="12" :md="6"><a-statistic title="成功金額（元）" :value="yuan(vdata.data.total.paidAmount)" /></a-col>
        <a-col :xs="12" :md="6"><a-statistic title="商戶手續費（元）" :value="yuan(vdata.data.total.feeAmount)" /></a-col>
      </a-row>
      <a-tabs>
        <a-tab-pane key="agent" tab="依代理">
          <a-table :columns="cols('代理')" :data-source="vdata.data.byAgent" size="small" row-key="id" :pagination="false">
            <template #bodyCell="{ column, record }"><template v-if="column.money">{{ yuan(record[column.dataIndex]) }}</template></template>
            <template #emptyText>這段期間沒有訂單</template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="mch" tab="依商戶">
          <a-table :columns="cols('商戶')" :data-source="vdata.data.byMch" size="small" row-key="id" :pagination="false">
            <template #bodyCell="{ column, record }"><template v-if="column.money">{{ yuan(record[column.dataIndex]) }}</template></template>
            <template #emptyText>這段期間沒有訂單</template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="day" tab="依日期">
          <a-table :columns="cols('日期')" :data-source="vdata.data.byDay" size="small" row-key="id" :pagination="false">
            <template #bodyCell="{ column, record }"><template v-if="column.money">{{ yuan(record[column.dataIndex]) }}</template></template>
            <template #emptyText>這段期間沒有訂單</template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { yuan } from '@/components/WalletPanel/walletText'
import { reactive } from 'vue'
defineProps({ me: { type: Object, required: true } })

const vdata: any = reactive({ range: [], data: null })
function cols(label) {
  const first = label === '日期' ? [{ title: '日期', dataIndex: 'id' }] : [{ title: label + '號', dataIndex: 'id' }, { title: '名稱', dataIndex: 'name' }]
  return first.concat([
    { title: '訂單數', dataIndex: 'total' },
    { title: '成功筆數', dataIndex: 'paid' },
    { title: '成功金額（元）', dataIndex: 'paidAmount', money: true },
    { title: '商戶手續費（元）', dataIndex: 'feeAmount', money: true },
  ] as any)
}
function load() {
  const [startDate, endDate] = vdata.range && vdata.range.length === 2 ? vdata.range : [undefined, undefined]
  req.list(API_URL_AGENT_PORTAL + '/branch/report', { startDate, endDate }).then((res) => (vdata.data = res))
}
load()
</script>
