<template>
  <!-- 快捷鍵一覽與設定：內建動作可改鍵或清除；頁面捷徑可自行新增。按「儲存」後立即生效 -->
  <a-modal
    :open="open"
    :title="$t('快捷鍵')"
    :width="760"
    :style="{ top: '32px' }"
    wrap-class-name="jp-sc-wrap"
    @cancel="close"
  >
    <p class="jp-sc-hint">
      <InfoCircleOutlined />
      {{ $t('按「變更」後直接按下新的組合鍵（Esc 取消）。在輸入框打字時，不含 Ctrl／Alt／⌘ 的快捷鍵不會觸發。設定依帳號存在這台裝置。') }}
    </p>

    <h4 class="jp-sc-title">{{ $t('常用動作') }}</h4>
    <ul class="jp-sc-list">
      <li v-for="a in ACTIONS" :key="a.id" :class="{ rec: rec === 'a:' + a.id }">
        <span class="name">{{ $t(a.label) }}</span>
        <span class="keys">
          <span v-if="rec === 'a:' + a.id" class="jp-sc-rec">{{ $t('請按下新的組合鍵…') }}</span>
          <ShortcutKeys v-else :combo="draft.actions[a.id]" />
        </span>
        <span class="ops">
          <a-button size="small" @click="startRec('a:' + a.id)">{{ rec === 'a:' + a.id ? $t('取消') : $t('變更') }}</a-button>
          <a-button size="small" type="text" :disabled="!draft.actions[a.id]" @click="draft.actions[a.id] = ''">{{ $t('清除') }}</a-button>
        </span>
      </li>
    </ul>

    <h4 class="jp-sc-title">
      {{ $t('頁面捷徑') }}
      <small>{{ $t('把常用的頁面設成一組按鍵，按下就直接打開') }}</small>
    </h4>
    <ul class="jp-sc-list">
      <li v-for="(p, i) in draft.pages" :key="i" :class="{ rec: rec === 'p:' + i }">
        <span class="name">
          <a-select
            v-model:value="p.name"
            show-search
            :options="pageOptions"
            :filter-option="filterPage"
            :placeholder="$t('選擇頁面')"
            style="width: 100%"
          />
        </span>
        <span class="keys">
          <span v-if="rec === 'p:' + i" class="jp-sc-rec">{{ $t('請按下新的組合鍵…') }}</span>
          <ShortcutKeys v-else :combo="p.combo" />
        </span>
        <span class="ops">
          <a-button size="small" @click="startRec('p:' + i)">{{ rec === 'p:' + i ? $t('取消') : $t('變更') }}</a-button>
          <a-button size="small" type="text" danger @click="removePage(i)">{{ $t('刪除') }}</a-button>
        </span>
      </li>
      <li v-if="!draft.pages.length" class="empty">{{ $t('還沒有頁面捷徑') }}</li>
    </ul>
    <div class="jp-sc-row-actions">
      <a-button class="jp-sc-add" @click="addPage"><PlusOutlined /> {{ $t('新增頁面捷徑') }}</a-button>
      <!-- 主選單圖示下方要不要顯示按鍵（不影響快捷鍵本身） -->
      <label class="jp-sc-toggle">
        <a-switch v-model:checked="draft.showOnHome" size="small" />
        <span>{{ $t('在主選單的圖示下顯示快捷鍵') }}</span>
      </label>
    </div>

    <p v-if="error" class="jp-sc-error"><WarningOutlined /> {{ error }}</p>

    <template #footer>
      <div class="jp-sc-foot">
        <a-popconfirm :title="$t('恢復成預設的快捷鍵？頁面捷徑會全部清除。')" :ok-text="$t('恢復預設')" :cancel-text="$t('取消')" @confirm="reset">
          <a-button type="link">{{ $t('恢復預設') }}</a-button>
        </a-popconfirm>
        <span>
          <a-button @click="close">{{ $t('取消') }}</a-button>
          <a-button type="primary" :disabled="!dirty" @click="save">{{ $t('儲存') }}</a-button>
        </span>
      </div>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, PlusOutlined, WarningOutlined } from '@ant-design/icons-vue'
import { t } from '@/i18n'
import type { MenuGroup } from '@/utils/menuGroups'
import { ACTIONS, comboFromEvent, comboProblem, comboText, config, defaultConfig, recording, saveConfig, whoUses } from './index'
import type { ShortcutConfig } from './index'
import ShortcutKeys from './ShortcutKeys.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  groups: { type: Array as () => MenuGroup[], default: () => [] },
})
const emit = defineEmits(['update:open'])

const clone = (c: ShortcutConfig): ShortcutConfig => JSON.parse(JSON.stringify(c))
const draft = ref<ShortcutConfig>(clone(config.value))
const rec = ref('') // 正在錄製的列：a:<動作> 或 p:<索引>
const error = ref('')

watch(
  () => props.open,
  (v) => {
    if (v) {
      draft.value = clone(config.value)
      error.value = ''
    } else stopRec()
  },
)
const dirty = computed(() => JSON.stringify(draft.value) !== JSON.stringify(config.value))

