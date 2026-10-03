<template>
  <!-- 家族樹：每張卡片是一位高級代理（第二代），裡面依序是他的直屬商戶、旗下代理（第三代）與各自的商戶 -->
  <div>
    <div class="tree-summary">
      <span>高級代理 <b>{{ seniors.length }}</b></span>
      <span>一般代理 <b>{{ agents.length - seniors.length }}</b></span>
      <span>已歸屬商戶 <b>{{ relas.length }}</b></span>
      <span v-if="orphans.length">未歸屬商戶 <b>{{ orphans.length }}</b></span>
      <a-button size="small" @click="load">重新整理</a-button>
    </div>
    <a-empty v-if="loaded && !seniors.length" description="還沒有高級代理，請先按「新建代理」" />
    <div class="tree-grid">
      <a-card v-for="senior in seniors" :key="senior.agentNo" class="family-card" size="small">
        <template #title>
          <div class="node-head">
            <a-tag color="purple">高級代理</a-tag>
            <b>{{ senior.agentName }}</b>
            <span class="no">{{ senior.agentNo }}</span>
            <a-badge :status="senior.state === 0 ? 'error' : 'processing'" :text="senior.state === 0 ? '停用' : '啟用'" />
          </div>
        </template>
        <template #extra>
          <a-dropdown>
            <a-button type="link" size="small">管理</a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item v-if="$access('ENT_AGENT_INFO_EDIT')" @click="emit('edit', senior)">修改</a-menu-item>
                <a-menu-item v-if="$access('ENT_AGENT_ACCOUNT')" @click="emit('accounts', senior)">登入帳號</a-menu-item>
                <a-menu-item v-if="$access('ENT_AGENT_PROFIT')" @click="emit('profit', senior)">分潤</a-menu-item>
                <a-menu-item v-if="$access('ENT_AGENT_INFO_DEL')" @click="emit('remove', senior)"><span style="color: #cf1322">刪除</span></a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>

        <div class="branch">
          <div class="branch-label">直屬商戶（{{ mchOf(senior.agentNo).length }}）</div>
          <div class="chips">
            <a-tag v-for="m in mchOf(senior.agentNo)" :key="m.mchNo" :color="m.state === 0 ? 'default' : 'green'">{{ m.name }}<span class="no">{{ m.mchNo }}</span></a-tag>
            <span v-if="!mchOf(senior.agentNo).length" class="none">沒有</span>
          </div>
        </div>

        <div v-for="son in sonsOf(senior.agentNo)" :key="son.agentNo" class="son">
          <div class="node-head">
            <a-tag color="blue">一般代理</a-tag>
            <b>{{ son.agentName }}</b>
            <span class="no">{{ son.agentNo }}</span>
            <a-badge :status="son.state === 0 ? 'error' : 'processing'" :text="son.state === 0 ? '停用' : '啟用'" />
            <span class="son-ops">
              <a v-if="$access('ENT_AGENT_INFO_EDIT')" @click="emit('edit', son)">修改</a>
              <a v-if="$access('ENT_AGENT_ACCOUNT')" @click="emit('accounts', son)">登入帳號</a>
              <a v-if="$access('ENT_AGENT_PROFIT')" @click="emit('profit', son)">分潤</a>
            </span>
          </div>
          <div class="chips">
            <a-tag v-for="m in mchOf(son.agentNo)" :key="m.mchNo" :color="m.state === 0 ? 'default' : 'green'">{{ m.name }}<span class="no">{{ m.mchNo }}</span></a-tag>
            <span v-if="!mchOf(son.agentNo).length" class="none">沒有商戶</span>
          </div>
        </div>
        <div v-if="!sonsOf(senior.agentNo).length" class="none" style="margin-top: 10px">還沒有旗下代理</div>
      </a-card>

      <a-card v-if="orphans.length" class="family-card" size="small" title="未歸屬任何代理的商戶">
        <div class="chips">
          <a-tag v-for="m in orphans" :key="m.mchNo">{{ m.mchShortName || m.mchName }}<span class="no">{{ m.mchNo }}</span></a-tag>
        </div>
        <div class="none" style="margin-top: 8px">到「商戶列表 → 代理綁定」指定直屬代理。</div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_AGENT_MCH_RELA, API_URL_MCH_LIST, req } from '@/api/manage'
import { computed, ref } from 'vue'

const emit = defineEmits(['edit', 'accounts', 'profit', 'remove'])
const agents = ref<any[]>([])
const relas = ref<any[]>([])
const mchs = ref<any[]>([])
const loaded = ref(false)

const seniors = computed(() => agents.value.filter((a) => a.agentLevel === 1))
const mchMap = computed(() => Object.fromEntries(mchs.value.map((m) => [m.mchNo, m])))
const orphans = computed(() => {
  const bound = new Set(relas.value.map((r) => r.mchNo))
  return mchs.value.filter((m) => !bound.has(m.mchNo))
})
function sonsOf(agentNo) {
  return agents.value.filter((a) => a.parentAgentNo === agentNo)
}
function mchOf(agentNo) {
  return relas.value
    .filter((r) => r.agentNo === agentNo)
    .map((r) => {
      const m = mchMap.value[r.mchNo] || {}
      return { mchNo: r.mchNo, name: m.mchShortName || m.mchName || '', state: m.state }
    })
}
function load() {
  Promise.all([
    req.list(API_URL_AGENT_INFO, { pageSize: -1 }),
    req.list(API_URL_AGENT_MCH_RELA, { pageSize: -1 }),
    req.list(API_URL_MCH_LIST, { pageSize: -1 }),
  ]).then(([a, r, m]) => {
    agents.value = a.records || []
    relas.value = r.records || []
    mchs.value = m.records || []
    loaded.value = true
  })
}
load()
defineExpose({ load })
</script>

<style scoped lang="less">
.tree-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  align-items: center;
  margin-bottom: 16px;
  color: rgba(0, 0, 0, 0.65);
}
.tree-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 16px;
  align-items: start;
}
.family-card {
  border-top: 3px solid #722ed1;
}
.node-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
}
.no {
  margin-left: 6px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  font-weight: 400;
}
.branch-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 6px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.none {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}
.son {
  margin-top: 12px;
  margin-left: 14px;
  padding: 10px 0 2px 14px;
  border-left: 2px solid #91caff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.son-ops {
  margin-left: auto;
  display: flex;
  gap: 10px;
  font-size: 13px;
}
@media (max-width: 520px) {
  .tree-grid {
    grid-template-columns: 1fr;
  }
}
</style>
