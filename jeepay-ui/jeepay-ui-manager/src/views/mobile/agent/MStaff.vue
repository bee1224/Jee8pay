<template>
  <!-- 我的子帳號（/agentPortal/staff）：建立、權限、啟用／停用、重設密碼 -->
  <button class="m-btn" @click="openForm">＋ 建立子帳號</button>
  <p class="m-muted">子帳號建立後沒有任何權限，請按「權限」勾選他可以看到的選單與按鈕。</p>
  <div class="m-list">
    <div v-for="s in vdata.list" :key="s.sysUserId" class="m-row">
      <div class="main">
        <b>{{ s.realname }}</b>
        <small>帳號 <span class="mono">{{ s.loginUsername }}</span></small>
        <small><span :class="['m-pill', s.entCount ? 'ok' : 'warn']">{{ s.entCount ? '已設定權限' : '尚未設定權限' }}</span></small>
      </div>
      <div class="end">
        <a-switch :checked="s.state === 1" size="small" @change="(v) => setState(s, v)" />
        <div style="display: flex; gap: 6px">
          <button class="m-btn sm ghost" @click="openEnts(s)">權限</button>
          <button class="m-btn sm ghost" @click="resetPwd(s)">重設密碼</button>
        </div>
      </div>
    </div>
    <div v-if="!vdata.list.length" class="m-empty">{{ vdata.loaded ? '尚未建立子帳號' : '載入中…' }}</div>
  </div>

  <MSheet v-model:open="vdata.form.open" title="建立子帳號">
    <a-form layout="vertical">
      <a-form-item label="帳號" required extra="儲存後系統產生 8 碼隨機密碼並顯示一次。">
        <a-input v-model:value="vdata.form.data.loginUsername" placeholder="2～32 碼中文、英文、數字或底線" autocomplete="off" size="large" />
      </a-form-item>
      <a-form-item label="名稱" required><a-input v-model:value="vdata.form.data.realname" size="large" /></a-form-item>
      <a-form-item label="手機號（選填）"><a-input v-model:value="vdata.form.data.telphone" placeholder="09 開頭 10 碼" inputmode="tel" size="large" /></a-form-item>
    </a-form>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">取消</button>
      <button class="m-btn" :disabled="vdata.form.saving" @click="save">儲存</button>
    </div>
  </MSheet>

  <MSheet v-model:open="vdata.ents.open" :title="'設定權限：' + vdata.ents.name">
    <div style="display: flex; gap: 8px">
      <button class="m-btn sm ghost" @click="vdata.ents.checked = allKeys.slice()">全選</button>
      <button class="m-btn sm ghost" @click="vdata.ents.checked = []">全部取消</button>
    </div>
    <a-tree v-model:checkedKeys="vdata.ents.checked" checkable check-strictly default-expand-all :tree-data="tree" :selectable="false" @check="onCheck" />
    <p class="m-muted">只能從你自己擁有的權限裡挑選；「我的子帳號」本身不能下放。儲存後立即生效。</p>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.ents.open = false">取消</button>
      <button class="m-btn" :disabled="vdata.ents.saving" @click="saveEnts">儲存</button>
    </div>
  </MSheet>
  <AgentCredentialModal ref="credentialModal" />
</template>

<script setup lang="ts">
import { computed, reactive, ref, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import AgentCredentialModal from '@/views/agent/AgentCredentialModal.vue'
import MSheet from '../MSheet.vue'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const URL = API_URL_AGENT_PORTAL + '/staff'
// 所有子帳號都會自動帶上的基礎權限，不列在樹上（與電腦版 BranchStaff 相同）
const BASE = ['ENT_COMMONS', 'ENT_C_USERINFO', 'ENT_AGENT_PORTAL', 'ENT_AGENT_PORTAL_VIEW']
const credentialModal = ref()
const vdata: any = reactive({
  list: [],
  loaded: false,
  grantable: [],
  form: { open: false, saving: false, data: {} },
  ents: { open: false, saving: false, sysUserId: null, name: '', checked: [] },
})
const tree = computed(() => {
  const items = vdata.grantable.filter((e) => !BASE.includes(e.entId))
  const ids = new Set(items.map((e) => e.entId))
  const node = (e) => ({ key: e.entId, title: e.entName, children: items.filter((c) => c.pid === e.entId).map(node) })
  return items.filter((e) => !ids.has(e.pid)).map(node)
})
const allKeys = computed(() => vdata.grantable.filter((e) => !BASE.includes(e.entId)).map((e) => e.entId))

function load() {
  req.list(URL, {}).then((r) => {
    vdata.list = r || []
    vdata.loaded = true
  })
}
function openForm() {
  vdata.form = { open: true, saving: false, data: {} }
}
function save() {
  const f = vdata.form.data
  if (!/^[A-Za-z0-9_一-鿿]{2,32}$/.test(f.loginUsername || '') || !f.realname) {
    $infoBox.message.warning('請填寫2～32 碼中文、英文、數字或底線的帳號與名稱')
    return
  }
  if (f.telphone && !/^09\d{8}$/.test(f.telphone)) {
    $infoBox.message.warning('手機號格式不正確（09 開頭 10 碼），不填也可以')
    return
  }
  vdata.form.saving = true
  req
    .add(URL, f)
    .then((res) => {
      vdata.form.open = false
      load()
      credentialModal.value.show({ ...res, subjectLabel: '子帳號', loginAt: '營運平台（請先設定權限）' })
    })
    .finally(() => (vdata.form.saving = false))
}
function openEnts(s) {
  vdata.ents = { open: true, saving: false, sysUserId: s.sysUserId, name: s.realname || s.loginUsername, checked: [] }
  const loadGrantable = vdata.grantable.length ? Promise.resolve() : req.list(URL + '/grantable', {}).then((r) => (vdata.grantable = r || []))
  loadGrantable.then(() =>
    req.list(`${URL}/${s.sysUserId}/ents`, {}).then((r) => (vdata.ents.checked = (r || []).filter((id) => !BASE.includes(id))))
  )
}
// 勾按鈕時自動帶上所屬選單（與電腦版相同）
function onCheck(keys, e) {
  const list = Array.isArray(keys) ? keys.slice() : keys.checked.slice()
  if (e.checked) {
    const item = vdata.grantable.find((g) => g.entId === e.node.key)
    if (item && allKeys.value.includes(item.pid) && !list.includes(item.pid)) list.push(item.pid)
  }
  vdata.ents.checked = list
}
function saveEnts() {
  const e = vdata.ents
  e.saving = true
  req
    .updateById(URL, e.sysUserId + '/ents', { entIds: Array.isArray(e.checked) ? e.checked : e.checked.checked })
    .then(() => {
      $infoBox.message.success('已儲存')
      e.open = false
      load()
    })
    .finally(() => (e.saving = false))
}
function setState(s, on) {
  req.updateById(URL, s.sysUserId + '/state', { state: on ? 1 : 0 }).then(() => {
    $infoBox.message.success(on ? '已啟用' : '已停用，對方會被立即登出')
    load()
  })
}
function resetPwd(s) {
  $infoBox.confirmPrimary('重設子帳號密碼？', `${s.realname || s.loginUsername} 的登入密碼會換成新的隨機密碼，舊密碼立即失效。`, () => {
    req.add(`${URL}/${s.sysUserId}/resetPassword`, {}).then((res) => {
      credentialModal.value.show({ ...res, subjectLabel: '子帳號', loginAt: '營運平台' })
    })
  })
}
load()
</script>
