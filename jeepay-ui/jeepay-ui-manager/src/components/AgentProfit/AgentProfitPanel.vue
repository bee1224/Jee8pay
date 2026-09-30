<template>
  <!-- 分潤統計與明細：代理後台（查自己）與平台代理列表（查指定代理）共用，差別只在 API 路徑 -->
  <div>
    <a-form layout="inline" style="margin-bottom: 12px">
      <a-form-item label="成功日期">
        <a-range-picker v-model:value="vdata.range" value-format="YYYY-MM-DD" />
      </a-form-item>
      <a-form-item><a-button type="primary" @click="load">查詢</a-button></a-form-item>
    </a-form>
    <a-row :gutter="16" style="margin-bottom: 12px">
      <a-col :span="8"><a-statistic title="成功訂單數" :value="vdata.summary.orderCount || 0" /></a-col>
      <a-col :span="8"><a-statistic title="交易金額（元）" :value="toYuan(vdata.summary.amount)" /></a-col>
      <a-col :span="8"><a-statistic title="分潤（元）" :value="toYuan(vdata.summary.profit)" :value-style="{ color: '#3f8600' }" /></a-col>
    </a-row>
    <a-table :columns="columns" :data-source="vdata.records" size="small" row-key="payOrderId"
      :pagination="{ current: vdata.pageNumber, pageSize: 10, total: vdata.total, onChange: (p) => { vdata.pageNumber = p; loadPage() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount'">{{ toYuan(record.amount) }}</template>
        <template v-if="column.key === 'profit'">{{ toYuan(record.profit) }}</template>
      </template>
      <template #emptyText>此期間沒有分潤（只計入支付成功、且有手續費快照的訂單）</template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { req } from '@/api/manage'
import { reactive, watch } from 'vue'

const props = defineProps({
  // 例如 /api/agentPortal/profits 或 /api/agentInfo/{agentNo}/profits
  baseUrl: { type: String, required: true },
})

const columns = [
  { title: '支付訂單號', dataIndex: 'payOrderId' },
  { title: '商戶', dataIndex: 'mchName' },
  { title: '支付方式', dataIndex: 'wayCode' },
  { key: 'amount', title: '金額（元）' },
  { key: 'profit', title: '分潤（元）' },
  { title: '成功時間', dataIndex: 'successTime' },
]

const vdata: any = reactive({ range: [], summary: {}, records: [], total: 0, pageNumber: 1 })

const toYuan = (fen) => (Number(fen || 0) / 100).toFixed(2)

function params() {
  const [startDate, endDate] = vdata.range || []
  return { startDate, endDate }
}

function loadPage() {
  req.list(props.baseUrl, { ...params(), pageNumber: vdata.pageNumber, pageSize: 10 }).then((res) => {
    vdata.records = res.records || []
    vdata.total = res.total || 0
  })
}

function load() {
  vdata.pageNumber = 1
  req.list(props.baseUrl + '/summary', params()).then((res) => {
    vdata.summary = res || {}
  })
  loadPage()
}

watch(() => props.baseUrl, load, { immediate: true })
</script>
