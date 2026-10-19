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

const yuan2 = (fen) => ntd(fen, 2)
const signed = (fen) => (Number(fen) > 0 ? '+' : '') + ntd(fen, 2)
const owner = (r) => `${OWNER_TYPE_NAMES[r.ownerType] || r.ownerType} ${r.ownerName || r.ownerId || ''}`.trim()
const onOff = (s) => (s === 1 ? ['啟用', 'ok'] : ['停用', 'mute'])
const realPhone = (t) => (String(t || '').startsWith('-') ? '' : t)
const agentOptions = (level?) => () => req.list(API_URL_AGENT_INFO, { pageSize: -1, ...(level ? { agentLevel: level } : {}) })

// ---------------- 用戶角色 ----------------
const sysUsers = {
  url: API_URL_SYS_USER_LIST, key: 'sysUserId',
  params: { sysType: 'MGR' },
  search: { param: 'realname', placeholder: '搜尋姓名' },
  filters: [{ label: '全部', params: {} }, { label: '啟用中', params: { state: 1 } }, { label: '已停用', params: { state: 0 } }],
  countText: (n) => `共 ${n} 位操作員`,
  editors: { info: defineAsyncComponent(() => import('@/views/sysuser/AddOrEdit.vue')), role: defineAsyncComponent(() => import('@/views/sysuser/RoleDist.vue')) },
  add: { label: '新增操作員', access: 'ENT_UR_USER_ADD', run: (_, c) => (c.editors.info.show(), false) },
  row: {
    title: (r) => r.realname || r.loginUsername,
    sub: (r) => [`帳號 ${r.loginUsername || ''}${realPhone(r.telphone) ? '・' + r.telphone : ''}`, r.belongInfoId && r.belongInfoId !== '0' ? `代理 ${r.belongInfoId}` : ''],
    pill: (r) => (r.isAdmin === 1 ? ['上帝', 'warn'] : onOff(r.state)),
  },
  detail: (r) => [['用戶ID', r.sysUserId], ['登入帳號', r.loginUsername], ['手機號', realPhone(r.telphone)], ['身分', r.isAdmin === 1 ? '上帝（全部權限）' : '一般操作員'], ['狀態', onOff(r.state)[0]], ['建立時間', r.createdAt]],
  actions: [
    { label: '修改', access: 'ENT_UR_USER_EDIT', run: (r, c) => (c.editors.info.show(r.sysUserId), false) },
    { label: '分配角色', access: 'ENT_UR_USER_UPD_ROLE', show: (r) => r.isAdmin !== 1, run: (r, c) => (c.editors.role.show(r.sysUserId), false) },
    { label: '停用', cls: 'danger', access: 'ENT_UR_USER_EDIT', show: (r) => r.state === 1 && r.isAdmin !== 1, confirm: (r) => `停用「${r.realname}」？停用後立即登出且不能再登入`, run: (r) => req.updateById(API_URL_SYS_USER_LIST, r.sysUserId, { state: 0 }).then(() => '已停用') },
    { label: '啟用', access: 'ENT_UR_USER_EDIT', show: (r) => r.state !== 1, run: (r) => req.updateById(API_URL_SYS_USER_LIST, r.sysUserId, { state: 1 }).then(() => '已啟用') },
    { label: '刪除', cls: 'danger', access: 'ENT_UR_USER_DELETE', show: (r) => r.isAdmin !== 1, confirm: (r) => `刪除操作員「${r.realname}」？`, run: (r) => req.delById(API_URL_SYS_USER_LIST, r.sysUserId).then(() => '已刪除') },
  ],
}

