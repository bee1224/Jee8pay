<template>
  <!-- 我的商戶（GET /agentPortal/merchants）；新增、重設密碼、渠道測試沿用電腦版同一組 API -->
  <div class="m-search">
    <SearchOutlined />
    <input v-model="vdata.q" type="search" :placeholder="$t('搜尋商戶名稱或商戶號')" />
  </div>
  <button v-if="$access('ENT_AGENT_PORTAL_MCH_ADD')" class="m-btn" @click="openForm">{{ $t('＋ 新增商戶') }}</button>
  <p class="m-muted">{{ $t('共 {0} 家', [rows.length]) }}</p>
  <div class="m-list">
    <button v-for="m in rows" :key="m.mchNo" class="m-row tap" @click="vdata.cur = m">
      <div class="main"><b>{{ m.mchName || m.mchNo }}</b><small><span class="mono">{{ m.mchNo }}</span>{{ $t('・直屬 {0}', [ownerName(m.agentNo)]) }}</small></div>
      <div class="end"><span :class="['m-pill', m.relation === '直屬' ? 'ok' : 'mute']">{{ m.relation ? $t(m.relation) : '—' }}</span></div>
    </button>
    <div v-if="!rows.length" class="m-empty">{{ vdata.loaded ? $t('沒有符合的商戶') : $t('載入中…') }}</div>
  </div>

  <MSheet :open="!!vdata.cur" :title="vdata.cur ? vdata.cur.mchName || vdata.cur.mchNo : ''" @update:open="(v) => !v && (vdata.cur = null)">
    <template v-if="vdata.cur">
      <dl class="m-kv">
        <dt>{{ $t('商戶號') }}</dt><dd class="mono">{{ vdata.cur.mchNo }}</dd>
        <dt>{{ $t('直屬代理') }}</dt><dd>{{ ownerName(vdata.cur.agentNo) }}</dd>
        <dt>{{ $t('關係') }}</dt><dd>{{ $t(vdata.cur.relation || '') }}</dd>
      </dl>
      <div class="m-actions">
        <button v-if="$access('ENT_AGENT_PORTAL_MCH_EDIT')" class="m-btn ghost" @click="resetPwd(vdata.cur)">{{ $t('重設密碼') }}</button>
        <button v-if="$access('ENT_AGENT_PORTAL_CHANNEL_TEST')" class="m-btn" @click="channelTest(vdata.cur)">{{ $t('渠道測試') }}</button>
      </div>
    </template>
  </MSheet>

  <MSheet v-model:open="vdata.form.open" :title="$t('新增商戶')">
    <a-form layout="vertical">
      <a-form-item :label="$t('商戶名稱')" required><a-input v-model:value="vdata.form.data.mchName" size="large" /></a-form-item>
      <a-form-item :label="$t('商戶簡稱')" required><a-input v-model:value="vdata.form.data.mchShortName" size="large" /></a-form-item>
      <a-form-item :label="$t('聯絡人姓名')" required><a-input v-model:value="vdata.form.data.contactName" size="large" /></a-form-item>
      <a-form-item :label="$t('聯絡人手機號')" required><a-input v-model:value="vdata.form.data.contactTel" :placeholder="$t('09 開頭 10 碼')" inputmode="tel" size="large" /></a-form-item>
      <a-form-item :label="$t('商戶登入帳號')" required :extra="$t('儲存後系統產生 8 碼隨機密碼並顯示一次。')">
        <a-input v-model:value="vdata.form.data.loginUsername" :placeholder="$t('4～32 碼英文、數字或底線')" autocomplete="off" size="large" />
      </a-form-item>
      <a-form-item v-if="vdata.me && vdata.me.agentLevel === 1" :label="$t('歸屬代理')">
        <a-select v-model:value="vdata.form.data.agentNo" :options="ownerOptions" size="large" />
      </a-form-item>
    </a-form>
    <p class="m-muted">{{ $t('商戶建立後還需要平台設定支付通道與費率，才能開始收款。') }}</p>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">{{ $t('取消') }}</button>
      <button class="m-btn" :disabled="vdata.form.saving" @click="save">{{ $t('儲存') }}</button>
    </div>
  </MSheet>

  <AgentCredentialModal ref="credentialModal" />
  <ChannelTestModal ref="channelTestModal" />
</template>

<script setup lang="ts">
import { computed, reactive, ref, getCurrentInstance } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import AgentCredentialModal from '@/views/agent/AgentCredentialModal.vue'
import ChannelTestModal from '@/views/agentPortal/sections/ChannelTestModal.vue'
import { agentMe } from '../mobileKit'
import MSheet from '../MSheet.vue'
import { t } from '@/i18n'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const credentialModal = ref()
const channelTestModal = ref()
const vdata: any = reactive({ me: null, list: [], loaded: false, q: '', cur: null, form: { open: false, saving: false, data: {} } })

const rows = computed(() => {
  const q = vdata.q.trim()
  return vdata.list.filter((m) => !q || (m.mchName || '').includes(q) || m.mchNo.includes(q))
})
function ownerName(agentNo) {
  if (!vdata.me) return agentNo
  if (agentNo === vdata.me.agentNo) return t('自己')
  const sub = (vdata.me.subAgents || []).find((a) => a.agentNo === agentNo)
  return sub ? sub.agentName : agentNo
}
const ownerOptions = computed(() =>
  vdata.me
    ? [{ value: vdata.me.agentNo, label: t('自己（{0}）', [vdata.me.agentName]) }].concat(
        (vdata.me.subAgents || []).map((a) => ({ value: a.agentNo, label: t('我的代理：{0}', [a.agentName]) }))
      )
    : []
)
function load() {
  req.list(API_URL_AGENT_PORTAL + '/merchants', {}).then((r) => {
    vdata.list = r || []
    vdata.loaded = true
  })
}
function openForm() {
  vdata.form = { open: true, saving: false, data: { agentNo: vdata.me && vdata.me.agentNo } }
}
function save() {
  const f = vdata.form.data
  if (!f.mchName || !f.mchShortName || !f.contactName || !/^09\d{8}$/.test(f.contactTel || '') || !/^[A-Za-z0-9_]{4,32}$/.test(f.loginUsername || '')) {
    $infoBox.message.warning(t('請填寫商戶名稱、簡稱、聯絡人、正確的手機號，以及 4～32 碼的登入帳號'))
    return
  }
  vdata.form.saving = true
  req
    .add(API_URL_AGENT_PORTAL + '/merchants', f)
    .then((res) => {
      vdata.form.open = false
      load()
      credentialModal.value.show({ ...res, subjectLabel: t('商戶'), loginAt: t('商戶平台') })
    })
    .finally(() => (vdata.form.saving = false))
}
function resetPwd(m) {
  $infoBox.confirmPrimary(t('重設商戶密碼？'), t('商戶 {0} 的登入密碼會換成新的隨機密碼，舊密碼立即失效。', [m.mchName || m.mchNo]), () => {
    req.add(`${API_URL_AGENT_PORTAL}/branch/merchants/${m.mchNo}/resetPassword`, {}).then((res) => {
      vdata.cur = null
      credentialModal.value.show({ ...res, subjectLabel: t('商戶'), loginAt: t('商戶平台') })
    })
  })
}
function channelTest(m) {
  vdata.cur = null
  channelTestModal.value.show(m)
}
agentMe().then((me) => (vdata.me = me)).catch(() => {})
load()
</script>
