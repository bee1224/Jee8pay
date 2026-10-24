/**
 * 上帝主選單各頁的手機版設定（給 MCrud 使用）：清單、搜尋、篩選、明細欄位、操作與表單。
 * API 一律沿用電腦版；新增／修改若電腦版已有抽屜編輯器就直接沿用（手機上抽屜為全寬）。
 */
import { defineAsyncComponent } from 'vue'
import {
  API_URL_AGENT_INFO, API_URL_FEE_TEMPLATES, API_URL_MCH_LIST, API_URL_MCH_NOTIFY_LIST, API_URL_PAYWAYS_LIST, API_URL_REFUND_ORDER_LIST,
  API_URL_RISK_BLACKLIST, API_URL_ROLE_LIST, API_URL_SYS_LOG, API_URL_SYS_USER_LIST, API_URL_TRANSFER_ORDER_LIST, API_URL_UAT_EDGE_ALLOWLIST,
  API_URL_WALLET, API_URL_WAY_ROUTES, API_URL_WITHDRAWS, mchNotifyResend, req,
} from '@/api/manage'
import { BIZ_TYPE_NAMES, OWNER_TYPE_NAMES, RISK_FLAG_NAMES, WITHDRAW_STATES } from '@/components/WalletPanel/walletText'
import { ntd } from '../mobileKit'
import { t } from '@/i18n'

const yuan2 = (fen) => ntd(fen, 2)
const signed = (fen) => (Number(fen) > 0 ? '+' : '') + ntd(fen, 2)
const owner = (r) => `${OWNER_TYPE_NAMES[r.ownerType] || r.ownerType} ${r.ownerName || r.ownerId || ''}`.trim()
const onOff = (s) => (s === 1 ? [t('啟用'), 'ok'] : [t('停用'), 'mute'])
const realPhone = (t) => (String(t || '').startsWith('-') ? '' : t)
const agentOptions = (level?) => () => req.list(API_URL_AGENT_INFO, { pageSize: -1, ...(level ? { agentLevel: level } : {}) })

// ---------------- 用戶角色 ----------------
const sysUsers = {
  url: API_URL_SYS_USER_LIST, key: 'sysUserId',
  params: { sysType: 'MGR' },
  search: { param: 'realname', placeholder: t('搜尋姓名') },
  filters: [{ label: t('全部'), params: {} }, { label: t('啟用中'), params: { state: 1 } }, { label: t('已停用'), params: { state: 0 } }],
  countText: (n) => t('共 {0} 位操作員', [n]),
  editors: { info: defineAsyncComponent(() => import('@/views/sysuser/AddOrEdit.vue')), role: defineAsyncComponent(() => import('@/views/sysuser/RoleDist.vue')) },
  add: { label: t('新增操作員'), access: 'ENT_UR_USER_ADD', run: (_, c) => (c.editors.info.show(), false) },
  row: {
    title: (r) => r.realname || r.loginUsername,
    sub: (r) => [t('帳號 {0}{1}', [r.loginUsername || '', realPhone(r.telphone) ? '・' + r.telphone : '']), r.belongInfoId && r.belongInfoId !== '0' ? t('代理 {0}', [r.belongInfoId]) : ''],
    pill: (r) => (r.isAdmin === 1 ? [t('上帝'), 'warn'] : onOff(r.state)),
  },
  detail: (r) => [[t('用戶ID'), r.sysUserId], [t('登入帳號'), r.loginUsername], [t('手機號'), realPhone(r.telphone)], [t('身分'), r.isAdmin === 1 ? t('上帝（全部權限）') : t('一般操作員')], [t('狀態'), onOff(r.state)[0]], [t('建立時間'), r.createdAt]],
  actions: [
    { label: t('修改'), access: 'ENT_UR_USER_EDIT', run: (r, c) => (c.editors.info.show(r.sysUserId), false) },
    { label: t('分配角色'), access: 'ENT_UR_USER_UPD_ROLE', show: (r) => r.isAdmin !== 1, run: (r, c) => (c.editors.role.show(r.sysUserId), false) },
    { label: t('停用'), cls: 'danger', access: 'ENT_UR_USER_EDIT', show: (r) => r.state === 1 && r.isAdmin !== 1, confirm: (r) => t('停用「{0}」？停用後立即登出且不能再登入', [r.realname]), run: (r) => req.updateById(API_URL_SYS_USER_LIST, r.sysUserId, { state: 0 }).then(() => t('已停用')) },
    { label: t('啟用'), access: 'ENT_UR_USER_EDIT', show: (r) => r.state !== 1, run: (r) => req.updateById(API_URL_SYS_USER_LIST, r.sysUserId, { state: 1 }).then(() => t('已啟用')) },
    { label: t('刪除'), cls: 'danger', access: 'ENT_UR_USER_DELETE', show: (r) => r.isAdmin !== 1, confirm: (r) => t('刪除操作員「{0}」？', [r.realname]), run: (r) => req.delById(API_URL_SYS_USER_LIST, r.sysUserId).then(() => t('已刪除')) },
  ],
}

