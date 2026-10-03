<template>
  <a-drawer v-model:open="vdata.open" :title="title" width="720" @close="stop">
    <a-alert type="info" show-icon style="margin-bottom: 16px" message="檔案在背景產生（通常數秒到數分鐘），完成後在這裡下載。檔案為 CSV，可用 Excel 開啟，保留 7 天。" />
    <a-table :columns="columns" :data-source="records" size="small" row-key="jobId" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'state'">
          <a-tag :color="STATES[record.state].color">{{ STATES[record.state].text }}</a-tag>
          <div v-if="record.errorMsg" style="font-size: 12px; color: #cf1322">{{ record.errorMsg }}</div>
        </template>
        <template v-if="column.key === 'op'">
          <a-button v-if="record.state === 2" type="link" @click="download(record)">下載</a-button>
        </template>
      </template>
      <template #emptyText>尚無匯出紀錄</template>
    </a-table>
  </a-drawer>
</template>

<script setup lang="ts">
// 匯出紀錄：各查詢頁按「匯出」後在同一頁等待與下載，只列出該頁的匯出類型
import { req } from '@/api/manage'
import { EXPORT_API, downloadExport } from '@/utils/exportJob'
import { reactive, computed, onBeforeUnmount, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const props = defineProps({
  jobTypes: { type: Array, default: () => [] }, // 只顯示這些匯出類型；空陣列為全部
  title: { type: String, default: '匯出紀錄' },
})

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
  { key: 'op', title: '', width: '80px' },
]
const vdata: any = reactive({ open: false, all: [] })
const records = computed(() =>
  props.jobTypes.length ? vdata.all.filter((r) => props.jobTypes.includes(r.jobType)) : vdata.all
)
let timer: any = null

function load() {
  req.list(EXPORT_API, { pageSize: 50 }).then((res) => (vdata.all = res.records || []))
}
function stop() {
  if (timer) clearInterval(timer)
  timer = null
}
function open() {
  vdata.open = true
  load()
  stop()
  // 有未完成的工作時每 5 秒更新一次
  timer = setInterval(() => {
    if (records.value.some((r) => r.state === 0 || r.state === 1)) load()
  }, 5000)
}
onBeforeUnmount(stop)

function download(record) {
  downloadExport(record).catch((e) => $infoBox.message.error(e.message))
}
defineExpose({ open })
</script>
