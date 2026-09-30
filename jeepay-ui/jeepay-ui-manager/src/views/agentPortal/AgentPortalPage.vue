<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <a-result v-if="vdata.error" status="info" :title="vdata.error" sub-title="代理帳號由平台在「代理管理 → 代理列表 → 登入帳號」開通。" />
      <template v-else-if="vdata.me">
        <a-descriptions :title="vdata.me.agentName" bordered size="small" :column="{ xs: 1, md: 3 }" style="margin-bottom: 16px">
          <a-descriptions-item label="代理號">{{ vdata.me.agentNo }}</a-descriptions-item>
          <a-descriptions-item label="層級">
            <a-tag :color="vdata.me.agentLevel === 1 ? 'purple' : 'blue'">{{ vdata.me.agentLevel === 1 ? '高級代理' : '一般代理' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="上級代理">{{ vdata.me.parentAgentNo || '—' }}</a-descriptions-item>
        </a-descriptions>

        <a-tabs v-model:activeKey="vdata.tab">
          <a-tab-pane key="wallet" tab="錢包與提現">
            <WalletPanel :baseUrl="API_URL_AGENT_PORTAL + '/wallet'" />
          </a-tab-pane>
          <a-tab-pane key="profit" tab="分潤">
            <AgentProfitPanel :baseUrl="API_URL_AGENT_PORTAL + '/profits'" />
          </a-tab-pane>
          <a-tab-pane key="mch" tab="旗下商戶">
            <a-table :columns="mchColumns" :data-source="vdata.merchants" size="small" row-key="mchNo">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'relation'">
                  <a-tag :color="record.relation === '推薦' ? 'orange' : 'green'">{{ record.relation }}</a-tag>
                </template>
              </template>
              <template #emptyText>尚無商戶</template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-if="vdata.me.agentLevel === 1" key="sub" tab="下級代理">
            <a-table :columns="subColumns" :data-source="vdata.me.subAgents || []" size="small" row-key="agentNo">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'state'">
                  <a-badge :status="record.state === 0 ? 'error' : 'processing'" :text="record.state === 0 ? '停用' : '啟用'" />
                </template>
              </template>
              <template #emptyText>尚無下級代理</template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="fee" :tab="canEditSub ? '費率' : '費率（唯讀）'">
            <a-card v-if="canEditSub" size="small" title="設定下級代理費率" style="margin-bottom: 12px">
              <a-form layout="inline">
                <a-form-item><a-select v-model:value="vdata.sub.targetId" placeholder="下級代理" style="width: 200px"
                  :options="(vdata.me.subAgents || []).map((a) => ({ value: a.agentNo, label: a.agentName }))" /></a-form-item>
                <a-form-item><a-select v-model:value="vdata.sub.layer" style="width: 120px">
                  <a-select-option value="AGENT">代理費</a-select-option><a-select-option value="REFERRER">推薦佣金</a-select-option></a-select></a-form-item>
                <a-form-item><a-select v-model:value="vdata.sub.wayCode" placeholder="支付方式" style="width: 180px"
                  :options="(vdata.me.payWays || []).map((w) => ({ value: w.wayCode, label: w.wayName }))" /></a-form-item>
                <a-form-item><a-input-number v-model:value="vdata.sub.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" /></a-form-item>
                <a-form-item><a-input-number v-model:value="vdata.sub.fixedYuan" :min="0" :precision="2" addon-after="元" /></a-form-item>
                <a-form-item><a-button type="primary" @click="saveSubRule">儲存</a-button></a-form-item>
              </a-form>
              <div style="margin-top: 6px; color: #888; font-size: 12px">設定後若任何受影響商戶的手續費合計超過其商戶費率，系統會拒絕並提示。</div>
            </a-card>
            <a-table :columns="feeColumns" :data-source="vdata.rules" size="small" row-key="ruleId">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'layer'">{{ { SR_AGENT: '高代費', AGENT: '代理費', REFERRER: '推薦佣金' }[record.layer] }}</template>
                <template v-if="column.key === 'value'">{{ (Number(record.rate) * 100).toFixed(4) }}% + {{ (record.fixedAmount / 100).toFixed(2) }} 元</template>
              </template>
              <template #emptyText>平台尚未設定費率</template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-card>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import AgentProfitPanel from '@/components/AgentProfit/AgentProfitPanel.vue'
import WalletPanel from '@/components/WalletPanel/WalletPanel.vue'
import { computed, reactive, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const mchColumns = [
  { title: '商戶號', dataIndex: 'mchNo' },
  { title: '商戶名稱', dataIndex: 'mchName' },
  { title: '直屬代理', dataIndex: 'agentNo' },
  { key: 'relation', title: '關係' },
]
const subColumns = [
  { title: '代理號', dataIndex: 'agentNo' },
  { title: '名稱', dataIndex: 'agentName' },
  { title: '聯絡人', dataIndex: 'contactName' },
  { key: 'state', title: '狀態' },
]
const feeColumns = [
  { title: '支付方式', dataIndex: 'wayCode' },
  { title: '代理號', dataIndex: 'targetId' },
  { key: 'layer', title: '費率層' },
  { key: 'value', title: '費率' },
]

const vdata: any = reactive({
  me: null,
  error: '',
  tab: 'wallet',
  merchants: [],
  rules: [],
  sub: { targetId: undefined, layer: 'AGENT', wayCode: undefined, pct: 0, fixedYuan: 0 },
})

const canEditSub = computed(() => vdata.me && vdata.me.agentLevel === 1 && $access('ENT_AGENT_PORTAL_FEE_EDIT'))

function loadRules() {
  req.list(API_URL_AGENT_PORTAL + '/feeRules', {}).then((r) => (vdata.rules = r || []))
}

function saveSubRule() {
  const s = vdata.sub
  if (!s.targetId || !s.wayCode) {
    $infoBox.message.warning('請選擇下級代理與支付方式')
    return
  }
  req
    .add(API_URL_AGENT_PORTAL + '/subAgentRules', {
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

// 代理號由後端依登入帳號決定；非代理帳號（例如平台超管）會收到「此頁僅供代理帳號使用」
req
  .list(API_URL_AGENT_PORTAL + '/me', {})
  .then((res) => {
    vdata.me = res
    req.list(API_URL_AGENT_PORTAL + '/merchants', {}).then((r) => (vdata.merchants = r || []))
    loadRules()
  })
  .catch((err) => {
    vdata.error = (err && err.msg) || '此頁僅供代理帳號使用'
  })
</script>