const roles = {
  url: API_URL_ROLE_LIST, key: 'roleId',
  search: { param: 'roleName', placeholder: t('搜尋角色名稱') },
  countText: (n) => t('共 {0} 個角色', [n]),
  editors: { info: defineAsyncComponent(() => import('@/views/role/AddOrEdit.vue')) },
  add: { label: t('新增角色'), access: 'ENT_UR_ROLE_ADD', run: (_, c) => (c.editors.info.show(), false) },
  row: { title: (r) => r.roleName, sub: (r) => [r.roleId] },
  detail: (r) => [[t('角色ID'), r.roleId], [t('名稱'), r.roleName], [t('建立時間'), r.createdAt]],
  actions: [
    { label: t('修改名稱與權限'), access: 'ENT_UR_ROLE_EDIT', run: (r, c) => (c.editors.info.show(r.roleId), false) },
    { label: t('刪除'), cls: 'danger', access: 'ENT_UR_ROLE_DEL', confirm: (r) => t('刪除角色「{0}」？已分配此角色的操作員會失去對應權限', [r.roleName]), run: (r) => req.delById(API_URL_ROLE_LIST, r.roleId).then(() => t('已刪除')) },
  ],
}

// ---------------- 費率範本 ----------------
const LAYERS = { PLATFORM: t('平臺費'), CHANNEL: t('渠道費'), SR_AGENT: t('團長費'), AGENT: t('隊長費') }
const feeTemplates = {
  url: API_URL_FEE_TEMPLATES, key: 'templateId', params: { pageSize: -1 }, paged: false,
  note: t('範本是一組「支付方式 × 費率層」的設定，可一次套用到多位代理或商戶。新增與修改費率內容請用電腦版。'),
  row: { title: (r) => r.templateName, sub: (r) => [r.remark, t('最後修改 {0}・{1}', [r.updatedBy || '—', r.updatedAt || ''])] },
  fetchDetail: (r) => req.getById(API_URL_FEE_TEMPLATES, r.templateId),
  detail: (r) => [[t('範本ID'), r.templateId], [t('備註'), r.remark], ...((r.items || []) as any[]).map((it) => [`${it.wayCode}・${LAYERS[it.layer] || it.layer}`, `${(Number(it.rate) * 100).toFixed(4).replace(/\.?0+$/, '')}% + ${ntd(it.fixedAmount, 2)}`])],
  actions: [
    {
      label: t('套用'), access: 'ENT_FEE_TEMPLATE', formTitle: (r) => t('套用：') + r.templateName,
      form: [
        { key: 'targetType', label: t('套用到'), type: 'seg', value: 'AGENT', options: [{ value: 'AGENT', label: t('代理') }, { value: 'MCH', label: t('商戶') }] },
        { key: 'agents', label: t('代理（團長套團長費、隊長套隊長費）'), type: 'select', multiple: true, value: () => [], options: [] },
        { key: 'mchs', label: t('商戶號（可多個，以逗號分隔）'), placeholder: 'M1739200101,M1739200102' },
      ],
      note: t('選「代理」時填第一欄，選「商戶」時填第二欄。'),
      submit: (v, r) => {
        const ids = v.targetType === 'AGENT' ? v.agents || [] : String(v.mchs || '').split(/[,，\s]+/).filter(Boolean)
        if (!ids.length) return false
        return req.add(`${API_URL_FEE_TEMPLATES}/${r.templateId}/apply`, { targetType: v.targetType, targetIds: JSON.stringify(ids) }).then((res) => t('已套用 {0} 筆', [res && res.applied != null ? res.applied : '']))
      },
    },
    { label: t('刪除'), cls: 'danger', access: 'ENT_FEE_TEMPLATE', confirm: (r) => t('刪除範本「{0}」？', [r.templateName]), run: (r) => req.delById(API_URL_FEE_TEMPLATES, r.templateId).then(() => t('已刪除')) },
  ],
  init: (c) => agentOptions()().then((res) => {
    const opts = (res.records || []).map((a) => ({ value: a.agentNo, label: `${a.agentLevel === 1 ? t('團長') : t('隊長')} ${a.agentName}（${a.agentNo}）` }))
    feeTemplates.actions[0].form[1].options = opts as any
  }),
}

