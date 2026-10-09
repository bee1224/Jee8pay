/**
 * 全局配置信息， 包含网站标题，  动态组件定义
 *
 * @author terrfly
 * @site https://www.jeepay.vip
 * @date 2021/5/8 07:18
 */

/** 应用配置项 **/
export default {
  APP_TITLE: '三把扇-營運平台', // 设置浏览器title
  ACCESS_TOKEN_NAME: 'iToken' // 设置请求token的名字， 用于请求header 和 localstorage中存在名称
}

/**
 * 与后端开发人员的路由名称及配置项
 * 组件名称 ：{ 默认跳转路径（如果后端配置则已动态配置为准）， 组件渲染 }
 * */
export const asyncRouteDefine = {

  'CurrentUserInfo': { defaultPath: '/current/userinfo', component: () => import('@/views/current/UserinfoPage.vue')  }, // 用户设置

  'MainPage': { defaultPath: '/main', component: () => import('@/views/dashboard/Analysis.vue')  },
  'SysUserPage': { defaultPath: '/users', component: () => import('@/views/sysuser/SysUserPage.vue')  },
  'RolePage': { defaultPath: '/roles', component: () => import('@/views/role/RolePage.vue')  },
  'EntPage': { defaultPath: '/ents', component: () => import('@/views/ent/EntPage.vue')  },
  'PayWayPage': { defaultPath: '/payways', component: () => import('@/views/payconfig/payWay/List.vue')  },
  'IfDefinePage': { defaultPath: '/ifdefines', component: () => import('@/views/payconfig/payIfDefine/List.vue')  },
  'AgentListPage': { defaultPath: '/agents', component: () => import('@/views/agent/AgentList.vue')  }, // 代理列表（ADR-0009）
  'AgentDetailPage': { defaultPath: '/agents/detail', component: () => import('@/views/agent/AgentDetail.vue')  }, // 團長詳情：商戶、渠道、隊長（ADR-0012）
  'ChannelAccountPage': { defaultPath: '/channels', component: () => import('@/views/agent/ChannelAccountList.vue')  }, // 渠道管理：全部團長的渠道帳號（ADR-0012）
  'FeeRulePage': { defaultPath: '/feeRules', component: () => import('@/views/feeRule/FeeRulePage.vue')  }, // 四層費率設定（ADR-0009）
  'FeeTemplatePage': { defaultPath: '/feeTemplates', component: () => import('@/views/feeRule/FeeTemplatePage.vue')  }, // 費率範本（ADR-0009 第四階段）
  'AgentPortalPage': { defaultPath: '/agentPortal', component: () => import('@/views/agentPortal/AgentPortalPage.vue')  }, // 代理後台（ADR-0009 第三階段）；五個選單共用同一頁，依 route name 顯示對應區塊
  'IsvListPage': { defaultPath: '/isv', component: () => import('@/views/isv/IsvList.vue')  }, // 服务商列表
  'MchListPage': { defaultPath: '/mch', component: () => import('@/views/mch/MchList.vue')  }, // 商户列表
  'MchAppPage': { defaultPath: '/apps', component: () => import ('@/views/mchApp/List.vue')  }, // 商户应用列表
  'PayOrderListPage': { defaultPath: '/payOrder', component: () => import('@/views/order/pay/PayOrderList.vue')  }, // 支付订单列表
  'RefundOrderListPage': { defaultPath: '/refundOrder', component: () => import('@/views/order/refund/RefundOrderList.vue')  }, // 退款订单列表
  'TransferOrderListPage': { defaultPath: '/transferOrder', component: () => import('@/views/order/transfer/TransferOrderList.vue')  }, // 转账订单
  'MchNotifyListPage': { defaultPath: '/notify', component: () => import('@/views/order/notify/MchNotifyList.vue')  }, // 商户通知列表
  'SysConfigPage': { defaultPath: '/config', component: () => import('@/views/sys/config/SysConfig.vue')  }, // 系统配置
  'WalletAccountPage': { defaultPath: '/wallet/accounts', component: () => import('@/views/wallet/WalletAccount.vue')  }, // 錢包帳戶（ADR-0010）
  'WalletLedgerPage': { defaultPath: '/wallet/ledger', component: () => import('@/views/wallet/WalletLedger.vue')  }, // 餘額流水（ADR-0010）
  'HistoryPayPage': { defaultPath: '/history/pay', component: () => import('@/views/history/HistoryPay.vue')  }, // 歷史查詢：代收查詢
  'HistoryPayoutPage': { defaultPath: '/history/payout', component: () => import('@/views/history/HistoryPayout.vue')  }, // 歷史查詢：代付查詢（尚未實作）
  'WayRoutePage': { defaultPath: '/wayRoutes', component: () => import('@/views/payconfig/wayRoute/WayRoutePage.vue')  }, // 通道路由（ADR-0011）
  'RiskBlacklistPage': { defaultPath: '/risk/blacklist', component: () => import('@/views/wallet/RiskBlacklist.vue')  }, // 風控黑名單（ADR-0010）
  'WithdrawAuditPage': { defaultPath: '/wallet/withdraw', component: () => import('@/views/wallet/WithdrawAudit.vue')  }, // 提現審核（ADR-0010）
  'UatEdgeAllowlistPage': { defaultPath: '/uatedge/allowlist', component: () => import('@/views/uatedge/UatEdgeAllowlist.vue')  }, // UAT Edge 白名單
  'SysLogPage': { defaultPath: '/log', component: () => import('@/views/sys/log/SysLog.vue')  } // 系统日志
}
