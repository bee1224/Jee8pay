<template>
  <!-- 手機版（768 以下）：頂部標題列＋底部分頁列；按「主選單」（Home）一律回到主選單畫面，沒有重新整理按鈕 -->
  <MobileShell
    v-if="isMobile"
    :title="mobile.launcher ? $t('主選單') : mobileTitle"
    :show-back="!mobile.launcher"
    :tabs="mobileTabs"
    :active-tab="mobileActiveTab"
    :page-key="mobile.launcher ? '_launcher' : route.fullPath"
    @back="mobile.launcher = true"
    @tab="onMobileTab"
  >
    <MobileLauncher v-if="mobile.launcher" :menu-data="vdata.menuData" :is-agent-user="vdata.isAgentUser" @open="openFromLauncher" />
    <component :is="mobileView" v-else-if="mobileView" :key="route.fullPath" />
    <div v-else class="m-desktop-page"><router-view /></div>
  </MobileShell>

  <jeepayLayout
    v-else
    v-model:openKeys="vdata.openKeys"
    v-model:selectedKeys="vdata.selectedKeys"
    v-model:collapsed="proLayoutObject.collapsed"
    layout="side"
    :active-name="route.path"
    :nav-theme="vdata.theme"
    :sider-width="260"
    :menu-data="vdata.menuData"
    is-mobile="true"
    :collapsed-button-render="false"
    :collapsed-width="80"
    @broken="(e) => (vdata.broken = e)"
  >
    <router-view v-if="vdata.isRouterAlive" />

    <!-- 菜单头部渲染插槽 -->
    <template #menuHeaderRender>
      <router-link :to="{ path: '/' }" style="display: flex; align-items: center">
        <!-- 代理帳號（團長、隊長、子帳號）的左側選單不顯示任何品牌名稱與標誌 -->
        <div v-if="vdata.isAgentUser"></div>
        <div v-else-if="!proLayoutObject.collapsed">
          <img src="@/assets/logo-icon.png" alt="logo" style="height: 32px" />
          <img
            src="@/assets/logo-text.png"
            alt="jeepay"
            style="width: 90px; margin: 5px 0 0 5px"
          />
        </div>
        <div v-else><img src="@/assets/logo-icon-collapsed.png" alt="logo" style="height: 32px" /></div>
      </router-link>
    </template>

    <!-- 自定义顶部内容， 包括刷新按钮，面包屑 -->
    <template #headerContentRender>
      <!-- 最顶部header的宽度： 小屏时，菜单栏改为抽屉浮动，所以直接撑满100vh, 大屏时要根据折叠状态减80或260 -->
      <div
        class="customhead"
        :style="{
          width: `calc(100vw - ${vdata.broken ? '0px' : proLayoutObject.collapsed ? '90px' : '270px'})`,
        }"
      >
        <div class="refash">
          <!-- 收起菜单栏 -->
          <MenuUnfoldOutlined
            v-if="proLayoutObject.collapsed"
            class="icon-style"
            @click="proLayoutObject.collapsed = false"
          />
          <!-- 展开菜单栏 -->
          <MenuFoldOutlined v-else class="icon-style" @click="proLayoutObject.collapsed = true" />

          <!-- 刷新按钮 -->
          <ReloadOutlined
            :class="{ 'icon-rotate': vdata.isRouterAlive }"
            class="icon-style"
            @click="clickRefsh"
          />
          <!-- 回到首页 -->
          <homeOutlined
            key="layout-main"
            :style="{ color: route.name === 'ENT_C_MAIN' ? 'var(--jp-primary)' : 'var(--jp-text-2)' }"
            class="icon-style icon-main"
            @click="toMain"
          />

          <a-breadcrumb style="margin-left: 20px">
            <template v-for="(item, index) in vdata.matchList" :key="index">
              <a-breadcrumb-item>{{ item.meta.title }}</a-breadcrumb-item>
            </template>
          </a-breadcrumb>
        </div>

        <!-- 个人信息部分 -->
        <div style="display: flex; align-items: center; gap: 8px">
          <LanguageButton />
          <a-dropdown>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="userInfo">
                  <setting-outlined style="margin-right: 10px" />
                  <span style="cursor: hand">{{ $t('個人資訊') }}</span>
                </a-menu-item>
                <a-menu-item @click="logout">
                  <LogoutOutlined style="margin-right: 10px" />
                  <span style="cursor: hand">{{ $t('退出登入') }}</span>
                </a-menu-item>
              </a-menu>
            </template>
            <div class="self-info">
              <a-avatar shape="square" class="square" :src="userStore.userInfo.avatarUrl" />
              <span>{{ userStore.userInfo.realname }}</span>
              <caret-down-outlined style="margin-left: 5px" />
            </div>
          </a-dropdown>
        </div>
      </div>
    </template>
  </jeepayLayout>