// ---------------- 訂單中心 ----------------
const REFUND_STATES = { 0: [t('訂單生成'), 'mute'], 1: [t('退款中'), 'warn'], 2: [t('退款成功'), 'ok'], 3: [t('退款失敗'), 'bad'], 4: [t('已關閉'), 'mute'] }
const refunds = {
  url: API_URL_REFUND_ORDER_LIST, key: 'refundOrderId', dates: 'created',
  search: { param: 'unionOrderId', placeholder: t('退款／支付／商戶單號') },
  filters: [{ label: t('全部'), params: {} }, { label: t('退款中'), params: { state: 1 } }, { label: t('成功'), params: { state: 2 } }, { label: t('失敗'), params: { state: 3 } }],
  countText: (n, r) => t('{0}・{1} 筆', [r, n]), empty: t('這段期間沒有退款'),
  row: { title: (r) => r.mchName || r.mchNo, sub: (r) => [r.refundOrderId, t('支付 {0}・{1}', [ntd(r.payAmount), r.createdAt])], amount: (r) => '−' + ntd(r.refundAmount), pill: (r) => REFUND_STATES[r.state] },
  fetchDetail: (r) => req.getById(API_URL_REFUND_ORDER_LIST, r.refundOrderId),
  detail: (r) => [[t('退款單號'), r.refundOrderId], [t('支付單號'), r.payOrderId], [t('商戶退款單號'), r.mchRefundNo], [t('商戶'), `${r.mchName || ''} ${r.mchNo}`], [t('支付金額'), ntd(r.payAmount)], [t('退款金額'), ntd(r.refundAmount)], [t('原因'), r.refundReason], [t('狀態'), (REFUND_STATES[r.state] || [])[0]], [t('錯誤'), r.errMsg], [t('建立'), r.createdAt], [t('完成'), r.successTime]],
}
const TRANSFER_STATES = { 0: [t('訂單生成'), 'mute'], 1: [t('轉帳中'), 'warn'], 2: [t('轉帳成功'), 'ok'], 3: [t('轉帳失敗'), 'bad'], 4: [t('已關閉'), 'mute'] }
const transfers = {
  url: API_URL_TRANSFER_ORDER_LIST, key: 'transferId', dates: 'created',
  search: { param: 'unionOrderId', placeholder: t('轉帳／商戶單號') },
  countText: (n, r) => t('{0}・{1} 筆', [r, n]), empty: t('這段期間沒有轉帳（目前上游渠道未提供代付）'),
  row: { title: (r) => `${r.mchName || r.mchNo} → ${r.accountName || '—'}`, sub: (r) => [r.transferId, `${r.bankName || ''} ${r.accountNo || ''}・${r.createdAt}`], amount: (r) => '−' + ntd(r.amount), pill: (r) => TRANSFER_STATES[r.state] },
  detail: (r) => [[t('轉帳單號'), r.transferId], [t('商戶單號'), r.mchOrderNo], [t('商戶'), `${r.mchName || ''} ${r.mchNo}`], [t('金額'), ntd(r.amount)], [t('收款人'), r.accountName], [t('收款帳號'), r.accountNo], [t('銀行'), r.bankName], [t('備註'), r.transferDesc], [t('狀態'), (TRANSFER_STATES[r.state] || [])[0]], [t('錯誤'), r.errMsg], [t('建立'), r.createdAt]],
}
const NOTIFY_STATES = { 1: [t('通知中'), 'warn'], 2: [t('已成功'), 'ok'], 3: [t('失敗'), 'bad'] }
const ORDER_TYPES = { 1: t('支付'), 2: t('退款'), 3: t('轉帳') }
const notifies = {
  url: API_URL_MCH_NOTIFY_LIST, key: 'notifyId', dates: 'created',
  search: { param: 'orderId', placeholder: t('訂單號') },
  filters: [{ label: t('全部'), params: {} }, { label: t('失敗'), params: { state: 3 } }, { label: t('通知中'), params: { state: 1 } }, { label: t('成功'), params: { state: 2 } }],
  countText: (n, r) => t('{0}・{1} 筆', [r, n]), empty: t('這段期間沒有商戶通知'),
  row: { title: (r) => r.orderId, sub: (r) => [t('{0}・商戶 {1}・{2}', [ORDER_TYPES[r.orderType] || '', r.mchNo, r.mchOrderNo || '']), t('已通知 {0} 次・{1}', [r.notifyCount || 0, r.createdAt])], pill: (r) => NOTIFY_STATES[r.state] },
  fetchDetail: (r) => req.getById(API_URL_MCH_NOTIFY_LIST, r.notifyId),
  detail: (r) => [[t('訂單'), r.orderId], [t('類型'), ORDER_TYPES[r.orderType]], [t('商戶'), r.mchNo], [t('商戶單號'), r.mchOrderNo], [t('狀態'), (NOTIFY_STATES[r.state] || [])[0]], [t('次數'), `${r.notifyCount || 0}／${r.notifyCountLimit || 6}`], [t('最後通知'), r.lastNotifyTime], [t('商戶回應'), r.resResult]],
  detailBlock: (r) => (r.notifyUrl ? t('通知網址\n') + r.notifyUrl : ''),
  actions: [{ label: t('重新發送'), access: 'ENT_MCH_NOTIFY_RESEND', show: (r) => r.state === 3, run: (r) => mchNotifyResend(r.notifyId).then(() => t('已重新排入通知')) }],
}

