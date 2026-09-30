<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>費率範本</template>
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
        message="範本只包含高代費、代理費與推薦佣金，可一次套用到多個代理或商戶。套用會逐筆寫入費率並留下變更紀錄；之後修改範本不會回溯已套用的費率。"
      />
      <a-button v-if="$access('ENT_FEE_TEMPLATE_EDIT')" type="primary" style="margin-bottom: 16px" @click="openEdit(null)">新增範本</a-button>
      <a-table :columns="columns" :data-source="vdata.templates" :pagination="false" size="small" row-key="templateId">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'op'">
            <a-button v-if="$access('ENT_FEE_TEMPLATE_EDIT')" type="link" @click="openEdit(record.templateId)">修改</a-button>
            <a-button v-if="$access('ENT_FEE_TEMPLATE_APPLY')" type="link" @click="openApply(record)">套用</a-button>
            <a-button v-if="$access('ENT_FEE_TEMPLATE_EDIT')" type="link" danger @click="removeFunc(record)">刪除</a-button>
          </template>
        </template>
        <template #emptyText>尚無範本</template>
      </a-table>
    </a-card>

    <a-modal v-model:open="vdata.edit.open" :title="vdata.edit.templateId ? '修改範本' : '新增範本'" width="820px" ok-text="儲存" @ok="submitEdit">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="範本名稱"><a-input v-model:value="vdata.edit.templateName" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="備註"><a-input v-model:value="vdata.edit.remark" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <a-table :columns="itemColumns" :data-source="vdata.edit.rows" :pagination="false" size="small" row-key="key">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'layer'">{{ LAYER_NAMES[record.layer] }}</template>
          <template v-if="column.key === 'pct'"><a-input-number v-model:value="record.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" /></template>
          <template v-if="column.key === 'fixedYuan'"><a-input-number v-model:value="record.fixedYuan" :min="0" :precision="2" addon-after="元" /></template>
          <template v-if="column.key === 'include'"><a-checkbox v-model:checked="record.include" /></template>
        </template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="vdata.apply.open" :title="'套用範本：' + vdata.apply.templateName" ok-text="套用" @ok="submitApply">
      <a-form layout="vertical">
        <a-form-item label="套用對象">
          <a-radio-group v-model:value="vdata.apply.targetType" @change="() => (vdata.apply.targetIds = [])">
            <a-radio value="AGENT">代理</a-radio>
            <a-radio value="MCH">商戶（寫入商戶覆寫）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="vdata.apply.targetType === 'AGENT'" label="代理（可多選；高級代理套高代費、一般代理套代理費，推薦佣金兩者都套）">
          <a-select v-model:value="vdata.apply.targetIds" mode="multiple" show-search option-filter-prop="label" :options="vdata.agentOptions" />
        </a-form-item>
        <a-form-item v-else label="商戶號（可多選，也可直接輸入商戶號後按 Enter）">
          <a-select v-model:value="vdata.apply.targetIds" mode="tags" option-filter-prop="label" :options="vdata.mchOptions" />
        </a-form-item>
      </a-form>
    </a-modal>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_FEE_TEMPLATES, API_URL_MCH_LIST, API_URL_PAYWAYS_LIST, req } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const LAYER_NAMES = { SR_AGENT: '高代費', AGENT: '代理費', REFERRER: '推薦佣金' }
const columns = [
  { title: '範本ID', dataIndex: 'templateId', width: '80px' },
  { title: '名稱', dataIndex: 'templateName' },
  { title: '備註', dataIndex: 'remark' },
  { title: '最後修改', dataIndex: 'updatedBy' },
  { title: '更新時間', dataIndex: 'updatedAt' },
  { key: 'op', title: '操作', width: '200px' },
]
const itemColumns = [
  { title: '支付方式', dataIndex: 'wayCode' },
  { key: 'layer', title: '費率層' },
  { key: 'pct', title: '百分比' },
  { key: 'fixedYuan', title: '單筆固定金額' },
  { key: 'include', title: '納入', width: '60px' },
]

const vdata: any = reactive({
  templates: [],
  wayCodes: [],
  agentOptions: [],
  mchOptions: [],
  edit: { open: false, templateId: null, templateName: '', remark: '', rows: [] },
  apply: { open: false, templateId: null, templateName: '', targetType: 'AGENT', targetIds: [] },
})

