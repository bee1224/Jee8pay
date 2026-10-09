<template>
  <!-- 黑名單：以團長為範圍，自己與旗下代理、商戶的提現都會比對 -->
  <div>
    <a-alert type="info" show-icon style="margin-bottom: 12px"
      message="這份名單只對你這一支生效：你、旗下代理與旗下商戶申請提現時，收款帳號或戶名命中就會被直接拒絕。平台另有一份全站名單。" />
    <a-form layout="inline" style="margin-bottom: 12px">
      <a-form-item>
        <a-select v-model:value="vdata.form.listType" style="width: 130px">
          <a-select-option value="BANK_ACCOUNT">收款帳號</a-select-option>
          <a-select-option value="ACCOUNT_NAME">戶名</a-select-option>
          <a-select-option value="PHONE">電話</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item><a-input v-model:value="vdata.form.listValue" placeholder="內容" style="width: 200px" /></a-form-item>
      <a-form-item><a-input v-model:value="vdata.form.remark" placeholder="備註（選填）" style="width: 200px" /></a-form-item>
      <a-form-item><a-button type="primary" @click="add">加入黑名單</a-button></a-form-item>
    </a-form>
    <a-table :columns="columns" :data-source="vdata.records" size="small" row-key="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">{{ TYPES[record.listType] || record.listType }}</template>
        <template v-if="column.key === 'op'"><a-button type="link" danger @click="remove(record)">移除</a-button></template>
      </template>
      <template #emptyText>黑名單是空的</template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
defineProps({ me: { type: Object, required: true } })
const BASE = API_URL_AGENT_PORTAL + '/branch/blacklist'
const TYPES = { BANK_ACCOUNT: '收款帳號', ACCOUNT_NAME: '戶名', PHONE: '電話' }
const columns = [
  { key: 'type', title: '類型', width: 110 },
  { title: '內容', dataIndex: 'listValue' },
  { title: '備註', dataIndex: 'remark' },
  { title: '建立人', dataIndex: 'createdBy' },
  { title: '建立時間', dataIndex: 'createdAt', width: 170 },
  { key: 'op', title: '', width: 80 },
]
const vdata: any = reactive({ records: [], form: { listType: 'BANK_ACCOUNT', listValue: '', remark: '' } })
function load() {
  req.list(BASE, {}).then((res) => (vdata.records = res || []))
}
function add() {
  if (!vdata.form.listValue.trim()) {
    $infoBox.message.warning('請填寫黑名單內容')
    return
  }
  req.add(BASE, vdata.form).then(() => {
    $infoBox.message.success('已加入')
    vdata.form = { listType: vdata.form.listType, listValue: '', remark: '' }
    load()
  })
}
function remove(record) {
  req.delById(BASE, record.id).then(() => {
    $infoBox.message.success('已移除')
    load()
  })
}
load()
</script>