// ---------------- 錢包與提現 ----------------
const walletAccounts = {
  url: API_URL_WALLET + '/accounts', key: 'accountId', params: { pageSize: -1 }, paged: false,
  // 「待覆核調帳」改走 /adjusts，列與操作依 _adjust 判斷
  load: (p) => (p._view === 'adjust'
    ? req.list(API_URL_WALLET + '/adjusts', { state: 0, pageSize: -1 }).then((res) => (res.records || []).map((a) => ({ ...a, _adjust: true })))
    : req.list(API_URL_WALLET + '/accounts', { ...p, _view: undefined })),
  search: { local: (r) => `${r.ownerId} ${r.ownerName || ''}`, placeholder: t('搜尋帳戶名稱或編號') },
  filters: [{ label: t('全部'), params: {} }, { label: t('代理'), params: { ownerType: 'AGENT' } }, { label: t('商戶'), params: { ownerType: 'MCH' } }, { label: t('平台'), params: { ownerType: 'PLATFORM' } }, { label: t('待覆核調帳'), params: { _view: 'adjust' } }],
  summary: (v) => ['PLATFORM', 'AGENT', 'MCH'].filter((t) => v.extra.summary && v.extra.summary[t]).map((t) => ({ label: t('{0}可用', [OWNER_TYPE_NAMES[t]]), value: yuan2(v.extra.summary[t].balance), hint: t('凍結 {0}', [yuan2(v.extra.summary[t].frozen)]) }))
    .concat(v.extra.pending && v.extra.pending.length ? [{ label: t('待覆核調帳'), value: t('{0} 筆', [v.extra.pending.length]), hint: t('點「待覆核調帳」處理') }] : []),
  onReload: (c) => {
    req.list(API_URL_WALLET + '/summary', {}).then((s) => (c.vdata.extra.summary = s || {}))
    req.list(API_URL_WALLET + '/adjusts', { state: 0, pageSize: -1 }).then((s) => (c.vdata.extra.pending = s.records || []))
  },
  init: (c) => walletAccounts.onReload(c),
  row: {
    title: (r) => (r._adjust ? t('調帳：{0}', [owner(r)]) : owner(r)),
    sub: (r) => (r._adjust ? [r.reason, t('申請人 {0}・{1}', [r.requesterName || '', r.createdAt])] : [r.payoutAccountNo ? t('收款 {0} ****{1}', [r.payoutBankName || '', String(r.payoutAccountNo).slice(-4)]) : t('尚未設定收款帳戶'), t('累計入帳 {0}', [yuan2(r.totalIn)])]),
    amount: (r) => (r._adjust ? signed(r.amount) : yuan2(r.balance)),
    pill: (r) => (r._adjust ? [t('待覆核'), 'warn'] : Number(r.frozen) > 0 ? [t('凍結 {0}', [ntd(r.frozen)]), 'warn'] : null),
  },
  detail: (r) => r._adjust ? [[t('帳戶'), owner(r)], [t('金額'), signed(r.amount)], [t('原因'), r.reason], [t('申請人'), r.requesterName], [t('申請時間'), r.createdAt]] : [[t('帳戶'), owner(r)], [t('可用'), yuan2(r.balance)], [t('凍結'), yuan2(r.frozen)], [t('累計入帳'), yuan2(r.totalIn)], [t('收款帳戶'), r.payoutAccountNo ? `${r.payoutBankName || ''}（${r.payoutBankCode || ''}）${r.payoutAccountNo} ${r.payoutAccountName || ''}` : t('尚未設定')]],
  actions: [
    { label: t('核准入帳'), access: 'ENT_WALLET_ADJUST_REVIEW', show: (r) => r._adjust, confirm: t('核准後立即入帳並留下流水（申請人不能核准自己的申請）'), run: (r) => req.add(`${API_URL_WALLET}/adjusts/${r.reqId}/approve`, {}).then(() => t('已核准')) },
    { label: t('駁回'), cls: 'danger', access: 'ENT_WALLET_ADJUST_REVIEW', show: (r) => r._adjust, confirm: t('駁回這筆調帳？'), run: (r) => req.add(`${API_URL_WALLET}/adjusts/${r.reqId}/reject`, {}).then(() => t('已駁回')) },
    {
      label: t('人工調帳'), access: 'ENT_WALLET_ADJUST', show: (r) => !r._adjust, formTitle: (r) => t('人工調帳：') + owner(r), submitText: t('送出覆核'),
      note: t('送出後需要另一位管理者在這頁的「待覆核調帳」核准才會入帳。'),
      form: [{ key: 'amount', label: t('金額（元，正數為加、負數為減）'), type: 'number', required: true }, { key: 'reason', label: t('原因'), type: 'textarea', required: true }],
      submit: (v, r) => req.add(API_URL_WALLET + '/adjusts', { accountId: r.accountId, amount: String(v.amount), reason: v.reason }).then(() => t('已送出覆核')),
    },
  ],
}
const walletLedger = {
  url: API_URL_WALLET + '/ledger', key: 'ledgerId', dates: 'date', pageSize: 30,
  search: { param: 'ownerId', placeholder: t('帳戶編號（代理號或商戶號）') },
  filters: [{ label: t('全部'), params: {} }, { label: t('代理'), params: { ownerType: 'AGENT' } }, { label: t('商戶'), params: { ownerType: 'MCH' } }, { label: t('平台'), params: { ownerType: 'PLATFORM' } }],
  countText: (n, r) => t('{0}・{1} 筆', [r, n]), empty: t('這段期間沒有異動'),
  row: { title: (r) => BIZ_TYPE_NAMES[r.bizType] || r.bizType, sub: (r) => [owner(r), `${r.createdAt}${r.remark ? '・' + r.remark : ''}`], amount: (r) => signed(r.amount), amountColor: (r) => (Number(r.amount) > 0 ? 'var(--jp-ok)' : '') },
  detail: (r) => [[t('類型'), BIZ_TYPE_NAMES[r.bizType] || r.bizType], [t('帳戶'), owner(r)], [t('單號'), r.bizId], [t('可用變動'), signed(r.amount)], [t('凍結變動'), signed(r.frozenChange)], [t('異動後可用'), yuan2(r.balanceAfter)], [t('說明'), r.remark], [t('操作者'), r.operatorName], [t('時間'), r.createdAt]],
}
const WSTATE_CLASS = { 0: 'warn', 1: 'ok', 2: 'bad', 3: 'mute' }
const withdraws = {
  url: API_URL_WITHDRAWS, key: 'withdrawId',
  filters: [{ label: t('待撥款'), params: { state: 0 } }, { label: t('已撥款'), params: { state: 1 } }, { label: t('已駁回'), params: { state: 2 } }, { label: t('全部'), params: {} }],
  countText: (n) => t('{0} 筆', [n]), empty: t('沒有提現單'),
  row: { title: owner, sub: (r) => [`${r.bankName || ''} ${r.accountNo || ''} ${r.accountName || ''}`, `${r.createdAt}${r.riskFlags ? t('・風控：') + r.riskFlags.split(',').filter(Boolean).map((f) => RISK_FLAG_NAMES[f] || f).join('、') : ''}`], amount: (r) => yuan2(r.actualAmount ?? r.amount), pill: (r) => [(WITHDRAW_STATES[r.state] || {}).text, WSTATE_CLASS[r.state]] },
  detail: (r) => [[t('提現單號'), r.withdrawId], [t('申請人'), owner(r)], [t('申請金額'), yuan2(r.amount)], [t('手續費'), r.fee != null ? yuan2(r.fee) : null], [t('實付'), yuan2(r.actualAmount)], [t('收款帳戶'), `${r.bankName || ''} ${r.accountNo || ''}`], [t('戶名'), r.accountName], [t('風控'), (r.riskFlags || '').split(',').filter(Boolean).map((f) => RISK_FLAG_NAMES[f] || f).join('、')], [t('團長同意'), r.agentApproveBy], [t('狀態'), (WITHDRAW_STATES[r.state] || {}).text], [t('撥款憑證'), r.paidRef], [t('說明'), r.reviewRemark], [t('申請時間'), r.createdAt]],
  actions: [
    { label: t('標記已撥款'), access: 'ENT_WALLET_WITHDRAW_REVIEW', show: (r) => r.state === 0, form: [{ key: 'paidRef', label: t('撥款憑證（轉帳序號）'), required: true }, { key: 'remark', label: t('備註') }], submit: (v, r) => req.add(`${API_URL_WITHDRAWS}/${r.withdrawId}/paid`, v).then(() => t('已標記撥款')) },
    { label: t('駁回'), cls: 'danger', access: 'ENT_WALLET_WITHDRAW_REVIEW', show: (r) => r.state === 0, form: [{ key: 'remark', label: t('駁回原因'), required: true, type: 'textarea' }], submit: (v, r) => req.add(`${API_URL_WITHDRAWS}/${r.withdrawId}/reject`, { remark: v.remark }).then(() => t('已駁回並解凍')) },
  ],
}
const BL_TYPES = { BANK_ACCOUNT: t('收款帳號'), ACCOUNT_NAME: t('戶名'), PHONE: t('電話') }
const riskBlacklist = {
  url: API_URL_RISK_BLACKLIST, key: 'id',
  countText: (n) => t('共 {0} 筆', [n]), empty: t('黑名單是空的'),
  row: { title: (r) => r.listValue, sub: (r) => [`${BL_TYPES[r.listType] || r.listType}・${r.scope === 'GLOBAL' ? t('全站') : t('代理 ') + r.scope}`, `${r.remark ? r.remark + '・' : ''}${r.createdBy || ''} ${r.createdAt || ''}`] },
  detail: (r) => [[t('類型'), BL_TYPES[r.listType]], [t('內容'), r.listValue], [t('範圍'), r.scope === 'GLOBAL' ? t('全站') : t('代理 ') + r.scope], [t('備註'), r.remark], [t('建立者'), r.createdBy], [t('建立時間'), r.createdAt]],
  add: {
    label: t('新增黑名單'), access: 'ENT_RISK_BLACKLIST_EDIT',
    form: [
      { key: 'listType', label: t('類型'), type: 'seg', value: 'BANK_ACCOUNT', options: Object.entries(BL_TYPES).map(([value, label]) => ({ value, label })) },
      { key: 'listValue', label: t('內容'), required: true },
      { key: 'scope', label: t('範圍'), type: 'select', value: 'GLOBAL', options: [{ value: 'GLOBAL', label: t('全站') }] },
      { key: 'remark', label: t('備註') },
    ],
    submit: (v) => req.add(API_URL_RISK_BLACKLIST, v).then(() => t('已新增')),
  },
  actions: [{ label: t('移除'), cls: 'danger', access: 'ENT_RISK_BLACKLIST_EDIT', confirm: (r) => t('移除黑名單「{0}」？', [r.listValue]), run: (r) => req.delById(API_URL_RISK_BLACKLIST, r.id).then(() => t('已移除')) }],
  init: () => agentOptions(1)().then((res) => {
    riskBlacklist.add.form[2].options = [{ value: 'GLOBAL', label: t('全站') }].concat((res.records || []).map((a) => ({ value: a.agentNo, label: t('團長 {0}', [a.agentName]) }))) as any
  }),
}

