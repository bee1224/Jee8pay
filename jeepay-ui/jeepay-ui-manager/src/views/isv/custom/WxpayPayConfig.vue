<template>
  <a-drawer
    title="填寫參數"
    width="40%"
    :closable="true"
    :maskClosable="false"
    v-model:open="vdata.open"
    :body-style="{ paddingBottom: '80px' }"
    @close="onClose"
  >
    <a-form ref="infoFormModel" :model="vdata.saveObject" layout="vertical" :rules="vdata.rules">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="支付介面費率" name="ifRate">
            <a-input v-model:value="vdata.saveObject.ifRate" placeholder="請輸入" suffix="%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="狀態" name="state">
            <a-radio-group v-model:value="vdata.saveObject.state">
              <a-radio :value="1">啟用</a-radio>
              <a-radio :value="0">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="備註" name="remark">
            <a-textarea v-model:value="vdata.saveObject.remark" placeholder="請輸入" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <a-divider orientation="left">
      <a-tag color="#FF4B33">{{ vdata.saveObject.ifCode }} 服務商參數設定</a-tag>
    </a-divider>
    <a-form
      ref="isvParamFormModel"
      :model="vdata.ifParams"
      layout="vertical"
      :rules="ifParamsRules"
    >
      <a-row :gutter="16">
        <a-col span="12">
          <a-form-item label="微信支付商戶號" name="mchId">
            <a-input v-model:value="vdata.ifParams.mchId" placeholder="請輸入" />
          </a-form-item>
        </a-col>
        <a-col span="12">
          <a-form-item label="應用AppID" name="appId">
            <a-input v-model:value="vdata.ifParams.appId" placeholder="請輸入" />
          </a-form-item>
        </a-col>
        <a-col span="12">
          <a-form-item label="應用AppSecret" name="appSecret">
            <a-input
              v-model:value="vdata.ifParams.appSecret"
              :placeholder="vdata.ifParams.appSecret_ph"
            />
          </a-form-item>
        </a-col>
        <a-col span="12">
          <a-form-item label="oauth2地址（置空將使用官方）" name="oauth2Url">
            <a-input v-model:value="vdata.ifParams.oauth2Url" placeholder="請輸入" />
          </a-form-item>
        </a-col>
        <a-col span="12">
          <a-form-item label="微信支付API版本" name="apiVersion">
            <a-radio-group v-model:value="vdata.ifParams.apiVersion" defaultValue="V2">
              <a-radio value="V2">V2</a-radio>
              <a-radio value="V3">V3</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col span="24">
          <a-form-item label="APIv2金鑰" name="key">
            <a-textarea v-model:value="vdata.ifParams.key" :placeholder="vdata.ifParams.key_ph" />
          </a-form-item>
        </a-col>
        <a-col span="24">
          <a-form-item label="APIv3金鑰" name="apiV3Key">
            <a-textarea
              v-model:value="vdata.ifParams.apiV3Key"
              :placeholder="vdata.ifParams.apiV3Key_ph"
            />
          </a-form-item>
        </a-col>
        <a-col span="24">
          <a-form-item label="序列號" name="serialNo">
            <a-textarea
              v-model:value="vdata.ifParams.serialNo"
              :placeholder="vdata.ifParams.serialNo_ph"
            />
          </a-form-item>
        </a-col>
        <a-col span="24">
          <a-form-item label="API證書(apiclient_cert.p12)" name="cert" class="margin-botomt-5">
            <a-input v-model:value="vdata.ifParams.cert" disabled="disabled" />
          </a-form-item>
          <JeepayUpload
            :action="vdata.action"
            :fileUrl="vdata.ifParams.cert"
            @uploadSuccess="uploadSuccess($event, 'cert')"
          >
            <template #uploadSlot="{ loading }">
              <a-button style="margin-top: 5px">
                <a-icon :type="vdata.loading ? 'loading' : 'upload'" />
                {{ vdata.loading ? '正在上傳' : '點選上傳' }}
              </a-button>
            </template>
          </JeepayUpload>
        </a-col>
        <a-col span="24">
          <a-form-item
            label="證書檔案(apiclient_cert.pem)"
            name="apiClientCert"
            class="margin-botomt-5"
          >
            <a-input v-model:value="vdata.ifParams.apiClientCert" disabled="disabled" />
            <JeepayUpload
              :action="vdata.action"
              :fileUrl="vdata.ifParams.apiClientCert"
              @uploadSuccess="uploadSuccess($event, 'apiClientCert')"
            >
              <template #uploadSlot="{ loading }">
                <a-button style="margin-top: 5px">
                  <a-icon :type="vdata.loading ? 'loading' : 'upload'" />
                  {{ vdata.loading ? '正在上傳' : '點選上傳' }}
                </a-button>
              </template>
            </JeepayUpload>
          </a-form-item>
        </a-col>
        <a-col span="24">
          <a-form-item
            label="私鑰檔案(apiclient_key.pem)"
            name="apiClientKey"
            class="margin-botomt-5"
          >
            <a-input v-model:value="vdata.ifParams.apiClientKey" disabled="disabled" />
            <JeepayUpload
              :action="vdata.action"
              :fileUrl="vdata.ifParams.apiClientKey"
              @uploadSuccess="uploadSuccess($event, 'apiClientKey')"
            >
              <template #uploadSlot="{ loading }">
                <a-button style="margin-top: 5px">
                  <a-icon :type="vdata.loading ? 'loading' : 'upload'" />
                  {{ vdata.loading ? '正在上傳' : '點選上傳' }}
                </a-button>
              </template>
            </JeepayUpload>
          </a-form-item>
        </a-col>
        <a-col span="24">
          <a-form-item label="微信側公鑰ID" name="wxpayPublicKeyId">
            <a-input v-model:value="vdata.ifParams.wxpayPublicKeyId" placeholder="請輸入" />
          </a-form-item>
        </a-col>
        <a-col span="24">
          <a-form-item label="微信側公鑰證書（pub_key.pem）" name="wxpayPublicKey" class="margin-botomt-5">
            <a-input v-model:value="vdata.ifParams.wxpayPublicKey" disabled="disabled" />
            <JeepayUpload
              :action="vdata.action"
              :fileUrl="vdata.ifParams.wxpayPublicKey"
              @uploadSuccess="uploadSuccess($event, 'wxpayPublicKey')"
            >
              <template #uploadSlot="{ loading }">
                <a-button style="margin-top: 5px">
                  <a-icon :type="vdata.loading ? 'loading' : 'upload'" />
                  {{ vdata.loading ? '正在上傳' : '點選上傳' }}
                </a-button>
              </template>
            </JeepayUpload>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <div class="drawer-btn-center" v-if="$access('ENT_MCH_PAY_CONFIG_ADD')">
      <a-button :style="{ marginRight: '8px' }" @click="onClose">取消</a-button>
      <a-button type="primary" @click="onSubmit" :loading="vdata.btnLoading">儲存</a-button>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { API_URL_ISV_PAYCONFIGS_LIST, req, getIsvPayConfigUnique, upload } from '@/api/manage'
