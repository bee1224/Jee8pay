<template>
  <!-- 費率設定（/api/feeRules）：先選支付方式，再看平台預設、各代理、商戶覆寫；平臺費變更需要另一位管理者覆核 -->
  <div class="m-chips">
    <button v-for="w in vdata.ways" :key="w.wayCode" :class="{ on: vdata.wayCode === w.wayCode }" @click="setWay(w.wayCode)">{{ w.wayName }}</button>
  </div>

  <template v-if="vdata.pending.length">
    <div class="m-section-title"><h3>{{ $t('待覆核（{0}）', [vdata.pending.length]) }}</h3></div>
    <div class="m-list">
      <div v-for="p in vdata.pending" :key="p.reqId" class="m-row" style="flex-wrap: wrap">
        <div class="main">
          <b>{{ targetText(p) }}・{{ LAYERS[p.layer] }}</b>
          <small>{{ p.wayCode }}・{{ ruleText(p) }}</small>
          <small>{{ $t('申請人 {0}・{1}', [p.requesterName, p.createdAt]) }}</small>
        </div>
        <div v-if="$access('ENT_FEE_RULE_REVIEW')" style="display: flex; gap: 6px">
          <button class="m-btn sm danger" @click="review(p, 'reject')">{{ $t('駁回') }}</button>
          <button class="m-btn sm" @click="review(p, 'approve')">{{ $t('核准') }}</button>
        </div>
      </div>
    </div>
  </template>

  <div class="m-section-title"><h3>{{ $t('平台預設') }}</h3><span class="m-muted">{{ $t('所有商戶的基本成本') }}</span></div>
  <div class="m-list">
    <button v-for="l in ['PLATFORM', 'CHANNEL']" :key="l" class="m-row tap" @click="open('DEFAULT', '', l, defaultRule(l), canPlatform)">
      <div class="main"><b>{{ LAYERS[l] }}</b><small>{{ l === 'PLATFORM' ? $t('平台向團長收取，只有上帝能改，需覆核') : $t('上游渠道的成本') }}</small></div>
      <b class="num">{{ ruleText(defaultRule(l)) }}</b>
    </button>
  </div>

  <div class="m-section-title"><h3>{{ $t('代理') }}</h3><span class="m-muted">{{ $t('{0} 位', [vdata.agents.length]) }}</span></div>
  <div class="m-search"><SearchOutlined /><input v-model="vdata.q" type="search" :placeholder="$t('搜尋代理名稱')" /></div>
  <div class="m-list">
    <button v-for="a in agentRows" :key="a.agentNo" class="m-row tap" @click="open('AGENT', a.agentNo, a.layer, a.rule, canAgent, a.agentName)">
      <div class="main"><b>{{ a.agentName }}<span class="m-role">{{ a.agentLevel === 1 ? $t('團長') : $t('隊長') }}</span></b><small>{{ LAYERS[a.layer] }}・<span class="mono">{{ a.agentNo }}</span></small></div>
      <b class="num" :style="{ color: a.rule ? '' : 'var(--jp-text-3)' }">{{ ruleText(a.rule) }}</b>
    </button>
  </div>

  <div class="m-section-title"><h3>{{ $t('商戶覆寫') }}</h3><button v-if="canAgent" class="m-btn sm ghost" style="width: auto" @click="open('MCH', '', undefined, null, true)">{{ $t('＋ 新增') }}</button></div>
  <div class="m-list">
    <button v-for="r in vdata.mchRules" :key="r.ruleId" class="m-row tap" @click="open('MCH', r.targetId, r.layer, r, canLayer(r.layer))">
      <div class="main"><b>{{ $t('商戶 {0}', [r.targetId]) }}</b><small>{{ LAYERS[r.layer] }}・{{ r.updatedBy || '' }}</small></div>
      <b class="num">{{ ruleText(r) }}</b>
    </button>
    <div v-if="!vdata.mchRules.length" class="m-empty">{{ $t('這個支付方式沒有商戶覆寫') }}</div>
  </div>

  <MSheet v-model:open="vdata.edit.open" :title="vdata.edit.title">
    <a-form layout="vertical">
      <a-form-item v-if="vdata.edit.targetType === 'MCH' && !vdata.edit.locked" :label="$t('商戶號')" required><a-input v-model:value="vdata.edit.targetId" size="large" /></a-form-item>
      <a-form-item v-if="vdata.edit.targetType === 'MCH' && !vdata.edit.locked" :label="$t('費率層')" required>
        <div class="m-seg"><button v-for="l in ['PLATFORM', 'CHANNEL', 'SR_AGENT', 'AGENT']" :key="l" type="button" :class="{ on: vdata.edit.layer === l }" @click="vdata.edit.layer = l">{{ LAYERS[l] }}</button></div>
      </a-form-item>
      <div class="m-actions">
        <a-form-item :label="$t('百分比')"><a-input-number v-model:value="vdata.edit.pct" :min="0" :max="99.9999" :precision="4" addon-after="%" size="large" style="width: 100%" :disabled="!vdata.edit.can" /></a-form-item>
        <a-form-item :label="$t('每筆固定')"><a-input-number v-model:value="vdata.edit.fixedYuan" :min="0" :precision="2" :addon-after="$t('元')" size="large" style="width: 100%" :disabled="!vdata.edit.can" /></a-form-item>
      </div>
    </a-form>
    <p class="m-muted">{{ $t('儲存後若任何商戶的手續費合計超過商戶費率，系統會拒絕並提示；平臺費會先送覆核。') }}</p>
    <div class="m-actions">
      <button v-if="vdata.edit.rule && vdata.edit.targetType === 'MCH' && vdata.edit.can" class="m-btn danger" @click="remove(vdata.edit.rule)">{{ $t('刪除覆寫') }}</button>
      <button v-else class="m-btn ghost" @click="vdata.edit.open = false">{{ $t('取消') }}</button>
      <button v-if="vdata.edit.can" class="m-btn" @click="save">{{ $t('儲存') }}</button>
    </div>
  </MSheet>