</template>
<script lang="ts" setup>
import { useRouter, useRoute, createRouterMatcher } from 'vue-router'
import { reactive, computed, watch, watchEffect, nextTick, getCurrentInstance } from 'vue'
import { MenuUnfoldOutlined, MenuFoldOutlined } from '@ant-design/icons-vue'
import { generatorDynamicRouter } from '@/router/generator-routers'
import { useUserStore } from '@/store/modules/user'
import { isMobile } from '@/views/mobile/mobileKit'
import MobileShell from '@/views/mobile/MobileShell.vue'
import MobileLauncher from '@/views/mobile/MobileLauncher.vue'
import MobileMe from '@/views/mobile/MobileMe.vue'
import { HomeOutlined, ProfileOutlined, WalletOutlined, UserOutlined } from '@ant-design/icons-vue'
import { defineAsyncComponent } from 'vue'
import { t } from '@/i18n'
import LanguageButton from '@/components/LanguagePicker/LanguageButton.vue'

const { $infoBox, $access, $hasAgentEnt } = getCurrentInstance()!.appContext.config.globalProperties

// store对象
const userStore = useUserStore()

const route = useRoute()

const router = useRouter()

const proLayoutObject = reactive({
  collapsed: false,
})

const vdata: any = reactive({
  broken: false, // 页面宽度是否小于992 true小屏   false大屏

  selectedKeys: [],
  openKeys: [],
  // default
  collapsed: false,

  layout: 'side',
  theme: 'light',
  menuData: [],
  isAgentUser: false,
  //刷新
  isRouterAlive: true,
  isSpin: false,
  siteInfo: '',
})

//个人信息页
function userInfo() {
  router.push({
    path: '/current/userinfo',
  })
}

// 登出函数
function logout() {
  $infoBox.confirmDanger(
    t('是否退出登入？'),
    t('你好{0}確認退出登入嗎？', [userStore.userInfo['realname']]),
    () => {
      userStore.logout()
    }
  )
}

// 递归面包屑对象，获取面包屑数组
const matchHandle = (currentMatch) => {
  vdata.matchList.unshift(currentMatch.record)

  if (currentMatch.parent) {
    matchHandle(currentMatch.parent)
  }
}

