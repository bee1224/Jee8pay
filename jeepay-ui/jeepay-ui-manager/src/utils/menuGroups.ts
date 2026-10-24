/**
 * 主選單分類：依登入者實際擁有的選單（權限）產生，手機版主選單與電腦版首頁共用。
 * 沒有子選單的放在最上面一組（不顯示標題）；有子選單的依分類名稱各一組，組內是可點進去的頁面。
 */
import { t } from '@/i18n'

export interface MenuGroup {
  key: string
  title: string
  icon?: string
  items: any[]
}

export const HOME_ROUTE_NAME = 'JP_HOME'

export interface MenuGroupOptions {
  /** 手機版：訂單後面多一格「查詢訂單」 */
  orderQuery?: boolean
  /** 電腦版：「其他選單」（MO，例如商戶管理）也列出來，與原本的左側選單一致 */
  showOther?: boolean
}

// 需要先選一筆資料才能打開的頁面（從列表點進去），不放在主選單
const DETAIL_PAGES = ['ENT_AGENT_DETAIL']

const visibleFor = (o: MenuGroupOptions) => (r) =>
  r && r.meta && !r.meta.hideInMenu && (o.showOther ? !DETAIL_PAGES.includes(r.name) : !r.hidden)

// 手機版的「訂單」（代理）／「訂單管理」（平台）後面多一格「查詢訂單」：同一頁、可選日期區間（不需另開權限）
function withQuery(r, o: MenuGroupOptions) {
  if (!o.orderQuery || (r.name !== 'ENT_AGENT_PORTAL_ORDER' && r.name !== 'ENT_PAY_ORDER')) return [r]
  return [r, { name: '_ORDER_QUERY', path: r.path + '?mode=query', meta: { title: t('查詢訂單'), icon: 'search-outlined' } }]
}

function leaves(r, o: MenuGroupOptions) {
  const visible = visibleFor(o)
  if (!r.children || !r.children.length) return visible(r) ? withQuery(r, o) : []
  return r.children.filter(visible).flatMap((c) => leaves(c, o))
}

export function menuGroups(menuData: any[], o: MenuGroupOptions = {}): MenuGroup[] {
  const visible = visibleFor(o)
  const top = (menuData || []).filter((r) => visible(r) && (!r.children || !r.children.length)).flatMap((r) => withQuery(r, o))
  const result: MenuGroup[] = top.length ? [{ key: '_top', title: '', items: top }] : []
  ;(menuData || [])
    .filter((r) => visible(r) && r.children && r.children.length)
    .forEach((r) => {
      const items = leaves(r, o)
      if (items.length) result.push({ key: r.path, title: r.meta.title, icon: r.meta.icon, items })
    })
  return result
}

/** 目前頁面所在的分類（找不到時回傳 null） */
export function groupOf(groups: MenuGroup[], routeName: string): MenuGroup | null {
  return groups.find((g) => g.items.some((i) => i.name === routeName)) || null
}
