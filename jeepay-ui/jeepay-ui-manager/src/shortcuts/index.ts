/**
 * 電腦版快捷鍵：內建動作（可改鍵）＋使用者自訂的「頁面捷徑」。
 * 組合鍵以實體按鍵（KeyboardEvent.code）記錄，不受輸入法與 Alt 產生的特殊字元影響；
 * Mod 代表 Windows 的 Ctrl、macOS 的 ⌘。設定依帳號存在這台裝置的瀏覽器。
 */
import { ref } from 'vue'
import { t } from '@/i18n'

export interface ActionDef {
  id: string
  label: string // 繁中原文（顯示時再 $t）
  combo: string // 預設組合鍵，空字串＝預設不設定
}

// 內建動作：順序即設定畫面的顯示順序
export const ACTIONS: ActionDef[] = [
  { id: 'palette', label: '快速搜尋頁面', combo: 'Mod+K' },
  { id: 'home', label: '回主選單', combo: 'Alt+H' },
  { id: 'prevTab', label: '同分類的上一頁', combo: 'Alt+[' },
  { id: 'nextTab', label: '同分類的下一頁', combo: 'Alt+]' },
  { id: 'refresh', label: '重新整理目前頁面', combo: 'Alt+R' },
  { id: 'language', label: '語言設定', combo: 'Alt+L' },
  { id: 'theme', label: '切換亮色／深色', combo: 'Alt+T' },
  { id: 'help', label: '快捷鍵一覽與設定', combo: 'Shift+/' },
]

export interface PageShortcut {
  name: string // 路由名稱（權限 ID），不受網址與語言影響
  combo: string
}
export interface ShortcutConfig {
  actions: Record<string, string>
  pages: PageShortcut[]
  showOnHome: boolean // 主選單圖示下方是否顯示頁面捷徑的按鍵
}

export const isMac = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent)

export const defaultConfig = (): ShortcutConfig => ({
  actions: Object.fromEntries(ACTIONS.map((a) => [a.id, a.combo])),
  pages: [],
  showOnHome: true,
})

// ───────── 儲存（依帳號、存在這台裝置） ─────────
const storageKey = (userId: string) => 'jeepay-shortcuts:' + (userId || 'anon')

export const config = ref<ShortcutConfig>(defaultConfig())
let currentUser = ''

export function loadConfig(userId: string) {
  currentUser = userId
  const base = defaultConfig()
  try {
    const raw = localStorage.getItem(storageKey(userId))
    if (raw) {
      const saved = JSON.parse(raw)
      // 新版本加入的動作沿用預設值；已移除的動作忽略
      for (const a of ACTIONS) if (saved.actions && typeof saved.actions[a.id] === 'string') base.actions[a.id] = saved.actions[a.id]
      if (Array.isArray(saved.pages)) base.pages = saved.pages.filter((p) => p && p.name && p.combo)
      if (typeof saved.showOnHome === 'boolean') base.showOnHome = saved.showOnHome
    }
  } catch (e) {
    // 讀不到（無痕模式等）就用預設
  }
  config.value = base
}

export function saveConfig(next: ShortcutConfig) {
  config.value = JSON.parse(JSON.stringify(next))
  try {
    localStorage.setItem(storageKey(currentUser), JSON.stringify(config.value))
  } catch (e) {
    // 存不了就只在這次使用期間生效
  }
}

// ───────── 組合鍵 ─────────
const MODIFIER_CODES = ['ControlLeft', 'ControlRight', 'AltLeft', 'AltRight', 'ShiftLeft', 'ShiftRight', 'MetaLeft', 'MetaRight', 'CapsLock', 'Fn']
const SYMBOLS: Record<string, string> = {
  Slash: '/', Period: '.', Comma: ',', Semicolon: ';', Quote: "'", Backquote: '`', Minus: '-', Equal: '=',
  BracketLeft: '[', BracketRight: ']', Backslash: '\\',
}
const NAMED = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Enter', 'Space', 'Home', 'End', 'PageUp', 'PageDown', 'Insert', 'Delete']

function keyName(code: string): string | null {
  let m = /^Key([A-Z])$/.exec(code)
  if (m) return m[1]
  m = /^(?:Digit|Numpad)([0-9])$/.exec(code)
  if (m) return m[1]
  if (/^F([1-9]|1[0-2])$/.test(code)) return code
  if (SYMBOLS[code]) return SYMBOLS[code]
  if (NAMED.includes(code)) return code
  return null // Esc、Tab、Backspace 等不能設成快捷鍵
}

