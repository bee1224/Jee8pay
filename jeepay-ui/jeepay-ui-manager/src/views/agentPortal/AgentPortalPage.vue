<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <a-result v-if="vdata.error" status="info" :title="vdata.error" sub-title="代理帳號由平台在「代理管理 → 代理列表 → 登入帳號」開通。" />
      <template v-else-if="vdata.me">
        <a-descriptions :title="vdata.me.agentName" bordered size="small" :column="{ xs: 1, md: 3 }" style="margin-bottom: 16px">
          <a-descriptions-item label="代理號">{{ vdata.me.agentNo }}</a-descriptions-item>
          <a-descriptions-item label="層級">
            <a-tag :color="vdata.me.agentLevel === 1 ? 'purple' : 'blue'">{{ vdata.me.agentLevel === 1 ? '團長' : '隊長' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="上級代理">{{ vdata.me.parentAgentNo || '—' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 每個區塊對應左側一個選單（route name → section），不再用分頁 -->
        <template v-if="section === 'wallet'">
          <WalletPanel :baseUrl="API_URL_AGENT_PORTAL + '/wallet'" />
          <a-card v-if="vdata.logins.length" size="small" title="最近登入紀錄" style="margin-top: 16px; max-width: 520px">
            <div v-for="l in vdata.logins" :key="l.sysLogId" style="display: flex; justify-content: space-between; font-size: 13px; padding: 2px 0">
              <span>{{ l.createdAt }}</span><span style="color: #888">{{ l.userIp }}</span>
            </div>
          </a-card>
        </template>
        <template v-else-if="section === 'profit'">
          <AgentProfitPanel :baseUrl="API_URL_AGENT_PORTAL + '/profits'" />
        </template>
        <template v-else-if="section === 'mch'">
            <a-button v-if="$access('ENT_AGENT_PORTAL_MCH_ADD')" type="primary" style="margin-bottom: 12px" @click="openMchForm">新增商戶</a-button>
            <a-table :columns="mchColumns" :data-source="vdata.merchants" size="small" row-key="mchNo">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'relation'">
                  <a-tag :color="record.relation === '直屬' ? 'green' : 'blue'">{{ record.relation }}</a-tag>
                </template>
                <template v-if="column.key === 'op'">
                  <template v-if="$access('ENT_AGENT_PORTAL_MCH_EDIT')">
                    <a-button v-if="vdata.me.agentLevel === 1" type="link" @click="openBind(record)">更換歸屬</a-button>
                    <a-button type="link" @click="resetMchPwd(record)">重設密碼</a-button>
                  </template>
                </template>
              </template>
              <template #emptyText>尚無商戶</template>
            </a-table>
        </template>
        <template v-else-if="section === 'sub'">
            <a-alert v-if="vdata.me.agentLevel !== 1" type="info" show-icon message="只有團長才有旗下代理。" />
            <a-button v-if="vdata.me.agentLevel === 1 && $access('ENT_AGENT_PORTAL_SUB_ADD')" type="primary" style="margin-bottom: 12px" @click="openSubForm">新增旗下代理</a-button>
            <a-table v-else :columns="subColumns" :data-source="vdata.me.subAgents || []" size="small" row-key="agentNo">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'state'">
                  <a-badge :status="record.state === 0 ? 'error' : 'processing'" :text="record.state === 0 ? '停用' : '啟用'" />
                </template>
              </template>
              <template #emptyText>尚無旗下代理</template>
            </a-table>
        </template>
        <template v-else-if="section === 'orders'">
            <a-form layout="inline" style="margin-bottom: 12px">
              <a-form-item><a-range-picker v-model:value="vdata.order.range" value-format="YYYY-MM-DD" /></a-form-item>
              <a-form-item><a-input v-model:value="vdata.order.unionOrderId" placeholder="支付／商戶／渠道訂單號" allow-clear style="width: 220px" /></a-form-item>
              <a-form-item><a-input v-model:value="vdata.order.mchNo" placeholder="商戶號" allow-clear style="width: 150px" /></a-form-item>
              <a-form-item>
                <a-select v-model:value="vdata.order.state" placeholder="支付狀態" allow-clear style="width: 130px">
                  <a-select-option v-for="(v, k) in ORDER_STATES" :key="k" :value="k">{{ v.text }}</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item><a-button type="primary" @click="searchOrders">搜尋</a-button></a-form-item>
            </a-form>
            <a-table :columns="orderColumns" :data-source="vdata.order.records" size="small" row-key="payOrderId" :scroll="{ x: 900 }"
              :pagination="{ current: vdata.order.page, pageSize: 20, total: vdata.order.total, showTotal: (t) => `共${t}筆`, onChange: (p) => { vdata.order.page = p; loadOrders() } }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">{{ (record.amount / 100).toFixed(2) }}</template>
                <template v-if="column.key === 'state'"><a-tag :color="(ORDER_STATES[record.state] || {}).color">{{ (ORDER_STATES[record.state] || {}).text || record.state }}</a-tag></template>
              </template>
              <template #emptyText>這段期間沒有訂單（只顯示自己與旗下代理直屬商戶的訂單）</template>
            </a-table>
        </template>
        <template v-else-if="section === 'oplog'">
            <a-alert type="info" show-icon style="margin-bottom: 12px" message="這裡列出旗下代理帳號做過的操作（新增商戶、申請提現、設定收款帳戶等）。" />
            <a-table :columns="logColumns" :data-source="vdata.log.records" size="small" row-key="sysLogId"
              :pagination="{ current: vdata.log.page, pageSize: 20, total: vdata.log.total, showTotal: (t) => `共${t}筆`, onChange: (p) => { vdata.log.page = p; loadLogs() } }">
              <template #emptyText>旗下代理還沒有任何操作紀錄</template>
            </a-table>
        </template>
        <template v-else-if="section === 'fee'">
            <a-card v-if="canEditSub" size="small" title="設定費率" style="margin-bottom: 12px">
              <a-form layout="inline">
                <a-form-item><a-select v-model:value="vdata.sub.targetKey" placeholder="設定對象" style="width: 240px" show-search option-filter-prop="label"
                  :options="feeTargetOptions" @change="onFeeTarget" /></a-form-item>
                <a-form-item><a-select v-model:value="vdata.sub.layer" style="width: 120px" :options="feeLayerOptions" /></a-form-item>
                <a-form-item><a-select v-model:value="vdata.sub.wayCode" placeholder="支付方式" style="width: 180px"
                  :options="(vdata.me.payWays || []).map((w) => ({ value: w.wayCode, label: w.wayName }))" /></a-form-item>
                <a-form-item><a-input-number v-model:value="vdata.sub.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" /></a-form-item>
                <a-form-item><a-input-number v-model:value="vdata.sub.fixedYuan" :min="0" :precision="2" addon-after="元" /></a-form-item>
                <a-form-item><a-button type="primary" @click="saveSubRule">儲存</a-button></a-form-item>
                <a-form-item><a-button @click="openTemplate">套用範本</a-button></a-form-item>
              </a-form>
              <div style="margin-top: 6px; color: #888; font-size: 12px">可設定自己的團長費、旗下代理的隊長費，或對單一商戶覆寫。平臺費與渠道費由平台設定。儲存後若任何旗下商戶的手續費合計超過其商戶費率，系統會拒絕並提示。</div>
            </a-card>
            <a-table :columns="feeColumns" :data-source="vdata.rules" size="small" row-key="ruleId">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'target'"><a-tag>{{ record.targetType === 'MCH' ? '商戶' : '代理' }}</a-tag>{{ record.targetId }}</template>
                <template v-if="column.key === 'layer'">{{ { SR_AGENT: '團長費', AGENT: '隊長費' }[record.layer] }}</template>
                <template v-if="column.key === 'value'">{{ (Number(record.rate) * 100).toFixed(4) }}% + {{ (record.fixedAmount / 100).toFixed(2) }} 元</template>
              </template>
              <template #emptyText>尚未設定費率</template>
            </a-table>
        </template>
        <template v-else-if="section === 'channels'">
          <a-alert type="info" show-icon style="margin-bottom: 12px" message="這裡列出平台派發給你的第三方支付渠道。要新增渠道或更換金鑰，請把資料交給平台處理。" />
          <a-table :columns="channelColumns" :data-source="vdata.channels" :pagination="false" size="small" row-key="accountId">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'ifName'">{{ record.ifName || record.ifCode }}</template>
              <template v-if="column.key === 'scope'">
                {{ (record.scopes || []).length ? '只限：' + record.scopes.map((s) => s.agentName || s.agentNo).join('、') : '全部可用' }}
              </template>
              <template v-if="column.key === 'owned'">
                <a-tag :color="record.owned ? 'green' : 'orange'">{{ record.owned ? '自己的' : '共用' }}</a-tag>
              </template>
              <template v-if="column.key === 'state'">
                <a-badge :status="record.state === 0 ? 'error' : 'processing'" :text="record.state === 0 ? '停用' : '啟用'" />
              </template>
            </template>
            <template #emptyText>平台尚未派發渠道給你</template>
          </a-table>
        </template>
        <BranchWallets v-else-if="section === 'branchWallets'" :me="vdata.me" />
        <WithdrawAudit v-else-if="section === 'withdrawAudit'" :me="vdata.me" />
        <BranchReport v-else-if="section === 'report'" :me="vdata.me" />
        <BranchRoutes v-else-if="section === 'routes'" :me="vdata.me" :merchants="vdata.merchants" />
        <BranchBlacklist v-else-if="section === 'blacklist'" :me="vdata.me" />
        <BranchBrand v-else-if="section === 'brand'" :me="vdata.me" @saved="loadMe" />
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
        <a-form-item v-if="vdata.me && vdata.me.agentLevel === 1" label="歸屬代理">
          <a-select v-model:value="vdata.mchForm.data.agentNo" :options="ownerOptions" />
        </a-form-item>
      </a-form>
      <div style="color: #888; font-size: 12px">商戶建立後還需要平台設定支付通道與費率，才能開始收款。</div>
    </a-modal>

    <a-modal v-model:open="vdata.subForm.open" title="新增旗下代理" :confirm-loading="vdata.subForm.saving" ok-text="儲存" cancel-text="取消" @ok="saveSub">
      <a-form layout="vertical">
        <a-form-item label="代理名稱" required><a-input v-model:value="vdata.subForm.data.agentName" /></a-form-item>
        <a-form-item label="聯絡人姓名"><a-input v-model:value="vdata.subForm.data.contactName" /></a-form-item>
        <a-form-item label="聯絡人手機號" required><a-input v-model:value="vdata.subForm.data.contactTel" placeholder="09 開頭 10 碼" /></a-form-item>
        <a-form-item label="登入帳號" required extra="旗下代理登入營運平台用；儲存後系統產生 8 碼隨機密碼並顯示一次。">
          <a-input v-model:value="vdata.subForm.data.loginUsername" placeholder="4～32 碼英文、數字或底線" autocomplete="off" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="vdata.bind.open" :title="'更換歸屬：' + vdata.bind.mchName" ok-text="儲存" cancel-text="取消" :confirm-loading="vdata.bind.saving" @ok="saveBind">
      <a-form layout="vertical">
        <a-form-item label="直屬代理" required><a-select v-model:value="vdata.bind.agentNo" :options="ownerOptions" /></a-form-item>
      </a-form>
      <div style="color: #888; font-size: 12px">更換後新訂單的分潤改歸新的直屬代理；已完成的訂單不受影響。</div>
    </a-modal>

    <a-modal v-model:open="vdata.tpl.open" title="套用費率範本" ok-text="套用" cancel-text="取消" :confirm-loading="vdata.tpl.saving" @ok="applyTemplate">
      <a-form layout="vertical">
        <a-form-item label="範本" required>
          <a-select v-model:value="vdata.tpl.templateId" :options="vdata.tpl.list.map((t) => ({ value: t.templateId, label: `${t.templateName}（${(t.items || []).length} 筆費率）` }))" />
        </a-form-item>
        <a-form-item label="套用到" required>
          <a-radio-group v-model:value="vdata.tpl.targetType" @change="vdata.tpl.targetIds = []">
            <a-radio value="AGENT">代理</a-radio><a-radio value="MCH">商戶</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="對象（可多選）" required>
          <a-select v-model:value="vdata.tpl.targetIds" mode="multiple" option-filter-prop="label"
            :options="vdata.tpl.targetType === 'AGENT' ? agentTargetOptions : mchTargetOptions" />
        </a-form-item>
      </a-form>
      <div style="color: #888; font-size: 12px">範本由平台建立。套用到代理時，只會寫入與該代理層級相符的費率層。</div>
    </a-modal>

    <AgentCredentialModal ref="credentialModal" />
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import AgentProfitPanel from '@/components/AgentProfit/AgentProfitPanel.vue'
import WalletPanel from '@/components/WalletPanel/WalletPanel.vue'
import AgentCredentialModal from '@/views/agent/AgentCredentialModal.vue'
import BranchWallets from './sections/BranchWallets.vue'
import WithdrawAudit from './sections/WithdrawAudit.vue'
import BranchReport from './sections/BranchReport.vue'
import BranchRoutes from './sections/BranchRoutes.vue'
import BranchBlacklist from './sections/BranchBlacklist.vue'
import BranchBrand from './sections/BranchBrand.vue'
import { computed, reactive, ref, watch, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const channelColumns = [
  { title: '渠道名稱', dataIndex: 'accountName' },
  { key: 'ifName', title: '第三方支付' },
  { key: 'owned', title: '歸屬' },
  { key: 'scope', title: '使用範圍' },
  { key: 'state', title: '狀態' },
]
const mchColumns = [
  { title: '商戶號', dataIndex: 'mchNo' },
  { title: '商戶名稱', dataIndex: 'mchName' },
  { title: '直屬代理', dataIndex: 'agentNo' },
  { key: 'relation', title: '關係' },
  { key: 'op', title: '操作', width: 190 },
]
const subColumns = [
  { title: '代理號', dataIndex: 'agentNo' },
  { title: '名稱', dataIndex: 'agentName' },
  { title: '聯絡人', dataIndex: 'contactName' },
  { key: 'state', title: '狀態' },
]
const feeColumns = [
  { title: '支付方式', dataIndex: 'wayCode' },
  { key: 'target', title: '對象' },
  { key: 'layer', title: '費率層' },
  { key: 'value', title: '費率' },
]

const vdata: any = reactive({
  me: null,
  error: '',
  merchants: [],
  channels: [],
  rules: [],
  sub: { targetKey: undefined, targetType: undefined, targetId: undefined, layer: undefined, wayCode: undefined, pct: 0, fixedYuan: 0 },
  logins: [],
  bind: { open: false, saving: false, mchNo: '', mchName: '', agentNo: undefined },
  tpl: { open: false, saving: false, list: [], templateId: undefined, targetType: 'AGENT', targetIds: [] },
  order: { range: [], unionOrderId: '', mchNo: '', state: undefined, records: [], total: 0, page: 1, loaded: false },
  log: { records: [], total: 0, page: 1, loaded: false },
  mchForm: { open: false, saving: false, data: {} },
  subForm: { open: false, saving: false, data: {} },
})

// 左側選單（權限 ID 即 route name）對應的區塊
const SECTIONS = {
  ENT_AGENT_PORTAL_HOME: 'wallet',
  ENT_AGENT_PORTAL_PROFIT: 'profit',
  ENT_AGENT_PORTAL_MCH: 'mch',
  ENT_AGENT_PORTAL_CHANNEL: 'channels',
  ENT_AGENT_PORTAL_SUB: 'sub',
  ENT_AGENT_PORTAL_FEE: 'fee',
  ENT_AGENT_PORTAL_ORDER: 'orders',
  ENT_AGENT_PORTAL_OPLOG: 'oplog',
  ENT_AGENT_PORTAL_BRANCH_WALLET: 'branchWallets',
  ENT_AGENT_PORTAL_WITHDRAW_AUDIT: 'withdrawAudit',
  ENT_AGENT_PORTAL_REPORT: 'report',
  ENT_AGENT_PORTAL_ROUTE: 'routes',
  ENT_AGENT_PORTAL_BLACKLIST: 'blacklist',
  ENT_AGENT_PORTAL_BRAND: 'brand',
}
const route = useRoute()
const section = computed(() => SECTIONS[route.name as string] || 'wallet')

const canEditSub = computed(() => vdata.me && vdata.me.agentLevel === 1 && $access('ENT_AGENT_PORTAL_FEE_EDIT'))

const ORDER_STATES = {
  0: { text: '訂單生成', color: 'blue' },
  1: { text: '支付中', color: 'orange' },
  2: { text: '支付成功', color: 'green' },
  3: { text: '支付失敗', color: 'volcano' },
  4: { text: '已撤銷', color: 'volcano' },
  5: { text: '已退款', color: 'volcano' },
  6: { text: '訂單關閉', color: '' },
}
const orderColumns = [
  { title: '支付訂單號', dataIndex: 'payOrderId', width: 220 },
  { title: '商戶號', dataIndex: 'mchNo', width: 130 },
  { title: '商戶訂單號', dataIndex: 'mchOrderNo' },
  { key: 'amount', title: '金額（元）', width: 110 },
  { key: 'state', title: '狀態', width: 100 },
  { title: '建立時間', dataIndex: 'createdAt', width: 170 },
]
const logColumns = [
  { title: '時間', dataIndex: 'createdAt', width: 180 },
  { title: '操作人', dataIndex: 'userName' },
  { title: '動作', dataIndex: 'methodRemark' },
  { title: 'IP', dataIndex: 'userIp' },
]
const credentialModal = ref()
const ownerOptions = computed(() =>
  vdata.me
    ? [{ value: vdata.me.agentNo, label: `自己（${vdata.me.agentName}）` }].concat(
        (vdata.me.subAgents || []).map((a) => ({ value: a.agentNo, label: `旗下代理：${a.agentName}` }))
      )
    : []
)

function today() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
function loadOrders() {
  const o = vdata.order
  const [start, end] = o.range && o.range.length === 2 ? o.range : [today(), today()]
  req
    .list(API_URL_AGENT_PORTAL + '/orders', {
      pageNumber: o.page,
      pageSize: 20,
      createdStart: `${start} 00:00:00`,
      createdEnd: `${end} 23:59:59`,
      unionOrderId: o.unionOrderId || undefined,
      mchNo: o.mchNo || undefined,
      state: o.state,
    })
    .then((res) => {
      o.records = res.records || []
      o.total = res.total || 0
      o.loaded = true
    })
}
function searchOrders() {
  vdata.order.page = 1
  loadOrders()
}
function loadLogs() {
  req.list(API_URL_AGENT_PORTAL + '/opLogs', { pageNumber: vdata.log.page, pageSize: 20 }).then((res) => {
    vdata.log.records = res.records || []
    vdata.log.total = res.total || 0
    vdata.log.loaded = true
  })
}
function loadChannels() {
  req.list(API_URL_AGENT_PORTAL + '/channels', {}).then((r) => (vdata.channels = r || []))
}
function loadMe() {
  return req.list(API_URL_AGENT_PORTAL + '/me', {}).then((res) => (vdata.me = res))
}
function loadMerchants() {
  req.list(API_URL_AGENT_PORTAL + '/merchants', {}).then((r) => (vdata.merchants = r || []))
}
function openMchForm() {
  vdata.mchForm = { open: true, saving: false, data: { agentNo: vdata.me.agentNo } }
}
function saveMch() {
  const f = vdata.mchForm.data
  if (!f.mchName || !f.mchShortName || !f.contactName || !/^09\d{8}$/.test(f.contactTel || '') || !/^[A-Za-z0-9_]{4,32}$/.test(f.loginUsername || '')) {
    $infoBox.message.warning('請填寫商戶名稱、簡稱、聯絡人、正確的手機號，以及 4～32 碼的登入帳號')
    return
  }
  vdata.mchForm.saving = true
  req
    .add(API_URL_AGENT_PORTAL + '/merchants', f)
    .then((res) => {
      vdata.mchForm.open = false
      loadMerchants()
      credentialModal.value.show({ ...res, subjectLabel: '商戶', loginAt: '商戶平台' })
    })
    .finally(() => (vdata.mchForm.saving = false))
}
function openSubForm() {
  vdata.subForm = { open: true, saving: false, data: {} }
}
function saveSub() {
  const f = vdata.subForm.data
  if (!f.agentName || !/^09\d{8}$/.test(f.contactTel || '') || !/^[A-Za-z0-9_]{4,32}$/.test(f.loginUsername || '')) {
    $infoBox.message.warning('請填寫代理名稱、正確的手機號，以及 4～32 碼的登入帳號')
    return
  }
  vdata.subForm.saving = true
  req
    .add(API_URL_AGENT_PORTAL + '/subAgents', f)
    .then((res) => {
      vdata.subForm.open = false
      loadMe()
      credentialModal.value.show({ ...res, subjectLabel: '旗下代理', loginAt: '營運平台' })
    })
    .finally(() => (vdata.subForm.saving = false))
}

function loadRules() {
  req.list(API_URL_AGENT_PORTAL + '/branch/feeRules', {}).then((r) => (vdata.rules = r || []))
}

// 設定對象：自己（團長費）、旗下代理（隊長費）、商戶（逐一覆寫）
const agentTargetOptions = computed(() =>
  vdata.me
    ? [{ value: vdata.me.agentNo, label: `自己（${vdata.me.agentName}）` }].concat(
        (vdata.me.subAgents || []).map((a) => ({ value: a.agentNo, label: `旗下代理：${a.agentName}` }))
      )
    : []
)
const mchTargetOptions = computed(() =>
  vdata.merchants.map((m) => ({ value: m.mchNo, label: `商戶：${m.mchName || ''}（${m.mchNo}）` }))
)
const feeTargetOptions = computed(() =>
  agentTargetOptions.value.map((o) => ({ value: 'AGENT:' + o.value, label: o.label }))
    .concat(mchTargetOptions.value.map((o) => ({ value: 'MCH:' + o.value, label: o.label })))
)
const feeLayerOptions = computed(() => {
  const s = vdata.sub
  const all = [
    { value: 'SR_AGENT', label: '團長費' },
    { value: 'AGENT', label: '隊長費' },
  ]
  if (s.targetType === 'MCH' || !s.targetType) return all
  return all.filter((l) => l.value === (s.targetId === vdata.me.agentNo ? 'SR_AGENT' : 'AGENT'))
})
function onFeeTarget(key) {
  const [type, id] = String(key).split(':')
  vdata.sub.targetType = type
  vdata.sub.targetId = id
  // 代理對象只有一種費率層可設，直接帶入；商戶覆寫才需要自己選
  vdata.sub.layer = type === 'AGENT' ? (id === vdata.me.agentNo ? 'SR_AGENT' : 'AGENT') : undefined
}
function saveSubRule() {
  const s = vdata.sub
  if (!s.targetId || !s.layer || !s.wayCode) {
    $infoBox.message.warning('請選擇設定對象、費率層與支付方式')
    return
  }
  req
    .add(API_URL_AGENT_PORTAL + '/branch/feeRules', {
      targetType: s.targetType,
      targetId: s.targetId,
      layer: s.layer,
      wayCode: s.wayCode,
      rate: (Number(s.pct || 0) / 100).toFixed(6),
      fixedAmount: Math.round(Number(s.fixedYuan || 0) * 100),
    })
    .then(() => {
      $infoBox.message.success('已儲存')
      loadRules()
    })
}
function openTemplate() {
  vdata.tpl = { open: true, saving: false, list: [], templateId: undefined, targetType: 'AGENT', targetIds: [] }
  req.list(API_URL_AGENT_PORTAL + '/branch/feeTemplates', {}).then((r) => (vdata.tpl.list = r || []))
}
function applyTemplate() {
  const t = vdata.tpl
  if (!t.templateId || !t.targetIds.length) {
    $infoBox.message.warning('請選擇範本與套用對象')
    return
  }
  t.saving = true
  req
    .add(`${API_URL_AGENT_PORTAL}/branch/feeTemplates/${t.templateId}/apply`, { targetType: t.targetType, targetIds: t.targetIds })
    .then((res) => {
      $infoBox.message.success(`已套用 ${res.applied} 筆費率` + (res.skipped ? `，略過 ${res.skipped} 筆層級不符的` : ''))
      t.open = false
      loadRules()
    })
    .finally(() => (t.saving = false))
}
function openBind(record) {
  vdata.bind = { open: true, saving: false, mchNo: record.mchNo, mchName: record.mchName || record.mchNo, agentNo: record.agentNo }
}
function saveBind() {
  const b = vdata.bind
  b.saving = true
  req
    .updateById(API_URL_AGENT_PORTAL + '/branch/merchants', b.mchNo + '/binding', { agentNo: b.agentNo })
    .then(() => {
      $infoBox.message.success('已更換')
      b.open = false
      loadMerchants()
    })
    .finally(() => (b.saving = false))
}
function resetMchPwd(record) {
  $infoBox.confirmPrimary('重設商戶密碼？', `商戶 ${record.mchName || record.mchNo} 的登入密碼會換成新的隨機密碼，舊密碼立即失效。`, () => {
    req.add(`${API_URL_AGENT_PORTAL}/branch/merchants/${record.mchNo}/resetPassword`, {}).then((res) => {
      credentialModal.value.show({ ...res, subjectLabel: '商戶', loginAt: '商戶平台' })
    })
  })
}

// 代理號由後端依登入帳號決定；非代理帳號（例如平台超管）會收到「此頁僅供代理帳號使用」
loadMe()
  .then(() => {
    loadMerchants()
    loadRules()
    req.list(API_URL_AGENT_PORTAL + '/branch/loginLogs', {}).then((r) => (vdata.logins = r || []))
    // 訂單與操作紀錄只在進到該選單時才載入
    watch(
      section,
      (sec) => {
        if (sec === 'orders' && !vdata.order.loaded) loadOrders()
        if (sec === 'oplog' && !vdata.log.loaded) loadLogs()
        if (sec === 'channels') loadChannels()
      },
      { immediate: true }
    )
  })
  .catch((err) => {
    vdata.error = (err && err.msg) || '此頁僅供代理帳號使用'
  })
</script>
