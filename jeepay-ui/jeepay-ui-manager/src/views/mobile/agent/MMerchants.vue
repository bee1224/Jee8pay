<template>
  <!-- 我的商戶（GET /agentPortal/merchants）；新增、重設密碼、渠道測試沿用電腦版同一組 API -->
  <div class="m-search">
    <SearchOutlined />
    <input v-model="vdata.q" type="search" placeholder="搜尋商戶名稱或商戶號" />
  </div>
  <button v-if="$access('ENT_AGENT_PORTAL_MCH_ADD')" class="m-btn" @click="openForm">＋ 新增商戶</button>
  <p class="m-muted">共 {{ rows.length }} 家</p>
  <div class="m-list">
    <button v-for="m in rows" :key="m.mchNo" class="m-row tap" @click="vdata.cur = m">
      <div class="main"><b>{{ m.mchName || m.mchNo }}</b><small><span class="mono">{{ m.mchNo }}</span>・直屬 {{ ownerName(m.agentNo) }}</small></div>
      <div class="end"><span :class="['m-pill', m.relation === '直屬' ? 'ok' : 'mute']">{{ m.relation || '—' }}</span></div>
    </button>
    <div v-if="!rows.length" class="m-empty">{{ vdata.loaded ? '沒有符合的商戶' : '載入中…' }}</div>
  </div>

  <MSheet :open="!!vdata.cur" :title="vdata.cur ? vdata.cur.mchName || vdata.cur.mchNo : ''" @update:open="(v) => !v && (vdata.cur = null)">
    <template v-if="vdata.cur">
      <dl class="m-kv">
        <dt>商戶號</dt><dd class="mono">{{ vdata.cur.mchNo }}</dd>
        <dt>直屬代理</dt><dd>{{ ownerName(vdata.cur.agentNo) }}</dd>
        <dt>關係</dt><dd>{{ vdata.cur.relation }}</dd>
      </dl>
      <div class="m-actions">
        <button v-if="$access('ENT_AGENT_PORTAL_MCH_EDIT')" class="m-btn ghost" @click="resetPwd(vdata.cur)">重設密碼</button>
        <button v-if="$access('ENT_AGENT_PORTAL_CHANNEL_TEST')" class="m-btn" @click="channelTest(vdata.cur)">渠道測試</button>
      </div>
    </template>
  </MSheet>

  <MSheet v-model:open="vdata.form.open" title="新增商戶">
    <a-form layout="vertical">
      <a-form-item label="商戶名稱" required><a-input v-model:value="vdata.form.data.mchName" size="large" /></a-form-item>
      <a-form-item label="商戶簡稱" required><a-input v-model:value="vdata.form.data.mchShortName" size="large" /></a-form-item>
      <a-form-item label="聯絡人姓名" required><a-input v-model:value="vdata.form.data.contactName" size="large" /></a-form-item>
      <a-form-item label="聯絡人手機號" required><a-input v-model:value="vdata.form.data.contactTel" placeholder="09 開頭 10 碼" inputmode="tel" size="large" /></a-form-item>
      <a-form-item label="商戶登入帳號" required extra="儲存後系統產生 8 碼隨機密碼並顯示一次。">
        <a-input v-model:value="vdata.form.data.loginUsername" placeholder="4～32 碼英文、數字或底線" autocomplete="off" size="large" />
      </a-form-item>
      <a-form-item v-if="vdata.me && vdata.me.agentLevel === 1" label="歸屬代理">
        <a-select v-model:value="vdata.form.data.agentNo" :options="ownerOptions" size="large" />
      </a-form-item>
    </a-form>
    <p class="m-muted">商戶建立後還需要平台設定支付通道與費率，才能開始收款。</p>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">取消</button>
      <button class="m-btn" :disabled="vdata.form.saving" @click="save">儲存</button>
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
  if (agentNo === vdata.me.agentNo) return '自己'
  const sub = (vdata.me.subAgents || []).find((a) => a.agentNo === agentNo)
  return sub ? sub.agentName : agentNo
}
const ownerOptions = computed(() =>
  vdata.me
    ? [{ value: vdata.me.agentNo, label: `自己（${vdata.me.agentName}）` }].concat(
        (vdata.me.subAgents || []).map((a) => ({ value: a.agentNo, label: `我的代理：${a.agentName}` }))
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
    $infoBox.message.warning('請填寫商戶名稱、簡稱、聯絡人、正確的手機號，以及 4～32 碼的登入帳號')
    return
  }
  vdata.form.saving = true
  req
    .add(API_URL_AGENT_PORTAL + '/merchants', f)
    .then((res) => {
      vdata.form.open = false
      load()
      credentialModal.value.show({ ...res, subjectLabel: '商戶', loginAt: '商戶平台' })
    })
    .finally(() => (vdata.form.saving = false))
}
function resetPwd(m) {
  $infoBox.confirmPrimary('重設商戶密碼？', `商戶 ${m.mchName || m.mchNo} 的登入密碼會換成新的隨機密碼，舊密碼立即失效。`, () => {
    req.add(`${API_URL_AGENT_PORTAL}/branch/merchants/${m.mchNo}/resetPassword`, {}).then((res) => {
      vdata.cur = null
      credentialModal.value.show({ ...res, subjectLabel: '商戶', loginAt: '商戶平台' })
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