// ---------------- 支付設定 ----------------
const payWays = {
  url: API_URL_PAYWAYS_LIST, key: 'wayCode',
  search: { param: 'wayName', placeholder: t('搜尋支付方式名稱') },
  editors: { info: defineAsyncComponent(() => import('@/views/payconfig/payWay/AddOrEdit.vue')) },
  add: { label: t('新增支付方式'), access: 'ENT_PC_WAY_ADD', run: (_, c) => (c.editors.info.show(), false) },
  row: { title: (r) => r.wayName, sub: (r) => [r.wayCode] },
  detail: (r) => [[t('代碼'), r.wayCode], [t('名稱'), r.wayName], [t('建立時間'), r.createdAt]],
  actions: [
    { label: t('修改'), access: 'ENT_PC_WAY_EDIT', run: (r, c) => (c.editors.info.show(r.wayCode), false) },
    { label: t('刪除'), cls: 'danger', access: 'ENT_PC_WAY_DEL', confirm: (r) => t('刪除支付方式「{0}」？', [r.wayName]), run: (r) => req.delById(API_URL_PAYWAYS_LIST, r.wayCode).then(() => t('已刪除')) },
  ],
}
const routeForm = (r?) => [
  { key: 'mchNo', label: t('適用商戶號（留空 = 全部商戶）'), value: () => (r && r.mchNo) || '' },
  { key: 'aliasWayCode', label: t('別名代碼'), required: true, value: () => (r && r.aliasWayCode) || 'IBON', extra: t('商戶下單時用的代碼，例如 IBON') },
  { key: 'targetWayCode', label: t('實際支付方式'), type: 'select', required: true, value: () => r && r.targetWayCode, options: (c) => c.vdata.extra.ways || [] },
  { key: 'minYuan', label: t('最低金額（元）'), type: 'number', precision: 0, min: 0, value: () => (r ? r.minAmount / 100 : 0) },
  { key: 'maxYuan', label: t('最高金額（元，0 = 不限）'), type: 'number', precision: 0, min: 0, value: () => (r ? r.maxAmount / 100 : 0) },
  { key: 'weight', label: t('權重（1～9）'), type: 'number', precision: 0, min: 1, max: 9, value: () => (r ? r.weight : 1) },
  { key: 'timeStart', label: t('開始時段（HH:mm，留空 = 全天）'), value: () => (r && r.timeStart) || '' },
  { key: 'timeEnd', label: t('結束時段'), value: () => (r && r.timeEnd) || '' },
  { key: 'state', label: t('狀態'), type: 'seg', value: () => (r ? r.state : 1), options: [{ value: 1, label: t('啟用') }, { value: 0, label: t('停用') }] },
  { key: 'remark', label: t('備註'), value: () => (r && r.remark) || '' },
]
const saveRoute = (v, r) => req.add(API_URL_WAY_ROUTES, { ...(r ? { routeId: r.routeId } : {}), mchNo: v.mchNo || '', aliasWayCode: v.aliasWayCode, targetWayCode: v.targetWayCode, minAmount: Math.round((v.minYuan || 0) * 100), maxAmount: Math.round((v.maxYuan || 0) * 100), weight: v.weight || 1, timeStart: v.timeStart || '', timeEnd: v.timeEnd || '', state: v.state, remark: v.remark }).then(() => t('已儲存'))
const wayRoutes = {
  url: API_URL_WAY_ROUTES, key: 'routeId', params: { pageSize: -1 }, paged: false,
  note: t('商戶用別名（例如 IBON）下單時，依金額、時段與權重分配到實際的支付方式（ADR-0011）。'),
  row: { title: (r) => `${r.aliasWayCode} → ${r.targetWayCode}`, sub: (r) => [t('{0}・權重 {1}', [r.mchNo ? t('商戶 ') + r.mchNo : t('全部商戶'), r.weight]), t('{0}～{1} 元・{2}', [r.minAmount / 100, r.maxAmount ? r.maxAmount / 100 : t('不限'), r.timeStart ? `${r.timeStart}～${r.timeEnd}` : t('全天')])], pill: (r) => onOff(r.state) },
  detail: (r) => [[t('別名'), r.aliasWayCode], [t('實際支付方式'), r.targetWayCode], [t('商戶'), r.mchNo || t('全部')], [t('金額'), t('{0}～{1} 元', [r.minAmount / 100, r.maxAmount ? r.maxAmount / 100 : t('不限')])], [t('時段'), r.timeStart ? `${r.timeStart}～${r.timeEnd}` : t('全天')], [t('權重'), r.weight], [t('狀態'), onOff(r.state)[0]], [t('備註'), r.remark]],
  add: { label: t('新增規則'), access: 'ENT_WAY_ROUTE_EDIT', form: () => routeForm(), submit: (v) => saveRoute(v, null) },
  actions: [
    { label: t('修改'), access: 'ENT_WAY_ROUTE_EDIT', form: (r) => routeForm(r), submit: (v, r) => saveRoute(v, r) },
    { label: t('刪除'), cls: 'danger', access: 'ENT_WAY_ROUTE_EDIT', confirm: (r) => t('刪除規則 {0} → {1}？', [r.aliasWayCode, r.targetWayCode]), run: (r) => req.delById(API_URL_WAY_ROUTES, r.routeId).then(() => t('已刪除')) },
  ],
  init: (c) => req.list(API_URL_PAYWAYS_LIST, { pageSize: -1 }).then((res) => (c.vdata.extra.ways = (res.records || []).map((w) => ({ value: w.wayCode, label: `${w.wayName}（${w.wayCode}）` })))),
}

