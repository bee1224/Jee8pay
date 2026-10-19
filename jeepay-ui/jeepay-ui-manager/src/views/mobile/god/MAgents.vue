<template>
  <!-- 上帝：團長列表＋家族樹（團長 → 隊長 → 商戶），可直接啟用／停用；點名稱進團長詳情 -->
  <div class="m-stats">
    <div class="m-stat"><small>團長</small><strong class="num">{{ leaders.length }}</strong></div>
    <div class="m-stat"><small>隊長</small><strong class="num">{{ vdata.agents.length - leaders.length }}</strong></div>
    <div class="m-stat"><small>商戶</small><strong class="num">{{ vdata.relas.length }}</strong></div>
  </div>
  <div class="m-search">
    <SearchOutlined />
    <input v-model="vdata.q" type="search" placeholder="搜尋團長、隊長、商戶名稱或編號" />
  </div>
  <div class="m-chips">
    <button v-for="f in FILTERS" :key="f.key" :class="{ on: vdata.filter === f.key }" @click="vdata.filter = f.key">{{ f.label }}</button>
  </div>
  <button v-if="$access('ENT_AGENT_INFO_ADD')" class="m-btn" @click="addOrEdit.show()">＋ 新增團長</button>

  <div v-for="a in shown" :key="a.agentNo" class="m-card" style="display: grid; gap: 10px; padding: 14px">
    <div style="display: flex; align-items: center; gap: 10px">
      <button class="m-avatar" style="width: 40px; height: 40px; font-size: 16px" :aria-label="vdata.open[a.agentNo] ? '收合' : '展開家族樹'" @click="toggle(a.agentNo)"><DownOutlined v-if="vdata.open[a.agentNo]" /><RightOutlined v-else /></button>
      <button style="flex: 1; min-width: 0; text-align: left" @click="openDetail(a)">
        <b>{{ a.agentName }}</b><span class="m-role">團長</span><span v-if="a.isHouse === 1" class="m-role">平台直屬</span>
        <div class="m-muted" style="margin: 2px 0 0">
          <span class="mono">{{ a.agentNo }}</span>・隊長 {{ subsOf(a.agentNo).length }}・商戶 {{ mchCount(a.agentNo) }}
        </div>
      </button>
      <a-switch :checked="a.state === 1" :disabled="!canEdit" @change="(v) => setState(a, v)" />
    </div>

    <div v-if="vdata.open[a.agentNo]" class="m-tree">
      <div class="m-tree-label">團長直屬商戶（{{ mchOf(a.agentNo).length }}）</div>
      <div class="m-tree-chips">
        <span v-for="m in mchOf(a.agentNo)" :key="m.mchNo" :class="['m-pill', m.state === 0 ? 'mute' : 'ok']">{{ m.name || m.mchNo }}</span>
        <span v-if="!mchOf(a.agentNo).length" class="m-muted" style="margin: 0">沒有</span>
      </div>
      <div v-for="s in subsOf(a.agentNo)" :key="s.agentNo" class="m-tree-son">
        <div style="display: flex; align-items: center; gap: 8px">
          <div style="flex: 1; min-width: 0">
            <b>{{ s.agentName }}</b><span class="m-role">隊長</span>
            <div class="m-muted" style="margin: 2px 0 0"><span class="mono">{{ s.agentNo }}</span>・商戶 {{ mchOf(s.agentNo).length }}</div>
          </div>
          <a-switch size="small" :checked="s.state === 1" :disabled="!canEdit" @change="(v) => setState(s, v)" />
        </div>
        <div class="m-tree-chips">
          <span v-for="m in mchOf(s.agentNo)" :key="m.mchNo" :class="['m-pill', m.state === 0 ? 'mute' : 'ok']">{{ m.name || m.mchNo }}</span>
        </div>
      </div>
    </div>
  </div>
  <div v-if="!shown.length" class="m-empty">{{ vdata.loaded ? '沒有符合的團長' : '載入中…' }}</div>

  <AgentAddOrEdit ref="addOrEdit" :callback-func="load" />
