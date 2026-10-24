import { t } from '@/i18n'
export default {

  requiredInput: (showText, type = 'string') => {
          return { type: type, required: true, message: t('請輸入{0}', [showText]) }
  },

  requiredSelect: (showText, type = 'string') => {
      return { type: type, required: true, message: t('請選擇{0}', [showText]) }
  },

  requiredUpload: (showText, type = 'string') => {
      return { type: type, required: true, message: t('請上傳{0}', [showText]) }
  },

  // 金额范围的判断  参数后面的问号，表示： 可选参数
  amountRange: (showText, minVal = 0.01, maxVal) => {
      return {
          validator: (rule, value) => {
            if (typeof (minVal) === 'number' && value < minVal) {
              // eslint-disable-next-line prefer-promise-reject-errors
              return Promise.reject(t('{0}不能小於{1}元', [showText, minVal]))
            }
            if (typeof (maxVal) === 'number' && value > maxVal) {
              // eslint-disable-next-line prefer-promise-reject-errors
              return Promise.reject(t('{0}不能大於{1}元', [showText, maxVal]))
            } else {
              return Promise.resolve()
            }
          }
       }
  },

  mobile: { pattern: /^09\d{8}$/, message: t('請輸入正確的手機號（09 開頭 10 碼）') },
  emall: { pattern: /^.+@.+(\.).+$/, message: t('請輸入正確的郵箱') },

  date: { pattern: /^\d{4}-\d{2}-\d{2}$/, message: t('請輸入正確的日期[yyyy-MM-dd]') },
  dateOrForever: { pattern: /^(長期|长期)|(\d{4}-\d{2}-\d{2})$/, message: t('請輸入正確的日期[yyyy-MM-dd]或選擇長期有效') }

}