// ---------------- 系統管理 ----------------
const sysLogs = {
  url: API_URL_SYS_LOG, key: 'sysLogId', dates: 'created',
  search: { param: 'userName', placeholder: t('搜尋操作人') },
  filters: [{ label: t('全部'), params: {} }, { label: t('營運平台'), params: { sysType: 'MGR' } }, { label: t('商戶平台'), params: { sysType: 'MCH' } }],
  countText: (n, r) => t('{0}・{1} 筆', [r, n]), empty: t('這段期間沒有操作紀錄'),
  row: { title: (r) => r.methodRemark, sub: (r) => [`${r.userName || ''}・${r.userIp}`, `${r.sysType === 'MCH' ? t('商戶平台') : t('營運平台')}・${r.createdAt}`] },
  fetchDetail: (r) => req.getById(API_URL_SYS_LOG, r.sysLogId),
  detail: (r) => [[t('操作'), r.methodRemark], [t('操作人'), `${r.userName || ''}（${r.userId || ''}）`], ['IP', r.userIp], [t('系統'), r.sysType === 'MCH' ? t('商戶平台') : t('營運平台')], [t('網址'), r.reqUrl], [t('時間'), r.createdAt]],
  detailBlock: (r) => [r.optReqParam ? t('請求參數（密碼與金鑰已遮罩）\n') + r.optReqParam : '', r.optResInfo ? t('回應\n') + String(r.optResInfo).slice(0, 2000) : ''].filter(Boolean).join('\n\n'),
}
const uatAllowlist = {
  url: API_URL_UAT_EDGE_ALLOWLIST, key: 'ip', paged: false,
  load: () => req.list(API_URL_UAT_EDGE_ALLOWLIST, {}).then((res) => ((uatAllowlist as any).status = res && res.applyStatus, (res && res.records) || [])),
  note: t('只有名單內的 IP 能呼叫 UAT 商戶 API。儲存後約 1 分鐘自動套用到外網入口。'),
  row: { title: (r) => r.ip, sub: (r) => [r.remark || t('（無備註）')], pill: (r) => (String(r.remark || '').includes('系統保留') ? [t('系統保留'), 'mute'] : null) },
  detail: (r) => [['IP / CIDR', r.ip], [t('備註'), r.remark]],
  add: { label: t('新增 IP'), access: 'ENT_UAT_EDGE_ALLOWLIST', form: [{ key: 'ip', label: t('IP 或 CIDR'), required: true, placeholder: t('203.0.113.10 或 203.0.113.0/24') }, { key: 'remark', label: t('備註') }], submit: (v) => req.add(API_URL_UAT_EDGE_ALLOWLIST + '/add', v).then(() => t('已新增，約 1 分鐘後生效')) },
  actions: [{ label: t('移除'), cls: 'danger', access: 'ENT_UAT_EDGE_ALLOWLIST', show: (r) => !String(r.remark || '').includes('系統保留'), confirm: (r) => t('移除 {0}？', [r.ip]), run: (r) => req.add(API_URL_UAT_EDGE_ALLOWLIST + '/remove', { ip: r.ip }).then(() => t('已移除，約 1 分鐘後生效')) }],
}

export const GOD_PAGES: Record<string, any> = {
  ENT_UR_USER: sysUsers,
  ENT_UR_ROLE: roles,
  ENT_FEE_TEMPLATE: feeTemplates,
  ENT_REFUND_ORDER: refunds,
  ENT_TRANSFER_ORDER: transfers,
  ENT_MCH_NOTIFY: notifies,
  ENT_WALLET_ACCOUNT: walletAccounts,
  ENT_WALLET_LEDGER: walletLedger,
  ENT_WALLET_WITHDRAW: withdraws,
  ENT_RISK_BLACKLIST: riskBlacklist,
  ENT_PC_WAY: payWays,
  ENT_WAY_ROUTE: wayRoutes,
  ENT_SYS_LOG: sysLogs,
  ENT_UAT_EDGE_ALLOWLIST: uatAllowlist,
}
export { LAYERS, API_URL_MCH_LIST }
