<template>
  <a-modal
    v-model:open="vdata.isShow"
    title="支付寶子商戶掃碼授權"
    @ok="handleOkFunc"
    @cancel="handleOkFunc"
  >
    <div style="text-align: center">
      <p>方式1： <br>  請商家登入【支付寶】APP, 掃描如下QR Code, 按提示授權： </p>
      <img style="margin-bottom: 10px" :src="vdata.apiResData.authQrImgUrl">
      <hr>

      <p style="margin-top: 10px">
        方式2： <br> <a-button v-clipboard:copy="vdata.apiResData.authUrl" v-clipboard:success="onCopySuccess" size="small" class="copy-btn">點選複製</a-button>
        連結並發送給商戶，商戶進入連結，按照頁面提示自主授權：
      </p>
      <a target="_blank" :href="vdata.apiResData.authUrl">{{ vdata.apiResData.authUrl }}</a>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { queryAlipayIsvsubMchAuthUrl } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

  const props = defineProps({
    callbackFunc: { type: Function, default: () => {} }
  })

  const vdata:any = reactive({
    isShow: false, // 是否显示弹层/抽屉
    appId: '',
    apiResData: {}
  })
  
  function show (appId) { // 弹层打开事件
    vdata.apiResData = {}
    vdata.appId = appId
    queryAlipayIsvsubMchAuthUrl(appId).then(res => {
      vdata.apiResData = res
      vdata.isShow = true
    })
  }

  function onCopySuccess () {
    $infoBox.message.success('複製成功')
  }

  function handleOkFunc() { // 点击【确认】按钮事件
    vdata.isShow = false
    if (props.callbackFunc) {
      props.callbackFunc()
    }
  }

  defineExpose({ show })
</script>