// 代理後台的選單分類（比照競品側邊欄）；分類只影響選單顯示，頁面路由不變。
// 沒列到的選單排在最後；一個分類只有一項時直接放在第一層，不另開資料夾。
const AGENT_MENU_GROUPS = [
  { key: 'users', title: t('用戶管理'), icon: 'team-outlined', items: ['ENT_AGENT_PORTAL_MCH', 'ENT_AGENT_PORTAL_SUB', 'ENT_AGENT_PORTAL_STAFF'] },
  { key: 'orders', title: t('訂單與報表'), icon: 'account-book-outlined', items: ['ENT_AGENT_PORTAL_ORDER', 'ENT_AGENT_PORTAL_REPORT'] },
  { key: 'money', title: t('帳務管理'), icon: 'wallet-outlined', items: ['ENT_AGENT_PORTAL_HOME', 'ENT_AGENT_PORTAL_PROFIT', 'ENT_AGENT_PORTAL_BRANCH_WALLET', 'ENT_AGENT_PORTAL_WITHDRAW_AUDIT'] },
  { key: 'channels', title: t('渠道設定'), icon: 'branches-outlined', items: ['ENT_AGENT_PORTAL_CHANNEL', 'ENT_AGENT_PORTAL_FEE', 'ENT_AGENT_PORTAL_ROUTE'] },
  { key: 'system', title: t('系統設定'), icon: 'setting-outlined', items: ['ENT_AGENT_PORTAL_BLACKLIST', 'ENT_AGENT_PORTAL_OPLOG'] },
]
function groupAgentMenus(items) {
  const used = new Set(AGENT_MENU_GROUPS.flatMap((g) => g.items))
  const result = items.filter((i) => i.name === 'ENT_AGENT_PORTAL_DASH')
  AGENT_MENU_GROUPS.forEach((g) => {
    const children = g.items.map((name) => items.find((i) => i.name === name)).filter(Boolean)
    if (children.length === 1) result.push(children[0])
    else if (children.length > 1) result.push({ path: '/agentGroup/' + g.key, name: 'AGENT_GROUP_' + g.key, meta: { title: g.title, icon: g.icon }, children })
  })
  return result.concat(items.filter((i) => i.name !== 'ENT_AGENT_PORTAL_DASH' && !used.has(i.name)))
}

// menu对象信息
generatorDynamicRouter().then((resRouter: any) => {
  // 过滤掉系统通用菜单
  const menus = resRouter[0].children.filter(
    (item) => item.name != 'ENT_COMMONS' && item.name != 'ENT_C_USERINFO'
  )
  // 「代理後台」只給代理帳號：平台帳號（含超管）不顯示；代理帳號則把底下的選單攤平到第一層
  const belong = userStore.userInfo['belongInfoId']
  const isAgentUser = !!belong && belong !== '0'
  vdata.isAgentUser = isAgentUser
  vdata.menuData = menus.flatMap((item) => {
    if (item.name !== 'ENT_AGENT_PORTAL') return [item]
    return isAgentUser ? groupAgentMenus(item.children || []) : []
  })

  vdata.routerMatch = createRouterMatcher(vdata.menuData, {})
})

watchEffect(() => {
  if (vdata.routerMatch) {
    // 根据当前路由的name 获取整体面包屑对象
    vdata.matchList = []
    const currentMatch = vdata.routerMatch.getRecordMatcher(route.name)

    // 递归面包屑对象，获取面包屑数组
    if (currentMatch) {
      matchHandle(currentMatch)
    }

    // 获取当前访问的路由，用于左侧菜单列表的展示
    if (vdata.matchList.length > 0) {
      vdata.selectedKeys = vdata.matchList.map((r) => r.path)
      vdata.openKeys = vdata.matchList.map((r) => r.path)
    }
  }
  vdata.routeName = route.meta.title
})

