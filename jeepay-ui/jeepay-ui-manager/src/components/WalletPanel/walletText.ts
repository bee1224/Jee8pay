import { t } from '@/i18n'
// 錢包畫面共用文字與換算（金額在後端一律以「分」儲存）
export const BIZ_TYPE_NAMES = {
  ORDER_SETTLE: t('訂單結算'),
  ORDER_REVERSE: t('退款沖回'),
  WITHDRAW_APPLY: t('提現凍結'),
  WITHDRAW_RELEASE: t('提現解凍'),
  WITHDRAW_PAID: t('提現撥款'),
  WITHDRAW_FEE: t('提現手續費'),
  ADJUST: t('人工調帳'),
  FREEZE: t('人工凍結'),
  UNFREEZE: t('人工解凍'),
}
export const OWNER_TYPE_NAMES = { PLATFORM: t('平台'), MCH: t('商戶'), AGENT: t('代理'), CHANNEL: t('上游渠道') }
export const WITHDRAW_STATES = {
  0: { text: t('待審核'), color: 'orange' },
  1: { text: t('已撥款'), color: 'green' },
  2: { text: t('已駁回'), color: 'red' },
  3: { text: t('已取消'), color: 'default' },
}
export const RISK_FLAG_NAMES = {
  BLACKLIST: t('黑名單'),
  DAILY_LIMIT: t('當日次數偏多'),
  RESTRICTED_BANK: t('限制銀行'),
  NEW_ACCOUNT: t('收款帳戶剛變更'),
}
export function yuan(fen) {
  return (Number(fen || 0) / 100).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
export function newReqNo() {
  return 'R' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
}
