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
              <a-button type="link" v-if="$access('ENT_AGENT_PROFIT')" @click="showProfit(record)">分潤</a-button>
              <a-button type="link" v-if="$access('ENT_AGENT_ACCOUNT')" @click="showAccounts(record)">登入帳號</a-button>
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

    <a-modal v-model:open="vdata.profit.open" :title="'分潤：' + vdata.profit.agentName" :footer="null" width="900px" destroyOnClose>
      <AgentProfitPanel v-if="vdata.profit.open" :baseUrl="`${API_URL_AGENT_INFO}/${vdata.profit.agentNo}/profits`" />
    </a-modal>

    <a-modal v-model:open="vdata.account.open" :title="'登入帳號：' + vdata.account.agentName" :footer="null" width="720px">
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 12px"
        message="代理帳號登入營運平台後只會看到「代理後台」。初始密碼為系統預設密碼，請通知代理首次登入後立即修改；停用或刪除帳號請至「系統管理 → 操作員」。"
      />
      <a-table :columns="accountColumns" :data-source="vdata.account.records" :pagination="false" size="small" row-key="sysUserId" style="margin-bottom: 16px">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">{{ record.state === 1 ? '啟用' : '停用' }}</template>
        </template>
        <template #emptyText>尚未開通</template>
      </a-table>
      <a-form layout="inline">
        <a-form-item label="登入帳號"><a-input v-model:value="vdata.account.form.loginUsername" style="width: 140px" /></a-form-item>
        <a-form-item label="姓名"><a-input v-model:value="vdata.account.form.realname" style="width: 120px" /></a-form-item>
        <a-form-item label="手機"><a-input v-model:value="vdata.account.form.telphone" placeholder="09 開頭 10 碼" style="width: 140px" /></a-form-item>
        <a-form-item><a-button type="primary" @click="createAccount">開通</a-button></a-form-item>
      </a-form>
    </a-modal>
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_AGENT_MCH_RELA, req } from '@/api/manage'
import AgentAddOrEdit from './AgentAddOrEdit.vue'
import AgentProfitPanel from '@/components/AgentProfit/AgentProfitPanel.vue'
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

const accountColumns = [
  { title: '登入帳號', dataIndex: 'loginUsername' },
  { title: '姓名', dataIndex: 'realname' },
  { title: '手機', dataIndex: 'telphone' },
  { key: 'state', title: '狀態' },
  { title: '建立時間', dataIndex: 'createdAt' },
]

const infoTable = ref()
const infoAddOrEdit = ref()

const vdata: any = reactive({
  btnLoading: false,
  searchData: {},
  mchModal: { open: false, agentName: '', records: [] },
  profit: { open: false, agentNo: '', agentName: '' },
  account: { open: false, agentNo: '', agentName: '', records: [], form: {} },
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
function showProfit(record) {
  vdata.profit = { open: true, agentNo: record.agentNo, agentName: record.agentName }
}
function loadAccounts() {
  req.list(`${API_URL_AGENT_INFO}/${vdata.account.agentNo}/accounts`, {}).then((res) => {
    vdata.account.records = res || []
  })
}
function showAccounts(record) {
  vdata.account = { open: true, agentNo: record.agentNo, agentName: record.agentName, records: [], form: {} }
  loadAccounts()
}
function createAccount() {
  const f = vdata.account.form
  if (!f.loginUsername || !f.realname || !/^09\d{8}$/.test(f.telphone || '')) {
    $infoBox.message.warning('請填寫登入帳號、姓名與正確的手機號（09 開頭 10 碼）')
    return
  }
  req.add(`${API_URL_AGENT_INFO}/${vdata.account.agentNo}/accounts`, f).then(() => {
    $infoBox.message.success('已開通，初始密碼為系統預設密碼')
    vdata.account.form = {}
    loadAccounts()
  })
}
</script>
