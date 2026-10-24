<template>
  <!-- 我的代理（資料來自 GET /agentPortal/me 的 subAgents；新增走 POST /agentPortal/subAgents） -->
  <p v-if="vdata.me && vdata.me.agentLevel !== 1" class="m-muted">{{ $t('只有團長可以建立代理。') }}</p>
  <template v-else>
    <button v-if="$access('ENT_AGENT_PORTAL_SUB_ADD')" class="m-btn" @click="openForm">{{ $t('＋ 新增代理') }}</button>
    <p class="m-muted">{{ $t('共 {0} 位', [subs.length]) }}</p>
    <div class="m-list">
      <div v-for="a in subs" :key="a.agentNo" class="m-row">
        <div class="m-avatar" style="width: 38px; height: 38px">{{ (a.agentName || '?').slice(0, 1) }}</div>
        <div class="main">
          <b>{{ a.agentName }}<span class="m-role">{{ $t('隊長') }}</span></b>
          <small><span class="mono">{{ a.agentNo }}</span>{{ a.contactName ? '・' + a.contactName : '' }}</small>
        </div>
        <span :class="['m-pill', a.state === 0 ? 'mute' : 'ok']">{{ a.state === 0 ? $t('停用') : $t('啟用') }}</span>
      </div>
      <div v-if="!subs.length" class="m-empty">{{ vdata.me ? $t('還沒有代理') : $t('載入中…') }}</div>
    </div>
  </template>

  <MSheet v-model:open="vdata.form.open" :title="$t('新增代理')">
    <a-form layout="vertical">
      <a-form-item :label="$t('代理名稱')" required><a-input v-model:value="vdata.form.data.agentName" size="large" /></a-form-item>
      <a-form-item :label="$t('聯絡人姓名')"><a-input v-model:value="vdata.form.data.contactName" size="large" /></a-form-item>
      <a-form-item :label="$t('聯絡人手機號（選填）')"><a-input v-model:value="vdata.form.data.contactTel" :placeholder="$t('09 開頭 10 碼')" inputmode="tel" size="large" /></a-form-item>
      <a-form-item :label="$t('登入帳號')" required :extra="$t('儲存後系統產生 8 碼隨機密碼並顯示一次。')">
        <a-input v-model:value="vdata.form.data.loginUsername" :placeholder="$t('2～32 碼中文、英文、數字或底線')" autocomplete="off" size="large" />
      </a-form-item>
    </a-form>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">{{ $t('取消') }}</button>
      <button class="m-btn" :disabled="vdata.form.saving" @click="save">{{ $t('儲存') }}</button>
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
import { t } from '@/i18n'

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
    $infoBox.message.warning(t('請填寫代理名稱，以及2～32 碼中文、英文、數字或底線的登入帳號'))
    return
  }
  if (f.contactTel && !/^09\d{8}$/.test(f.contactTel)) {
    $infoBox.message.warning(t('手機號格式不正確（09 開頭 10 碼），不填也可以'))
    return
  }
  vdata.form.saving = true
  req
    .add(API_URL_AGENT_PORTAL + '/subAgents', f)
    .then((res) => {
      vdata.form.open = false
      agentMe(true).then((me) => (vdata.me = me))
      credentialModal.value.show({ ...res, subjectLabel: t('我的代理'), loginAt: t('營運平台') })
    })
    .finally(() => (vdata.form.saving = false))
}
agentMe().then((me) => (vdata.me = me)).catch(() => {})
</script>
