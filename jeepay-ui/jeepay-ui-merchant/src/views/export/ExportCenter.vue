<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>下載中心</template>
      <template #extra><a-button @click="load">重新整理</a-button></template>
      <a-alert type="info" show-icon style="margin-bottom: 16px" message="在各列表頁按「匯出」後，檔案會在背景產生（通常數秒到數分鐘），完成後在這裡下載。檔案為 CSV，可用 Excel 開啟，保留 7 天。" />
      <a-table :columns="columns" :data-source="vdata.records" size="small" row-key="jobId" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <a-tag :color="STATES[record.state].color">{{ STATES[record.state].text }}</a-tag>
            <div v-if="record.errorMsg" style="font-size: 12px; color: #cf1322">{{ record.errorMsg }}</div>
          </template>
          <template v-if="column.key === 'op'">
            <a-button v-if="record.state === 2" type="link" @click="download(record)">下載</a-button>
          </template>
        </template>
        <template #emptyText>尚無匯出工作</template>
      </a-table>
    </a-card>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { req } from '@/api/manage'
import { EXPORT_API, downloadExport } from '@/utils/exportJob'
import { reactive, onBeforeUnmount, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const STATES = {
  0: { text: '排隊中', color: 'default' },
  1: { text: '產生中', color: 'processing' },
  2: { text: '完成', color: 'green' },
  3: { text: '失敗', color: 'red' },
}
const columns = [
  { title: '檔名', dataIndex: 'fileName' },
  { key: 'state', title: '狀態' },
  { title: '筆數', dataIndex: 'rowCount' },
  { title: '建立時間', dataIndex: 'createdAt' },
  { title: '完成時間', dataIndex: 'finishedAt' },
  { key: 'op', title: '', width: '80px' },
]
const vdata: any = reactive({ records: [] })

function load() {
  req.list(EXPORT_API, { pageSize: 50 }).then((res) => (vdata.records = res.records || []))
}
load()
// 有未完成的工作時每 5 秒更新一次
const timer = setInterval(() => {
  if (vdata.records.some((r) => r.state === 0 || r.state === 1)) load()
}, 5000)
onBeforeUnmount(() => clearInterval(timer))

function download(record) {
  downloadExport(record).catch((e) => $infoBox.message.error(e.message))
}
</script>