import { reactive, ref, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const props = defineProps({
  callbackFunc: { type: Function, default: () => ({}) },
})

const vdata: any = reactive({
  btnLoading: false,
  open: false, // 抽屉开关
  isAdd: true,
  action: upload.cert, // 上传文件地址
  saveObject: {}, // 保存的对象
  ifParams: { apiVersion: 'V2' }, // 参数配置对象
  rules: {
    ifRate: [
      {
        required: false,
        pattern: /^(([1-9]{1}\d{0,1})|(0{1}))(\.\d{1,4})?$/,
        message: '請輸入0-100之間的數字，最多四位小數',
        trigger: 'blur',
      },
    ],
  },
})

const ifParamsRules = reactive({
  mchId: [{ required: true, message: '請輸入微信支付商戶號', trigger: 'blur' }],
  appId: [{ required: true, message: '請輸入應用AppID', trigger: 'blur' }],
  appSecret: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (vdata.isAdd && !value) {
          return Promise.reject('請輸入應用AppSecret')
        }
        return Promise.resolve()
      },
    },
  ],
  key: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (vdata.ifParams.apiVersion === 'V2' && vdata.isAdd && !value) {
          return Promise.reject('請輸入API金鑰')
        }
        return Promise.resolve()
      },
    },
  ],
  apiV3Key: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (vdata.ifParams.apiVersion === 'V3' && vdata.isAdd && !value) {
          return Promise.reject('請輸入API V3金鑰')
        }
        return Promise.resolve()
      },
    },
  ],
  serialNo: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (vdata.ifParams.apiVersion === 'V3' && vdata.isAdd && !value) {
          return Promise.reject('請輸入序列號')
        }
        return Promise.resolve()
      },
    },
  ],
  cert: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (vdata.ifParams.apiVersion === 'V3' && vdata.isAdd && !value) {
          return Promise.reject('請上傳API證書(apiclient_cert.p12)')
        }
        return Promise.resolve()
      },
    },
  ],
  apiClientCert: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (vdata.ifParams.apiVersion === 'V3' && vdata.isAdd && !value) {
          return Promise.reject('請上傳證書檔案(apiclient_cert.pem)')
        }
        return Promise.resolve()
      },
    },
  ],
  apiClientKey: [
    {
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (vdata.ifParams.apiVersion === 'V3' && !value) {
          return Promise.reject('請上傳私鑰檔案(apiclient_key.pem)')
        }
        return Promise.resolve()
      },
    },
  ],
})

