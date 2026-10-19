<template>
  <!-- 費率（GET/POST /agentPortal/branch/feeRules）：團長可設自己的團長費、隊長費或單一商戶覆寫 -->
  <button v-if="canEdit" class="m-btn" @click="openForm()">＋ 設定費率</button>
  <p class="m-muted">平臺費與渠道費由平台設定。儲存後若任何我的商戶的手續費合計超過其商戶費率，系統會拒絕並提示。</p>
  <div class="m-list">
    <button v-for="r in vdata.rules" :key="r.ruleId" class="m-row" :class="{ tap: canEdit }" @click="canEdit && openForm(r)">
      <div class="main">
        <b>{{ targetName(r) }}</b>
        <small>{{ LAYERS[r.layer] || r.layer }}・{{ r.wayCode }}</small>
      </div>
      <div class="end"><b class="num">{{ (Number(r.rate) * 100).toFixed(4).replace(/\.?0+$/, '') }}%</b><small class="m-muted num" style="margin: 0">+ {{ ntd(r.fixedAmount, 2) }}</small></div>
    </button>
    <div v-if="!vdata.rules.length" class="m-empty">{{ vdata.loaded ? '尚未設定費率' : '載入中…' }}</div>
  </div>

  <MSheet v-model:open="vdata.form.open" title="設定費率">
    <a-form layout="vertical">
      <a-form-item label="設定對象">
        <a-select v-model:value="vdata.form.targetKey" :options="targetOptions" show-search option-filter-prop="label" size="large" @change="onTarget" />
      </a-form-item>
      <a-form-item label="費率層"><a-select v-model:value="vdata.form.layer" :options="layerOptions" size="large" /></a-form-item>
      <a-form-item label="支付方式">
        <a-select v-model:value="vdata.form.wayCode" :options="((vdata.me && vdata.me.payWays) || []).map((w) => ({ value: w.wayCode, label: w.wayName }))" size="large" />
      </a-form-item>
      <div class="m-actions">
        <a-form-item label="百分比"><a-input-number v-model:value="vdata.form.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" size="large" style="width: 100%" /></a-form-item>
        <a-form-item label="每筆固定"><a-input-number v-model:value="vdata.form.fixedYuan" :min="0" :precision="2" addon-after="元" size="large" style="width: 100%" /></a-form-item>
      </div>
    </a-form>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">取消</button>
      <button class="m-btn" @click="save">儲存</button>
    </div>
  </MSheet>
</template>

<script setup lang="ts">
import { computed, reactive, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { agentMe, ntd } from '../mobileKit'
import MSheet from '../MSheet.vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const LAYERS = { SR_AGENT: '團長費', AGENT: '隊長費' }
const vdata: any = reactive({ me: null, merchants: [], rules: [], loaded: false, form: { open: false } })
const canEdit = computed(() => vdata.me && vdata.me.agentLevel === 1 && $access('ENT_AGENT_PORTAL_FEE_EDIT'))

const agentTargets = computed(() =>
  vdata.me
    ? [{ value: 'AGENT:' + vdata.me.agentNo, label: `自己（${vdata.me.agentName}）` }].concat(
        (vdata.me.subAgents || []).map((a) => ({ value: 'AGENT:' + a.agentNo, label: `我的代理：${a.agentName}` }))
      )
    : []
)
const targetOptions = computed(() => agentTargets.value.concat(vdata.merchants.map((m) => ({ value: 'MCH:' + m.mchNo, label: `商戶：${m.mchName || m.mchNo}` }))))
const layerOptions = computed(() => {
  const f = vdata.form
  const all = [{ value: 'SR_AGENT', label: '團長費' }, { value: 'AGENT', label: '隊長費' }]
  if (f.targetType !== 'AGENT') return all
  return all.filter((l) => l.value === (f.targetId === vdata.me.agentNo ? 'SR_AGENT' : 'AGENT'))
})
function targetName(r) {
  const hit = targetOptions.value.find((o) => o.value === `${r.targetType}:${r.targetId}`)
  return hit ? hit.label : `${r.targetType === 'MCH' ? '商戶' : '代理'} ${r.targetId}`
}
function onTarget(key) {
  const [type, id] = String(key).split(':')
  vdata.form.targetType = type
  vdata.form.targetId = id
  vdata.form.layer = type === 'AGENT' ? (id === vdata.me.agentNo ? 'SR_AGENT' : 'AGENT') : undefined
}
function openForm(r?) {
  vdata.form = r
    ? { open: true, targetKey: `${r.targetType}:${r.targetId}`, targetType: r.targetType, targetId: r.targetId, layer: r.layer, wayCode: r.wayCode, pct: Number(r.rate) * 100, fixedYuan: r.fixedAmount / 100 }
    : { open: true, targetKey: undefined, targetType: undefined, targetId: undefined, layer: undefined, wayCode: undefined, pct: 0, fixedYuan: 0 }
}
function loadRules() {
  req.list(API_URL_AGENT_PORTAL + '/branch/feeRules', {}).then((r) => {
    vdata.rules = r || []
    vdata.loaded = true
  })
}
function save() {
  const f = vdata.form
  if (!f.targetId || !f.layer || !f.wayCode) {
    $infoBox.message.warning('請選擇設定對象、費率層與支付方式')
    return
  }
  req
    .add(API_URL_AGENT_PORTAL + '/branch/feeRules', {
      targetType: f.targetType,
      targetId: f.targetId,
      layer: f.layer,
      wayCode: f.wayCode,
      rate: (Number(f.pct || 0) / 100).toFixed(6),
      fixedAmount: Math.round(Number(f.fixedYuan || 0) * 100),
    })
    .then(() => {
      $infoBox.message.success('已儲存')
      f.open = false
      loadRules()
    })
}
agentMe().then((me) => (vdata.me = me)).catch(() => {})
req.list(API_URL_AGENT_PORTAL + '/merchants', {}).then((r) => (vdata.merchants = r || []))
loadRules()
</script>
