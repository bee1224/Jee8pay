/**
 * 多國語系：以繁體中文原文當翻譯鍵（t('訂單管理')），字典放在 ./locales/<code>.json，只載入目前語言。
 * 找不到翻譯時依 fallback 鏈退回，最後顯示繁體中文原文；{0}、{1} 為參數位置。
 * 語言存在這台裝置的瀏覽器；儲存後整頁重新載入，讓選單、表格欄位、元件語系與日期格式一次全部換掉。
 */
import { ref, shallowRef } from 'vue'

export interface LocaleDef {
  code: string
  name: string // 該語言自己的寫法，切換清單一律顯示這個
  zh: string // 繁體中文說明
  sample: string // 預覽用句子
  fallback?: string
  dayjs: string
}

export const DEFAULT_LOCALE = 'zh-TW'
export const LOCALES: LocaleDef[] = [
  { code: 'zh-TW', name: '繁體中文', zh: '繁體中文（台灣）', sample: '訂單管理・今日成交', dayjs: 'zh-tw' },
  { code: 'zh-CN', name: '简体中文', zh: '簡體中文', sample: '订单管理・今日成交', dayjs: 'zh-cn' },
  { code: 'yue', name: '粵語', zh: '粵語（香港）', sample: '訂單管理・今日成交', fallback: 'zh-TW', dayjs: 'zh-hk' },
  { code: 'en-GB', name: 'English (UK)', zh: '英語（英式）', sample: 'Orders · Today’s sales', fallback: 'en-US', dayjs: 'en-gb' },
  { code: 'en-US', name: 'English (US)', zh: '美語', sample: 'Orders · Today’s sales', dayjs: 'en' },
  { code: 'id', name: 'Bahasa Indonesia', zh: '印尼語', sample: 'Pesanan · Penjualan hari ini', dayjs: 'id' },
  { code: 'ja', name: '日本語', zh: '日本語', sample: '注文管理・本日の売上', dayjs: 'ja' },
  { code: 'ko', name: '한국어', zh: '韓國語', sample: '주문 관리 · 오늘 매출', dayjs: 'ko' },
  { code: 'vi', name: 'Tiếng Việt', zh: '越南語', sample: 'Đơn hàng · Doanh thu hôm nay', dayjs: 'vi' },
  { code: 'th', name: 'ไทย', zh: '泰語', sample: 'คำสั่งซื้อ · ยอดขายวันนี้', dayjs: 'th' },
  { code: 'hi', name: 'हिन्दी', zh: '印地語（印度）', sample: 'ऑर्डर · आज की बिक्री', dayjs: 'hi' },
  { code: 'ru', name: 'Русский', zh: '俄語', sample: 'Заказы · Продажи за сегодня', dayjs: 'ru' },
  { code: 'fil', name: 'Filipino', zh: '菲律賓語', sample: 'Mga order · Benta ngayong araw', fallback: 'en-US', dayjs: 'tl-ph' },
]

const KEY = 'jeepay-locale'
const byCode = (c: string) => LOCALES.find((l) => l.code === c)

function readSaved(): string {
  try {
    const v = localStorage.getItem(KEY)
    if (v && byCode(v)) return v
  } catch (e) {
    // 無痕模式等讀不到時用預設
  }
  return DEFAULT_LOCALE
}

export const locale = ref<string>(readSaved())
export const currentLocale = () => byCode(locale.value) || LOCALES[0]
/** 給 Intl 用的語言代碼（粵語用香港中文） */
export const intlLocale = () => (locale.value === 'yue' ? 'zh-HK' : locale.value)

const dict = shallowRef<Record<string, string>>({})
export const antdLocale = shallowRef<any>(undefined)

export function t(src: string, params?: any[]): string {
  let s = dict.value[src]
  if (s === undefined || s === '') s = src
  if (params) s = s.replace(/\{(\d+)\}/g, (m, i) => (params[i] === undefined || params[i] === null ? '' : String(params[i])))
  return s
}

const files = import.meta.glob('./locales/*.json')