</template>

<script setup lang="ts">
import { computed, reactive, ref, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { SearchOutlined, DownOutlined, RightOutlined } from '@ant-design/icons-vue'
import { API_URL_AGENT_INFO, API_URL_AGENT_MCH_RELA, API_URL_MCH_LIST, req } from '@/api/manage'
import AgentAddOrEdit from '@/views/agent/AgentAddOrEdit.vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const router = useRouter()
const addOrEdit = ref()
const canEdit = $access('ENT_AGENT_INFO_EDIT')
const FILTERS = [
  { key: 'all', label: '全部' },
  { key: 'on', label: '啟用中' },
  { key: 'off', label: '已停用' },
]
const vdata: any = reactive({ agents: [], relas: [], mchs: [], loaded: false, q: '', filter: 'all', open: {} })

const leaders = computed(() => vdata.agents.filter((a) => a.agentLevel === 1))
const mchMap = computed(() => Object.fromEntries(vdata.mchs.map((m) => [m.mchNo, m])))
function subsOf(agentNo) {
  return vdata.agents.filter((a) => a.parentAgentNo === agentNo)
}
function mchOf(agentNo) {
  return vdata.relas
    .filter((r) => r.agentNo === agentNo)
    .map((r) => {
      const m = mchMap.value[r.mchNo] || {}
      return { mchNo: r.mchNo, name: m.mchShortName || m.mchName || '', state: m.state }
    })
}
function mchCount(agentNo) {
  return mchOf(agentNo).length + subsOf(agentNo).reduce((n, s) => n + mchOf(s.agentNo).length, 0)
}
// 搜尋比對團長、旗下隊長與所有商戶；命中時自動展開
const shown = computed(() => {
  const q = vdata.q.trim()
  return leaders.value.filter((a) => {
    if (vdata.filter === 'on' && a.state !== 1) return false
    if (vdata.filter === 'off' && a.state === 1) return false
    if (!q) return true
    const names = [a.agentName, a.agentNo]
      .concat(subsOf(a.agentNo).flatMap((s) => [s.agentName, s.agentNo]))
      .concat([a.agentNo].concat(subsOf(a.agentNo).map((s) => s.agentNo)).flatMap((no) => mchOf(no).flatMap((m) => [m.name, m.mchNo])))
    return names.some((n) => String(n || '').includes(q))
  })
})
function toggle(no) {
  vdata.open[no] = !vdata.open[no]
}
function openDetail(a) {
  router.push({ path: '/agents/detail', query: { agentNo: a.agentNo } })
}
function setState(a, on) {
  const doIt = () =>
    req.updateById(API_URL_AGENT_INFO, a.agentNo, { state: on ? 1 : 0 }).then(() => {
      $infoBox.message.success(on ? '已啟用' : '已停用')
      a.state = on ? 1 : 0
    })
  if (on) doIt()
  else $infoBox.confirmDanger(`停用「${a.agentName}」？`, '停用後這位代理與他的子帳號無法使用代理後台，也不能再綁定新商戶；旗下商戶照常收款。', doIt)
}
function load() {
  Promise.all([
    req.list(API_URL_AGENT_INFO, { pageSize: -1 }),
    req.list(API_URL_AGENT_MCH_RELA, { pageSize: -1 }),
    req.list(API_URL_MCH_LIST, { pageSize: -1 }),
  ]).then(([a, r, m]) => {
    vdata.agents = a.records || []
    vdata.relas = r.records || []
    vdata.mchs = m.records || []
    vdata.loaded = true
  })
}
load()
</script>

<style scoped lang="less">
.m-tree {
  display: grid;
  gap: 10px;
  padding-left: 6px;
}
.m-tree-label {
  font-size: 12px;
  color: var(--jp-text-3);
}
.m-tree-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.m-tree-son {
  display: grid;
  gap: 8px;
  margin-left: 10px;
  padding: 8px 0 2px 12px;
  border-left: 2px solid var(--jp-primary-soft);
}
</style>
