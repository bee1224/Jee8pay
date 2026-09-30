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
          <a-tab-pane key="fee" tab="費率（唯讀）">
            <a-table :columns="feeColumns" :data-source="vdata.rules" size="small" row-key="ruleId">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'layer'">{{ record.layer === 'SR_AGENT' ? '高代費' : '代理費' }}</template>
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
import { reactive } from 'vue'

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

const vdata: any = reactive({ me: null, error: '', tab: 'profit', merchants: [], rules: [] })

// 代理號由後端依登入帳號決定；非代理帳號（例如平台超管）會收到「此頁僅供代理帳號使用」
req
  .list(API_URL_AGENT_PORTAL + '/me', {})
  .then((res) => {
    vdata.me = res
    req.list(API_URL_AGENT_PORTAL + '/merchants', {}).then((r) => (vdata.merchants = r || []))
    req.list(API_URL_AGENT_PORTAL + '/feeRules', {}).then((r) => (vdata.rules = r || []))
  })
  .catch((err) => {
    vdata.error = (err && err.msg) || '此頁僅供代理帳號使用'
  })
</script>
