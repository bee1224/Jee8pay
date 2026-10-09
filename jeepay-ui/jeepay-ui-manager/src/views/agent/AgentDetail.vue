<template>
  <!-- 團長詳情（ADR-0012）：上帝一律從團長點進來，看他的商戶、渠道與隊長 -->
  <page-header-wrapper>
    <a-card :bordered="false">
      <a-button type="link" style="padding-left: 0; margin-bottom: 8px" @click="router.push('/agents')">← 回團長列表</a-button>
      <a-result v-if="vdata.error" status="warning" :title="vdata.error" />
      <template v-else-if="vdata.agent">
        <a-descriptions bordered size="small" :column="{ xs: 1, md: 3 }" style="margin-bottom: 16px">
          <template #title>
            {{ vdata.agent.agentName }}
            <a-tag color="purple" style="margin-left: 8px">團長</a-tag>
            <a-tag v-if="vdata.agent.isHouse === 1" color="gold">平台直屬</a-tag>
          </template>
          <a-descriptions-item label="代理號">{{ vdata.agent.agentNo }}</a-descriptions-item>
          <a-descriptions-item label="聯絡人">{{ vdata.agent.contactName || '—' }} {{ vdata.agent.contactTel || '' }}</a-descriptions-item>
          <a-descriptions-item label="狀態">
            <a-badge :status="vdata.agent.state === 0 ? 'error' : 'processing'" :text="vdata.agent.state === 0 ? '停用' : '啟用'" />
          </a-descriptions-item>
        </a-descriptions>

        <a-tabs v-model:activeKey="vdata.tab">
          <a-tab-pane v-if="$access('ENT_MCH_LIST')" key="mch" :tab="`商戶（${vdata.merchants.length}）`">
            <a-button v-if="$access('ENT_MCH_INFO_ADD')" type="primary" style="margin-bottom: 12px" @click="openMchForm">新增商戶</a-button>
            <a-table :columns="mchColumns" :data-source="vdata.merchants" :pagination="false" size="small" row-key="mchNo">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'owner'">
                  <a-tag :color="record.agentNo === vdata.agent.agentNo ? 'green' : 'blue'">{{ ownerName(record.agentNo) }}</a-tag>
                </template>
                <template v-if="column.key === 'mchState'">
                  <a-badge :status="record.mchState === 0 ? 'error' : 'processing'" :text="record.mchState === 0 ? '停用' : '啟用'" />
                </template>
                <template v-if="column.key === 'op'">
                  <a-button v-if="$access('ENT_MCH_INFO_EDIT')" type="link" @click="mchEdit.show(record.mchNo)">修改</a-button>
                  <a-button v-if="$access('ENT_MCH_APP_CONFIG')" type="link" @click="router.push({ path: '/apps', query: { mchNo: record.mchNo } })">應用設定</a-button>
                  <a-button v-if="$access('ENT_MCH_AGENT_BIND')" type="link" @click="mchBind.show(record.mchNo)">更換歸屬</a-button>
                  <a-button v-if="$access('ENT_MCH_INFO_DEL')" type="link" style="color: red" @click="removeMch(record)">刪除</a-button>
                </template>
              </template>
              <template #emptyText>這位團長還沒有商戶</template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-if="$access('ENT_CHANNEL_ACCOUNT_LIST')" key="channel" tab="渠道">
            <ChannelAccountPanel :sr-agent-no="vdata.agent.agentNo" :agent-name="vdata.agent.agentName" />
          </a-tab-pane>
          <a-tab-pane key="sub" :tab="`隊長（${vdata.subAgents.length}）`">
            <a-table :columns="subColumns" :data-source="vdata.subAgents" :pagination="false" size="small" row-key="agentNo">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'state'">
                  <a-badge :status="record.state === 0 ? 'error' : 'processing'" :text="record.state === 0 ? '停用' : '啟用'" />
                </template>
                <template v-if="column.key === 'mchCount'">{{ vdata.merchants.filter((m) => m.agentNo === record.agentNo).length }}</template>
              </template>
              <template #emptyText>這位團長還沒有隊長（到「團長列表」按「新建代理」，層級選隊長）</template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-card>

    <a-modal v-model:open="vdata.mchForm.open" title="新增商戶" :confirm-loading="vdata.mchForm.saving" ok-text="儲存" cancel-text="取消" @ok="saveMch">
      <a-form layout="vertical">
        <a-form-item label="商戶名稱" required><a-input v-model:value="vdata.mchForm.data.mchName" /></a-form-item>
        <a-form-item label="商戶簡稱" required><a-input v-model:value="vdata.mchForm.data.mchShortName" /></a-form-item>
        <a-form-item label="聯絡人姓名" required><a-input v-model:value="vdata.mchForm.data.contactName" /></a-form-item>
        <a-form-item label="聯絡人手機號" required><a-input v-model:value="vdata.mchForm.data.contactTel" placeholder="09 開頭 10 碼" /></a-form-item>
        <a-form-item label="商戶登入帳號" required extra="商戶登入商戶平台用；儲存後系統產生 8 碼隨機密碼並顯示一次。">
          <a-input v-model:value="vdata.mchForm.data.loginUsername" placeholder="4～32 碼英文、數字或底線" autocomplete="off" />
        </a-form-item>
        <a-form-item label="歸屬" extra="直接歸在團長底下，或歸給他的某位隊長。">
          <a-select v-model:value="vdata.mchForm.data.targetAgentNo" :options="ownerOptions" />
        </a-form-item>
      </a-form>
    </a-modal>

    <MchAddOrEdit ref="mchEdit" :callback-func="loadMerchants" />
    <MchAgentBind ref="mchBind" :callback-func="loadMerchants" />
    <AgentCredentialModal ref="credentialModal" />
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_MCH_LIST, req, reqLoad } from '@/api/manage'
import ChannelAccountPanel from './ChannelAccountPanel.vue'
import AgentCredentialModal from './AgentCredentialModal.vue'
import MchAddOrEdit from '@/views/mch/AddOrEdit.vue'
import MchAgentBind from '@/views/mch/MchAgentBind.vue'
import { computed, reactive, ref, watch, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const route = useRoute()
const router = useRouter()
const mchEdit = ref()
const mchBind = ref()
const credentialModal = ref()

const mchColumns = [
  { title: '商戶名稱', dataIndex: 'mchName' },
  { title: '商戶號', dataIndex: 'mchNo' },
  { key: 'owner', title: '歸屬' },
  { key: 'mchState', title: '狀態' },
  { key: 'op', title: '操作', width: '300px', align: 'center' },
]
const subColumns = [
  { title: '隊長名稱', dataIndex: 'agentName' },
  { title: '代理號', dataIndex: 'agentNo' },
  { title: '聯絡人', dataIndex: 'contactName' },
  { key: 'mchCount', title: '商戶數' },
  { key: 'state', title: '狀態' },
]

const vdata: any = reactive({
  error: '',
  tab: 'mch',
  agent: null,
  merchants: [],
  subAgents: [],
  mchForm: { open: false, saving: false, data: {} },
})

const ownerOptions = computed(() =>
  vdata.agent
    ? [
        { value: vdata.agent.agentNo, label: `團長 ${vdata.agent.agentName}` },
        ...vdata.subAgents.filter((a) => a.state === 1).map((a) => ({ value: a.agentNo, label: `隊長 ${a.agentName}` })),
      ]
    : [],
)

function ownerName(agentNo) {
  if (vdata.agent && agentNo === vdata.agent.agentNo) return '團長直屬'
  const sub = vdata.subAgents.find((a) => a.agentNo === agentNo)
  return sub ? `隊長 ${sub.agentName}` : agentNo
}

function loadMerchants() {
  if (!$access('ENT_MCH_LIST')) return
  req.list(`${API_URL_AGENT_INFO}/${vdata.agent.agentNo}/merchants`, {}).then((res) => (vdata.merchants = res || []))
}

function load(agentNo) {
  vdata.error = ''
  vdata.agent = null
  vdata.merchants = []
  vdata.subAgents = []
  if (!agentNo) {
    vdata.error = '請從「團長列表」點選一位團長'
    return
  }
  req.getById(API_URL_AGENT_INFO, agentNo).then((res) => {
    if (res.agentLevel !== 1) {
      vdata.error = '這不是團長，請從「團長列表」點選一位團長'
      return
    }
    vdata.agent = res
    loadMerchants()
    req.list(API_URL_AGENT_INFO, { parentAgentNo: agentNo, pageSize: -1 }).then((r) => (vdata.subAgents = r.records || []))
  })
}

function openMchForm() {
  vdata.mchForm = { open: true, saving: false, data: { targetAgentNo: vdata.agent.agentNo } }
}

function saveMch() {
  const f = vdata.mchForm.data
  if (!f.mchName || !f.mchShortName || !f.contactName || !/^09\d{8}$/.test(f.contactTel || '') || !/^[A-Za-z0-9_]{4,32}$/.test(f.loginUsername || '')) {
    $infoBox.message.warning('請填寫商戶名稱、簡稱、聯絡人、正確的手機號，以及 4～32 碼的登入帳號')
    return
  }
  vdata.mchForm.saving = true
  req
    .add(`${API_URL_AGENT_INFO}/${vdata.agent.agentNo}/merchants`, f)
    .then((res) => {
      vdata.mchForm.open = false
      loadMerchants()
      credentialModal.value.show({ ...res, subjectLabel: '商戶', loginAt: '商戶平台' })
    })
    .finally(() => (vdata.mchForm.saving = false))
}

function removeMch(record) {
  $infoBox.confirmDanger('確認刪除？', '該操作將刪除商戶下所有設定及用戶資訊', () => {
    reqLoad.delById(API_URL_MCH_LIST, record.mchNo).then(() => {
      $infoBox.message.success('刪除成功')
      loadMerchants()
    })
  })
}

watch(() => route.query.agentNo, (v) => route.path === '/agents/detail' && load(v as string), { immediate: true })
</script>
