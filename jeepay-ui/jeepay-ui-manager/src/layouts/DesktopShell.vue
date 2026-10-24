<template>
  <!-- 電腦版外框：沒有左側選單。首頁（/home）依分類排列各頁面；進入頁面後，頂部列可回主選單，下方分頁籤切換同一分類的其他頁面 -->
  <div class="jp-desk dk-app">
    <header class="dk-top">
      <div class="dk-bar">
        <button class="dk-brand" :aria-label="$t('主選單')" @click="emit('home')">
          <!-- 代理帳號（團長、隊長、子帳號）不顯示任何品牌名稱與標誌 -->
          <span v-if="isAgentUser" class="dk-brand-ic"><AppstoreFilled /></span>
          <template v-else>
            <img src="@/assets/logo-icon.png" alt="logo" style="height: 30px" />
            <img src="@/assets/logo-text.png" alt="jeepay" style="width: 84px; margin: 4px 0 0 4px" />
          </template>
        </button>

        <nav class="dk-crumb" :aria-label="$t('目前位置')">
          <button :class="['dk-home', { on: atHome }]" @click="emit('home')">
            <AppstoreOutlined />
            <span>{{ $t('主選單') }}</span>
          </button>
          <template v-if="!atHome">
            <RightOutlined class="dk-sep" />
            <span v-if="group && group.title" class="dk-crumb-group">{{ group.title }}</span>
            <RightOutlined v-if="group && group.title" class="dk-sep" />
            <b class="dk-crumb-page">{{ pageTitle }}</b>
          </template>
        </nav>

        <div class="dk-actions">
          <!-- 快速搜尋：沒有左側選單時用鍵盤直接找頁面 -->
          <button class="dk-search" :title="hint('搜尋頁面', 'palette')" @click="palette = true">
            <SearchOutlined />
            <span class="dk-search-text">{{ $t('搜尋頁面') }}</span>
            <ShortcutKeys :combo="config.actions.palette" small hide-empty />
          </button>
          <button v-if="!atHome" class="dk-icon-btn" :title="hint('重新整理', 'refresh')" @click="refresh">
            <ReloadOutlined :class="{ 'dk-spin': spinning }" />
          </button>
          <button class="dk-icon-btn" :title="hint('快捷鍵', 'help')" @click="settings = true">
            <svg class="dk-kbd-ic" viewBox="0 0 24 24" aria-hidden="true"><rect x="2.5" y="6" width="19" height="12" rx="2.5" /><path d="M6.5 10h.01M10 10h.01M13.5 10h.01M17 10h.01M7.5 14h9" /></svg>
          </button>
          <LanguageButton />
          <a-dropdown placement="bottomRight">
            <template #overlay>
              <a-menu>
                <a-menu-item @click="emit('userinfo')">
                  <SettingOutlined style="margin-right: 10px" />
                  <span>{{ $t('個人資訊') }}</span>
                </a-menu-item>
                <a-menu-item @click="settings = true">
                  <svg class="dk-kbd-ic" style="margin-right: 10px; width: 14px; height: 14px" viewBox="0 0 24 24" aria-hidden="true"><rect x="2.5" y="6" width="19" height="12" rx="2.5" /><path d="M6.5 10h.01M10 10h.01M13.5 10h.01M17 10h.01M7.5 14h9" /></svg>
                  <span>{{ $t('快捷鍵設定') }}</span>
                </a-menu-item>
                <a-menu-item @click="emit('logout')">
                  <LogoutOutlined style="margin-right: 10px" />
                  <span>{{ $t('退出登入') }}</span>
                </a-menu-item>
              </a-menu>
            </template>
            <button class="dk-user">
              <a-avatar :src="userStore.userInfo.avatarUrl" :size="32" />
              <span>{{ userStore.userInfo.realname }}</span>
              <DownOutlined style="font-size: 11px" />
            </button>
          </a-dropdown>
        </div>
      </div>

      <!-- 同一分類的頁面：直接切換，不必回主選單 -->
      <div v-if="!atHome && group && group.items.length > 1" class="dk-tabs" role="tablist">
        <button
          v-for="i in group.items"
          :key="i.path"
          role="tab"
          :aria-selected="i.name === route.name"
          :class="{ on: i.name === route.name }"
          :title="pageHint(i)"
          @click="emit('open', i.path)"
        >
          <component :is="i.meta.icon" />
          {{ i.meta.title }}
        </button>
      </div>
    </header>

    <main class="dk-main">
      <DesktopHome v-if="atHome" :groups="groups" :is-agent-user="isAgentUser" @open="(p) => emit('open', p)" />
      <router-view v-else-if="alive" />
      <footer class="dk-foot">{{ $t('Copyright © 2025-2026 三把扇科技 All rights reserved.') }}</footer>
    </main>

    <CommandPalette v-model:open="palette" :groups="groups" @open-page="(p) => emit('open', p)" @action="runAction" />
    <ShortcutSettings v-model:open="settings" :groups="groups" />
    <LanguageModal v-model:open="language" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AppstoreFilled, AppstoreOutlined, RightOutlined, ReloadOutlined, DownOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { HOME_ROUTE_NAME, groupOf } from '@/utils/menuGroups'
