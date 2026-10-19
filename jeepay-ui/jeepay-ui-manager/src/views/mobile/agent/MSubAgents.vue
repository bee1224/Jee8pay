<template>
  <!-- 我的代理（資料來自 GET /agentPortal/me 的 subAgents；新增走 POST /agentPortal/subAgents） -->
  <p v-if="vdata.me && vdata.me.agentLevel !== 1" class="m-muted">只有團長可以建立代理。</p>
  <template v-else>
    <button v-if="$access('ENT_AGENT_PORTAL_SUB_ADD')" class="m-btn" @click="openForm">＋ 新增代理</button>
    <p class="m-muted">共 {{ subs.length }} 位</p>
    <div class="m-list">
      <div v-for="a in subs" :key="a.agentNo" class="m-row">
        <div class="m-avatar" style="width: 38px; height: 38px">{{ (a.agentName || '?').slice(0, 1) }}</div>
        <div class="main">
          <b>{{ a.agentName }}<span class="m-role">隊長</span></b>
          <small><span class="mono">{{ a.agentNo }}</span>{{ a.contactName ? '・' + a.contactName : '' }}</small>
        </div>
        <span :class="['m-pill', a.state === 0 ? 'mute' : 'ok']">{{ a.state === 0 ? '停用' : '啟用' }}</span>
      </div>
      <div v-if="!subs.length" class="m-empty">{{ vdata.me ? '還沒有代理' : '載入中…' }}</div>
    </div>
  </template>

  <MSheet v-model:open="vdata.form.open" title="新增代理">
    <a-form layout="vertical">
      <a-form-item label="代理名稱" required><a-input v-model:value="vdata.form.data.agentName" size="large" /></a-form-item>
      <a-form-item label="聯絡人姓名"><a-input v-model:value="vdata.form.data.contactName" size="large" /></a-form-item>
      <a-form-item label="聯絡人手機號（選填）"><a-input v-model:value="vdata.form.data.contactTel" placeholder="09 開頭 10 碼" inputmode="tel" size="large" /></a-form-item>
      <a-form-item label="登入帳號" required extra="儲存後系統產生 8 碼隨機密碼並顯示一次。">
        <a-input v-model:value="vdata.form.data.loginUsername" placeholder="2～32 碼中文、英文、數字或底線" autocomplete="off" size="large" />
      </a-form-item>
    </a-form>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">取消</button>
      <button class="m-btn" :disabled="vdata.form.saving" @click="save">儲存</button>
    </div>
  </MSheet>
  <AgentCredentialModal ref="credentialModal" />
</template>

<script setup lang="ts">
import { computed, reactive, ref, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import AgentCredentialModal from '@/views/agent/AgentCredentialModal.vue'
import { agentMe } from '../mobileKit'
import MSheet from '../MSheet.vue'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const credentialModal = ref()
const vdata: any = reactive({ me: null, form: { open: false, saving: false, data: {} } })
const subs = computed(() => (vdata.me && vdata.me.subAgents) || [])

function openForm() {
  vdata.form = { open: true, saving: false, data: {} }
}
function save() {
  const f = vdata.form.data
  if (!f.agentName || !/^[A-Za-z0-9_一-鿿]{2,32}$/.test(f.loginUsername || '')) {
    $infoBox.message.warning('請填寫代理名稱，以及2～32 碼中文、英文、數字或底線的登入帳號')
    return
  }
  if (f.contactTel && !/^09\d{8}$/.test(f.contactTel)) {
    $infoBox.message.warning('手機號格式不正確（09 開頭 10 碼），不填也可以')
    return
  }
  vdata.form.saving = true
  req
    .add(API_URL_AGENT_PORTAL + '/subAgents', f)
    .then((res) => {
      vdata.form.open = false
      agentMe(true).then((me) => (vdata.me = me))
      credentialModal.value.show({ ...res, subjectLabel: '我的代理', loginAt: '營運平台' })
    })
    .finally(() => (vdata.form.saving = false))
}
agentMe().then((me) => (vdata.me = me)).catch(() => {})
</script>