const roles = {
  url: API_URL_ROLE_LIST, key: 'roleId',
  search: { param: 'roleName', placeholder: '搜尋角色名稱' },
  countText: (n) => `共 ${n} 個角色`,
  editors: { info: defineAsyncComponent(() => import('@/views/role/AddOrEdit.vue')) },
  add: { label: '新增角色', access: 'ENT_UR_ROLE_ADD', run: (_, c) => (c.editors.info.show(), false) },
  row: { title: (r) => r.roleName, sub: (r) => [r.roleId] },
  detail: (r) => [['角色ID', r.roleId], ['名稱', r.roleName], ['建立時間', r.createdAt]],
  actions: [
    { label: '修改名稱與權限', access: 'ENT_UR_ROLE_EDIT', run: (r, c) => (c.editors.info.show(r.roleId), false) },
    { label: '刪除', cls: 'danger', access: 'ENT_UR_ROLE_DEL', confirm: (r) => `刪除角色「${r.roleName}」？已分配此角色的操作員會失去對應權限`, run: (r) => req.delById(API_URL_ROLE_LIST, r.roleId).then(() => '已刪除') },
  ],
}

// ---------------- 費率範本 ----------------
const LAYERS = { PLATFORM: '平臺費', CHANNEL: '渠道費', SR_AGENT: '團長費', AGENT: '隊長費' }
const feeTemplates = {
  url: API_URL_FEE_TEMPLATES, key: 'templateId', params: { pageSize: -1 }, paged: false,
  note: '範本是一組「支付方式 × 費率層」的設定，可一次套用到多位代理或商戶。新增與修改費率內容請用電腦版。',
  row: { title: (r) => r.templateName, sub: (r) => [r.remark, `最後修改 ${r.updatedBy || '—'}・${r.updatedAt || ''}`] },
  fetchDetail: (r) => req.getById(API_URL_FEE_TEMPLATES, r.templateId),
  detail: (r) => [['範本ID', r.templateId], ['備註', r.remark], ...((r.items || []) as any[]).map((it) => [`${it.wayCode}・${LAYERS[it.layer] || it.layer}`, `${(Number(it.rate) * 100).toFixed(4).replace(/\.?0+$/, '')}% + ${ntd(it.fixedAmount, 2)}`])],
  actions: [
    {
      label: '套用', access: 'ENT_FEE_TEMPLATE', formTitle: (r) => '套用：' + r.templateName,
      form: [
        { key: 'targetType', label: '套用到', type: 'seg', value: 'AGENT', options: [{ value: 'AGENT', label: '代理' }, { value: 'MCH', label: '商戶' }] },
        { key: 'agents', label: '代理（團長套團長費、隊長套隊長費）', type: 'select', multiple: true, value: () => [], options: [] },
        { key: 'mchs', label: '商戶號（可多個，以逗號分隔）', placeholder: 'M1739200101,M1739200102' },
      ],
      note: '選「代理」時填第一欄，選「商戶」時填第二欄。',
      submit: (v, r) => {
        const ids = v.targetType === 'AGENT' ? v.agents || [] : String(v.mchs || '').split(/[,，\s]+/).filter(Boolean)
        if (!ids.length) return false
        return req.add(`${API_URL_FEE_TEMPLATES}/${r.templateId}/apply`, { targetType: v.targetType, targetIds: JSON.stringify(ids) }).then((res) => `已套用 ${res && res.applied != null ? res.applied : ''} 筆`)
      },
    },
    { label: '刪除', cls: 'danger', access: 'ENT_FEE_TEMPLATE', confirm: (r) => `刪除範本「${r.templateName}」？`, run: (r) => req.delById(API_URL_FEE_TEMPLATES, r.templateId).then(() => '已刪除') },
  ],
  init: (c) => agentOptions()().then((res) => {
    const opts = (res.records || []).map((a) => ({ value: a.agentNo, label: `${a.agentLevel === 1 ? '團長' : '隊長'} ${a.agentName}（${a.agentNo}）` }))
    feeTemplates.actions[0].form[1].options = opts as any
  }),
}

