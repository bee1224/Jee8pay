/**
 * 亮色／暗色主題：預設跟隨登入裝置的系統設定（prefers-color-scheme），也可在「我的 → 外觀」固定為亮色或深色。
 * 選擇存在這台裝置的瀏覽器；html[data-theme] 供自訂樣式使用，Ant Design 元件由 App.vue 的 ConfigProvider 切換演算法。
 */
import { computed, ref, watch } from 'vue'
import { ConfigProvider, theme } from 'ant-design-vue'
import { t } from '@/i18n'

export type ThemePref = 'auto' | 'light' | 'dark'
const KEY = 'jeepay-theme'
const media = window.matchMedia('(prefers-color-scheme: dark)')

const systemDark = ref(media.matches)
media.addEventListener('change', (e) => (systemDark.value = e.matches))

function readPref(): ThemePref {
  try {
    const v = localStorage.getItem(KEY)
    return v === 'light' || v === 'dark' ? v : 'auto'
  } catch (e) {
    return 'auto'
  }
}
export const themePref = ref<ThemePref>(readPref())
export function setThemePref(p: ThemePref) {
  themePref.value = p
  try {
    if (p === 'auto') localStorage.removeItem(KEY)
    else localStorage.setItem(KEY, p)
  } catch (e) {
    // 無痕模式等無法儲存時，只在這次使用期間生效
  }
}
export const THEME_LABELS: Record<ThemePref, string> = { auto: t('跟隨裝置'), light: t('亮色'), dark: t('深色') }

export const isDark = computed(() => (themePref.value === 'auto' ? systemDark.value : themePref.value === 'dark'))

// Ant Design 元件沿用同一套字形與藍色主色（與 global.less 的 --jp-* 一致）
// 字形清單依介面語言切換，定義在 global.less 的 --jp-font
const FONT_FAMILY = 'var(--jp-font)'
export const antTheme = () => ({
  algorithm: isDark.value ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: { colorPrimary: '#2563eb', fontFamily: FONT_FAMILY, borderRadius: 8 },
})

function apply(dark: boolean) {
  const root = document.documentElement
  root.dataset.theme = dark ? 'dark' : 'light'
  root.style.colorScheme = dark ? 'dark' : 'light'

  // 手機瀏覽器網址列顏色
  let meta = document.querySelector('meta[name="theme-color"]') as HTMLMetaElement | null
  if (!meta) {
    meta = document.createElement('meta')
    meta.name = 'theme-color'
    document.head.appendChild(meta)
  }
  meta.content = dark ? '#141414' : '#ffffff'

  // Modal.confirm、message 等靜態方法不在 App 的 ConfigProvider 之內，需另外設定
  ConfigProvider.config({ theme: antTheme() })
}

watch(isDark, apply, { immediate: true })
