<template>
  <!-- 權限管理：選單與按鈕的樹狀清單，可展開、啟用／停用、修改 -->
  <div class="m-seg">
    <button v-for="t in TYPES" :key="t.key" :class="{ on: vdata.sysType === t.key }" @click="setType(t.key)">{{ t.label }}</button>
  </div>
  <div class="m-search">
    <SearchOutlined />
    <input v-model="vdata.q" type="search" placeholder="搜尋權限名稱或代碼" />
  </div>
  <p class="m-muted">共 {{ flat.length }} 項；停用的選單與按鈕，所有人都看不到。</p>
  <div class="m-list">
    <div v-for="e in visible" :key="e.entId" class="m-row" :style="{ paddingLeft: 12 + e._depth * 18 + 'px' }">
      <button v-if="e.children && e.children.length" class="m-ent-toggle" :aria-label="vdata.open[e.entId] ? '收合' : '展開'" @click="toggle(e.entId)"><DownOutlined v-if="vdata.open[e.entId] || vdata.q" /><RightOutlined v-else /></button>
      <span v-else class="m-ent-toggle" />
      <div class="main">
        <b>{{ e.entName }}</b>
        <small><span :class="['m-pill', TYPE_CLASS[e.entType] || 'mute']" style="margin-right: 6px">{{ ENT_TYPES[e.entType] || e.entType }}</span><span class="mono">{{ e.entId }}</span></small>
      </div>
      <div class="end" style="flex-direction: row; display: flex; align-items: center; gap: 8px">
        <button v-if="canEdit" class="m-btn sm ghost" @click="editor.show(e.entId, vdata.sysType)">修改</button>
        <a-switch size="small" :checked="e.state === 1" :disabled="!canEdit" @change="(v) => setState(e, v)" />
      </div>
    </div>
    <div v-if="!visible.length" class="m-empty">{{ vdata.loaded ? '沒有符合的權限' : '載入中…' }}</div>
  </div>
  <EntAddOrEdit ref="editor" :callback-func="load" />
</template>

<script setup lang="ts">
import { computed, reactive, ref, getCurrentInstance } from 'vue'
import { SearchOutlined, DownOutlined, RightOutlined } from '@ant-design/icons-vue'
import { API_URL_ENT_LIST, getEntTree, req } from '@/api/manage'
import EntAddOrEdit from '@/views/ent/AddOrEdit.vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const canEdit = $access('ENT_UR_ROLE_ENT_EDIT')
const editor = ref()
const TYPES = [
  { key: 'MGR', label: '營運平台' },
  { key: 'MCH', label: '商戶平台' },
]
const ENT_TYPES = { ML: '選單', MO: '頁面', PB: '按鈕' }
const TYPE_CLASS = { ML: 'ok', MO: 'mute', PB: 'warn' }
const vdata: any = reactive({ sysType: 'MGR', tree: [], loaded: false, q: '', open: {} })

const flat = computed(() => {
  const out: any[] = []
  const walk = (list, depth, parents) => (list || []).forEach((e) => {
    out.push({ ...e, _depth: depth, _parents: parents })
    walk(e.children, depth + 1, parents.concat(e.entId))
  })
  walk(vdata.tree, 0, [])
  return out
})
// 搜尋時列出命中的項目（含上層）；沒搜尋時依展開狀態顯示
const visible = computed(() => {
  const q = vdata.q.trim().toLowerCase()
  if (q) {
    const hit = new Set<string>()
    flat.value.forEach((e) => {
      if (`${e.entName} ${e.entId}`.toLowerCase().includes(q)) [e.entId, ...e._parents].forEach((id) => hit.add(id))
    })
    return flat.value.filter((e) => hit.has(e.entId))
  }
  return flat.value.filter((e) => e._parents.every((p) => vdata.open[p]))
})
function toggle(id) {
  vdata.open[id] = !vdata.open[id]
}
function setType(t) {
  vdata.sysType = t
  vdata.open = {}
  load()
}
function setState(e, on) {
  req.updateById(API_URL_ENT_LIST, e.entId, { state: on ? 1 : 0, sysType: vdata.sysType }).then(() => {
    $infoBox.message.success(on ? '已啟用' : '已停用')
    load()
  })
}
function load() {
  getEntTree(vdata.sysType).then((r) => {
    vdata.tree = r || []
    vdata.loaded = true
  })
}
load()
</script>

<style scoped lang="less">
.m-ent-toggle {
  width: 22px;
  flex: none;
  color: var(--jp-text-3);
  font-size: 15px;
}
</style>
