<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>四層手續費設定</template>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
        message="各層手續費 = 平臺費 + 渠道費 + 團長費 + 隊長費，每層為「百分比 + 單筆固定金額」，應不超過商戶在支付通道上的費率（可用「風險檢查」確認）。單一商戶覆寫優先於預設。"
        description="平臺費與渠道費的變更須由另一位具覆核權限的管理者核准後才生效；團長費與隊長費儲存即生效。"
      />
      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item label="支付方式">
          <a-select v-model:value="vdata.wayCode" style="width: 240px" :options="vdata.wayOptions" @change="reloadAll" />
        </a-form-item>
        <a-form-item v-if="$access('ENT_FEE_RULE_BATCH')">
          <a-button @click="openBatch">批次設定（跨支付方式）</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="riskFunc">風險檢查</a-button>
        </a-form-item>
      </a-form>

      <a-card v-if="vdata.pending.length" size="small" style="margin-bottom: 16px">
        <template #title>待覆核的平臺費／渠道費變更 <a-badge :count="vdata.pending.length" /></template>
        <a-table :columns="pendingColumns" :data-source="vdata.pending" :pagination="false" size="small" row-key="reqId">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'target'">{{ record.targetType === 'DEFAULT' ? '平台預設' : '商戶 ' + record.targetId }}</template>
            <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
            <template v-if="column.key === 'value'">
              {{ record.action === 'DELETE' ? '刪除規則' : ruleText(record) }}
            </template>
            <template v-if="column.key === 'op'">
              <template v-if="$access('ENT_FEE_RULE_REVIEW')">
                <a-button type="link" @click="reviewFunc(record, 'approve')">核准</a-button>
                <a-button type="link" danger @click="reviewFunc(record, 'reject')">駁回</a-button>
              </template>
              <span v-else>待覆核</span>
            </template>
          </template>
        </a-table>
      </a-card>

      <template v-if="vdata.wayCode">
        <a-card size="small" style="margin-bottom: 16px">
          <template #title>
            平台預設（平臺費／渠道費）
            <a-tag v-if="!canPlatform" color="default" style="margin-left: 8px">🔒 僅平台管理者可修改</a-tag>
          </template>
          <a-row :gutter="16">
            <a-col v-for="layer in platformLayers" :key="layer" :xs="24" :md="12">
              <a-form layout="vertical">
                <a-form-item :label="LAYER_NAMES[layer]">
                  <a-input-group compact>
                    <a-input-number v-model:value="vdata.defaults[layer].pct" :min="0" :max="99.9999" :precision="4" addon-after="%" :disabled="!canPlatform" style="width: 45%" />
                    <a-input-number v-model:value="vdata.defaults[layer].fixedYuan" :min="0" :precision="2" addon-after="元／筆" :disabled="!canPlatform" style="width: 45%" />
                  </a-input-group>
                  <a-button v-if="canPlatform" type="link" @click="saveRule('DEFAULT', '', layer, vdata.defaults[layer])">儲存</a-button>
                </a-form-item>
              </a-form>
            </a-col>
          </a-row>
        </a-card>

        <a-card size="small" title="代理費率（團長設團長費、隊長設隊長費）" style="margin-bottom: 16px">
          <a-table :columns="agentColumns" :data-source="vdata.agentRows" :pagination="false" size="small" row-key="key">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'agentName'">
                <a-tag :color="record.agentLevel === 1 ? 'purple' : 'blue'">{{ record.agentLevel === 1 ? '高級' : '一般' }}</a-tag>
                {{ record.agentName }}（{{ record.agentNo }}）
              </template>
              <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
              <template v-if="column.key === 'value'">{{ ruleText(record.rule) }}</template>
              <template v-if="column.key === 'op'">
                <a-button type="link" :disabled="!canAgent" @click="openEdit('AGENT', record.agentNo, record.layer, record.rule)">設定</a-button>
                <a-button v-if="record.rule" type="link" danger :disabled="!canAgent" @click="removeRule(record.rule)">清除</a-button>
              </template>
            </template>
          </a-table>
        </a-card>

        <a-card size="small" title="單一商戶覆寫" style="margin-bottom: 16px">
          <a-button type="primary" :disabled="!canAgent && !canPlatform" style="margin-bottom: 12px" @click="openEdit('MCH', '', undefined, null)">新增覆寫</a-button>
          <a-table :columns="mchColumns" :data-source="vdata.mchRules" :pagination="false" size="small" row-key="ruleId">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
              <template v-if="column.key === 'value'">{{ ruleText(record) }}</template>
              <template v-if="column.key === 'op'">
                <a-button type="link" :disabled="!canEditLayer(record.layer)" @click="openEdit('MCH', record.targetId, record.layer, record)">修改</a-button>
                <a-button type="link" danger :disabled="!canEditLayer(record.layer)" @click="removeRule(record)">刪除</a-button>
              </template>
            </template>
            <template #emptyText>尚無商戶覆寫</template>
          </a-table>
        </a-card>

        <a-card size="small" title="試算" style="margin-bottom: 16px">
          <a-form layout="inline" style="margin-bottom: 12px">
            <a-form-item label="商戶號"><a-input v-model:value="vdata.preview.mchNo" style="width: 200px" /></a-form-item>
            <a-form-item label="金額"><a-input-number v-model:value="vdata.preview.amountYuan" :min="0" :precision="2" addon-after="元" /></a-form-item>
            <a-form-item><a-button type="primary" @click="previewFunc">試算</a-button></a-form-item>
          </a-form>
          <template v-if="vdata.preview.result">
            <a-table :columns="previewColumns" :data-source="vdata.preview.result.layers" :pagination="false" size="small" row-key="layer">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
                <template v-if="column.key === 'rate'">{{ toPct(record.rate) }}% + {{ toYuan(record.fixedAmount) }} 元</template>
                <template v-if="column.key === 'source'">{{ sourceText(record.source) }}</template>
                <template v-if="column.key === 'fee'">{{ toYuan(record.fee) }} 元</template>
              </template>
            </a-table>
            <div style="margin-top: 8px">
              手續費合計 <b>{{ toYuan(vdata.preview.result.totalFee) }}</b> 元，商戶實收 <b>{{ toYuan(vdata.preview.result.netAmount) }}</b> 元
              <a-tag v-if="!vdata.preview.result.valid" color="red" style="margin-left: 8px">手續費超過訂單金額</a-tag>
            </div>
          </template>
        </a-card>

        <a-card v-if="$access('ENT_FEE_RULE_LOG')" size="small" title="變更紀錄（此支付方式，最近 20 筆）">
          <a-table :columns="logColumns" :data-source="vdata.logs" :pagination="false" size="small" row-key="logId" />
        </a-card>
      </template>
    </a-card>

    <a-modal v-model:open="vdata.risk.open" title="風險檢查：四層合計超過商戶手續費" :footer="null" width="760px">
      <a-form layout="inline" style="margin-bottom: 12px">
        <a-form-item label="參考金額"><a-input-number v-model:value="vdata.risk.amountYuan" :min="1" :precision="0" addon-after="元" /></a-form-item>
        <a-form-item><a-button type="primary" @click="riskFunc">重新檢查</a-button></a-form-item>
      </a-form>
      <a-table :columns="riskColumns" :data-source="vdata.risk.rows" :pagination="false" size="small" :row-key="(r) => r.mchNo + r.appId + r.wayCode">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'mchRate'">{{ toPct(record.mchRate) }}%</template>
          <template v-if="column.key === 'mchFee'">{{ toYuan(record.mchFee) }} 元</template>
          <template v-if="column.key === 'totalFee'"><span style="color: red">{{ toYuan(record.totalFee) }} 元</span></template>
        </template>
        <template #emptyText>沒有發現問題：所有啟用中的商戶通道，四層合計都未超過商戶手續費</template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="vdata.batch.open" title="批次設定代理層費率" width="820px" ok-text="全部儲存" @ok="submitBatch">
      <a-alert type="info" show-icon style="margin-bottom: 12px" message="同一次儲存在同一交易內完成，任一筆不合法則全部不生效。平臺費與渠道費須逐筆送覆核，不在此設定。" />
      <a-form layout="inline" style="margin-bottom: 12px">
        <a-form-item label="對象">
          <a-radio-group v-model:value="vdata.batch.targetType" @change="loadBatch">
            <a-radio value="AGENT">代理</a-radio>
            <a-radio value="MCH">商戶</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="vdata.batch.targetType === 'AGENT'">
          <a-select v-model:value="vdata.batch.targetId" style="width: 280px" placeholder="選擇代理" show-search option-filter-prop="label"
            :options="[...new Map(vdata.agentRows.map((a) => [a.agentNo, { value: a.agentNo, label: (a.agentLevel === 1 ? '［高級］' : '［一般］') + a.agentName }])).values()]" @change="loadBatch" />
        </a-form-item>
        <a-form-item v-else>
          <a-input v-model:value="vdata.batch.targetId" style="width: 200px" placeholder="商戶號" @pressEnter="loadBatch" />
          <a-button style="margin-left: 8px" @click="loadBatch">載入</a-button>
        </a-form-item>
      </a-form>
      <a-table v-if="vdata.batch.rows.length" :columns="batchColumns" :data-source="vdata.batch.rows" :pagination="false" size="small" row-key="key">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
          <template v-if="column.key === 'pct'"><a-input-number v-model:value="record.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" /></template>
          <template v-if="column.key === 'fixedYuan'"><a-input-number v-model:value="record.fixedYuan" :min="0" :precision="2" addon-after="元" /></template>
          <template v-if="column.key === 'include'"><a-checkbox v-model:checked="record.include" /></template>
        </template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="vdata.edit.open" :title="vdata.edit.title" @ok="submitEdit" ok-text="儲存">
      <a-form layout="vertical">
        <a-form-item v-if="vdata.edit.targetType === 'MCH'" label="商戶號">
          <a-input v-model:value="vdata.edit.targetId" :disabled="vdata.edit.lockTarget" />
        </a-form-item>
        <a-form-item v-if="vdata.edit.targetType === 'MCH'" label="費率層">
          <a-select v-model:value="vdata.edit.layer" :disabled="vdata.edit.lockTarget">
            <a-select-option v-for="layer in ALL_LAYERS" :key="layer" :value="layer" :disabled="!canEditLayer(layer)">
              {{ LAYER_NAMES[layer] }}{{ canEditLayer(layer) ? '' : '（無權限）' }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="百分比"><a-input-number v-model:value="vdata.edit.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" /></a-form-item>
        <a-form-item label="單筆固定金額"><a-input-number v-model:value="vdata.edit.fixedYuan" :min="0" :precision="2" addon-after="元" /></a-form-item>
      </a-form>
    </a-modal>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_FEE_RULES, API_URL_PAYWAYS_LIST, req } from '@/api/manage'