import type { MenuGroup } from '@/utils/menuGroups'
import { t } from '@/i18n'
import { isDark, setThemePref } from '@/utils/colorScheme'
import { comboFromEvent, comboText, config, loadConfig, recording, shouldIgnore } from '@/shortcuts'
import LanguageButton from '@/components/LanguagePicker/LanguageButton.vue'
import LanguageModal from '@/components/LanguagePicker/LanguageModal.vue'
import CommandPalette from '@/shortcuts/CommandPalette.vue'
import ShortcutSettings from '@/shortcuts/ShortcutSettings.vue'
import ShortcutKeys from '@/shortcuts/ShortcutKeys.vue'
import DesktopHome from './DesktopHome.vue'

const props = defineProps({
  groups: { type: Array as () => MenuGroup[], default: () => [] },
  isAgentUser: { type: Boolean, default: false },
})
const emit = defineEmits(['home', 'open', 'userinfo', 'logout'])
const route = useRoute()
const userStore = useUserStore()

const atHome = computed(() => route.name === HOME_ROUTE_NAME)
// 目前頁面所在的分類：先找選單上的頁面；不在選單上的頁面（例如團長詳情）用它上層的選單分類
const group = computed(() => {
  const byName = groupOf(props.groups, route.name as string)
  if (byName) return byName
  const top = route.matched[1]
  return (top && props.groups.find((g) => g.key === top.path)) || null
})
const pageTitle = computed(() => (route.meta.title as string) || '')

// 重新整理目前頁面（重建元件，不重新載入整個網頁）
const alive = ref(true)
const spinning = ref(false)
function refresh() {
  alive.value = false
  spinning.value = true
  nextTick(() => (alive.value = true))
  setTimeout(() => (spinning.value = false), 400)
}

// ───────── 快捷鍵 ─────────
const palette = ref(false)
const settings = ref(false)
const language = ref(false)

// 依帳號載入（同一台裝置不同帳號各自設定）
watch(
  () => userStore.userInfo.sysUserId,
  (id) => loadConfig(String(id || '')),
  { immediate: true },
)

const hint = (label: string, action: string) => {
  const c = config.value.actions[action]
  return c ? `${t(label)} (${comboText(c)})` : t(label)
}
const pageHint = (item) => {
  const p = config.value.pages.find((x) => x.name === item.name)
  return p ? `${item.meta.title} (${comboText(p.combo)})` : item.meta.title
}

// 同分類的上一頁／下一頁（在主選單時從第一個分類開始）
function stepTab(dir: number) {
  const g = group.value
  if (!g || !g.items.length) return
  const i = g.items.findIndex((x) => x.name === route.name)
  const next = g.items[(i + dir + g.items.length) % g.items.length]
  emit('open', next.path)
}

function runAction(id: string) {
  if (id === 'palette') palette.value = !palette.value
  else if (id === 'home') emit('home')
  else if (id === 'prevTab') stepTab(-1)
  else if (id === 'nextTab') stepTab(1)
  else if (id === 'refresh') !atHome.value && refresh()
  else if (id === 'language') language.value = true
  else if (id === 'theme') setThemePref(isDark.value ? 'light' : 'dark')
  else if (id === 'help') settings.value = true
}

function findPage(name: string) {
  for (const g of props.groups) for (const i of g.items) if (i.name === name) return i
  return null
}

