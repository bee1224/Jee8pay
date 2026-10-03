// 背景匯出：查詢頁的「匯出」只建立背景工作，完成後在同一頁的「匯出紀錄」下載
import { req } from '@/api/manage'
import storage from '@/utils/jeepayStorageWrapper'
import appConfig from '@/config/appConfig'

export const EXPORT_API = '/api/exports'

export function submitExport(jobType: string, params: any = {}) {
  const body = { ...params, jobType }
  Object.keys(body).forEach((k) => (body[k] === undefined || body[k] === '' || body[k] === null) && delete body[k])
  return req.add(EXPORT_API, body)
}

/** 帶登入憑證下載檔案（不能用一般連結，因為 API 需要 iToken 標頭） */
export async function downloadExport(job: any) {
  const base = (import.meta as any).env.VITE_API_BASE_URL || ''
  const resp = await fetch(`${base}${EXPORT_API}/${job.jobId}/file`, {
    headers: { [appConfig.ACCESS_TOKEN_NAME]: storage.getToken() },
  })
  const type = resp.headers.get('Content-Type') || ''
  if (!resp.ok || type.includes('application/json')) {
    let msg = '下載失敗'
    try {
      msg = (await resp.json()).msg || msg
    } catch (e) {
      // 非 JSON 回應，維持預設訊息
    }
    throw new Error(msg)
  }
  const blob = await resp.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = job.fileName || `export-${job.jobId}.csv`
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 5000)
}
