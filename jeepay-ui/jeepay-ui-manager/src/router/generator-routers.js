import { BasicLayout, BlankLayout, PageView, RouteView } from '@/layouts'
import { useUserStore } from '@/store/modules/user'
import { asyncRouteDefine } from '@/config/appConfig'
import { t } from '@/i18n'
import { HOME_ROUTE_NAME } from '@/utils/menuGroups'

// 前端路由表 = 基础定义 + 动态组件
const constantRouterComponents = Object.assign(
  {
    // 基础页面 layout 必须引入
    BasicLayout: { component: BasicLayout },
    BlankLayout: { component: BlankLayout },
    RouteView: { component: RouteView },
    PageView: { component: PageView },
    // 403: () => import('@/views/exception/403.vue'),
    // 404: () => import('@/views/exception/404.vue'),
    // 500: () => import('@/views/exception/500.vue'),
  },
  asyncRouteDefine
)

// 前端未找到页面路由
const notFoundRouter = {
  path: '*',
  component: () => import('@/views/exception/404.vue'),
}

// 主選單（首頁）：電腦版與手機版都先顯示分類，再點進各頁面；畫面由 BasicLayout 依路由名稱自己畫，這裡不需要元件內容
const homeRouter = {
  path: '/home',
  name: HOME_ROUTE_NAME,
  component: { render: () => null },
  meta: { title: t('主選單') },
}

// 根级菜单
const rootRouter = {
  name: 'index',
  path: '/',
  component: BasicLayout,
  redirect: '/home', // 根页面【/】默认跳转到主選單
  children: [],
  meta: { title: t('主頁') },
}

/**
 * 动态生成菜单
 * @param token
 * @returns {Promise<Router>}
 */
export const generatorDynamicRouter = () => {
  return new Promise((resolve, reject) => {
    // 根据树状结构生成路由格式
    rootRouter.children = [homeRouter, ...generator(useUserStore().userInfo.allMenuRouteTree)]

    // 构建完整路由
    resolve([rootRouter, notFoundRouter])
  })
}

/**
 * 格式化树形结构数据 生成 vue-router 层级路由表
 *
 * @param routerMap
 * @returns {*}
 */
export const generator = (allMenuRouteTreeArray) => {
  const menuResult = []

  // 遍历map
  allMenuRouteTreeArray.map((item) => {
    const defComponent = constantRouterComponents[item.componentName || item.entId]

    // 找不到组件 || 其他菜单
    if (!defComponent) {
      return
    }

    // 跳转uri
    let path = item.menuUri || defComponent.defaultPath

    // 没有配置path, 如果为目录则允许为空， 否则不在加载此配置
    if (!path) {
      if (item.children && item.children.length > 0) {
        path = `/${item.entId}`
      } else {
        return // 不再加载此配置项
      }
    }

    const currentRouter = {
      // 如果路由设置了 path，则作为默认 path，否则 路由地址 为默认配置
      path: path,
      // 路由名称，建议唯一
      name: item.entId,
      // 该路由对应页面的 组件 :方案2 (动态加载)
      component:
        (defComponent && defComponent.component) ||
        (() => import(/* @vite-ignore */ `@/views/${item.componentName}`)),
      // meta: 页面标题, 菜单图标, 页面权限(供指令权限用，可去掉)
      meta: {
        title: item.entName,
        icon: item.menuIcon + '-outlined',
        keepAlive: false,
      },
      hidden: item.entType === 'MO', // 当其他菜单时需要隐藏显示
    }
    // 是否有子菜单，并递归处理
    if (item.children && item.children.length > 0) {
      // Recursion
      currentRouter.children = generator(item.children)
    }
    menuResult.push(currentRouter)
  })

  return menuResult
}
