<template>
  <page-header-wrapper>
    <a-card>
      <div class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <jeepay-text-up :placeholder="'代理號'" v-model:value="vdata.searchData.agentNo" />
            <jeepay-text-up :placeholder="'代理名稱'" v-model:value="vdata.searchData.agentName" />
            <a-select v-model:value="vdata.searchData.agentLevel" placeholder="代理層級" class="table-head-layout">
              <a-select-option value="">全部層級</a-select-option>
              <a-select-option value="1">高級代理</a-select-option>
              <a-select-option value="2">一般代理</a-select-option>
            </a-select>
            <a-select v-model:value="vdata.searchData.state" placeholder="狀態" class="table-head-layout">
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="0">停用</a-select-option>
              <a-select-option value="1">啟用</a-select-option>
            </a-select>
            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="queryFunc" :loading="vdata.btnLoading">搜尋</a-button>
              <a-button style="margin-left: 8px" @click="() => (vdata.searchData = {})">重置</a-button>
            </span>
          </div>
        </a-form>
      </div>

      <JeepayTable
        @btnLoadClose="vdata.btnLoading = false"
        ref="infoTable"
        :initData="true"
        :reqTableDataFunc="reqTableDataFunc"
        :tableColumns="tableColumns"
        :searchData="vdata.searchData"
        rowKey="agentNo"
      >
        <template #opRow>
          <a-button v-if="$access('ENT_AGENT_INFO_ADD')" type="primary" @click="addFunc" style="margin-bottom: 30px">
            新建代理
          </a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'agentName'">
            <b>{{ record.agentName }}</b>
          </template>
          <template v-if="column.key === 'agentLevel'">
            <a-tag :color="record.agentLevel === 1 ? 'purple' : 'blue'">
              {{ record.agentLevel === 1 ? '高級代理' : '一般代理' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'parentAgentNo'">
            {{ record.parentAgentNo || '—' }}
          </template>
          <template v-if="column.key === 'state'">
            <a-badge :status="record.state === 0 ? 'error' : 'processing'" :text="record.state === 0 ? '停用' : '啟用'" />
          </template>
          <template v-if="column.key === 'op'">
            <JeepayTableColumns>
              <a-button type="link" v-if="$access('ENT_AGENT_INFO_EDIT')" @click="editFunc(record.agentNo)">修改</a-button>
              <a-button type="link" @click="showMchList(record)">旗下商戶</a-button>
              <a-button type="link" v-if="$access('ENT_AGENT_INFO_DEL')" style="color: red" @click="delFunc(record.agentNo)">
                刪除
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
    </a-card>

    <AgentAddOrEdit ref="infoAddOrEdit" :callbackFunc="searchFunc" />

    <a-modal v-model:open="vdata.mchModal.open" :title="'旗下商戶：' + vdata.mchModal.agentName" :footer="null" width="640px">
      <a-table :columns="mchColumns" :data-source="vdata.mchModal.records" :pagination="false" size="small" row-key="mchNo">
        <template #emptyText>尚未綁定商戶（請至「商戶列表 → 代理綁定」設定）</template>
      </a-table>
    </a-modal>
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_AGENT_MCH_RELA, req } from '@/api/manage'
import AgentAddOrEdit from './AgentAddOrEdit.vue'
import { reactive, ref, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const tableColumns = [
  { key: 'agentName', title: '代理名稱', width: '200px', fixed: 'left' },
  { key: 'agentNo', title: '代理號', dataIndex: 'agentNo' },
  { key: 'agentLevel', title: '層級' },
  { key: 'parentAgentNo', title: '上級代理' },
  { key: 'contactName', title: '聯絡人', dataIndex: 'contactName' },
  { key: 'state', title: '狀態' },
  { key: 'createdAt', dataIndex: 'createdAt', title: '建立日期' },
  { key: 'op', title: '操作', width: '240px', fixed: 'right', align: 'center' },
]

const mchColumns = [
  { title: '商戶號', dataIndex: 'mchNo' },
  { title: '關係', dataIndex: 'relation' },
  { title: '最後修改', dataIndex: 'updatedAt' },
]

const infoTable = ref()
const infoAddOrEdit = ref()

const vdata: any = reactive({
  btnLoading: false,
  searchData: {},
  mchModal: { open: false, agentName: '', records: [] },
})

function queryFunc() {
  vdata.btnLoading = true
  infoTable.value.refTable(true)
}
function reqTableDataFunc(params) {
  return req.list(API_URL_AGENT_INFO, params)
}
function searchFunc() {
  infoTable.value.refTable(true)
}
function addFunc() {
  infoAddOrEdit.value.show()
}
function editFunc(agentNo) {
  infoAddOrEdit.value.show(agentNo)
}
function delFunc(agentNo) {
  $infoBox.confirmDanger('確認刪除？', '需先移除旗下一般代理與商戶綁定；該代理的費率規則會一併刪除並留下紀錄', () => {
    req.delById(API_URL_AGENT_INFO, agentNo).then(() => {
      infoTable.value.refTable(false)
      $infoBox.message.success('刪除成功')
    })
  })
}
function showMchList(record) {
  vdata.mchModal.agentName = record.agentName
  vdata.mchModal.records = []
  vdata.mchModal.open = true
  Promise.all([
    req.list(API_URL_AGENT_MCH_RELA, { agentNo: record.agentNo, pageSize: -1 }),
    req.list(API_URL_AGENT_MCH_RELA, { referrerAgentNo: record.agentNo, pageSize: -1 }),
  ]).then(([direct, referred]) => {
    const rows = (direct.records || []).map((r) => ({ ...r, relation: '直屬代理' }))
    ;(referred.records || []).forEach((r) => rows.push({ ...r, relation: '推薦人' }))
    vdata.mchModal.records = rows
  })
}
</script>