// ---------------- 手機版 ----------------
// 有手機版畫面的頁面（route name = 權限 ID）；其餘頁面在手機上直接顯示電腦版元件
const M = (name) => defineAsyncComponent(() => import(`../views/mobile/agent/${name}.vue`))
const MOBILE_VIEWS = {
  ENT_AGENT_PORTAL_DASH: M('MDashboard'),
  ENT_AGENT_PORTAL_MCH: M('MMerchants'),
  ENT_AGENT_PORTAL_SUB: M('MSubAgents'),
  ENT_AGENT_PORTAL_STAFF: M('MStaff'),
  ENT_AGENT_PORTAL_ORDER: M('MOrders'),
  ENT_PAY_ORDER: M('MOrders'),
  ENT_AGENT_PORTAL_REPORT: M('MReport'),
  ENT_AGENT_PORTAL_HOME: M('MWallet'),
  ENT_AGENT_PORTAL_PROFIT: M('MProfit'),
  ENT_AGENT_PORTAL_BRANCH_WALLET: M('MBranchWallets'),
  ENT_AGENT_PORTAL_WITHDRAW_AUDIT: M('MWithdrawAudit'),
  ENT_AGENT_PORTAL_CHANNEL: M('MChannels'),
  ENT_AGENT_PORTAL_FEE: M('MFee'),
  ENT_AGENT_PORTAL_ROUTE: M('MRoutes'),
  ENT_AGENT_PORTAL_BLACKLIST: M('MBlacklist'),
  ENT_AGENT_PORTAL_OPLOG: M('MOpLog'),
  ENT_C_USERINFO: MobileMe,
  // 上帝
  ENT_AGENT_INFO: defineAsyncComponent(() => import('../views/mobile/god/MAgents.vue')),
  ENT_AGENT_DETAIL: defineAsyncComponent(() => import('../views/mobile/god/MAgentDetail.vue')),
  ENT_PC_IF_DEFINE: defineAsyncComponent(() => import('../views/mobile/god/MIfDefines.vue')),
  ENT_C_MAIN: defineAsyncComponent(() => import('../views/mobile/god/MGodHome.vue')),
  ENT_UR_ROLE_ENT: defineAsyncComponent(() => import('../views/mobile/god/MEnts.vue')),
  ENT_SYS_CONFIG_INFO: defineAsyncComponent(() => import('../views/mobile/god/MSysConfig.vue')),
  ENT_FEE_RULE: defineAsyncComponent(() => import('../views/mobile/god/MFeeRules.vue')),
  ENT_CHANNEL_ACCOUNT: defineAsyncComponent(() => import('../views/agent/ChannelAccountPanel.vue')),
  ENT_HISTORY_PAY: M('MOrders'),
  ENT_HISTORY_PAYOUT: M('MOrders'),
  ENT_UR_USER: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_UR_ROLE: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_FEE_TEMPLATE: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_REFUND_ORDER: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_TRANSFER_ORDER: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_MCH_NOTIFY: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_WALLET_ACCOUNT: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_WALLET_LEDGER: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_WALLET_WITHDRAW: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_RISK_BLACKLIST: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_PC_WAY: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_WAY_ROUTE: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_SYS_LOG: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_UAT_EDGE_ALLOWLIST: defineAsyncComponent(() => import('../views/mobile/god/MGodPage.vue')),
  ENT_SYS_MONITOR: defineAsyncComponent(() => import('../views/monitor/MonitorPage.vue')),
  ENT_TELEGRAM_BOT: defineAsyncComponent(() => import('../views/telegram/TelegramBotPage.vue')),
  ENT_API_TEST: defineAsyncComponent(() => import('../views/apitest/ApiTestPage.vue')),
  ENT_LOGIN_DEVICE: defineAsyncComponent(() => import('../views/loginDevice/LoginDevicePage.vue')),
}
// 手機一打開先顯示主選單
const mobile = reactive({ launcher: true })
const mobileView = computed(() => MOBILE_VIEWS[route.name as string] || null)
const mobileTitle = computed(() =>
  route.name === 'ENT_C_USERINFO' ? t('我的') : route.query.mode === 'query' ? t('查詢訂單') : (route.meta.title as string) || ''
)