const pageOptions = computed(() =>
  props.groups.map((g) => ({
    label: g.title || t('常用'),
    options: g.items.map((i) => ({ value: i.name, label: i.meta.title })),
  })),
)
const filterPage = (input, opt) => String(opt.label || '').toLowerCase().includes(input.toLowerCase())
const pageTitle = (name) => {
  for (const g of props.groups) for (const i of g.items) if (i.name === name) return i.meta.title
  return name
}
const actionLabel = (id) => t((ACTIONS.find((a) => a.id === id) || { label: id }).label)

function addPage() {
  draft.value.pages.push({ name: undefined as any, combo: '' })
  startRec('p:' + (draft.value.pages.length - 1))
}
function removePage(i) {
  draft.value.pages.splice(i, 1)
  stopRec()
}

// ───────── 錄製組合鍵 ─────────
function startRec(row: string) {
  if (rec.value === row) return stopRec()
  rec.value = row
  error.value = ''
  recording.value = true
  window.addEventListener('keydown', onRecKey, true)
}
function stopRec() {
  rec.value = ''
  recording.value = false
  window.removeEventListener('keydown', onRecKey, true)
}
onBeforeUnmount(stopRec)

function onRecKey(e: KeyboardEvent) {
  // 錄製中的按鍵一律攔下，不讓頁面或瀏覽器處理
  e.preventDefault()
  e.stopPropagation()
  if (e.key === 'Escape') return stopRec()
  const combo = comboFromEvent(e)
  if (!combo) return // 只按了 Ctrl／Alt 等修飾鍵，繼續等
  const problem = comboProblem(combo)
  if (problem) {
    error.value = problem
    return
  }
  const row = rec.value
  // 已被其他項目使用：改成新的這一項，原本的清掉並提示
  const used = whoUses(draft.value, combo)
  const self = row.startsWith('a:') ? { kind: 'action', id: row.slice(2) } : null
  if (used && !(self && used.kind === 'action' && used.id === self.id)) {
    const selfPage = row.startsWith('p:') && used.kind === 'page' && draft.value.pages[Number(row.slice(2))].combo === combo
    if (!selfPage) {
      if (used.kind === 'action') draft.value.actions[used.id] = ''
      else draft.value.pages.forEach((p) => p.combo === combo && (p.combo = ''))
      message.info(t('{0} 原本給「{1}」使用，已改給這一項', [comboText(combo), used.kind === 'action' ? actionLabel(used.id) : pageTitle(used.id)]))
    }
  }
  if (row.startsWith('a:')) draft.value.actions[row.slice(2)] = combo
  else draft.value.pages[Number(row.slice(2))].combo = combo
  error.value = ''
  stopRec()
}

function save() {
  stopRec()
  const incomplete = draft.value.pages.some((p) => !p.name || !p.combo)
  if (incomplete) {
    error.value = t('每個頁面捷徑都要選頁面並設定按鍵')
    return
  }
  saveConfig(draft.value)
  message.success(t('快捷鍵已儲存'))
  emit('update:open', false)
}
function close() {
  stopRec()
  emit('update:open', false)
}
function reset() {
  stopRec()
  draft.value = defaultConfig()
  error.value = ''
}
</script>

<style lang="less">
.jp-sc-wrap .ant-modal-body {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding-right: 4px;
}
.jp-sc-hint {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin: 0 0 16px;
  color: var(--jp-text-2);
  font-size: 13px;
  line-height: 1.6;
  .anticon {
    margin-top: 4px;
    color: var(--jp-primary);
  }
}
.jp-sc-title {
  margin: 18px 0 8px;
  font-size: 14px;
  font-weight: 650;
  color: var(--jp-text);
  small {
    margin-left: 8px;
    font-weight: 400;
    font-size: 12px;
    color: var(--jp-text-3);
  }
}
.jp-sc-list {
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid var(--jp-card-edge);
  border-radius: 14px;
  background: var(--jp-card-bg);
  overflow: hidden;
  li {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 190px auto;
    align-items: center;
    gap: 12px;
    min-height: 44px;
    padding: 6px 14px;
    & + li {
      border-top: 1px solid var(--jp-card-line);
    }
    &.rec {
      background: var(--jp-primary-soft);
    }
    &.empty {
      display: block;
      text-align: center;
      color: var(--jp-text-3);
    }
  }
  .name {
    color: var(--jp-text);
    font-weight: 500;
  }
  .ops {
    display: flex;
    gap: 4px;
    justify-content: flex-end;
  }
}
.jp-sc-rec {
  color: var(--jp-primary);
  font-weight: 600;
  font-size: 13px;
  animation: jp-sc-blink 1.2s ease-in-out infinite;
}
@keyframes jp-sc-blink {
  50% {
    opacity: 0.45;
  }
}
.jp-sc-row-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}
.jp-sc-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--jp-text-2);
  font-size: 13px;
}
.jp-sc-error {
  margin: 14px 0 0;
  color: var(--jp-bad);
  font-size: 13px;
}
.jp-sc-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  > span {
    display: flex;
    gap: 8px;
  }
}
</style>
