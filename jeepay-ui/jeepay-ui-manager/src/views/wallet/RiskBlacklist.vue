<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>風控黑名單</template>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
        message="提現時比對收款帳號與戶名，命中即拒絕。範圍選「全平台」對所有人生效；選團長則只對該代理轄下的代理與商戶生效。"
      />
      <a-form v-if="$access('ENT_RISK_BLACKLIST_EDIT')" layout="inline" style="margin-bottom: 16px">
        <a-form-item>
          <a-select v-model:value="vdata.form.listType" style="width: 130px">
            <a-select-option v-for="(n, k) in TYPE_NAMES" :key="k" :value="k">{{ n }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item><a-input v-model:value="vdata.form.listValue" placeholder="帳號／戶名／電話" /></a-form-item>
        <a-form-item>
          <a-select v-model:value="vdata.form.scope" style="width: 220px" show-search option-filter-prop="label" :options="vdata.scopeOptions" />
        </a-form-item>
        <a-form-item><a-input v-model:value="vdata.form.remark" placeholder="備註" /></a-form-item>
        <a-form-item><a-button type="primary" @click="addFunc">加入黑名單</a-button></a-form-item>
      </a-form>
      <a-table :columns="columns" :data-source="vdata.records" size="small" row-key="id"
        :pagination="{ current: vdata.page, pageSize: 20, total: vdata.total, onChange: (p) => { vdata.page = p; load() } }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'listType'">{{ TYPE_NAMES[record.listType] }}</template>
          <template v-if="column.key === 'scope'">{{ record.scope === 'GLOBAL' ? '全平台' : '代理 ' + record.scope }}</template>
          <template v-if="column.key === 'op'">
            <a-button v-if="$access('ENT_RISK_BLACKLIST_EDIT')" type="link" danger @click="delFunc(record)">移除</a-button>
          </template>
        </template>
        <template #emptyText>尚無黑名單</template>
      </a-table>
    </a-card>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_RISK_BLACKLIST, req } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const TYPE_NAMES = { BANK_ACCOUNT: '收款帳號', ACCOUNT_NAME: '戶名', PHONE: '電話' }
const columns = [
  { key: 'listType', title: '類型' },
  { title: '內容', dataIndex: 'listValue' },
  { key: 'scope', title: '範圍' },
  { title: '備註', dataIndex: 'remark' },
  { title: '建立者', dataIndex: 'createdBy' },
  { title: '建立時間', dataIndex: 'createdAt' },
  { key: 'op', title: '', width: '80px' },
]

const vdata: any = reactive({
  records: [],
  total: 0,
  page: 1,
  scopeOptions: [{ value: 'GLOBAL', label: '全平台' }],
  form: { listType: 'BANK_ACCOUNT', listValue: '', scope: 'GLOBAL', remark: '' },
})

function load() {
  req.list(API_URL_RISK_BLACKLIST, { pageNumber: vdata.page, pageSize: 20 }).then((res) => {
    vdata.records = res.records || []
    vdata.total = res.total || 0
  })
}
load()
if ($access('ENT_AGENT_LIST')) {
  req.list(API_URL_AGENT_INFO, { agentLevel: 1, pageSize: -1 }).then((res) => {
    vdata.scopeOptions = [{ value: 'GLOBAL', label: '全平台' }].concat(
      (res.records || []).map((a) => ({ value: a.agentNo, label: `團長 ${a.agentName}（${a.agentNo}）` })),
    )
  })
}

function addFunc() {
  if (!vdata.form.listValue) {
    $infoBox.message.warning('請填寫內容')
    return
  }
  req.add(API_URL_RISK_BLACKLIST, vdata.form).then(() => {
    $infoBox.message.success('已加入')
    vdata.form.listValue = ''
    vdata.form.remark = ''
    load()
  })
}
function delFunc(record) {
  $infoBox.confirmDanger('確認移除？', '', () => {
    req.delById(API_URL_RISK_BLACKLIST, record.id).then(() => {
      $infoBox.message.success('已移除')
      load()
    })
  })
}
</script>
