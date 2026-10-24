<template>
  <!-- 快速搜尋：輸入頁面名稱或分類，↑↓ 選擇、Enter 前往、Esc 關閉；也列出可用的快捷動作 -->
  <a-modal
    :open="open"
    :footer="null"
    :closable="false"
    :width="620"
    :style="{ top: '12vh' }"
    wrap-class-name="jp-palette-wrap"
    destroy-on-close
    @cancel="emit('update:open', false)"
  >
    <div class="jp-palette" @keydown="onKey">
      <div class="jp-palette-input">
        <SearchOutlined />
        <input
          ref="inputRef"
          v-model="q"
          :placeholder="$t('搜尋頁面或動作…')"
          :aria-label="$t('搜尋頁面或動作…')"
          autocomplete="off"
          spellcheck="false"
        />
        <ShortcutKeys combo="Mod+K" small />
      </div>
      <ul ref="listRef" class="jp-palette-list" role="listbox">
        <li v-if="!items.length" class="jp-palette-empty">{{ $t('找不到符合的頁面') }}</li>
        <template v-for="(it, i) in items" :key="it.key">
          <li v-if="i === 0 || items[i - 1].section !== it.section" class="jp-palette-section">{{ it.section }}</li>
          <li
            role="option"
            :aria-selected="i === active"
            :class="['jp-palette-item', { on: i === active }]"
            :data-i="i"
            @mousemove="active = i"
            @click="choose(it)"
          >
            <span class="ic"><component :is="it.icon" /></span>
            <span class="txt">
              <b>{{ it.title }}</b>
              <small v-if="it.sub">{{ it.sub }}</small>
            </span>
            <ShortcutKeys :combo="it.combo" small hide-empty />
          </li>
        </template>
      </ul>
      <div class="jp-palette-foot">
        <span><ShortcutKeys combo="ArrowUp" small /><ShortcutKeys combo="ArrowDown" small /> {{ $t('選擇') }}</span>
        <span><ShortcutKeys combo="Enter" small /> {{ $t('前往') }}</span>
        <span><kbd class="jp-kbd small">Esc</kbd> {{ $t('關閉') }}</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { t } from '@/i18n'
import type { MenuGroup } from '@/utils/menuGroups'
import { ACTIONS, config } from './index'
import ShortcutKeys from './ShortcutKeys.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  groups: { type: Array as () => MenuGroup[], default: () => [] },
})
const emit = defineEmits(['update:open', 'open-page', 'action'])

const q = ref('')
const active = ref(0)
const inputRef = ref<HTMLInputElement>()
const listRef = ref<HTMLElement>()

const ACTION_ICONS = { home: 'appstore-outlined', prevTab: 'left-outlined', nextTab: 'right-outlined', refresh: 'reload-outlined', language: 'global-outlined', theme: 'bg-colors-outlined', help: 'question-circle-outlined' }

const pageCombo = (name) => (config.value.pages.find((p) => p.name === name) || { combo: '' }).combo
const allItems = computed(() => {
  const pages = props.groups.flatMap((g) =>
    g.items.map((i) => ({
      key: 'p:' + i.path,
      section: t('頁面'),
      title: i.meta.title,
      sub: g.title || t('常用'),
      icon: i.meta.icon,
      combo: pageCombo(i.name),
      path: i.path,
    })),
  )
  const actions = ACTIONS.filter((a) => a.id !== 'palette').map((a) => ({
    key: 'a:' + a.id,
    section: t('動作'),
    title: t(a.label),
    sub: '',
    icon: ACTION_ICONS[a.id],
    combo: config.value.actions[a.id],
    action: a.id,
  }))
  return [...pages, ...actions]
})
// 名稱或分類包含輸入的文字（不分大小寫，空白分開的每個字都要符合）
const items = computed(() => {
  const words = q.value.trim().toLowerCase().split(/\s+/).filter(Boolean)
  if (!words.length) return allItems.value
  return allItems.value.filter((it) => {
    const hay = (it.title + ' ' + it.sub).toLowerCase()
    return words.every((w) => hay.includes(w))
  })
})
watch(items, () => (active.value = 0))

watch(
  () => props.open,
  (v) => {
    if (!v) return
    q.value = ''
    active.value = 0
    nextTick(() => setTimeout(() => inputRef.value && inputRef.value.focus(), 50))
  },
)

function choose(it) {
  emit('update:open', false)
  if (it.path) emit('open-page', it.path)
  else emit('action', it.action)
}
function scrollToActive() {
  nextTick(() => {
    const el = listRef.value && listRef.value.querySelector(`[data-i="${active.value}"]`)
    if (el) (el as HTMLElement).scrollIntoView({ block: 'nearest' })
  })
}
function onKey(e: KeyboardEvent) {
  const n = items.value.length
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    if (n) active.value = (active.value + 1) % n
    scrollToActive()
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    if (n) active.value = (active.value - 1 + n) % n
    scrollToActive()
  } else if (e.key === 'Enter' && !e.isComposing) {
    e.preventDefault()
    if (items.value[active.value]) choose(items.value[active.value])
  }
}
</script>

<style lang="less">
.jp-palette-wrap .ant-modal-content {
  padding: 0 !important;
  overflow: hidden;
}
.jp-palette {
  display: flex;
  flex-direction: column;
  max-height: 70vh;
}
.jp-palette-input {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--jp-card-line);
  font-size: 18px;
  color: var(--jp-text-3);
  input {
    flex: 1;
    min-width: 0;
    border: 0;
    outline: 0;
    background: transparent;
    font: inherit;
    font-size: 16px;
    color: var(--jp-text);
    &::placeholder {
      color: var(--jp-text-3);
    }
  }
}
.jp-palette-list {
  flex: 1;
  overflow-y: auto;
  margin: 0;
  padding: 6px 8px 8px;
  list-style: none;
}
.jp-palette-section {
  padding: 10px 10px 4px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--jp-text-3);
}
.jp-palette-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 12px;
  cursor: pointer;
  .ic {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    display: grid;
    place-items: center;
    flex: none;
    font-size: 15px;
    color: var(--jp-primary);
    background: var(--jp-primary-soft);
  }
  .txt {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    b {
      font-weight: 600;
      color: var(--jp-text);
    }
    small {
      color: var(--jp-text-3);
      font-size: 12px;
    }
  }
  &.on {
    background: var(--jp-primary-soft);
    .ic {
      color: #fff;
      background: var(--jp-brand-grad);
      box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3);
    }
  }
}
.jp-palette-empty {
  padding: 28px 12px;
  text-align: center;
  color: var(--jp-text-3);
}
.jp-palette-foot {
  display: flex;
  gap: 18px;
  padding: 10px 18px;
  border-top: 1px solid var(--jp-card-line);
  color: var(--jp-text-3);
  font-size: 12px;
  span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
}
</style>
