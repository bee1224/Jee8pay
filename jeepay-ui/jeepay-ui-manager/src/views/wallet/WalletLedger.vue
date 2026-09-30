<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>餘額流水</template>
      <a-form layout="inline" style="margin-bottom: 12px">
        <a-form-item>
          <a-select v-model:value="vdata.query.ownerType" style="width: 130px" placeholder="帳戶類型" allow-clear>
            <a-select-option v-for="(n, k) in OWNER_TYPE_NAMES" :key="k" :value="k">{{ n }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item><a-input v-model:value="vdata.query.ownerId" placeholder="商戶號／代理號" allow-clear /></a-form-item>
        <a-form-item>
          <a-select v-model:value="vdata.query.bizType" style="width: 140px" placeholder="類型" allow-clear>
            <a-select-option v-for="(n, k) in BIZ_TYPE_NAMES" :key="k" :value="k">{{ n }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item><a-input v-model:value="vdata.query.bizId" placeholder="訂單號／提現單號" allow-clear /></a-form-item>
        <a-form-item><a-range-picker v-model:value="vdata.range" value-format="YYYY-MM-DD" /></a-form-item>
        <a-form-item><a-button type="primary" @click="search">搜尋</a-button></a-form-item>
      </a-form>
      <a-table :columns="columns" :data-source="vdata.records" size="small" row-key="ledgerId"
        :pagination="{ current: vdata.page, pageSize: 20, total: vdata.total, onChange: (p) => { vdata.page = p; load() } }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'owner'"><a-tag>{{ OWNER_TYPE_NAMES[record.ownerType] }}</a-tag>{{ record.ownerId }}</template>
          <template v-if="column.key === 'bizType'">{{ BIZ_TYPE_NAMES[record.bizType] || record.bizType }}</template>
          <template v-if="column.key === 'amount'">
            <span :style="{ color: record.amount < 0 ? '#cf1322' : record.amount > 0 ? '#389e0d' : 'inherit' }">{{ yuan(record.amount) }}</span>
          </template>
          <template v-if="column.key === 'frozenChange'">{{ yuan(record.frozenChange) }}</template>
          <template v-if="column.key === 'balance'">{{ yuan(record.balanceBefore) }} → {{ yuan(record.balanceAfter) }}</template>
        </template>
      </a-table>
    </a-card>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_WALLET, req } from '@/api/manage'
import { reactive } from 'vue'
import { BIZ_TYPE_NAMES, OWNER_TYPE_NAMES, yuan } from '@/components/WalletPanel/walletText'

const columns = [
  { title: '時間', dataIndex: 'createdAt', width: '170px' },
  { key: 'owner', title: '帳戶' },
  { key: 'bizType', title: '類型' },
  { title: '單號', dataIndex: 'bizId' },
  { key: 'amount', title: '可用變動（元）' },
  { key: 'frozenChange', title: '凍結變動（元）' },
  { key: 'balance', title: '可用餘額（元）' },
  { title: '說明', dataIndex: 'remark' },
  { title: '操作者', dataIndex: 'operatorName' },
]

const vdata: any = reactive({ query: {}, range: [], records: [], total: 0, page: 1 })

function load() {
  const [startDate, endDate] = vdata.range || []
  req.list(API_URL_WALLET + '/ledger', { ...vdata.query, startDate, endDate, pageNumber: vdata.page, pageSize: 20 }).then((res) => {
    vdata.records = res.records || []
    vdata.total = res.total || 0
  })
}
function search() {
  vdata.page = 1
  load()
}
load()
</script>
