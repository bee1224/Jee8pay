/**
 * 登入成功後回報一次裝置資訊（不阻擋登入，失敗也不提示）。
 *
 * 網頁無法讀取 IMEI，「裝置識別碼」是第一次登入時在這台裝置的瀏覽器產生並保存的隨機 ID；清除瀏覽器資料或換瀏覽器會變。
 * 型號：Android 的 Chrome／Edge 由 User-Agent Client Hints 取得；iPhone 的 Safari 只能分辨 iPhone／iPad。
 * GPS：瀏覽器會詢問使用者是否允許；拒絕或逾時只記錄狀態，不影響登入。
 */
import request from '@/http/request'

const ID_KEY = 'jeepay-device-id'

function deviceId(): string | undefined {
  try {
    let id = localStorage.getItem(ID_KEY)
    if (!id) {
      id = (crypto as any).randomUUID ? (crypto as any).randomUUID() : 'D' + Date.now().toString(36) + Math.random().toString(36).slice(2, 12)
      localStorage.setItem(ID_KEY, id as string)
    }
    return id as string
  } catch (e) {
    return undefined
  }
}

async function modelAndOs(): Promise<{ deviceModel?: string; deviceOs?: string }> {
  const uad = (navigator as any).userAgentData
  if (uad && uad.getHighEntropyValues) {
    try {
      const v = await uad.getHighEntropyValues(['model', 'platform', 'platformVersion'])
      return { deviceModel: v.model || (uad.mobile ? '' : '電腦'), deviceOs: [v.platform, v.platformVersion].filter(Boolean).join(' ') }
    } catch (e) {
      // 取不到就退回 User-Agent 判斷
    }
  }
  const ua = navigator.userAgent
  const ios = ua.match(/(iPhone|iPad)[^)]*OS (\d+)[_.](\d+)/)
  if (ios) return { deviceModel: ios[1], deviceOs: `iOS ${ios[2]}.${ios[3]}` }
  const android = ua.match(/Android ([\d.]+);\s*([^;)]+)/)
  if (android) return { deviceModel: android[2].replace(/\s*Build\/.*$/, '').trim(), deviceOs: `Android ${android[1]}` }
  if (/Windows/.test(ua)) return { deviceModel: '電腦', deviceOs: 'Windows' }
  if (/Mac OS X/.test(ua)) return { deviceModel: '電腦', deviceOs: 'macOS' }
  return {}
}

function position(): Promise<any> {
  if (!navigator.geolocation) return Promise.resolve({ geoState: 'UNSUPPORTED' })
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (p) => resolve({ geoState: 'GRANTED', latitude: p.coords.latitude, longitude: p.coords.longitude, accuracy: Math.round(p.coords.accuracy) }),
      (err) => resolve({ geoState: err.code === 1 ? 'DENIED' : err.code === 3 ? 'TIMEOUT' : 'UNAVAILABLE' }),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    )
  })
}

export async function reportLoginDevice() {
  try {
    const [mo, geo] = await Promise.all([modelAndOs(), position()])
    await request.request({ url: '/api/current/loginDevice', method: 'post', data: { deviceId: deviceId(), ...mo, ...geo } })
  } catch (e) {
    // 回報失敗不影響使用
  }
}