/** 鍵盤事件轉成組合鍵字串（例如 Mod+Shift+K）；只按了修飾鍵或不支援的按鍵時回傳 null */
export function comboFromEvent(e: KeyboardEvent): string | null {
  if (MODIFIER_CODES.includes(e.code)) return null
  const key = keyName(e.code)
  if (!key) return null
  if (!isMac && e.metaKey) return null // Windows 鍵留給系統
  const parts: string[] = []
  if (isMac ? e.metaKey : e.ctrlKey) parts.push('Mod')
  if (isMac && e.ctrlKey) parts.push('Ctrl')
  if (e.altKey) parts.push('Alt')
  if (e.shiftKey) parts.push('Shift')
  parts.push(key)
  return parts.join('+')
}

const GLYPH_MAC: Record<string, string> = { Mod: '⌘', Ctrl: '⌃', Alt: '⌥', Shift: '⇧' }
const GLYPH_KEY: Record<string, string> = { ArrowUp: '↑', ArrowDown: '↓', ArrowLeft: '←', ArrowRight: '→', Enter: '↵', Space: 'Space' }

/** 顯示用：拆成一個個按鍵（macOS 用 ⌘⌥⇧ 符號） */
export function comboKeys(combo: string): string[] {
  if (!combo) return []
  if (combo === 'Shift+/') return ['?']
  return combo.split('+').map((p) => (isMac && GLYPH_MAC[p]) || (p === 'Mod' ? 'Ctrl' : GLYPH_KEY[p] || p))
}
export const comboText = (combo: string) => comboKeys(combo).join(isMac ? '' : '+')

const hasModifier = (combo: string) => /(^|\+)(Mod|Ctrl|Alt)\+/.test(combo)

// 瀏覽器或系統本身要用的組合鍵，不能拿來設定
const RESERVED = new Set([
  'Mod+C', 'Mod+V', 'Mod+X', 'Mod+A', 'Mod+Z', 'Mod+Shift+Z', 'Mod+Y', 'Mod+F', 'Mod+P', 'Mod+S', 'Mod+R', 'Mod+Shift+R',
  'Mod+W', 'Mod+T', 'Mod+N', 'Mod+Q', 'Mod+L', 'Mod+D', 'Mod+H', 'Mod+M', 'Mod+Shift+T', 'Mod+Shift+N', 'Mod+Shift+I', 'Mod+Shift+J',
  'Mod+Shift+C', 'Mod+[', 'Mod+]', 'Mod+1', 'Mod+2', 'Mod+3', 'Mod+4', 'Mod+5', 'Mod+6', 'Mod+7', 'Mod+8', 'Mod+9', 'Mod+0',
  'Alt+F', 'Alt+E', 'Alt+D', 'Alt+ArrowLeft', 'Alt+ArrowRight', 'Alt+Home', 'F1', 'F5', 'F11', 'F12', 'Mod+Space',
])

/** 檢查能不能設定；可以就回傳 null，否則回傳原因（已翻譯） */
export function comboProblem(combo: string): string | null {
  if (RESERVED.has(combo)) return t('{0} 是瀏覽器或系統使用的按鍵，請換一組', [comboText(combo)])
  if (!hasModifier(combo) && !/^F\d+$/.test(combo) && combo !== 'Shift+/') {
    return t('請搭配 {0} 或 {1} 一起按，避免打字時誤觸', [isMac ? '⌘' : 'Ctrl', isMac ? '⌥' : 'Alt'])
  }
  return null
}

/** 不含 Ctrl／Alt／⌘ 的快捷鍵（例如 ?）在輸入框打字時不觸發 */
export function shouldIgnore(e: KeyboardEvent, combo: string): boolean {
  if (hasModifier(combo)) return false
  const el = e.target as HTMLElement | null
  if (!el) return false
  const tag = el.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el.isContentEditable
}

/** 組合鍵目前被誰使用：回傳 { kind: 'action', id } 或 { kind: 'page', name } */
export function whoUses(cfg: ShortcutConfig, combo: string): { kind: 'action' | 'page'; id: string } | null {
  if (!combo) return null
  for (const a of ACTIONS) if (cfg.actions[a.id] === combo) return { kind: 'action', id: a.id }
  const p = cfg.pages.find((x) => x.combo === combo)
  return p ? { kind: 'page', id: p.name } : null
}

/** 設定畫面錄製組合鍵時暫停全域快捷鍵 */
export const recording = ref(false)