// ---------------- 訂單中心 ----------------
const REFUND_STATES = { 0: ['訂單生成', 'mute'], 1: ['退款中', 'warn'], 2: ['退款成功', 'ok'], 3: ['退款失敗', 'bad'], 4: ['已關閉', 'mute'] }
const refunds = {
  url: API_URL_REFUND_ORDER_LIST, key: 'refundOrderId', dates: 'created',
  search: { param: 'unionOrderId', placeholder: '退款／支付／商戶單號' },
  filters: [{ label: '全部', params: {} }, { label: '退款中', params: { state: 1 } }, { label: '成功', params: { state: 2 } }, { label: '失敗', params: { state: 3 } }],
  countText: (n, r) => `${r}・${n} 筆`, empty: '這段期間沒有退款',
  row: { title: (r) => r.mchName || r.mchNo, sub: (r) => [r.refundOrderId, `支付 ${ntd(r.payAmount)}・${r.createdAt}`], amount: (r) => '−' + ntd(r.refundAmount), pill: (r) => REFUND_STATES[r.state] },
  fetchDetail: (r) => req.getById(API_URL_REFUND_ORDER_LIST, r.refundOrderId),
  detail: (r) => [['退款單號', r.refundOrderId], ['支付單號', r.payOrderId], ['商戶退款單號', r.mchRefundNo], ['商戶', `${r.mchName || ''} ${r.mchNo}`], ['支付金額', ntd(r.payAmount)], ['退款金額', ntd(r.refundAmount)], ['原因', r.refundReason], ['狀態', (REFUND_STATES[r.state] || [])[0]], ['錯誤', r.errMsg], ['建立', r.createdAt], ['完成', r.successTime]],
}
const TRANSFER_STATES = { 0: ['訂單生成', 'mute'], 1: ['轉帳中', 'warn'], 2: ['轉帳成功', 'ok'], 3: ['轉帳失敗', 'bad'], 4: ['已關閉', 'mute'] }
const transfers = {
  url: API_URL_TRANSFER_ORDER_LIST, key: 'transferId', dates: 'created',
  search: { param: 'unionOrderId', placeholder: '轉帳／商戶單號' },
  countText: (n, r) => `${r}・${n} 筆`, empty: '這段期間沒有轉帳（目前上游渠道未提供代付）',
  row: { title: (r) => `${r.mchName || r.mchNo} → ${r.accountName || '—'}`, sub: (r) => [r.transferId, `${r.bankName || ''} ${r.accountNo || ''}・${r.createdAt}`], amount: (r) => '−' + ntd(r.amount), pill: (r) => TRANSFER_STATES[r.state] },
  detail: (r) => [['轉帳單號', r.transferId], ['商戶單號', r.mchOrderNo], ['商戶', `${r.mchName || ''} ${r.mchNo}`], ['金額', ntd(r.amount)], ['收款人', r.accountName], ['收款帳號', r.accountNo], ['銀行', r.bankName], ['備註', r.transferDesc], ['狀態', (TRANSFER_STATES[r.state] || [])[0]], ['錯誤', r.errMsg], ['建立', r.createdAt]],
}
const NOTIFY_STATES = { 1: ['通知中', 'warn'], 2: ['已成功', 'ok'], 3: ['失敗', 'bad'] }
const ORDER_TYPES = { 1: '支付', 2: '退款', 3: '轉帳' }
const notifies = {
  url: API_URL_MCH_NOTIFY_LIST, key: 'notifyId', dates: 'created',
  search: { param: 'orderId', placeholder: '訂單號' },
  filters: [{ label: '全部', params: {} }, { label: '失敗', params: { state: 3 } }, { label: '通知中', params: { state: 1 } }, { label: '成功', params: { state: 2 } }],
  countText: (n, r) => `${r}・${n} 筆`, empty: '這段期間沒有商戶通知',
  row: { title: (r) => r.orderId, sub: (r) => [`${ORDER_TYPES[r.orderType] || ''}・商戶 ${r.mchNo}・${r.mchOrderNo || ''}`, `已通知 ${r.notifyCount || 0} 次・${r.createdAt}`], pill: (r) => NOTIFY_STATES[r.state] },
  fetchDetail: (r) => req.getById(API_URL_MCH_NOTIFY_LIST, r.notifyId),
  detail: (r) => [['訂單', r.orderId], ['類型', ORDER_TYPES[r.orderType]], ['商戶', r.mchNo], ['商戶單號', r.mchOrderNo], ['狀態', (NOTIFY_STATES[r.state] || [])[0]], ['次數', `${r.notifyCount || 0}／${r.notifyCountLimit || 6}`], ['最後通知', r.lastNotifyTime], ['商戶回應', r.resResult]],
  detailBlock: (r) => (r.notifyUrl ? '通知網址\n' + r.notifyUrl : ''),
  actions: [{ label: '重新發送', access: 'ENT_MCH_NOTIFY_RESEND', show: (r) => r.state === 3, run: (r) => mchNotifyResend(r.notifyId).then(() => '已重新排入通知') }],
}