function findMenu(names: string[]) {
  const walk = (list) => {
    for (const r of list || []) {
      if (names.includes(r.name)) return r
      const hit = walk(r.children)
      if (hit) return hit
    }
    return null
  }
  return walk(vdata.menuData)
}
// 底部分頁：代理帳號用代理後台的訂單與錢包，平台帳號用平台的訂單管理與錢包帳戶；沒有權限的分頁不顯示
const ORDER_NAMES = ['ENT_AGENT_PORTAL_ORDER', 'ENT_PAY_ORDER']
const WALLET_NAMES = ['ENT_AGENT_PORTAL_HOME', 'ENT_WALLET_ACCOUNT']
const mobileTabs = computed(() => {
  const tabs: any[] = [{ key: 'home', label: t('主選單'), icon: HomeOutlined }]
  const order = findMenu(ORDER_NAMES)
  if (order) tabs.push({ key: 'order', label: t('訂單'), icon: ProfileOutlined, path: order.path })
  const wallet = findMenu(WALLET_NAMES)
  if (wallet) tabs.push({ key: 'wallet', label: t('錢包'), icon: WalletOutlined, path: wallet.path })
  tabs.push({ key: 'me', label: t('我的'), icon: UserOutlined, path: '/current/userinfo' })
  return tabs
})
const mobileActiveTab = computed(() => {
  if (mobile.launcher) return 'home'
  const n = route.name as string
  if (ORDER_NAMES.includes(n)) return 'order'
  if (WALLET_NAMES.includes(n)) return 'wallet'
  if (n === 'ENT_C_USERINFO') return 'me'
  return ''
})
function onMobileTab(t) {
  if (t.key === 'home') {
    mobile.launcher = true
    return
  }
  openFromLauncher(t.path)
}
// path 可能帶查詢字串（例如 ?mode=query），用字串交給 router 解析
function openFromLauncher(path) {
  mobile.launcher = false
  if (path !== route.fullPath) router.push(path)
}
watch(
  () => route.fullPath,
  () => (mobile.launcher = false)
)

// 回主页
function toMain() {
  router.push({
    path: '/main',
  })
}

const breadcrumb = computed(() =>
  router.currentRoute.value.matched.concat().map((item) => {
    return {
      path: item.path,
      breadcrumbName: item.meta.title || '',
    }
  })
)

const handleCollapsed = () => {
  vdata.collapsed = !vdata.collapsed
}

const clickRefsh = () => {
  vdata.isSpin = true
  vdata.isRouterAlive = false
  nextTick(() => {
    vdata.isRouterAlive = true
  })
  vdata.isSpin = false
}
</script>

<style scoped lang="less">
.brand-title {
  margin-left: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--jp-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
:deep(.ant-pro-global-header-layout-side) {
  background-color: transparent;
  box-shadow: none;
  padding: 25px 25px;
}
// 路由名称
.router-name {
  display: inline-block;
  margin-right: 15px;
  color: #000;
  font-size: 20px;
}

// 图标部分
.refash {
  display: flex;
  align-items: center;
  justify-self: center;
  height: 100%;

  .icon-style {
    flex: 1;

    line-height: 56px;
    display: inline-block;
    height: 100%;
    font-size: 16px;
    color: var(--jp-text-2);
    padding: 0 20px;

    &:hover {
      color: var(--jp-text) !important;
    }
  }

  .icon-main {
    border-right: 1px solid var(--jp-border);
    border-left: 1px solid var(--jp-border);
  }
}
.customhead {
  background-color: var(--jp-surface);
  z-index: 9;

  position: fixed;
  top: 0;
  right: 10px;

  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--jp-border);
  height: 56px;

  // 页签， 撑满剩余部分
  .tabs {
    flex: 1;

    display: flex;
    justify-content: flex-start;
  }
}

// 个人信息
.self-info {
  display: flex;
  align-items: center;
  align-items: center;

  height: 56px;
  padding: 0 20px;
  border-left: 1px solid var(--jp-border);

  &:hover {
    cursor: pointer;
  }

  .square {
    margin-right: 10px;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    &:hover {
      cursor: pointer;
    }
  }
}