function onKeydown(e: KeyboardEvent) {
  if (recording.value || e.repeat || e.isComposing) return
  const combo = comboFromEvent(e)
  if (!combo) return
  const cfg = config.value
  const actionId = Object.keys(cfg.actions).find((id) => cfg.actions[id] === combo)
  const page = actionId ? null : cfg.pages.find((p) => p.combo === combo)
  if (!actionId && !page) return
  if (shouldIgnore(e, combo)) return
  // 快速搜尋或設定視窗開著時，只接受開關快速搜尋
  if ((palette.value || settings.value) && actionId !== 'palette') return
  e.preventDefault()
  e.stopPropagation()
  if (actionId) return runAction(actionId)
  const item = findPage(page!.name)
  if (item) emit('open', item.path)
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<style lang="less">
// 版面與元件外觀沿用 desktop-theme.less 的 .jp-desk（背景、卡片、表格、按鈕），這裡只放外框與首頁
.dk-app {
  // 整個外框自己捲動（頂部列 sticky），背景固定不隨內容截斷
  height: 100vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  // 只重設外框與首頁自己的按鈕；頁面內的 Ant Design 按鈕不動
  :where(.dk-top, .dk-hello, .dk-glance, .dk-groups) :where(button:not(.ant-btn)) {
    font: inherit;
    color: inherit;
    background: none;
    border: 0;
    padding: 0;
    cursor: pointer;
  }
}

// ───────── 頂部：浮動毛玻璃列＋分頁籤 ─────────
.dk-top {
  position: sticky;
  top: 0;
  z-index: 50;
  padding: 12px 20px 0;
}
.dk-bar,
.dk-tabs {
  border: 1px solid var(--jp-card-edge);
  background: linear-gradient(180deg, var(--jp-glass) 0%, var(--jp-glass-2) 100%);
  -webkit-backdrop-filter: saturate(180%) blur(22px);
  backdrop-filter: saturate(180%) blur(22px);
  box-shadow: var(--jp-shadow-1), var(--jp-card-gloss);
}
.dk-bar {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 12px 0 16px;
  border-radius: 18px;
}
.dk-brand {
  display: flex;
  align-items: center;
  flex: none;
  padding-right: 16px !important;
  border-right: 1px solid var(--jp-card-line) !important;
  height: 36px;
}
.dk-brand-ic {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 17px;
  background: var(--jp-brand-grad);
  box-shadow: var(--jp-brand-glow), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.dk-crumb {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
  overflow: hidden;
  color: var(--jp-text-2);
  .dk-sep {
    font-size: 11px;
    color: var(--jp-text-3);
  }
  .dk-crumb-page {
    color: var(--jp-text);
    font-weight: 650;
    font-size: 15px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
.dk-home {
  display: inline-flex !important;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 14px !important;
  border-radius: 12px;
  font-weight: 600;
  color: var(--jp-primary) !important;
  background: var(--jp-primary-soft) !important;
  transition: all 0.2s ease;
  &:hover,
  &.on {
    color: #fff !important;
    background: var(--jp-brand-grad) !important;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.28);
  }
}
.dk-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
}
.dk-search {
  display: inline-flex !important;
  align-items: center;
  gap: 8px;
  height: 36px;
  min-width: 220px;
  padding: 0 8px 0 12px !important;
  margin-right: 6px;
  border-radius: 12px;
  color: var(--jp-text-3) !important;
  background: var(--jp-card-bg) !important;
  border: 1px solid var(--jp-card-edge) !important;
  box-shadow: var(--jp-card-gloss), inset 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: border-color 0.2s ease;
  .dk-search-text {
    flex: 1;
    text-align: left;
  }
  &:hover {
    border-color: var(--jp-primary) !important;
    color: var(--jp-text-2) !important;
  }
}
@media (max-width: 1100px) {
  .dk-search {
    min-width: 0;
    .dk-search-text,
    .jp-kbd-row {
      display: none;
    }
  }
}
.dk-kbd-ic {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  vertical-align: -3px;
}
.dk-icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid !important;
  place-items: center;
  color: var(--jp-text-2) !important;
  &:hover {
    color: var(--jp-primary) !important;
    background: var(--jp-primary-soft) !important;
  }
}
.dk-spin {
  animation: dk-rotate 0.4s linear;
}
@keyframes dk-rotate {
  to {
    transform: rotate(360deg);
  }
}
.dk-user {
  display: inline-flex !important;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 10px 0 6px !important;
  margin-left: 4px;
  border-radius: 14px;
  font-weight: 600;
  &:hover {
    background: var(--jp-primary-soft) !important;
  }
  .ant-avatar {
    box-shadow: 0 0 0 2px var(--jp-surface), 0 4px 12px rgba(37, 99, 235, 0.3);
  }
}
.dk-tabs {
  margin-top: 10px;
  padding: 5px;
  border-radius: 16px;
  display: flex;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
  button {
    flex: none;
    display: inline-flex !important;
    align-items: center;
    gap: 7px;
    height: 36px;
    padding: 0 14px !important;
    border-radius: 12px;
    color: var(--jp-text-2) !important;
    font-weight: 500;
    white-space: nowrap;
    transition: background 0.2s ease, color 0.2s ease;
    &:hover {
      color: var(--jp-text) !important;
      background: var(--jp-primary-soft) !important;
    }
    &.on {
      color: #fff !important;
      font-weight: 600;
      background: var(--jp-brand-grad) !important;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.28);
    }
  }
}

