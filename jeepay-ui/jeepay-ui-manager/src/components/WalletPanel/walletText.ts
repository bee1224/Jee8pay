// 錢包畫面共用文字與換算（金額在後端一律以「分」儲存）
export const BIZ_TYPE_NAMES = {
  ORDER_SETTLE: '訂單結算',
  ORDER_REVERSE: '退款沖回',
  WITHDRAW_APPLY: '提現凍結',
  WITHDRAW_RELEASE: '提現解凍',
  WITHDRAW_PAID: '提現撥款',
  WITHDRAW_FEE: '提現手續費',
  ADJUST: '人工調帳',
}
export const OWNER_TYPE_NAMES = { PLATFORM: '平台', MCH: '商戶', AGENT: '代理', CHANNEL: '上游渠道' }
export const WITHDRAW_STATES = {
  0: { text: '待審核', color: 'orange' },
  1: { text: '已撥款', color: 'green' },
  2: { text: '已駁回', color: 'red' },
  3: { text: '已取消', color: 'default' },
}
export const RISK_FLAG_NAMES = {
  BLACKLIST: '黑名單',
  DAILY_LIMIT: '當日次數偏多',
  RESTRICTED_BANK: '限制銀行',
  NEW_ACCOUNT: '收款帳戶剛變更',
}
export function yuan(fen) {
  return (Number(fen || 0) / 100).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
export function newReqNo() {
  return 'R' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
}
