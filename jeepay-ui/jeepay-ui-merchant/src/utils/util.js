import { t } from '@/i18n'

// 定义全局自增ID
var atomicLong = 1

export function timeFix () {
  const time = new Date()
  const hour = time.getHours()
  return hour < 9 ? t('早上好') : hour <= 11 ? t('上午好') : hour <= 13 ? t('中午好') : hour < 20 ? t('下午好') : t('晚上好')
}

export function isIE () {
  const bw = window.navigator.userAgent
  const compare = (s) => bw.indexOf(s) >= 0
  const ie11 = (() => 'ActiveXObject' in window)()
  return compare('MSIE') || ie11
}

/** 生成自增序列号（不重复） **/
export function genRowKey () {
  return new Date().getTime() + '_' + (atomicLong++)
}