// ---------------- 錢包與提現 ----------------
const walletAccounts = {
  url: API_URL_WALLET + '/accounts', key: 'accountId', params: { pageSize: -1 }, paged: false,
  // 「待覆核調帳」改走 /adjusts，列與操作依 _adjust 判斷
  load: (p) => (p._view === 'adjust'
    ? req.list(API_URL_WALLET + '/adjusts', { state: 0, pageSize: -1 }).then((res) => (res.records || []).map((a) => ({ ...a, _adjust: true })))
    : req.list(API_URL_WALLET + '/accounts', { ...p, _view: undefined })),
  search: { local: (r) => `${r.ownerId} ${r.ownerName || ''}`, placeholder: '搜尋帳戶名稱或編號' },
  filters: [{ label: '全部', params: {} }, { label: '代理', params: { ownerType: 'AGENT' } }, { label: '商戶', params: { ownerType: 'MCH' } }, { label: '平台', params: { ownerType: 'PLATFORM' } }, { label: '待覆核調帳', params: { _view: 'adjust' } }],
  summary: (v) => ['PLATFORM', 'AGENT', 'MCH'].filter((t) => v.extra.summary && v.extra.summary[t]).map((t) => ({ label: `${OWNER_TYPE_NAMES[t]}可用`, value: yuan2(v.extra.summary[t].balance), hint: `凍結 ${yuan2(v.extra.summary[t].frozen)}` }))
    .concat(v.extra.pending && v.extra.pending.length ? [{ label: '待覆核調帳', value: v.extra.pending.length + ' 筆', hint: '點「待覆核調帳」處理' }] : []),
  onReload: (c) => {
    req.list(API_URL_WALLET + '/summary', {}).then((s) => (c.vdata.extra.summary = s || {}))
    req.list(API_URL_WALLET + '/adjusts', { state: 0, pageSize: -1 }).then((s) => (c.vdata.extra.pending = s.records || []))
  },
  init: (c) => walletAccounts.onReload(c),
  row: {
    title: (r) => (r._adjust ? `調帳：${owner(r)}` : owner(r)),
    sub: (r) => (r._adjust ? [r.reason, `申請人 ${r.requesterName || ''}・${r.createdAt}`] : [r.payoutAccountNo ? `收款 ${r.payoutBankName || ''} ****${String(r.payoutAccountNo).slice(-4)}` : '尚未設定收款帳戶', `累計入帳 ${yuan2(r.totalIn)}`]),
    amount: (r) => (r._adjust ? signed(r.amount) : yuan2(r.balance)),
    pill: (r) => (r._adjust ? ['待覆核', 'warn'] : Number(r.frozen) > 0 ? [`凍結 ${ntd(r.frozen)}`, 'warn'] : null),
  },
  detail: (r) => r._adjust ? [['帳戶', owner(r)], ['金額', signed(r.amount)], ['原因', r.reason], ['申請人', r.requesterName], ['申請時間', r.createdAt]] : [['帳戶', owner(r)], ['可用', yuan2(r.balance)], ['凍結', yuan2(r.frozen)], ['累計入帳', yuan2(r.totalIn)], ['收款帳戶', r.payoutAccountNo ? `${r.payoutBankName || ''}（${r.payoutBankCode || ''}）${r.payoutAccountNo} ${r.payoutAccountName || ''}` : '尚未設定']],
  actions: [
    { label: '核准入帳', access: 'ENT_WALLET_ADJUST_REVIEW', show: (r) => r._adjust, confirm: '核准後立即入帳並留下流水（申請人不能核准自己的申請）', run: (r) => req.add(`${API_URL_WALLET}/adjusts/${r.reqId}/approve`, {}).then(() => '已核准') },
    { label: '駁回', cls: 'danger', access: 'ENT_WALLET_ADJUST_REVIEW', show: (r) => r._adjust, confirm: '駁回這筆調帳？', run: (r) => req.add(`${API_URL_WALLET}/adjusts/${r.reqId}/reject`, {}).then(() => '已駁回') },
    {
      label: '人工調帳', access: 'ENT_WALLET_ADJUST', show: (r) => !r._adjust, formTitle: (r) => '人工調帳：' + owner(r), submitText: '送出覆核',
      note: '送出後需要另一位管理者在這頁的「待覆核調帳」核准才會入帳。',
      form: [{ key: 'amount', label: '金額（元，正數為加、負數為減）', type: 'number', required: true }, { key: 'reason', label: '原因', type: 'textarea', required: true }],
      submit: (v, r) => req.add(API_URL_WALLET + '/adjusts', { accountId: r.accountId, amount: String(v.amount), reason: v.reason }).then(() => '已送出覆核'),
    },
  ],
}
const walletLedger = {
  url: API_URL_WALLET + '/ledger', key: 'ledgerId', dates: 'date', pageSize: 30,
  search: { param: 'ownerId', placeholder: '帳戶編號（代理號或商戶號）' },
  filters: [{ label: '全部', params: {} }, { label: '代理', params: { ownerType: 'AGENT' } }, { label: '商戶', params: { ownerType: 'MCH' } }, { label: '平台', params: { ownerType: 'PLATFORM' } }],
  countText: (n, r) => `${r}・${n} 筆`, empty: '這段期間沒有異動',
  row: { title: (r) => BIZ_TYPE_NAMES[r.bizType] || r.bizType, sub: (r) => [owner(r), `${r.createdAt}${r.remark ? '・' + r.remark : ''}`], amount: (r) => signed(r.amount), amountColor: (r) => (Number(r.amount) > 0 ? 'var(--jp-ok)' : '') },
  detail: (r) => [['類型', BIZ_TYPE_NAMES[r.bizType] || r.bizType], ['帳戶', owner(r)], ['單號', r.bizId], ['可用變動', signed(r.amount)], ['凍結變動', signed(r.frozenChange)], ['異動後可用', yuan2(r.balanceAfter)], ['說明', r.remark], ['操作者', r.operatorName], ['時間', r.createdAt]],
}
const WSTATE_CLASS = { 0: 'warn', 1: 'ok', 2: 'bad', 3: 'mute' }
const withdraws = {
  url: API_URL_WITHDRAWS, key: 'withdrawId',
  filters: [{ label: '待撥款', params: { state: 0 } }, { label: '已撥款', params: { state: 1 } }, { label: '已駁回', params: { state: 2 } }, { label: '全部', params: {} }],
  countText: (n) => `${n} 筆`, empty: '沒有提現單',
  row: { title: owner, sub: (r) => [`${r.bankName || ''} ${r.accountNo || ''} ${r.accountName || ''}`, `${r.createdAt}${r.riskFlags ? '・風控：' + r.riskFlags.split(',').filter(Boolean).map((f) => RISK_FLAG_NAMES[f] || f).join('、') : ''}`], amount: (r) => yuan2(r.actualAmount ?? r.amount), pill: (r) => [(WITHDRAW_STATES[r.state] || {}).text, WSTATE_CLASS[r.state]] },
  detail: (r) => [['提現單號', r.withdrawId], ['申請人', owner(r)], ['申請金額', yuan2(r.amount)], ['手續費', r.fee != null ? yuan2(r.fee) : null], ['實付', yuan2(r.actualAmount)], ['收款帳戶', `${r.bankName || ''} ${r.accountNo || ''}`], ['戶名', r.accountName], ['風控', (r.riskFlags || '').split(',').filter(Boolean).map((f) => RISK_FLAG_NAMES[f] || f).join('、')], ['團長同意', r.agentApproveBy], ['狀態', (WITHDRAW_STATES[r.state] || {}).text], ['撥款憑證', r.paidRef], ['說明', r.reviewRemark], ['申請時間', r.createdAt]],
  actions: [
    { label: '標記已撥款', access: 'ENT_WALLET_WITHDRAW_REVIEW', show: (r) => r.state === 0, form: [{ key: 'paidRef', label: '撥款憑證（轉帳序號）', required: true }, { key: 'remark', label: '備註' }], submit: (v, r) => req.add(`${API_URL_WITHDRAWS}/${r.withdrawId}/paid`, v).then(() => '已標記撥款') },
    { label: '駁回', cls: 'danger', access: 'ENT_WALLET_WITHDRAW_REVIEW', show: (r) => r.state === 0, form: [{ key: 'remark', label: '駁回原因', required: true, type: 'textarea' }], submit: (v, r) => req.add(`${API_URL_WITHDRAWS}/${r.withdrawId}/reject`, { remark: v.remark }).then(() => '已駁回並解凍') },
  ],
}
const BL_TYPES = { BANK_ACCOUNT: '收款帳號', ACCOUNT_NAME: '戶名', PHONE: '電話' }
const riskBlacklist = {
  url: API_URL_RISK_BLACKLIST, key: 'id',
  countText: (n) => `共 ${n} 筆`, empty: '黑名單是空的',
  row: { title: (r) => r.listValue, sub: (r) => [`${BL_TYPES[r.listType] || r.listType}・${r.scope === 'GLOBAL' ? '全站' : '代理 ' + r.scope}`, `${r.remark ? r.remark + '・' : ''}${r.createdBy || ''} ${r.createdAt || ''}`] },
  detail: (r) => [['類型', BL_TYPES[r.listType]], ['內容', r.listValue], ['範圍', r.scope === 'GLOBAL' ? '全站' : '代理 ' + r.scope], ['備註', r.remark], ['建立者', r.createdBy], ['建立時間', r.createdAt]],
  add: {
    label: '新增黑名單', access: 'ENT_RISK_BLACKLIST_EDIT',
    form: [
      { key: 'listType', label: '類型', type: 'seg', value: 'BANK_ACCOUNT', options: Object.entries(BL_TYPES).map(([value, label]) => ({ value, label })) },
      { key: 'listValue', label: '內容', required: true },
      { key: 'scope', label: '範圍', type: 'select', value: 'GLOBAL', options: [{ value: 'GLOBAL', label: '全站' }] },
      { key: 'remark', label: '備註' },
    ],
    submit: (v) => req.add(API_URL_RISK_BLACKLIST, v).then(() => '已新增'),
  },
  actions: [{ label: '移除', cls: 'danger', access: 'ENT_RISK_BLACKLIST_EDIT', confirm: (r) => `移除黑名單「${r.listValue}」？`, run: (r) => req.delById(API_URL_RISK_BLACKLIST, r.id).then(() => '已移除') }],
  init: () => agentOptions(1)().then((res) => {
    riskBlacklist.add.form[2].options = [{ value: 'GLOBAL', label: '全站' }].concat((res.records || []).map((a) => ({ value: a.agentNo, label: `團長 ${a.agentName}` }))) as any
  }),
}