import { computed, reactive, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const ALL_LAYERS = ['PLATFORM', 'CHANNEL', 'SR_AGENT', 'AGENT']
const platformLayers = ['PLATFORM', 'CHANNEL']
const LAYER_NAMES = { PLATFORM: '平臺費', CHANNEL: '渠道費', SR_AGENT: '團長費', AGENT: '隊長費' }

const canPlatform = computed(() => $access('ENT_FEE_RULE_PLATFORM_EDIT'))
const canAgent = computed(() => $access('ENT_FEE_RULE_EDIT'))
function canEditLayer(layer) {
  return platformLayers.includes(layer) ? canPlatform.value : canAgent.value
}

const agentColumns = [
  { key: 'agentName', title: '代理' },
  { key: 'layer', title: '費率層' },
  { key: 'value', title: '目前設定' },
  { key: 'op', title: '操作', width: '160px' },
]
const mchColumns = [
  { key: 'targetId', title: '商戶號', dataIndex: 'targetId' },
  { key: 'layer', title: '費率層' },
  { key: 'value', title: '覆寫值' },
  { key: 'updatedBy', title: '修改者', dataIndex: 'updatedBy' },
  { key: 'op', title: '操作', width: '160px' },
]
const previewColumns = [
  { key: 'layer', title: '費率層' },
  { key: 'rate', title: '費率' },
  { key: 'source', title: '來源' },
  { key: 'fee', title: '手續費' },
]
const pendingColumns = [
  { title: '申請時間', dataIndex: 'createdAt' },
  { title: '支付方式', dataIndex: 'wayCode' },
  { key: 'target', title: '對象' },
  { key: 'layer', title: '費率層' },
  { key: 'value', title: '申請內容' },
  { title: '申請人', dataIndex: 'requesterName' },
  { key: 'op', title: '操作', width: '140px' },
]
const riskColumns = [
  { title: '商戶號', dataIndex: 'mchNo' },
  { title: '應用', dataIndex: 'appId' },
  { title: '支付方式', dataIndex: 'wayCode' },
  { key: 'mchRate', title: '商戶費率' },
  { key: 'mchFee', title: '商戶手續費' },
  { key: 'totalFee', title: '四層合計' },
]
const batchColumns = [
  { title: '支付方式', dataIndex: 'wayCode' },
  { key: 'layer', title: '費率層' },
  { key: 'pct', title: '百分比' },
  { key: 'fixedYuan', title: '單筆固定金額' },
  { key: 'include', title: '儲存', width: '60px' },
]
const logColumns = [
  { title: '時間', dataIndex: 'createdAt' },
  { title: '對象', dataIndex: 'targetType' },
  { title: '對象ID', dataIndex: 'targetId' },
  { title: '層', dataIndex: 'layer' },
  { title: '動作', dataIndex: 'action' },
  { title: '變更前', dataIndex: 'beforeValue' },
  { title: '變更後', dataIndex: 'afterValue' },
  { title: '操作者', dataIndex: 'operatorName' },
]

const vdata: any = reactive({
  wayCode: undefined,
  wayOptions: [],
  defaults: { PLATFORM: { pct: 0, fixedYuan: 0 }, CHANNEL: { pct: 0, fixedYuan: 0 } },
  agentRows: [],
  mchRules: [],
  logs: [],
  preview: { mchNo: '', amountYuan: 1000, result: null },
  pending: [],
  risk: { open: false, amountYuan: 1000, rows: [] },
  batch: { open: false, targetType: 'AGENT', targetId: undefined, rows: [] },
  edit: { open: false, title: '', targetType: '', targetId: '', layer: undefined, pct: 0, fixedYuan: 0, lockTarget: false },
})

// 單位換算：畫面用 % 與「元」，後端存比例與「分」
function toPct(rate) {
  return Number((Number(rate || 0) * 100).toFixed(4))
}
function toRate(pct) {
  return (Number(pct || 0) / 100).toFixed(6)
}
function toYuan(fen) {
  return (Number(fen || 0) / 100).toFixed(2)
}
function toFen(yuan) {
  return Math.round(Number(yuan || 0) * 100)
}
function ruleText(rule) {
  return rule ? `${toPct(rule.rate)}% + ${toYuan(rule.fixedAmount)} 元` : '未設定（0）'
}
function sourceText(source) {
  if (source === 'MCH') return '商戶覆寫'
  if (source === 'DEFAULT') return '平台預設'
  if (source === 'NONE') return '未設定'
  return '代理 ' + source.replace('AGENT:', '')
}

req.list(API_URL_PAYWAYS_LIST, { pageSize: -1 }).then((res) => {
  vdata.wayOptions = (res.records || []).map((w) => ({ value: w.wayCode, label: `${w.wayName}（${w.wayCode}）` }))
})

function loadPending() {
  req.list(API_URL_FEE_RULES + '/changeReqs', { state: 0, pageSize: -1 }).then((res) => {
    vdata.pending = res.records || []
  })
}
loadPending()

function reviewFunc(record, action) {
  const verb = action === 'approve' ? '核准' : '駁回'
  $infoBox.confirmPrimary(`確認${verb}這筆申請？`, action === 'approve' ? '核准後立即生效，並留下變更紀錄' : '', () => {
    req.add(`${API_URL_FEE_RULES}/changeReqs/${record.reqId}/${action}`, {}).then(() => {
      $infoBox.message.success('已' + verb)
      loadPending()
      if (vdata.wayCode) reloadAll()
    })
  })
}

function riskFunc() {
  vdata.risk.open = true
  req.list(API_URL_FEE_RULES + '/risk', { wayCode: vdata.wayCode, amount: toFen(vdata.risk.amountYuan) }).then((res) => {
    vdata.risk.rows = res || []
  })
}

function openBatch() {
  vdata.batch = { open: true, targetType: 'AGENT', targetId: undefined, rows: [] }
  if (!vdata.agentRows.length) {
    req.list(API_URL_AGENT_INFO, { pageSize: -1 }).then((res) => {
      vdata.agentRows = (res.records || []).map((a) => ({ ...a, key: a.agentNo, layer: a.agentLevel === 1 ? 'SR_AGENT' : 'AGENT' }))
    })
  }
}

function loadBatch() {
  const b = vdata.batch
  b.rows = []
  if (!b.targetId) return
  let layers = ['SR_AGENT', 'AGENT']
  if (b.targetType === 'AGENT') {
    const agent = vdata.agentRows.find((a) => a.agentNo === b.targetId)
    layers = [agent && agent.agentLevel === 1 ? 'SR_AGENT' : 'AGENT']
  }
  req.list(API_URL_FEE_RULES, { targetType: b.targetType, targetId: b.targetId, pageSize: -1 }).then((res) => {
    const rules = res.records || []
    const rows = []
    vdata.wayOptions.forEach((w) => {
      layers.forEach((layer) => {
        const r = rules.find((x) => x.wayCode === w.value && x.layer === layer)
        rows.push({
          key: w.value + layer,
          wayCode: w.value,
          layer,
          pct: r ? toPct(r.rate) : 0,
          fixedYuan: r ? Number(toYuan(r.fixedAmount)) : 0,
          include: !!r,
        })
      })
    })
    b.rows = rows
  })
}

function submitBatch() {
  const b = vdata.batch
  const rules = b.rows
    .filter((r) => r.include)
    .map((r) => ({ wayCode: r.wayCode, targetType: b.targetType, targetId: b.targetId, layer: r.layer, rate: toRate(r.pct), fixedAmount: toFen(r.fixedYuan) }))
  if (!rules.length) {
    $infoBox.message.warning('請勾選要儲存的列')
    return
  }
  req.add(API_URL_FEE_RULES + '/batch', { rules: JSON.stringify(rules) }).then((count) => {
    $infoBox.message.success(`已儲存 ${count} 筆`)
    vdata.batch.open = false
    if (vdata.wayCode) reloadAll()
  })
}

function doneMessage(res) {
  if (res && res.pendingReview) {
    $infoBox.message.info('已送出覆核，需另一位管理者核准後才生效')
    loadPending()
  } else {
    $infoBox.message.success('已儲存')
  }
}

function reloadAll() {
  Promise.all([
    req.list(API_URL_FEE_RULES, { wayCode: vdata.wayCode, pageSize: -1 }),
    req.list(API_URL_AGENT_INFO, { pageSize: -1 }),
  ]).then(([ruleRes, agentRes]) => {
    const rules = ruleRes.records || []
    platformLayers.forEach((layer) => {
      const r = rules.find((x) => x.targetType === 'DEFAULT' && x.layer === layer)
      vdata.defaults[layer] = { pct: r ? toPct(r.rate) : 0, fixedYuan: r ? Number(toYuan(r.fixedAmount)) : 0 }
    })
    // 每個代理一列：依層級為團長費或隊長費
    const rows = []
    ;(agentRes.records || []).forEach((a) => {
      ;[a.agentLevel === 1 ? 'SR_AGENT' : 'AGENT'].forEach((layer) => {
        rows.push({ ...a, key: a.agentNo + layer, layer, rule: rules.find((x) => x.targetType === 'AGENT' && x.targetId === a.agentNo && x.layer === layer) })
      })
    })
    vdata.agentRows = rows
    vdata.mchRules = rules.filter((x) => x.targetType === 'MCH')
  })
  if ($access('ENT_FEE_RULE_LOG')) {
    req.list(API_URL_FEE_RULES + '/logs', { wayCode: vdata.wayCode, pageSize: 20 }).then((res) => {
      vdata.logs = res.records || []
    })
  }
}

function saveRule(targetType, targetId, layer, value) {
  return req
    .add(API_URL_FEE_RULES, {
      wayCode: vdata.wayCode,
      targetType,
      targetId,
      layer,
      rate: toRate(value.pct),
      fixedAmount: toFen(value.fixedYuan),
    })
    .then((res) => {
      doneMessage(res)
      reloadAll()
    })
}

function openEdit(targetType, targetId, layer, rule) {
  vdata.edit = {
    open: true,
    title: targetType === 'MCH' ? '商戶覆寫' : `設定${LAYER_NAMES[layer]}（${targetId}）`,
    targetType,
    targetId,
    layer,
    pct: rule ? toPct(rule.rate) : 0,
    fixedYuan: rule ? Number(toYuan(rule.fixedAmount)) : 0,
    lockTarget: !!rule,
  }
}

function submitEdit() {
  const e = vdata.edit
  if (e.targetType === 'MCH' && (!e.targetId || !e.layer)) {
    $infoBox.message.warning('請填寫商戶號並選擇費率層')
    return
  }
  saveRule(e.targetType, e.targetId, e.layer, { pct: e.pct, fixedYuan: e.fixedYuan }).then(() => {
    vdata.edit.open = false
  })
}

function removeRule(rule) {
  $infoBox.confirmDanger('確認刪除這條費率規則？', '刪除後會退回上一層來源（預設或代理設定），並留下變更紀錄', () => {
    req.delById(API_URL_FEE_RULES, rule.ruleId).then((res) => {
      if (res && res.pendingReview) {
        doneMessage(res)
      } else {
        $infoBox.message.success('已刪除')
      }
      reloadAll()
    })
  })
}

function previewFunc() {
  if (!vdata.preview.mchNo) {
    $infoBox.message.warning('請輸入商戶號')
    return
  }
  req
    .list(API_URL_FEE_RULES + '/preview', { mchNo: vdata.preview.mchNo, wayCode: vdata.wayCode, amount: toFen(vdata.preview.amountYuan) })
    .then((res) => {
      vdata.preview.result = res
    })
}
</script>