// Ant Design 與 dayjs 的語系檔要寫成固定字串，打包工具才找得到
const ANTD: Record<string, () => Promise<any>> = {
  'zh-TW': () => import('ant-design-vue/es/locale/zh_TW'),
  'zh-CN': () => import('ant-design-vue/es/locale/zh_CN'),
  yue: () => import('ant-design-vue/es/locale/zh_HK'),
  'en-GB': () => import('ant-design-vue/es/locale/en_GB'),
  'en-US': () => import('ant-design-vue/es/locale/en_US'),
  id: () => import('ant-design-vue/es/locale/id_ID'),
  ja: () => import('ant-design-vue/es/locale/ja_JP'),
  ko: () => import('ant-design-vue/es/locale/ko_KR'),
  vi: () => import('ant-design-vue/es/locale/vi_VN'),
  th: () => import('ant-design-vue/es/locale/th_TH'),
  hi: () => import('ant-design-vue/es/locale/hi_IN'),
  ru: () => import('ant-design-vue/es/locale/ru_RU'),
  fil: () => import('ant-design-vue/es/locale/en_US'), // Ant Design 沒有菲律賓語，元件內建文字用英文
}
const DAYJS: Record<string, () => Promise<any>> = {
  'zh-tw': () => import('dayjs/locale/zh-tw'),
  'zh-cn': () => import('dayjs/locale/zh-cn'),
  'zh-hk': () => import('dayjs/locale/zh-hk'),
  'en-gb': () => import('dayjs/locale/en-gb'),
  en: () => Promise.resolve(),
  id: () => import('dayjs/locale/id'),
  ja: () => import('dayjs/locale/ja'),
  ko: () => import('dayjs/locale/ko'),
  vi: () => import('dayjs/locale/vi'),
  th: () => import('dayjs/locale/th'),
  hi: () => import('dayjs/locale/hi'),
  ru: () => import('dayjs/locale/ru'),
  'tl-ph': () => import('dayjs/locale/tl-ph'),
}

async function loadDict(code: string): Promise<Record<string, string>> {
  const def = byCode(code)
  if (!def || code === DEFAULT_LOCALE) return {}
  const base = def.fallback ? await loadDict(def.fallback) : {}
  const loader = files[`./locales/${code}.json`]
  const own = loader ? ((await loader()) as any).default || {} : {}
  return { ...base, ...own }
}

/** 啟動時呼叫一次：載入字典與元件語系，必須在其他畫面模組執行前完成 */
export async function initLocale() {
  const def = currentLocale()
  const [d, antd, dayjs] = await Promise.all([
    loadDict(def.code).catch(() => ({})),
    ANTD[def.code]().then((m) => m.default).catch(() => undefined),
    import('dayjs').then((m) => m.default),
    DAYJS[def.dayjs]().catch(() => undefined),
  ])
  dict.value = d
  antdLocale.value = antd
  dayjs.locale(def.dayjs)
  document.documentElement.lang = def.code
  document.title = t(document.title)
}

/** 儲存語言並整頁重新載入 */
export function saveLocale(code: string) {
  if (!byCode(code)) return
  try {
    if (code === DEFAULT_LOCALE) localStorage.removeItem(KEY)
    else localStorage.setItem(KEY, code)
  } catch (e) {
    // 存不了就只換這一次
  }
  locale.value = code
  switching.value = true
  window.location.reload()
}

/** 重新載入前顯示「切換中」遮罩 */
export const switching = ref(false)

/** 把後端傳來的選單名稱換成目前語言（遞迴） */
export function translateMenuTree(list: any[]) {
  ;(list || []).forEach((m) => {
    if (m.entName) m.entName = t(m.entName)
    if (m.children) translateMenuTree(m.children)
  })
  return list
}

// 列表 API 回傳的資料名稱（支付方式、渠道、角色、權限、系統配置、操作日誌）在資料庫裡是繁中，
// 只在字典有對應時換成目前語言。逐筆讀取（編輯表單用）的回應不轉，避免把譯文存回資料庫。
const LABEL_FIELDS = ['wayName', 'ifName', 'roleName', 'entName', 'configName', 'configDesc', 'groupName', 'methodRemark', 'agentName']
function translateRow(r: any) {
  if (!r || typeof r !== 'object') return
  for (const f of LABEL_FIELDS) {
    const v = r[f]
    if (typeof v === 'string' && dict.value[v]) r[f] = dict.value[v]
  }
  if (Array.isArray(r.children)) r.children.forEach(translateRow)
  if (Array.isArray(r.wayCodes)) r.wayCodes.forEach(translateRow)
  if (Array.isArray(r.payWays)) r.payWays.forEach(translateRow)
}
export function translateListLabels(data: any) {
  if (locale.value === DEFAULT_LOCALE || !data) return data
  if (Array.isArray(data)) data.forEach(translateRow)
  else if (Array.isArray(data.records)) data.records.forEach(translateRow)
  else {
    if (Array.isArray(data.payWays)) data.payWays.forEach(translateRow) // 代理後台 me
    if (Array.isArray(data.agents)) data.agents.forEach(translateRow) // 登入裝置總覽
  }
  return data
}
