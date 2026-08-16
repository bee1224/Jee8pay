<template>
  <div>
    <a-modal
      v-model:open="vdata.open"
      title="等待支付"
      @ok="handleClose"
      :footer="null"
      :width="300"
    >
      <div style="width: 100%; margin-bottom: 20px; text-align: center">
        <img v-if="vdata.apiRes.payDataType == 'codeImgUrl'" :src="vdata.apiRes.payData" alt="" />
        <span v-else-if="vdata.apiRes.payDataType == 'payurl'">
          等待使用者支付
          <hr />
          如瀏覽器未正確跳轉請點擊：
          <a :href="vdata.apiRes.payData" target="_blank">支付地址</a>
          <a-button
            size="small"
            class="copy-btn"
            v-clipboard:copy="vdata.apiRes.payData"
            v-clipboard:success="onCopy"
          >
            複製連結
          </a-button>
        </span>
        <span v-else>等待使用者支付,請稍後</span>
      </div>
      <p class="describe">
        <img src="@/assets/payTestImg/wx_app.svg" alt="" v-show="vdata.wxApp" />
        <!-- 微信图标 -->
        <img src="@/assets/payTestImg/ali_app.svg" alt="" v-show="vdata.aliApp" />
        <!-- 支付宝图标 -->
        <span>{{ vdata.payText }}</span>
      </p>
    </a-modal>
  </div>
</template>
<script setup lang="tsx">
import ReconnectingWebSocket from 'reconnectingwebsocket'
import { getWebSocketPrefix } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const vdata: any = reactive({
  open: false,
  payText: '', // 二维码底部描述文字
  wxApp: false, // 微信二维码图片是否展示
  aliApp: false, // 支付宝二维码图片是否展示
  apiRes: {}, // 接口返回数据包
  payOrderWebSocket: null, // 支付订单webSocket对象
})

const emit = defineEmits(['closeBarCode'])

function onCopy() {
  $infoBox.message.success('複製成功')
}

// 二维码以及条码弹窗
function showModal(wayCode, apiRes) {
  // 关闭上一个webSocket监听
  if (vdata.payOrderWebSocket) {
    vdata.payOrderWebSocket.close()
  }

  vdata.apiRes = apiRes
  vdata.wxApp = false
  vdata.aliApp = false
  vdata.open = true // 打开弹窗

  // 根据不同的支付方式，展示不同的信息
  vdata.payText = ''
  if (wayCode === 'WX_NATIVE' || wayCode === 'WX_JSAPI') {
    // 微信二维码
    vdata.wxApp = true
    vdata.payText = '請使用微信"掃一掃"掃碼支付'
  } else if (wayCode === 'ALI_QR' || wayCode === 'ALI_JSAPI' || wayCode === 'ALI_OC') {
    // 支付宝二维码
    vdata.aliApp = true
    vdata.payText = '請使用支付寶"掃一掃"掃碼支付'
  } else if (wayCode === 'QR_CASHIER') {
    // 聚合支付二维码
    vdata.wxApp = true
    vdata.aliApp = true
    vdata.payText = '支援微信、支付寶掃碼'
  }

  // 此处判断接口中返回的orderState，值为0，1 代表支付中，直接放行无需处理，2 成功 3 失败
  if (apiRes.orderState === 2 || apiRes.orderState === 3) {
    if (apiRes.orderState === 2) {
      handleClose()
      const succModal = $infoBox.modalSuccess('支付成功', <div>2s後自動關閉...</div>)
      setTimeout(() => {
        succModal.destroy()
      }, 2000)
      emit('closeBarCode') // 关闭条码框
    } else if (apiRes.orderState === 3) {
      handleClose()
      emit('closeBarCode') // 关闭条码框
      $infoBox.modalError(
        '支付失敗',
        <div>
          <div>錯誤碼：{apiRes.errCode}</div>
          <div>錯誤訊息：{apiRes.errMsg}</div>
        </div>
      )
    }
    return
  }

  // h5 或者 wap
  if (wayCode === 'WX_H5' || wayCode === 'ALI_WAP') {
    vdata.payText = '請複製連結到手機端打開'
  } else {
    // 跳转到PC网站
    if (apiRes.payDataType === 'payurl') {
      window.open(apiRes.payData)
    }
  }
  // 如果上面未关闭条码框，则代表进入webScoket，那么先在此处关闭条码框
  emit('closeBarCode') // 关闭条码框
  // 监听响应结果
  vdata.payOrderWebSocket = new ReconnectingWebSocket(
    getWebSocketPrefix() + '/api/anon/ws/payOrder/' + apiRes.payOrderId + '/' + new Date().getTime()
  )
  vdata.payOrderWebSocket.onopen = () => {}
  vdata.payOrderWebSocket.onmessage = (msgObject) => {
    const resMsgObject = JSON.parse(msgObject.data)
    if (resMsgObject.state === 2) {
      handleClose()
      const succModal = $infoBox.modalSuccess('支付成功', <div>2s後自動關閉...</div>)
      setTimeout(() => {
        succModal.destroy()
      }, 2000)
    } else {
      handleClose()
      $infoBox.modalError(
        '支付失敗',
        <div>
          <div>錯誤碼：{apiRes.errCode}</div>
          <div>錯誤訊息：{apiRes.errMsg}</div>
        </div>
      )
    }
  }
}
function handleClose() {
  if (vdata.payOrderWebSocket) {
    vdata.payOrderWebSocket.close()
  }
  vdata.open = false
}

defineExpose({ showModal })
</script>
<style lang="less" scoped>
.describe {
  img {
    width: 30px;
    height: 25px;
  }
}
</style>