const infoFormModel = ref()
const isvParamFormModel = ref()

// 弹层打开事件
function show(isvNo, record) {
  if (infoFormModel.value) {
    infoFormModel.value.resetFields()
  }
  if (isvParamFormModel.value) {
    isvParamFormModel.value.resetFields()
  }

  // 数据初始化
  vdata.saveObject = {
    infoId: isvNo,
    ifCode: record.ifCode,
    state: record.ifConfigState === 0 ? 0 : 1,
  }

  // 参数配置对象，数据初始化
  vdata.ifParams = {
    apiVersion: 'V2',
    appSecret: '',
    appSecret_ph: '請輸入',
    key: '',
    key_ph: '請輸入',
    apiV3Key: '',
    apiV3Key_ph: '請輸入',
    serialNo: '',
    serialNo_ph: '請輸入',
    wxpayPublicKeyId: '',
    wxpayPublicKey: ''
  }
  vdata.open = true
  getIsvPayConfig()
}
// 支付参数配置
function getIsvPayConfig() {
  // 获取支付参数
  getIsvPayConfigUnique(vdata.saveObject.infoId, vdata.saveObject.ifCode).then((res) => {
    if (res && res.ifParams) {
      vdata.saveObject = res
      vdata.ifParams = JSON.parse(res.ifParams)

      vdata.ifParams.appSecret_ph = vdata.ifParams.appSecret
      vdata.ifParams.appSecret = ''

      vdata.ifParams.key_ph = vdata.ifParams.key
      vdata.ifParams.key = ''

      vdata.ifParams.apiV3Key_ph = vdata.ifParams.apiV3Key
      vdata.ifParams.apiV3Key = ''

      vdata.ifParams.serialNo_ph = vdata.ifParams.serialNo
      vdata.ifParams.serialNo = ''

      vdata.isAdd = false
    } else if (res === undefined) {
      vdata.isAdd = true
    }
  })
}
// 表单提交
function onSubmit() {
  infoFormModel.value.validate().then((valid) => {
    isvParamFormModel.value.validate().then((valid2) => {
      if (valid && valid2) {
        // 验证通过
        const reqParams: any = {}
        reqParams.infoId = vdata.saveObject.infoId
        reqParams.ifCode = vdata.saveObject.ifCode
        reqParams.ifRate = vdata.saveObject.ifRate
        reqParams.state = vdata.saveObject.state
        reqParams.remark = vdata.saveObject.remark
        // 支付参数配置不能为空
        if (Object.keys(vdata.ifParams).length === 0) {
          $infoBox.message.error('參數不能為空！')
          return
        }
        // 脱敏数据为空时，删除该key
        clearEmptyKey('appSecret')
        clearEmptyKey('key')
        clearEmptyKey('apiV3Key')
        clearEmptyKey('serialNo')
        reqParams.ifParams = JSON.stringify(vdata.ifParams)
        // 请求接口
        if (Object.keys(reqParams).length === 0) {
          $infoBox.message.error('參數不能為空！')
          return
        }

        vdata.btnLoading = true

        req
          .add(API_URL_ISV_PAYCONFIGS_LIST, reqParams)
          .then((res) => {
            $infoBox.message.success('儲存成功')
            vdata.open = false
            props.callbackFunc()
          })
          .catch(() => {
            $infoBox.message.error('儲存失敗，請重試')
          })
          .finally(() => {
            vdata.btnLoading = false
          })
      }
    })
  })
}
// 脱敏数据为空时，删除对应key
function clearEmptyKey(key) {
  if (!vdata.ifParams[key]) {
    vdata.ifParams[key] = undefined
  }
  vdata.ifParams[key + '_ph'] = undefined
}
// 上传文件成功回调方法，参数value为文件地址，name是自定义参数
function uploadSuccess(value, name) {
  vdata.ifParams[name] = value
  // this.$forceUpdate()
}
function onClose() {
  vdata.open = false
}

defineExpose({ show })
</script>
<style lang="less" scoped></style>
