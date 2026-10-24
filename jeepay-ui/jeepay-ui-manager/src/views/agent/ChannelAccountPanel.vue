<template>
  <!-- 渠道帳號（ADR-0012 第一階段）：上帝替一位團長建立帳號、輸入金鑰並派發；金鑰只在這裡輸入，列表不顯示 -->
  <div>
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 12px"
      :message="srAgentNo ? $t('渠道帳號是一組第三方支付金鑰，屬於這位團長。只有上帝能建立、修改與派發；團長在自己的後台只看得到名稱與狀態。') : $t('這裡列出全部團長的渠道帳號。每個帳號是一組第三方支付金鑰，屬於一位團長；只有上帝能建立、修改與派發。')"
      :description="$t('目前下單仍使用商戶應用上的金鑰；改為依渠道帳號下單是下一階段。')"
    />
    <a-button v-if="$access('ENT_CHANNEL_ACCOUNT_EDIT')" type="primary" style="margin-bottom: 12px" @click="openForm()">{{ $t('新增渠道帳號') }}</a-button>
    <!-- 手機：卡片排列，操作與電腦版相同（共用下方的視窗） -->
    <div v-if="isMobile" class="m-list">
      <div v-for="record in vdata.records" :key="record.accountId" class="m-row" style="flex-direction: column; align-items: stretch; gap: 6px">
        <div style="display: flex; justify-content: space-between; gap: 8px">
          <b>{{ record.accountName }}</b>
          <span :class="['m-pill', record.state === 0 ? 'mute' : 'ok']">{{ record.state === 0 ? $t('停用') : $t('啟用') }}</span>
        </div>
        <small class="m-muted" style="margin: 0">
          {{ record.ifName || record.ifCode }}・
          <template v-if="!srAgentNo">{{ $t('所屬 {0}', [record.ownerName || record.ownerSrAgentNo]) }}</template>
          <template v-else-if="record.ownerSrAgentNo === srAgentNo">{{ $t('自己的') }}</template>
          <template v-else>{{ $t('由 {0} 共用', [record.ownerName || record.ownerSrAgentNo]) }}</template>
        </small>
        <small class="m-muted" style="margin: 0">
          {{ $t('範圍：{0} ・共用：{1}', [scopeOf(record).length ? $t('只限 ') + scopeOf(record).map((s) => s.agentName || s.agentNo).join('、') : $t('這一支全部可用'), !record.shareable ? $t('不共用') : sharedNames(record) || $t('已開啟，尚未加派')]) }}
        </small>
        <div v-if="$access('ENT_CHANNEL_ACCOUNT_EDIT')" style="display: flex; flex-wrap: wrap; gap: 6px">
          <template v-if="!srAgentNo || record.ownerSrAgentNo === srAgentNo">
            <button class="m-btn sm ghost" @click="openForm(record)">{{ $t('修改') }}</button>
            <button class="m-btn sm ghost" @click="openScope(record)">{{ $t('使用範圍') }}</button>
            <button class="m-btn sm ghost" @click="openShare(record)">{{ $t('派發／共用') }}</button>
            <button class="m-btn sm danger" @click="removeAccount(record)">{{ $t('刪除') }}</button>
          </template>
          <button v-else class="m-btn sm ghost" @click="openScope(record)">{{ $t('使用範圍') }}</button>
        </div>
      </div>
      <div v-if="!vdata.records.length" class="m-empty">{{ vdata.loading ? $t('載入中…') : srAgentNo ? $t('這位團長還沒有渠道帳號') : $t('還沒有任何渠道帳號') }}</div>
    </div>
    <a-table v-else :columns="columns" :data-source="vdata.records" :loading="vdata.loading" :pagination="false" size="small" row-key="accountId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'ifCode'">{{ record.ifName || record.ifCode }}</template>
        <template v-if="column.key === 'owner'">
          <span v-if="!srAgentNo">{{ record.ownerName || record.ownerSrAgentNo }}</span>
          <a-tag v-else-if="record.ownerSrAgentNo === srAgentNo" color="green">{{ $t('自己的') }}</a-tag>
          <a-tag v-else color="orange">{{ $t('由 {0} 共用', [record.ownerName || record.ownerSrAgentNo]) }}</a-tag>
        </template>
        <template v-if="column.key === 'shared'">
          <span v-if="!record.shareable">{{ $t('不共用') }}</span>
          <span v-else>{{ sharedNames(record) || $t('已開啟，尚未加派') }}</span>
        </template>
        <template v-if="column.key === 'scope'">
          <span v-if="!scopeOf(record).length">{{ $t('這一支全部可用') }}</span>
          <span v-else>{{ $t('只限：{0}', [scopeOf(record).map((s) => s.agentName || s.agentNo).join('、')]) }}</span>
        </template>
        <template v-if="column.key === 'state'">
          <a-badge :status="record.state === 0 ? 'error' : 'processing'" :text="record.state === 0 ? $t('停用') : $t('啟用')" />
        </template>
        <template v-if="column.key === 'op'">
          <a-button v-if="$access('ENT_CHANNEL_ACCOUNT_EDIT') && srAgentNo && record.ownerSrAgentNo !== srAgentNo" type="link" @click="openScope(record)">{{ $t('使用範圍') }}</a-button>
          <template v-if="$access('ENT_CHANNEL_ACCOUNT_EDIT') && (!srAgentNo || record.ownerSrAgentNo === srAgentNo)">
            <a-button type="link" @click="openForm(record)">{{ $t('修改') }}</a-button>
            <a-button type="link" @click="openScope(record)">{{ $t('使用範圍') }}</a-button>
            <a-button type="link" @click="openShare(record)">{{ $t('共用') }}</a-button>
            <a-button type="link" style="color: red" @click="removeAccount(record)">{{ $t('刪除') }}</a-button>
          </template>
        </template>
      </template>
      <template #emptyText>{{ srAgentNo ? $t('這位團長還沒有渠道帳號') : $t('還沒有任何渠道帳號') }}</template>
    </a-table>

    <a-modal
      v-model:open="vdata.form.open"
      :title="vdata.form.accountId ? $t('修改渠道帳號') : $t('新增渠道帳號')"
      :confirm-loading="vdata.form.saving"
      :ok-text="$t('儲存')"
      :cancel-text="$t('取消')"
      width="560px"
      @ok="saveForm"
    >
      <a-form layout="vertical">
        <a-form-item v-if="srAgentNo" :label="$t('所屬團長')">{{ agentName }}（{{ srAgentNo }}）</a-form-item>
        <a-form-item v-else-if="vdata.form.accountId" :label="$t('所屬團長')">{{ vdata.form.ownerName }}</a-form-item>
        <a-form-item v-else :label="$t('所屬團長')" required :extra="$t('建立後不可更換；要給其他團長用請開啟共用後加派。')">
          <a-select v-model:value="vdata.form.ownerSrAgentNo" :placeholder="$t('請選擇團長')" show-search option-filter-prop="label"
            :options="vdata.seniors.map((a) => ({ value: a.agentNo, label: `${a.agentName}（${a.agentNo}）` }))" />
        </a-form-item>
        <a-form-item :label="$t('支付接口')" required>
          <a-select
            v-model:value="vdata.form.ifCode"
            :disabled="!!vdata.form.accountId"
            :placeholder="$t('請選擇第三方支付接口')"
            :options="vdata.defines.map((d) => ({ value: d.ifCode, label: `${d.ifName}（${d.ifCode}）` }))"
            @change="onIfCodeChange"
          />
        </a-form-item>
        <a-form-item :label="$t('帳號名稱')" required :extra="$t('給自己和團長辨識用，例如「RYO 主帳號」。')">
          <a-input v-model:value="vdata.form.accountName" :maxlength="64" />
        </a-form-item>
        <a-form-item :label="$t('狀態')">
          <a-radio-group v-model:value="vdata.form.state">
            <a-radio :value="1">{{ $t('啟用') }}</a-radio>
            <a-radio :value="0">{{ $t('停用') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('備註')"><a-input v-model:value="vdata.form.remark" :maxlength="128" /></a-form-item>
        <a-divider v-if="vdata.form.fields.length" orientation="left">{{ $t('金鑰') }}</a-divider>
        <a-form-item v-for="f in vdata.form.fields" :key="f.name" :label="f.desc" :required="f.verify === 'required'">
          <a-radio-group v-if="f.type === 'radio'" v-model:value="vdata.form.params[f.name]">
            <a-radio v-for="o in f.options" :key="o.value" :value="o.value">{{ o.title }}</a-radio>
          </a-radio-group>
          <a-input
            v-else
            v-model:value="vdata.form.params[f.name]"
            autocomplete="off"
            :placeholder="f.star === '1' && vdata.form.masked[f.name] ? $t('{0}（不修改請留空）', [vdata.form.masked[f.name]]) : $t('請輸入')"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="vdata.scope.open" :title="$t('使用範圍：') + vdata.scope.accountName" :confirm-loading="vdata.scope.saving" :ok-text="$t('儲存')" :cancel-text="$t('取消')" @ok="saveScope">
      <a-form layout="vertical">
        <a-form-item :label="$t('可使用的隊長')" :extra="$t('不選 = 這位團長這一支全部可用（含團長直屬商戶）。選了之後，只有被選到的隊長的商戶能用，團長直屬商戶也不能用。')">
          <a-select v-model:value="vdata.scope.agentNos" mode="multiple" allow-clear :placeholder="$t('不限定')" option-filter-prop="label" :options="vdata.scope.options" />
        </a-form-item>
      </a-form>
      <div style="color: #888; font-size: 12px">{{ $t('渠道仍屬於團長。目前下單尚未依渠道帳號取金鑰，這個限定會在下一階段開始生效。') }}</div>
    </a-modal>

    <a-modal v-model:open="vdata.share.open" :title="$t('共用：') + vdata.share.accountName" :footer="null" width="520px">
      <a-form layout="vertical">
        <a-form-item :label="$t('允許加派給其他團長')" :extra="$t('預設關閉：一個帳號只屬於一位團長。開啟後才能加派。')">
          <a-switch :checked="vdata.share.shareable" :loading="vdata.share.saving" @change="toggleShareable" />
        </a-form-item>
      </a-form>
      <template v-if="vdata.share.shareable">
        <a-table :columns="shareColumns" :data-source="vdata.share.agents" :pagination="false" size="small" row-key="srAgentNo">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'agent'">{{ record.agentName }}（{{ record.srAgentNo }}）<a-tag v-if="record.owner" color="green">{{ $t('擁有者') }}</a-tag></template>
            <template v-if="column.key === 'op'">
              <a-button v-if="!record.owner" type="link" style="color: red" @click="revoke(record)">{{ $t('收回') }}</a-button>
            </template>
          </template>
        </a-table>
        <div style="display: flex; gap: 8px; margin-top: 12px">
          <a-select v-model:value="vdata.share.target" style="flex: 1" :placeholder="$t('選擇要加派的團長')" show-search option-filter-prop="label" :options="grantOptions" />
          <a-button type="primary" :disabled="!vdata.share.target" @click="grant">{{ $t('加派') }}</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_CHANNEL_ACCOUNT, API_URL_IFDEFINES_LIST, req } from '@/api/manage'
import { computed, reactive, watch, getCurrentInstance } from 'vue'
import { isMobile } from '@/views/mobile/mobileKit'
import { t } from '@/i18n'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const props = defineProps({
  // 有值：只看這位團長的帳號（團長詳情頁）；空字串：上帝看全部（渠道管理）
  srAgentNo: { type: String, default: '' },
  agentName: { type: String, default: '' },
})

const columns = [
  { title: t('帳號名稱'), dataIndex: 'accountName' },
  { key: 'ifCode', title: t('支付接口') },
  { key: 'owner', title: t('所屬團長') },
  { key: 'scope', title: t('使用範圍') },
  { key: 'shared', title: t('共用給') },
  { key: 'state', title: t('狀態') },
  { key: 'op', title: t('操作'), width: '260px', align: 'center' },
]
const shareColumns = [
  { key: 'agent', title: t('可使用的團長') },
  { key: 'op', title: '', width: '80px' },
]

const emptyForm = () => ({ open: false, saving: false, accountId: '', ownerSrAgentNo: undefined, ownerName: '', ifCode: undefined, accountName: '', state: 1, remark: '', fields: [] as any[], params: {} as any, masked: {} as any })
const vdata: any = reactive({
  loading: false,
  records: [],
  defines: [],
  seniors: [],
  form: emptyForm(),
  scope: { open: false, saving: false, accountId: '', accountName: '', srAgentNo: '', agentNos: [], options: [] },
  share: { open: false, saving: false, accountId: '', accountName: '', shareable: false, agents: [], target: undefined },
})

const grantOptions = computed(() =>
  vdata.seniors
    .filter((a) => !vdata.share.agents.some((g) => g.srAgentNo === a.agentNo))
    .map((a) => ({ value: a.agentNo, label: `${a.agentName}（${a.agentNo}）` })),
)

// 使用範圍以帳號擁有者（或目前這位團長）為準
function scopeOwner(record) {
  return props.srAgentNo || record.ownerSrAgentNo
}
function scopeOf(record) {
  return (record.scopes || []).filter((s) => s.srAgentNo === scopeOwner(record))
}
function openScope(record) {
  const sr = scopeOwner(record)
  vdata.scope = { open: true, saving: false, accountId: record.accountId, accountName: record.accountName, srAgentNo: sr, agentNos: scopeOf(record).map((s) => s.agentNo), options: [] }
  req.list(API_URL_AGENT_INFO, { parentAgentNo: sr, pageSize: -1 }).then((res) => {
    vdata.scope.options = (res.records || []).map((a) => ({ value: a.agentNo, label: `${a.agentName}（${a.agentNo}）` }))
  })
}
function saveScope() {
  const sc = vdata.scope
  sc.saving = true
  req
    .updateById(API_URL_CHANNEL_ACCOUNT, sc.accountId + '/scope', { srAgentNo: sc.srAgentNo, agentNos: sc.agentNos })
    .then(() => {
      sc.open = false
      $infoBox.message.success(t('儲存成功'))
      load()
    })
    .finally(() => (sc.saving = false))
}

function sharedNames(record) {
  return (record.agents || [])
    .filter((g) => !g.owner)
    .map((g) => g.agentName || g.srAgentNo)
    .join('、')
}

function load() {
  vdata.loading = true
  return req
    .list(API_URL_CHANNEL_ACCOUNT, props.srAgentNo ? { srAgentNo: props.srAgentNo } : {})
    .then((res) => (vdata.records = res || []))
    .finally(() => (vdata.loading = false))
}

function loadSeniors() {
  return req.list(API_URL_AGENT_INFO, { agentLevel: 1, state: 1, pageSize: -1 }).then((res) => (vdata.seniors = res.records || []))
}

function loadDefines() {
  const seniors = props.srAgentNo ? Promise.resolve() : loadSeniors()
  if (vdata.defines.length) return seniors
  return Promise.all([
    seniors,
    req.list(API_URL_IFDEFINES_LIST, { state: 1 }).then((res) => {
      vdata.defines = (res || []).filter((d) => d.isMchMode === 1 && d.state === 1)
    }),
  ])
}

// 依接口定義產生金鑰欄位
function buildFields(ifCode) {
  const define = vdata.defines.find((d) => d.ifCode === ifCode)
  let list: any[] = []
  try {
    list = JSON.parse((define && define.normalMchParams) || '[]')
  } catch (e) {
    list = []
  }
  return (Array.isArray(list) ? list : []).map((item) => ({
    ...item,
    options: item.type === 'radio' ? item.values.split(',').map((v, i) => ({ value: v, title: item.titles.split(',')[i] })) : [],
  }))
}

function onIfCodeChange(ifCode) {
  vdata.form.fields = buildFields(ifCode)
  vdata.form.params = {}
  vdata.form.masked = {}
}

function openForm(record?) {
  loadDefines().then(() => {
    vdata.form = emptyForm()
    if (!record) {
      vdata.form.open = true
      return
    }
    req.getById(API_URL_CHANNEL_ACCOUNT, record.accountId).then((res) => {
      const masked = JSON.parse(res.ifParams || '{}')
      const fields = buildFields(res.ifCode)
      const params: any = {}
      // 遮罩欄位留空表示不修改，其餘欄位帶出現值
      fields.forEach((f) => (params[f.name] = f.star === '1' ? '' : masked[f.name]))
      Object.assign(vdata.form, { open: true, ownerName: record.ownerName || record.ownerSrAgentNo, accountId: res.accountId, ifCode: res.ifCode, accountName: res.accountName, state: res.state, remark: res.remark, fields, params, masked })
    })
  })
}

function saveForm() {
  const f = vdata.form
  const owner = props.srAgentNo || f.ownerSrAgentNo
  if (!f.ifCode || !f.accountName || (!f.accountId && !owner)) {
    $infoBox.message.warning(t('請選擇所屬團長與支付接口，並填寫帳號名稱'))
    return
  }
  const params: any = {}
  for (const field of f.fields) {
    const v = f.params[field.name]
    const empty = v === undefined || v === null || v === ''
    // 修改時遮罩欄位留空 = 保留原值
    if (empty && f.accountId && field.star === '1') continue
    if (empty && field.verify === 'required') {
      $infoBox.message.warning(t('請填寫') + field.desc)
      return
    }
    if (!empty) params[field.name] = v
  }
  const body: any = { accountName: f.accountName, state: f.state, remark: f.remark, ifParams: Object.keys(params).length ? JSON.stringify(params) : '' }
  f.saving = true
  const call = f.accountId
    ? req.updateById(API_URL_CHANNEL_ACCOUNT, f.accountId, body)
    : req.add(API_URL_CHANNEL_ACCOUNT, { ...body, ifCode: f.ifCode, ownerSrAgentNo: owner })
  call
    .then(() => {
      f.open = false
      $infoBox.message.success(t('儲存成功'))
      load()
    })
    .finally(() => (f.saving = false))
}

function removeAccount(record) {
  $infoBox.confirmDanger(t('確認刪除？'), t('將刪除渠道帳號「{0}」與它的金鑰', [record.accountName]), () => {
    req.delById(API_URL_CHANNEL_ACCOUNT, record.accountId).then(() => {
      $infoBox.message.success(t('刪除成功'))
      load()
    })
  })
}

function syncShare(accountId) {
  const record = vdata.records.find((r) => r.accountId === accountId)
  if (record) {
    vdata.share.shareable = record.shareable === 1
    vdata.share.agents = record.agents || []
  }
}

function openShare(record) {
  vdata.share = { open: true, saving: false, accountId: record.accountId, accountName: record.accountName, shareable: false, agents: [], target: undefined }
  syncShare(record.accountId)
  loadSeniors()
}

function toggleShareable(checked) {
  vdata.share.saving = true
  req
    .updateById(API_URL_CHANNEL_ACCOUNT, vdata.share.accountId, { shareable: checked ? 1 : 0 })
    .then(() => load())
    .then(() => syncShare(vdata.share.accountId))
    .finally(() => (vdata.share.saving = false))
}

function grant() {
  req
    .add(`${API_URL_CHANNEL_ACCOUNT}/${vdata.share.accountId}/agents`, { srAgentNo: vdata.share.target })
    .then(() => {
      vdata.share.target = undefined
      return load()
    })
    .then(() => syncShare(vdata.share.accountId))
}

function revoke(row) {
  req
    .delById(`${API_URL_CHANNEL_ACCOUNT}/${vdata.share.accountId}/agents`, row.srAgentNo)
    .then(() => load())
    .then(() => syncShare(vdata.share.accountId))
}

watch(() => props.srAgentNo, load, { immediate: true })
</script>
