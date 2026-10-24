<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>{{ $t('四層手續費設定') }}</template>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
        :message="$t('各層手續費 = 平臺費 + 渠道費 + 團長費 + 隊長費，每層為「百分比 + 單筆固定金額」，應不超過商戶在支付通道上的費率（可用「風險檢查」確認）。單一商戶覆寫優先於預設。')"
        :description="$t('平臺費與渠道費的變更須由另一位具覆核權限的管理者核准後才生效；團長費與隊長費儲存即生效。')"
      />
      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item :label="$t('支付方式')">
          <a-select v-model:value="vdata.wayCode" style="width: 240px" :options="vdata.wayOptions" @change="reloadAll" />
        </a-form-item>
        <a-form-item v-if="$access('ENT_FEE_RULE_BATCH')">
          <a-button @click="openBatch">{{ $t('批次設定（跨支付方式）') }}</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="riskFunc">{{ $t('風險檢查') }}</a-button>
        </a-form-item>
      </a-form>

      <a-card v-if="vdata.pending.length" size="small" style="margin-bottom: 16px">
        <template #title>{{ $t('待覆核的平臺費／渠道費變更') }} <a-badge :count="vdata.pending.length" /></template>
        <a-table :columns="pendingColumns" :data-source="vdata.pending" :pagination="false" size="small" row-key="reqId">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'target'">{{ record.targetType === 'DEFAULT' ? $t('平台預設') : $t('商戶 ') + record.targetId }}</template>
            <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
            <template v-if="column.key === 'value'">
              {{ record.action === 'DELETE' ? $t('刪除規則') : ruleText(record) }}
            </template>
            <template v-if="column.key === 'op'">
              <template v-if="$access('ENT_FEE_RULE_REVIEW')">
                <a-button type="link" @click="reviewFunc(record, 'approve')">{{ $t('核准') }}</a-button>
                <a-button type="link" danger @click="reviewFunc(record, 'reject')">{{ $t('駁回') }}</a-button>
              </template>
              <span v-else>{{ $t('待覆核') }}</span>
            </template>
          </template>
        </a-table>
      </a-card>

      <template v-if="vdata.wayCode">
        <a-card size="small" style="margin-bottom: 16px">
          <template #title>
            {{ $t('平台預設（平臺費／渠道費）') }}
            <a-tag v-if="!canPlatform" color="default" style="margin-left: 8px">{{ $t('🔒 僅平台管理者可修改') }}</a-tag>
          </template>
          <a-row :gutter="16">
            <a-col v-for="layer in platformLayers" :key="layer" :xs="24" :md="12">
              <a-form layout="vertical">
                <a-form-item :label="LAYER_NAMES[layer]">
                  <a-input-group compact>
                    <a-input-number v-model:value="vdata.defaults[layer].pct" :min="0" :max="99.9999" :precision="4" addon-after="%" :disabled="!canPlatform" style="width: 45%" />
                    <a-input-number v-model:value="vdata.defaults[layer].fixedYuan" :min="0" :precision="2" :addon-after="$t('元／筆')" :disabled="!canPlatform" style="width: 45%" />
                  </a-input-group>
                  <a-button v-if="canPlatform" type="link" @click="saveRule('DEFAULT', '', layer, vdata.defaults[layer])">{{ $t('儲存') }}</a-button>
                </a-form-item>
              </a-form>
            </a-col>
          </a-row>
        </a-card>

        <a-card size="small" :title="$t('代理費率（團長設團長費、隊長設隊長費）')" style="margin-bottom: 16px">
          <a-table :columns="agentColumns" :data-source="vdata.agentRows" :pagination="false" size="small" row-key="key">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'agentName'">
                <a-tag :color="record.agentLevel === 1 ? 'purple' : 'blue'">{{ record.agentLevel === 1 ? $t('高級') : $t('一般') }}</a-tag>
                {{ record.agentName }}（{{ record.agentNo }}）
              </template>
              <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
              <template v-if="column.key === 'value'">{{ ruleText(record.rule) }}</template>
              <template v-if="column.key === 'op'">
                <a-button type="link" :disabled="!canAgent" @click="openEdit('AGENT', record.agentNo, record.layer, record.rule)">{{ $t('設定') }}</a-button>
                <a-button v-if="record.rule" type="link" danger :disabled="!canAgent" @click="removeRule(record.rule)">{{ $t('清除') }}</a-button>
              </template>
            </template>
          </a-table>
        </a-card>

        <a-card size="small" :title="$t('單一商戶覆寫')" style="margin-bottom: 16px">
          <a-button type="primary" :disabled="!canAgent && !canPlatform" style="margin-bottom: 12px" @click="openEdit('MCH', '', undefined, null)">{{ $t('新增覆寫') }}</a-button>
          <a-table :columns="mchColumns" :data-source="vdata.mchRules" :pagination="false" size="small" row-key="ruleId">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
              <template v-if="column.key === 'value'">{{ ruleText(record) }}</template>
              <template v-if="column.key === 'op'">
                <a-button type="link" :disabled="!canEditLayer(record.layer)" @click="openEdit('MCH', record.targetId, record.layer, record)">{{ $t('修改') }}</a-button>
                <a-button type="link" danger :disabled="!canEditLayer(record.layer)" @click="removeRule(record)">{{ $t('刪除') }}</a-button>
              </template>
            </template>
            <template #emptyText>{{ $t('尚無商戶覆寫') }}</template>
          </a-table>
        </a-card>

        <a-card size="small" :title="$t('試算')" style="margin-bottom: 16px">
          <a-form layout="inline" style="margin-bottom: 12px">
            <a-form-item :label="$t('商戶號')"><a-input v-model:value="vdata.preview.mchNo" style="width: 200px" /></a-form-item>
            <a-form-item :label="$t('金額')"><a-input-number v-model:value="vdata.preview.amountYuan" :min="0" :precision="2" :addon-after="$t('元')" /></a-form-item>
            <a-form-item><a-button type="primary" @click="previewFunc">{{ $t('試算') }}</a-button></a-form-item>
          </a-form>
          <template v-if="vdata.preview.result">
            <a-table :columns="previewColumns" :data-source="vdata.preview.result.layers" :pagination="false" size="small" row-key="layer">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
                <template v-if="column.key === 'rate'">{{ $t('{0}% + {1} 元', [toPct(record.rate), toYuan(record.fixedAmount)]) }}</template>
                <template v-if="column.key === 'source'">{{ sourceText(record.source) }}</template>
                <template v-if="column.key === 'fee'">{{ $t('{0} 元', [toYuan(record.fee)]) }}</template>
              </template>
            </a-table>
            <div style="margin-top: 8px">
              {{ $t('手續費合計') }} <b>{{ toYuan(vdata.preview.result.totalFee) }}</b> {{ $t('元，商戶實收') }} <b>{{ toYuan(vdata.preview.result.netAmount) }}</b> {{ $t('元') }}
              <a-tag v-if="!vdata.preview.result.valid" color="red" style="margin-left: 8px">{{ $t('手續費超過訂單金額') }}</a-tag>
            </div>
          </template>
        </a-card>

        <a-card v-if="$access('ENT_FEE_RULE_LOG')" size="small" :title="$t('變更紀錄（此支付方式，最近 20 筆）')">
          <a-table :columns="logColumns" :data-source="vdata.logs" :pagination="false" size="small" row-key="logId" />
        </a-card>
      </template>
    </a-card>

    <a-modal v-model:open="vdata.risk.open" :title="$t('風險檢查：四層合計超過商戶手續費')" :footer="null" width="760px">
      <a-form layout="inline" style="margin-bottom: 12px">
        <a-form-item :label="$t('參考金額')"><a-input-number v-model:value="vdata.risk.amountYuan" :min="1" :precision="0" :addon-after="$t('元')" /></a-form-item>
        <a-form-item><a-button type="primary" @click="riskFunc">{{ $t('重新檢查') }}</a-button></a-form-item>
      </a-form>
      <a-table :columns="riskColumns" :data-source="vdata.risk.rows" :pagination="false" size="small" :row-key="(r) => r.mchNo + r.appId + r.wayCode">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'mchRate'">{{ toPct(record.mchRate) }}%</template>
          <template v-if="column.key === 'mchFee'">{{ $t('{0} 元', [toYuan(record.mchFee)]) }}</template>
          <template v-if="column.key === 'totalFee'"><span style="color: red">{{ $t('{0} 元', [toYuan(record.totalFee)]) }}</span></template>
        </template>
        <template #emptyText>{{ $t('沒有發現問題：所有啟用中的商戶通道，四層合計都未超過商戶手續費') }}</template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="vdata.batch.open" :title="$t('批次設定代理層費率')" width="820px" :ok-text="$t('全部儲存')" @ok="submitBatch">
      <a-alert type="info" show-icon style="margin-bottom: 12px" :message="$t('同一次儲存在同一交易內完成，任一筆不合法則全部不生效。平臺費與渠道費須逐筆送覆核，不在此設定。')" />
      <a-form layout="inline" style="margin-bottom: 12px">
        <a-form-item :label="$t('對象')">
          <a-radio-group v-model:value="vdata.batch.targetType" @change="loadBatch">
            <a-radio value="AGENT">{{ $t('代理') }}</a-radio>
            <a-radio value="MCH">{{ $t('商戶') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="vdata.batch.targetType === 'AGENT'">
          <a-select v-model:value="vdata.batch.targetId" style="width: 280px" :placeholder="$t('選擇代理')" show-search option-filter-prop="label"
            :options="[...new Map(vdata.agentRows.map((a) => [a.agentNo, { value: a.agentNo, label: (a.agentLevel === 1 ? $t('［高級］') : $t('［一般］')) + a.agentName }])).values()]" @change="loadBatch" />
        </a-form-item>
        <a-form-item v-else>
          <a-input v-model:value="vdata.batch.targetId" style="width: 200px" :placeholder="$t('商戶號')" @pressEnter="loadBatch" />
          <a-button style="margin-left: 8px" @click="loadBatch">{{ $t('載入') }}</a-button>
        </a-form-item>
      </a-form>
      <a-table v-if="vdata.batch.rows.length" :columns="batchColumns" :data-source="vdata.batch.rows" :pagination="false" size="small" row-key="key">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
          <template v-if="column.key === 'pct'"><a-input-number v-model:value="record.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" /></template>
          <template v-if="column.key === 'fixedYuan'"><a-input-number v-model:value="record.fixedYuan" :min="0" :precision="2" :addon-after="$t('元')" /></template>
          <template v-if="column.key === 'include'"><a-checkbox v-model:checked="record.include" /></template>
        </template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="vdata.edit.open" :title="vdata.edit.title" @ok="submitEdit" :ok-text="$t('儲存')">
      <a-form layout="vertical">
        <a-form-item v-if="vdata.edit.targetType === 'MCH'" :label="$t('商戶號')">
          <a-input v-model:value="vdata.edit.targetId" :disabled="vdata.edit.lockTarget" />
        </a-form-item>
        <a-form-item v-if="vdata.edit.targetType === 'MCH'" :label="$t('費率層')">
          <a-select v-model:value="vdata.edit.layer" :disabled="vdata.edit.lockTarget">
            <a-select-option v-for="layer in ALL_LAYERS" :key="layer" :value="layer" :disabled="!canEditLayer(layer)">
              {{ LAYER_NAMES[layer] }}{{ canEditLayer(layer) ? '' : $t('（無權限）') }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('百分比')"><a-input-number v-model:value="vdata.edit.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" /></a-form-item>
        <a-form-item :label="$t('單筆固定金額')"><a-input-number v-model:value="vdata.edit.fixedYuan" :min="0" :precision="2" :addon-after="$t('元')" /></a-form-item>
      </a-form>
    </a-modal>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_FEE_RULES, API_URL_PAYWAYS_LIST, req } from '@/api/manage'
import { computed, reactive, getCurrentInstance } from 'vue'
import { t } from '@/i18n'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const ALL_LAYERS = ['PLATFORM', 'CHANNEL', 'SR_AGENT', 'AGENT']
const platformLayers = ['PLATFORM', 'CHANNEL']
const LAYER_NAMES = { PLATFORM: t('平臺費'), CHANNEL: t('渠道費'), SR_AGENT: t('團長費'), AGENT: t('隊長費') }

const canPlatform = computed(() => $access('ENT_FEE_RULE_PLATFORM_EDIT'))
const canAgent = computed(() => $access('ENT_FEE_RULE_EDIT'))
function canEditLayer(layer) {
  return platformLayers.includes(layer) ? canPlatform.value : canAgent.value
}

const agentColumns = [
  { key: 'agentName', title: t('代理') },
  { key: 'layer', title: t('費率層') },
  { key: 'value', title: t('目前設定') },
  { key: 'op', title: t('操作'), width: '160px' },
]
const mchColumns = [
  { key: 'targetId', title: t('商戶號'), dataIndex: 'targetId' },
  { key: 'layer', title: t('費率層') },
  { key: 'value', title: t('覆寫值') },
  { key: 'updatedBy', title: t('修改者'), dataIndex: 'updatedBy' },
  { key: 'op', title: t('操作'), width: '160px' },
]
const previewColumns = [
  { key: 'layer', title: t('費率層') },
  { key: 'rate', title: t('費率') },
  { key: 'source', title: t('來源') },
  { key: 'fee', title: t('手續費') },
]
const pendingColumns = [
  { title: t('申請時間'), dataIndex: 'createdAt' },
  { title: t('支付方式'), dataIndex: 'wayCode' },
  { key: 'target', title: t('對象') },
  { key: 'layer', title: t('費率層') },
  { key: 'value', title: t('申請內容') },
  { title: t('申請人'), dataIndex: 'requesterName' },
  { key: 'op', title: t('操作'), width: '140px' },
]
const riskColumns = [
  { title: t('商戶號'), dataIndex: 'mchNo' },
  { title: t('應用'), dataIndex: 'appId' },
  { title: t('支付方式'), dataIndex: 'wayCode' },
  { key: 'mchRate', title: t('商戶費率') },
  { key: 'mchFee', title: t('商戶手續費') },
  { key: 'totalFee', title: t('四層合計') },
]
const batchColumns = [
  { title: t('支付方式'), dataIndex: 'wayCode' },
  { key: 'layer', title: t('費率層') },
  { key: 'pct', title: t('百分比') },
  { key: 'fixedYuan', title: t('單筆固定金額') },
  { key: 'include', title: t('儲存'), width: '60px' },
]
const logColumns = [
  { title: t('時間'), dataIndex: 'createdAt' },
  { title: t('對象'), dataIndex: 'targetType' },
  { title: t('對象ID'), dataIndex: 'targetId' },
  { title: t('層'), dataIndex: 'layer' },
  { title: t('動作'), dataIndex: 'action' },
  { title: t('變更前'), dataIndex: 'beforeValue' },
  { title: t('變更後'), dataIndex: 'afterValue' },
  { title: t('操作者'), dataIndex: 'operatorName' },
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
  return rule ? t('{0}% + {1} 元', [toPct(rule.rate), toYuan(rule.fixedAmount)]) : t('未設定（0）')
}
function sourceText(source) {
  if (source === 'MCH') return t('商戶覆寫')
  if (source === 'DEFAULT') return t('平台預設')
  if (source === 'NONE') return t('未設定')
  return t('代理 ') + source.replace('AGENT:', '')
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
  $infoBox.confirmPrimary(action === 'approve' ? t('確認核准這筆申請？') : t('確認駁回這筆申請？'), action === 'approve' ? t('核准後立即生效，並留下變更紀錄') : '', () => {
    req.add(`${API_URL_FEE_RULES}/changeReqs/${record.reqId}/${action}`, {}).then(() => {
      $infoBox.message.success(action === 'approve' ? t('已核准') : t('已駁回'))
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
    $infoBox.message.warning(t('請勾選要儲存的列'))
    return
  }
  req.add(API_URL_FEE_RULES + '/batch', { rules: JSON.stringify(rules) }).then((count) => {
    $infoBox.message.success(t('已儲存 {0} 筆', [count]))
    vdata.batch.open = false
    if (vdata.wayCode) reloadAll()
  })
}

function doneMessage(res) {
  if (res && res.pendingReview) {
    $infoBox.message.info(t('已送出覆核，需另一位管理者核准後才生效'))
    loadPending()
  } else {
    $infoBox.message.success(t('已儲存'))
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
    title: targetType === 'MCH' ? t('商戶覆寫') : t('設定{0}（{1}）', [LAYER_NAMES[layer], targetId]),
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
    $infoBox.message.warning(t('請填寫商戶號並選擇費率層'))
    return
  }
  saveRule(e.targetType, e.targetId, e.layer, { pct: e.pct, fixedYuan: e.fixedYuan }).then(() => {
    vdata.edit.open = false
  })
}

function removeRule(rule) {
  $infoBox.confirmDanger(t('確認刪除這條費率規則？'), t('刪除後會退回上一層來源（預設或代理設定），並留下變更紀錄'), () => {
    req.delById(API_URL_FEE_RULES, rule.ruleId).then((res) => {
      if (res && res.pendingReview) {
        doneMessage(res)
      } else {
        $infoBox.message.success(t('已刪除'))
      }
      reloadAll()
    })
  })
}

function previewFunc() {
  if (!vdata.preview.mchNo) {
    $infoBox.message.warning(t('請輸入商戶號'))
    return
  }
  req
    .list(API_URL_FEE_RULES + '/preview', { mchNo: vdata.preview.mchNo, wayCode: vdata.wayCode, amount: toFen(vdata.preview.amountYuan) })
    .then((res) => {
      vdata.preview.result = res
    })
}
</script>
