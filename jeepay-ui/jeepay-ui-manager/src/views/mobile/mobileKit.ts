/**
 * 手機版共用工具：是否為手機寬度、代理資料快取、今日日期區間、金額格式。
 * 手機版畫面只換呈現方式，資料一律走既有的代理後台／平台 API。
 */
import { ref } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { t } from '@/i18n'

/** 手機寬度門檻：768 以下用手機版外框，其餘維持電腦版 */
const media = window.matchMedia('(max-width: 768px)')
export const isMobile = ref(media.matches)
media.addEventListener('change', (e) => (isMobile.value = e.matches))

/** 代理自己的資料（代理號、層級、我的代理、可用支付方式）；同一次登入只取一次 */
let mePromise: Promise<any> | null = null
export function agentMe(force = false): Promise<any> {
  if (!mePromise || force) {
    mePromise = req.list(API_URL_AGENT_PORTAL + '/me', {}).catch((e) => {
      mePromise = null
      throw e
    })
  }
  return mePromise
}
export function resetAgentMe() {
  mePromise = null
}

const pad = (n: number) => String(n).padStart(2, '0')
export function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
/** 訂單類 API 使用 createdStart／createdEnd */
export function todayCreatedRange() {
  const t = todayStr()
  return { createdStart: `${t} 00:00:00`, createdEnd: `${t} 23:59:59` }
}
/** 錢包、提現、操作紀錄類 API 使用 startDate／endDate（迄日含當日） */
export function todayDateRange() {
  const t = todayStr()
  return { startDate: t, endDate: t }
}

/** 金額一律以「分」儲存；顯示為 NT$1,234 或帶小數 */
export function ntd(fen: any, digits = 0) {
  const v = Number(fen || 0) / 100
  return 'NT$' + v.toLocaleString('zh-TW', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}
/** 只取時間 HH:mm（今日列表用） */
export function hhmm(dt: any) {
  const s = String(dt || '')
  const m = s.match(/(\d{2}):(\d{2})/)
  return m ? `${m[1]}:${m[2]}` : s
}

export const PAY_STATES: Record<number, [string, string]> = {
  0: [t('訂單生成'), 'mute'],
  1: [t('支付中'), 'warn'],
  2: [t('支付成功'), 'ok'],
  3: [t('支付失敗'), 'bad'],
  4: [t('已撤銷'), 'bad'],
  5: [t('已退款'), 'mute'],
  6: [t('已關閉'), 'mute'],
}
export const TRANSFER_STATES: Record<number, [string, string]> = {
  0: [t('訂單生成'), 'mute'],
  1: [t('處理中'), 'warn'],
  2: [t('代付成功'), 'ok'],
  3: [t('代付失敗'), 'bad'],
  4: [t('已關閉'), 'mute'],
}