// 畫面用 % 與「元」，後端存比例與「分」
const toPct = (rate) => Number((Number(rate || 0) * 100).toFixed(4))
const toRate = (pct) => (Number(pct || 0) / 100).toFixed(6)
const toYuan = (fen) => Number((Number(fen || 0) / 100).toFixed(2))
const toFen = (yuan) => Math.round(Number(yuan || 0) * 100)

function load() {
  req.list(API_URL_FEE_TEMPLATES, { pageSize: -1 }).then((res) => {
    vdata.templates = res.records || []
  })
}
load()
req.list(API_URL_PAYWAYS_LIST, { pageSize: -1 }).then((res) => {
  vdata.wayCodes = (res.records || []).map((w) => w.wayCode)
})

function openEdit(templateId) {
  const build = (items) => {
    const rows = []
    vdata.wayCodes.forEach((wayCode) => {
      ;['SR_AGENT', 'AGENT', 'REFERRER'].forEach((layer) => {
        const it = items.find((x) => x.wayCode === wayCode && x.layer === layer)
        rows.push({ key: wayCode + layer, wayCode, layer, pct: it ? toPct(it.rate) : 0, fixedYuan: it ? toYuan(it.fixedAmount) : 0, include: !!it })
      })
    })
    return rows
  }
  if (!templateId) {
    vdata.edit = { open: true, templateId: null, templateName: '', remark: '', rows: build([]) }
    return
  }
  req.getById(API_URL_FEE_TEMPLATES, templateId).then((res) => {
    vdata.edit = { open: true, templateId, templateName: res.templateName, remark: res.remark, rows: build(res.items || []) }
  })
}

function submitEdit() {
  const e = vdata.edit
  const items = e.rows.filter((r) => r.include).map((r) => ({ wayCode: r.wayCode, layer: r.layer, rate: toRate(r.pct), fixedAmount: toFen(r.fixedYuan) }))
  if (!e.templateName || !items.length) {
    $infoBox.message.warning('請填寫範本名稱並至少勾選一筆費率')
    return
  }
  req.add(API_URL_FEE_TEMPLATES, { templateId: e.templateId, templateName: e.templateName, remark: e.remark, items: JSON.stringify(items) }).then(() => {
    $infoBox.message.success('已儲存')
    vdata.edit.open = false
    load()
  })
}

function removeFunc(record) {
  $infoBox.confirmDanger('確認刪除範本？', '已套用到代理或商戶的費率不受影響', () => {
    req.delById(API_URL_FEE_TEMPLATES, record.templateId).then(() => {
      $infoBox.message.success('已刪除')
      load()
    })
  })
}

function openApply(record) {
  vdata.apply = { open: true, templateId: record.templateId, templateName: record.templateName, targetType: 'AGENT', targetIds: [] }
  req.list(API_URL_AGENT_INFO, { pageSize: -1 }).then((res) => {
    vdata.agentOptions = (res.records || []).map((a) => ({ value: a.agentNo, label: `${a.agentLevel === 1 ? '［高級］' : '［一般］'}${a.agentName}（${a.agentNo}）` }))
  })
  // 沒有商戶列表權限時不載入選項，仍可直接輸入商戶號
  if ($access('ENT_MCH_LIST')) {
    req.list(API_URL_MCH_LIST, { pageSize: 100 }).then((res) => {
      vdata.mchOptions = (res.records || []).map((m) => ({ value: m.mchNo, label: `${m.mchName}（${m.mchNo}）` }))
    })
  }
}

function submitApply() {
  const a = vdata.apply
  if (!a.targetIds.length) {
    $infoBox.message.warning('請選擇套用對象')
    return
  }
  req.add(`${API_URL_FEE_TEMPLATES}/${a.templateId}/apply`, { targetType: a.targetType, targetIds: JSON.stringify(a.targetIds) }).then((res) => {
    $infoBox.message.success(`已套用 ${res.applied} 筆${res.skipped ? `，略過 ${res.skipped} 筆（層級不符）` : ''}`)
    vdata.apply.open = false
  })
}
</script>