// 刷新图标旋转
.icon-rotate {
  animation: rotateAnimation 0.3s linear; /* 动画名称、时长和速度曲线 */
}
/* 定义旋转动画 */
@keyframes rotateAnimation {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.customfoot {
  padding: 10px 0;

  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>

<style lang="less">
.ant-menu {
  background: transparent;
}

.ant-layout-sider-children {
  //导航整体的右侧新增一条border
  border-right: 1px solid var(--jp-border);
  background: transparent;
  padding-bottom: 30px;
}
.ant-layout-sider-light {
  //亮色模式下整体的背景色
  background: var(--jp-sider);
}

.ant-menu-inline-collapsed {
  //收起状态下，导航内容区域两侧的padding变小
  padding: 0 10px !important;
}
.ant-menu-submenu-title {
  //使导航在收起状态下，带有子item的一级菜单的图标居中
  margin: 0 auto;
}
.ant-menu-item {
  margin: 0 auto !important; //使导航在收起状态下，没有子item的一级菜单图标居中
  border-radius: 3px;
  &:hover {
    background: @primary-glass; //导航item的hover样式
  }
}
.ant-menu-sub.ant-menu-inline > .ant-menu-item {
  //二级导航item的上下边距
  margin: 5px 0 !important;
}
.ant-menu-vertical.ant-menu-sub {
  //收起状态下，子菜单的容器padding
  padding: 5px;
}
.ant-menu-dark .ant-menu-inline.ant-menu-sub {
  //暗色模式下，子菜单背景颜色调整
  background: #001529;
}
.ant-menu-sub.ant-menu-inline {
  //亮色模式下，子菜单背景颜色调整
  background-color: var(--jp-sider);
}

.ant-menu:not(.ant-menu-horizontal) .ant-menu-item-selected {
  //选中状态下的item的样式
  background-color: var(--ant-primary-color) !important;
  color: #fff !important;
  border-radius: 3px;
  & a:hover {
    color: #fff !important;
  }
}
.ant-menu-item-selected a {
  //选中状态下的文字颜色继承父元素
  color: inherit !important;
}
.ant-menu-item .ant-menu-item-icon + span {
  //去掉文字变色的延迟动画效果
  transition: 0s;
}
.ant-menu-vertical .ant-menu-item::after,
.ant-menu-vertical-left .ant-menu-item::after,
.ant-menu-vertical-right .ant-menu-item::after,
.ant-menu-inline .ant-menu-item::after {
  //取消选中状态下item右侧的蓝色方条
  content: none;
}

//header样式
.ant-layout-header {
  background-color: transparent;
  height: auto !important;
  width: 100%;
  padding: 25px 0 !important;
  line-height: inherit !important;
}

aside {
  max-width: 300px;
}

aside::before {
  // content: '';
  position: absolute;
  background-color: @jee-theme;
  width: 10px;
  height: 100vh;
  left: 0;
  top: 0;
  z-index: 99;
}

/*
  菜单栏底部  亮色与暗色
*/
.menu-footer-box {
  width: 100%;
  height: 100%;

  display: flex;
  flex-direction: column;

  padding: 16px 16px 20px;
  border-top: 1px solid #eff1f4;

  .menu-footer {
    box-sizing: border-box;
    padding: 8px 20px;

    border: 1px solid #eff0f2;
    cursor: pointer;
    border-radius: 8px;
    background: #eff0f2;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #535454;
    transition: 0.3s ease;
    margin-bottom: 16px;
    &:hover {
      border: 1px dashed var(--ant-primary-color);
      color: var(--ant-primary-color);
      background: @primary-glass;
    }
    .bi-circle-half {
      transform: rotate(45deg);
      transition: 0.2s ease;
    }
    span {
      font-weight: normal;
      font-size: 14px;
      letter-spacing: 0.05em;
      text-align: left;
      margin-left: 8px;
      transition: 0.2s ease;
    }
  }

  .version {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 0;
    padding-top: 16px;
    border-top: 1px solid #eff1f4;

    .label {
      color: rgba(0, 0, 0, 0.5);
    }
  }
}

.menu-footer-box-dark {
  border-top: 1px solid #a6adb4;

  .menu-footer {
    background: rgba(235, 238, 242, 0);
    border: 1px dashed #4d5c6a;
    color: #fff;
    .bi-circle-half {
      transform: rotate(225deg);
    }
    .verson p span {
      color: #ffffff;
    }
  }

  .version {
    border-top: 1px solid #a6adb4;
    color: #fff;
    .label {
      color: #fff;
    }
  }
}
</style>
<style>
.ant-layout-header {
  background-color: transparent !important;
  padding: 0 !important;
}

.ant-layout-content {
  padding: 80px 20px 20px;
  margin: 0;
  height: auto;
}

.ant-layout:first-child .ant-layout {
  background-color: var(--jp-surface);
  overflow-y: scroll;
  overflow-x: hidden;
}
</style>
