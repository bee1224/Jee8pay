<template>
  <!-- 我的子帳號：建立員工帳號、逐項勾選權限、停用與重設密碼（比照競品「我的子帳號」） -->
  <div>
    <a-alert type="info" show-icon style="margin-bottom: 12px"
      message="子帳號是給你自己的員工用的登入帳號。建立後沒有任何權限，請按「權限」勾選他可以看到的選單與按鈕。" />
    <a-button type="primary" style="margin-bottom: 12px" @click="openForm">建立子帳號</a-button>
    <a-table :columns="columns" :data-source="vdata.list" size="small" row-key="sysUserId" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'ents'">
          <a-tag v-if="!record.entCount" color="orange">尚未設定</a-tag>
          <span v-else>已設定</span>
        </template>
        <template v-if="column.key === 'state'">
          <a-switch :checked="record.state === 1" checked-children="啟用" un-checked-children="停用" @change="(v) => setState(record, v)" />
        </template>
        <template v-if="column.key === 'op'">
          <a-button type="link" @click="openEnts(record)">權限</a-button>
          <a-button type="link" @click="resetPwd(record)">重設密碼</a-button>
        </template>
      </template>
      <template #emptyText>尚未建立子帳號</template>
    </a-table>

    <a-modal v-model:open="vdata.form.open" title="建立子帳號" :confirm-loading="vdata.form.saving" ok-text="確定" cancel-text="取消" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="帳號" required extra="儲存後系統產生 8 碼隨機密碼並顯示一次。">
          <a-input v-model:value="vdata.form.data.loginUsername" placeholder="2～32 碼中文、英文、數字或底線" autocomplete="off" />
        </a-form-item>
        <a-form-item label="名稱" required><a-input v-model:value="vdata.form.data.realname" /></a-form-item>
        <a-form-item label="手機號（選填）"><a-input v-model:value="vdata.form.data.telphone" placeholder="09 開頭 10 碼，可不填" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="vdata.ents.open" :title="'設定權限：' + vdata.ents.name" :confirm-loading="vdata.ents.saving" ok-text="儲存" cancel-text="取消" width="560px" @ok="saveEnts">
      <div style="margin-bottom: 8px">
        <a-button size="small" @click="vdata.ents.checked = allKeys.slice()">全選</a-button>
        <a-button size="small" style="margin-left: 8px" @click="vdata.ents.checked = []">全部取消</a-button>
      </div>
      <a-tree v-model:checkedKeys="vdata.ents.checked" checkable check-strictly default-expand-all :tree-data="tree" :selectable="false"
        style="max-height: 420px; overflow: auto" @check="onCheck" />
      <div style="color: #888; font-size: 12px; margin-top: 8px">只能從你自己擁有的權限裡挑選；「我的子帳號」本身不能下放。儲存後立即生效。</div>
    </a-modal>

    <AgentCredentialModal ref="credentialModal" />
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import AgentCredentialModal from '@/views/agent/AgentCredentialModal.vue'
import { computed, reactive, ref, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
defineProps({ me: { type: Object, required: true } })

const URL = API_URL_AGENT_PORTAL + '/staff'
// 所有子帳號都會自動帶上的基礎權限，不列在樹上
const BASE = ['ENT_COMMONS', 'ENT_C_USERINFO', 'ENT_AGENT_PORTAL', 'ENT_AGENT_PORTAL_VIEW']
const columns = [
  { title: '帳號', dataIndex: 'loginUsername' },
  { title: '名稱', dataIndex: 'realname' },
  { title: '手機號', dataIndex: 'telphone' },
  { key: 'ents', title: '權限' },
  { key: 'state', title: '啟用狀態', width: 110 },
  { title: '建立時間', dataIndex: 'createdAt', width: 170 },
  { key: 'op', title: '操作', width: 170 },
]
const credentialModal = ref()
const vdata: any = reactive({
  list: [],
  grantable: [],
  form: { open: false, saving: false, data: {} },
  ents: { open: false, saving: false, sysUserId: null, name: '', checked: [] },
})

// 兩層：選單 → 底下的按鈕；父層不在可授予清單裡的按鈕（例如掛在別的選單下）直接列在最上層
const tree = computed(() => {
  const items = vdata.grantable.filter((e) => !BASE.includes(e.entId))
  const ids = new Set(items.map((e) => e.entId))
  const node = (e) => ({ key: e.entId, title: e.entName, children: items.filter((c) => c.pid === e.entId).map(node) })
  return items.filter((e) => !ids.has(e.pid)).map(node)
})
const allKeys = computed(() => vdata.grantable.filter((e) => !BASE.includes(e.entId)).map((e) => e.entId))

function load() {
  req.list(URL, {}).then((r) => (vdata.list = r || []))
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
function openEnts(record) {
  vdata.ents = { open: true, saving: false, sysUserId: record.sysUserId, name: record.realname || record.loginUsername, checked: [] }
  const loadGrantable = vdata.grantable.length ? Promise.resolve() : req.list(URL + '/grantable', {}).then((r) => (vdata.grantable = r || []))
  loadGrantable.then(() =>
    req.list(`${URL}/${record.sysUserId}/ents`, {}).then((r) => (vdata.ents.checked = (r || []).filter((id) => !BASE.includes(id))))
  )
}
// check-strictly 模式下 checkedKeys 會變成 { checked, halfChecked }，統一攤平成陣列；勾按鈕時自動帶上所屬選單
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
  const entIds = Array.isArray(e.checked) ? e.checked : e.checked.checked
  e.saving = true
  req
    .updateById(URL, e.sysUserId + '/ents', { entIds })
    .then(() => {
      $infoBox.message.success('已儲存')
      e.open = false
      load()
    })
    .finally(() => (e.saving = false))
}
function setState(record, on) {
  req.updateById(URL, record.sysUserId + '/state', { state: on ? 1 : 0 }).then(() => {
    $infoBox.message.success(on ? '已啟用' : '已停用，對方會被立即登出')
    load()
  })
}
function resetPwd(record) {
  $infoBox.confirmPrimary('重設子帳號密碼？', `${record.realname || record.loginUsername} 的登入密碼會換成新的隨機密碼，舊密碼立即失效。`, () => {
    req.add(`${URL}/${record.sysUserId}/resetPassword`, {}).then((res) => {
      credentialModal.value.show({ ...res, subjectLabel: '子帳號', loginAt: '營運平台' })
    })
  })
}
load()
</script>
