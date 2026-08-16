export default {

  requiredInput: (showText, type = 'string') => {
          return { type: type, required: true, message: '請輸入' + showText }
  },

  requiredSelect: (showText, type = 'string') => {
      return { type: type, required: true, message: '請選擇' + showText }
  },

  requiredUpload: (showText, type = 'string') => {
      return { type: type, required: true, message: '請上傳' + showText }
  },

  // 金额范围的判断  参数后面的问号，表示： 可选参数
  amountRange: (showText, minVal = 0.01, maxVal) => {
      return {
          validator: (rule, value) => {
            if (typeof (minVal) === 'number' && value < minVal) {
              // eslint-disable-next-line prefer-promise-reject-errors
              return Promise.reject(`${showText}不能小於${minVal}元`)
            }
            if (typeof (maxVal) === 'number' && value > maxVal) {
              // eslint-disable-next-line prefer-promise-reject-errors
              return Promise.reject(`${showText}不能大於${maxVal}元`)
            } else {
              return Promise.resolve()
            }
          }
       }
  },

  mobile: { pattern: /^1\d{10}$/, message: '請輸入正確的手機號' },
  emall: { pattern: /^.+@.+(\.).+$/, message: '請輸入正確的郵箱' },

  date: { pattern: /^\d{4}-\d{2}-\d{2}$/, message: '請輸入正確的日期[yyyy-MM-dd]' },
  dateOrForever: { pattern: /^(長期|长期)|(\d{4}-\d{2}-\d{2})$/, message: '請輸入正確的日期[yyyy-MM-dd]或選擇長期有效' }

}