// ---------------- 支付設定 ----------------
const payWays = {
  url: API_URL_PAYWAYS_LIST, key: 'wayCode',
  search: { param: 'wayName', placeholder: '搜尋支付方式名稱' },
  editors: { info: defineAsyncComponent(() => import('@/views/payconfig/payWay/AddOrEdit.vue')) },
  add: { label: '新增支付方式', access: 'ENT_PC_WAY_ADD', run: (_, c) => (c.editors.info.show(), false) },
  row: { title: (r) => r.wayName, sub: (r) => [r.wayCode] },
  detail: (r) => [['代碼', r.wayCode], ['名稱', r.wayName], ['建立時間', r.createdAt]],
  actions: [
    { label: '修改', access: 'ENT_PC_WAY_EDIT', run: (r, c) => (c.editors.info.show(r.wayCode), false) },
    { label: '刪除', cls: 'danger', access: 'ENT_PC_WAY_DEL', confirm: (r) => `刪除支付方式「${r.wayName}」？`, run: (r) => req.delById(API_URL_PAYWAYS_LIST, r.wayCode).then(() => '已刪除') },
  ],
}
const routeForm = (r?) => [
  { key: 'mchNo', label: '適用商戶號（留空 = 全部商戶）', value: () => (r && r.mchNo) || '' },
  { key: 'aliasWayCode', label: '別名代碼', required: true, value: () => (r && r.aliasWayCode) || 'IBON', extra: '商戶下單時用的代碼，例如 IBON' },
  { key: 'targetWayCode', label: '實際支付方式', type: 'select', required: true, value: () => r && r.targetWayCode, options: (c) => c.vdata.extra.ways || [] },
  { key: 'minYuan', label: '最低金額（元）', type: 'number', precision: 0, min: 0, value: () => (r ? r.minAmount / 100 : 0) },
  { key: 'maxYuan', label: '最高金額（元，0 = 不限）', type: 'number', precision: 0, min: 0, value: () => (r ? r.maxAmount / 100 : 0) },
  { key: 'weight', label: '權重（1～9）', type: 'number', precision: 0, min: 1, max: 9, value: () => (r ? r.weight : 1) },
  { key: 'timeStart', label: '開始時段（HH:mm，留空 = 全天）', value: () => (r && r.timeStart) || '' },
  { key: 'timeEnd', label: '結束時段', value: () => (r && r.timeEnd) || '' },
  { key: 'state', label: '狀態', type: 'seg', value: () => (r ? r.state : 1), options: [{ value: 1, label: '啟用' }, { value: 0, label: '停用' }] },
  { key: 'remark', label: '備註', value: () => (r && r.remark) || '' },
]
const saveRoute = (v, r) => req.add(API_URL_WAY_ROUTES, { ...(r ? { routeId: r.routeId } : {}), mchNo: v.mchNo || '', aliasWayCode: v.aliasWayCode, targetWayCode: v.targetWayCode, minAmount: Math.round((v.minYuan || 0) * 100), maxAmount: Math.round((v.maxYuan || 0) * 100), weight: v.weight || 1, timeStart: v.timeStart || '', timeEnd: v.timeEnd || '', state: v.state, remark: v.remark }).then(() => '已儲存')
const wayRoutes = {
  url: API_URL_WAY_ROUTES, key: 'routeId', params: { pageSize: -1 }, paged: false,
  note: '商戶用別名（例如 IBON）下單時，依金額、時段與權重分配到實際的支付方式（ADR-0011）。',
  row: { title: (r) => `${r.aliasWayCode} → ${r.targetWayCode}`, sub: (r) => [`${r.mchNo ? '商戶 ' + r.mchNo : '全部商戶'}・權重 ${r.weight}`, `${r.minAmount / 100}～${r.maxAmount ? r.maxAmount / 100 : '不限'} 元・${r.timeStart ? `${r.timeStart}～${r.timeEnd}` : '全天'}`], pill: (r) => onOff(r.state) },
  detail: (r) => [['別名', r.aliasWayCode], ['實際支付方式', r.targetWayCode], ['商戶', r.mchNo || '全部'], ['金額', `${r.minAmount / 100}～${r.maxAmount ? r.maxAmount / 100 : '不限'} 元`], ['時段', r.timeStart ? `${r.timeStart}～${r.timeEnd}` : '全天'], ['權重', r.weight], ['狀態', onOff(r.state)[0]], ['備註', r.remark]],
  add: { label: '新增規則', access: 'ENT_WAY_ROUTE_EDIT', form: () => routeForm(), submit: (v) => saveRoute(v, null) },
  actions: [
    { label: '修改', access: 'ENT_WAY_ROUTE_EDIT', form: (r) => routeForm(r), submit: (v, r) => saveRoute(v, r) },
    { label: '刪除', cls: 'danger', access: 'ENT_WAY_ROUTE_EDIT', confirm: (r) => `刪除規則 ${r.aliasWayCode} → ${r.targetWayCode}？`, run: (r) => req.delById(API_URL_WAY_ROUTES, r.routeId).then(() => '已刪除') },
  ],
  init: (c) => req.list(API_URL_PAYWAYS_LIST, { pageSize: -1 }).then((res) => (c.vdata.extra.ways = (res.records || []).map((w) => ({ value: w.wayCode, label: `${w.wayName}（${w.wayCode}）` })))),
}

