// 先載入語言字典，再載入其他模組：畫面模組在載入時就會呼叫 t()，字典必須先到位
import { initLocale } from '@/i18n'

initLocale().finally(() => import('./appMain'))