// ───────── 內容 ─────────
.dk-main {
  flex: 1;
  padding: 16px 20px 0;
  display: flex;
  flex-direction: column;
  > :not(.dk-foot) {
    flex-shrink: 0;
  }
}
.dk-foot {
  margin-top: auto;
  padding: 36px 0 24px;
  text-align: center;
  color: var(--jp-text-3);
  font-size: 12.5px;
}

// ───────── 首頁 ─────────
.dk-hello {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 4px 20px;
}
.dk-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  flex: none;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  background: var(--jp-brand-grad);
  box-shadow: 0 0 0 3px var(--jp-surface), 0 6px 16px rgba(37, 99, 235, 0.3);
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.dk-hello-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  b {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 0.02em;
    color: var(--jp-text);
  }
  > span {
    color: var(--jp-text-3);
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
.dk-role {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(14, 165, 233, 0.14));
}
html[data-theme='dark'] .dk-role {
  color: #93c5fd;
}

.dk-glance {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.dk-stat {
  display: flex !important;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 18px 20px !important;
  border-radius: 18px;
  text-align: left;
  background: var(--jp-card-bg) !important;
  border: 1px solid var(--jp-card-edge) !important;
  box-shadow: var(--jp-card-gloss), var(--jp-shadow-1);
  small {
    color: var(--jp-text-3);
    font-weight: 500;
  }
  strong {
    font-size: 26px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    letter-spacing: -0.01em;
  }
  &.main {
    position: relative;
    overflow: hidden;
    color: #fff !important;
    background: var(--jp-brand-grad) !important;
    border-color: transparent !important;
    box-shadow: var(--jp-brand-glow), inset 0 1px 0 rgba(255, 255, 255, 0.3);
    small {
      color: rgba(255, 255, 255, 0.8);
    }
  }
}

// 分類：每一區一張卡，區內是可點進去的頁面圖示；寬螢幕自動排成多欄
.dk-groups {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 18px;
  align-items: start;
}
.dk-group {
  padding: 18px 18px 14px;
  border-radius: 20px;
  background: var(--jp-card-bg);
  border: 1px solid var(--jp-card-edge);
  box-shadow: var(--jp-card-gloss), var(--jp-shadow-1);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  backdrop-filter: blur(14px) saturate(140%);
  h3 {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 12px;
    font-size: 16px;
    font-weight: 650;
    color: var(--jp-text);
    small {
      margin-left: auto;
      font-size: 12px;
      font-weight: 500;
      color: var(--jp-text-3);
    }
  }
}
.dk-group-ic {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: var(--jp-primary);
  background: var(--jp-primary-soft);
  font-size: 15px;
}
.dk-tiles {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(112px, 1fr));
  gap: 6px;
}
.dk-tile {
  position: relative;
  display: flex !important;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 14px 6px 12px !important;
  border-radius: 16px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--jp-text) !important;
  transition: background 0.2s ease, transform 0.15s ease;
  &:hover {
    background: var(--jp-primary-soft) !important;
    .dk-ic {
      transform: translateY(-2px);
      box-shadow: 0 10px 18px -6px rgba(37, 99, 235, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.35);
    }
  }
  &:active {
    transform: scale(0.97);
  }
  &:focus-visible {
    outline: 2px solid var(--jp-primary);
    outline-offset: 2px;
  }
}
.dk-tile-key {
  margin-top: -4px;
  opacity: 0.85;
}
.dk-tile-name {
  text-align: center;
  line-height: 1.35;
  font-size: 13.5px;
}
// 圖示：與手機主選單相同的六色漸層＋上半部光澤
.dk-ic {
  position: relative;
  overflow: hidden;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 23px;
  color: #fff;
  background: var(--jp-ic-1);
  box-shadow: 0 6px 14px -4px rgba(37, 99, 235, 0.42), inset 0 1px 0 rgba(255, 255, 255, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  &::after {
    content: '';
    position: absolute;
    inset: 0 0 50% 0;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0));
    pointer-events: none;
  }
}
.dk-tile:nth-child(6n + 2) .dk-ic { background: var(--jp-ic-2); }
.dk-tile:nth-child(6n + 3) .dk-ic { background: var(--jp-ic-3); }
.dk-tile:nth-child(6n + 4) .dk-ic { background: var(--jp-ic-4); }
.dk-tile:nth-child(6n + 5) .dk-ic { background: var(--jp-ic-5); }
.dk-tile:nth-child(6n + 6) .dk-ic { background: var(--jp-ic-6); }
.dk-badge {
  position: absolute;
  top: 8px;
  right: calc(50% - 36px);
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  line-height: 20px;
  color: #fff;
  background: #ef4444;
  box-shadow: 0 0 0 2px var(--jp-surface);
}
</style>