// ---------------- 系統管理 ----------------
const sysLogs = {
  url: API_URL_SYS_LOG, key: 'sysLogId', dates: 'created',
  search: { param: 'userName', placeholder: '搜尋操作人' },
  filters: [{ label: '全部', params: {} }, { label: '營運平台', params: { sysType: 'MGR' } }, { label: '商戶平台', params: { sysType: 'MCH' } }],
  countText: (n, r) => `${r}・${n} 筆`, empty: '這段期間沒有操作紀錄',
  row: { title: (r) => r.methodRemark, sub: (r) => [`${r.userName || ''}・${r.userIp}`, `${r.sysType === 'MCH' ? '商戶平台' : '營運平台'}・${r.createdAt}`] },
  fetchDetail: (r) => req.getById(API_URL_SYS_LOG, r.sysLogId),
  detail: (r) => [['操作', r.methodRemark], ['操作人', `${r.userName || ''}（${r.userId || ''}）`], ['IP', r.userIp], ['系統', r.sysType === 'MCH' ? '商戶平台' : '營運平台'], ['網址', r.reqUrl], ['時間', r.createdAt]],
  detailBlock: (r) => [r.optReqParam ? '請求參數（密碼與金鑰已遮罩）\n' + r.optReqParam : '', r.optResInfo ? '回應\n' + String(r.optResInfo).slice(0, 2000) : ''].filter(Boolean).join('\n\n'),
}
const uatAllowlist = {
  url: API_URL_UAT_EDGE_ALLOWLIST, key: 'ip', paged: false,
  load: () => req.list(API_URL_UAT_EDGE_ALLOWLIST, {}).then((res) => ((uatAllowlist as any).status = res && res.applyStatus, (res && res.records) || [])),
  note: '只有名單內的 IP 能呼叫 UAT 商戶 API。儲存後約 1 分鐘自動套用到外網入口。',
  row: { title: (r) => r.ip, sub: (r) => [r.remark || '（無備註）'], pill: (r) => (String(r.remark || '').includes('系統保留') ? ['系統保留', 'mute'] : null) },
  detail: (r) => [['IP / CIDR', r.ip], ['備註', r.remark]],
  add: { label: '新增 IP', access: 'ENT_UAT_EDGE_ALLOWLIST', form: [{ key: 'ip', label: 'IP 或 CIDR', required: true, placeholder: '203.0.113.10 或 203.0.113.0/24' }, { key: 'remark', label: '備註' }], submit: (v) => req.add(API_URL_UAT_EDGE_ALLOWLIST + '/add', v).then(() => '已新增，約 1 分鐘後生效') },
  actions: [{ label: '移除', cls: 'danger', access: 'ENT_UAT_EDGE_ALLOWLIST', show: (r) => !String(r.remark || '').includes('系統保留'), confirm: (r) => `移除 ${r.ip}？`, run: (r) => req.add(API_URL_UAT_EDGE_ALLOWLIST + '/remove', { ip: r.ip }).then(() => '已移除，約 1 分鐘後生效') }],
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