</template>

<script setup lang="ts">
import { computed, reactive, getCurrentInstance } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { API_URL_AGENT_INFO, API_URL_FEE_RULES, API_URL_PAYWAYS_LIST, req } from '@/api/manage'
import MSheet from '../MSheet.vue'
import { t } from '@/i18n'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const LAYERS = { PLATFORM: t('平臺費'), CHANNEL: t('渠道費'), SR_AGENT: t('團長費'), AGENT: t('隊長費') }
const canPlatform = $access('ENT_FEE_RULE_PLATFORM_EDIT')
const canAgent = $access('ENT_FEE_RULE_EDIT')
const canLayer = (l) => (['PLATFORM', 'CHANNEL'].includes(l) ? canPlatform : canAgent)
const vdata: any = reactive({ ways: [], wayCode: '', rules: [], agents: [], mchRules: [], pending: [], q: '', edit: { open: false } })

const toPct = (rate) => Number((Number(rate || 0) * 100).toFixed(4))
const ruleText = (r) => (r ? t('{0}% + {1} 元', [toPct(r.rate), (Number(r.fixedAmount || 0) / 100).toFixed(2)]) : t('未設定'))
const targetText = (p) => (p.targetType === 'DEFAULT' ? t('平台預設') : p.targetType === 'MCH' ? t('商戶 ') + p.targetId : t('代理 ') + p.targetId)
const defaultRule = (l) => vdata.rules.find((x) => x.targetType === 'DEFAULT' && x.layer === l)
const agentRows = computed(() => {
  const q = vdata.q.trim()
  return vdata.agents
    .filter((a) => !q || (a.agentName || '').includes(q) || a.agentNo.includes(q))
    .map((a) => {
      const layer = a.agentLevel === 1 ? 'SR_AGENT' : 'AGENT'
      return { ...a, layer, rule: vdata.rules.find((x) => x.targetType === 'AGENT' && x.targetId === a.agentNo && x.layer === layer) }
    })
})

function load() {
  if (!vdata.wayCode) return
  req.list(API_URL_FEE_RULES, { wayCode: vdata.wayCode, pageSize: -1 }).then((r) => {
    vdata.rules = r.records || []
    vdata.mchRules = vdata.rules.filter((x) => x.targetType === 'MCH')
  })
  req.list(API_URL_FEE_RULES + '/changeReqs', { state: 0, pageSize: -1 }).then((r) => (vdata.pending = r.records || []))
}
function setWay(w) {
  vdata.wayCode = w
  load()
}
function open(targetType, targetId, layer, rule, can, name?) {
  vdata.edit = {
    open: true, can, rule, targetType, targetId, layer, locked: !!rule || targetType !== 'MCH',
    title: targetType === 'DEFAULT' ? t('平台預設・{0}', [LAYERS[layer]]) : targetType === 'AGENT' ? `${name}・${LAYERS[layer]}` : rule ? t('商戶 {0}・{1}', [targetId, LAYERS[layer]]) : t('新增商戶覆寫'),
    pct: rule ? toPct(rule.rate) : 0, fixedYuan: rule ? Number(rule.fixedAmount || 0) / 100 : 0,
  }
}
function save() {
  const e = vdata.edit
  if (e.targetType === 'MCH' && (!e.targetId || !e.layer)) return $infoBox.message.warning(t('請填寫商戶號並選擇費率層'))
  req
    .add(API_URL_FEE_RULES, { wayCode: vdata.wayCode, targetType: e.targetType, targetId: e.targetId, layer: e.layer, rate: (Number(e.pct || 0) / 100).toFixed(6), fixedAmount: Math.round(Number(e.fixedYuan || 0) * 100) })
    .then((res) => {
      $infoBox.message[res && res.pendingReview ? 'info' : 'success'](res && res.pendingReview ? t('已送出覆核，需另一位管理者核准後才生效') : t('已儲存'))
      e.open = false
      load()
    })
}
function remove(rule) {
  $infoBox.confirmDanger(t('刪除這筆商戶覆寫？'), t('刪除後改用代理與平台預設的費率'), () => {
    req.delById(API_URL_FEE_RULES, rule.ruleId).then(() => {
      $infoBox.message.success(t('已刪除'))
      vdata.edit.open = false
      load()
    })
  })
}
function review(p, action) {
  $infoBox.confirmPrimary(action === 'approve' ? t('核准這筆費率申請？') : t('駁回這筆費率申請？'), action === 'approve' ? t('核准後立即生效（申請人不能核准自己的申請）') : '', () => {
    req.add(`${API_URL_FEE_RULES}/changeReqs/${p.reqId}/${action}`, {}).then(() => {
      $infoBox.message.success(action === 'approve' ? t('已核准') : t('已駁回'))
      load()
    })
  })
}
req.list(API_URL_PAYWAYS_LIST, { pageSize: -1 }).then((r) => {
  vdata.ways = r.records || []
  if (vdata.ways.length) setWay(vdata.ways[0].wayCode)
})
req.list(API_URL_AGENT_INFO, { pageSize: -1 }).then((r) => (vdata.agents = r.records || []))
</script>
