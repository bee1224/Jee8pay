<template>
  <a-drawer
    v-model:open="vdata.open"
    :title="vdata.isAdd ? $t('新增應用') : $t('修改應用')"
    width="40%"
    :maskClosable="false"
    @close="onClose"
  >
    <a-form ref="infoFormModel" :model="vdata.saveObject" layout="vertical" :rules="vdata.rules">
      <a-row :gutter="16">
        <a-col :span="12" v-if="!vdata.isAdd">
          <a-form-item :label="$t('應用 AppId')" prop="appId">
            <a-input
              v-model:value="vdata.saveObject.appId"
              :placeholder="$t('請輸入')"
              :disabled="!vdata.isAdd"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('應用名稱')" prop="appName">
            <a-input v-model:value="vdata.saveObject.appName" :placeholder="$t('請輸入')" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('狀態')" prop="state">
            <a-radio-group v-model:value="vdata.saveObject.state">
              <a-radio :value="1">{{ $t('啟用') }}</a-radio>
              <a-radio :value="0">{{ $t('停用') }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('商戶公鑰（ML-DSA-65）')" name="mchPublicKey">
            <a-textarea
              v-model:value="vdata.saveObject.mchPublicKey"
              :auto-size="{ minRows: 4, maxRows: 10 }"
              style="font-family: monospace; font-size: 12px"
              :placeholder="'-----BEGIN PUBLIC KEY-----\n' + $t('商戶自行產生 ML-DSA-65 金鑰對後，只貼公鑰；私鑰留在商戶系統，不要交給任何人') + '\n-----END PUBLIC KEY-----'"
            />
            <div style="color: rgba(0, 0, 0, 0.45); margin-top: 4px">
              {{ $t('商戶 API 一律以 ML-DSA-65 簽名（signType=ML-DSA-65）。沒有公鑰的應用無法呼叫 API；修改時留空表示不變更。') }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('平台公鑰（交給商戶，用來驗證平台回應與通知）')">
            <a-textarea
              :value="vdata.platformPublicKey"
              readonly
              :auto-size="{ minRows: 4, maxRows: 10 }"
              style="font-family: monospace; font-size: 12px"
              :placeholder="$t('系統尚未設定平台簽名公鑰')"
            />
            <a-button style="margin-top: 10px" :disabled="!vdata.platformPublicKey" @click="copyPlatformKey">
              {{ $t('複製平台公鑰') }}
            </a-button>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('備註')" prop="remark">
            <a-input v-model:value="vdata.saveObject.remark" :placeholder="$t('請輸入')" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <div class="drawer-btn-center">
      <a-button @click="onClose" :style="{ marginRight: '8px' }">{{ $t('取消') }}</a-button>
      <a-button type="primary" @click="onSubmit">{{ $t('儲存') }}</a-button>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { API_URL_MCH_APP, req } from '@/api/manage'
import { reactive, ref, getCurrentInstance } from 'vue'
import { t } from '@/i18n'
const { $infoBox, $access, $hasAgentEnt, $SYS_NAME_MAP } =
  getCurrentInstance()!.appContext.config.globalProperties

const props: any = defineProps({
  callbackFunc: { type: Function, default: () => {} },
})

const infoFormModel = ref()

const vdata: any = reactive({
  isAdd: true, // 新增 or 修改
  open: false, // 抽屉开关
  appId: '', // 应用AppId
  saveObject: {}, // 数据对象
  rules: {
    appName: [{ required: true, message: t('請輸入應用名稱'), trigger: 'blur' }],
  },
})

// 抽屉显示
function show(appId) {
  vdata.isAdd = !appId
  // 数据清空
  vdata.saveObject = {
    state: 1,
    mchPublicKey: '',
  }

  if (infoFormModel.value) {
    infoFormModel.value.resetFields()
  }

  loadPlatformKey()
  if (!vdata.isAdd) {
    // 修改信息 延迟展示弹层
    vdata.appId = appId
    // 拉取详情
    req.getById(API_URL_MCH_APP, appId).then((res) => {
      vdata.saveObject = res
    })
    vdata.open = true
  } else {
    vdata.open = true // 展示弹层信息
  }
}
// 表单提交
function onSubmit() {
  infoFormModel.value.validate().then((valid) => {
    if (valid) {
      // 验证通过
      // 请求接口
      if (vdata.isAdd) {
        req.add(API_URL_MCH_APP, vdata.saveObject).then((res) => {
          $infoBox.message.success(t('新增成功'))
          vdata.open = false
          props.callbackFunc() // 刷新列表
        })
      } else {
        if (!vdata.saveObject.mchPublicKey) {
          delete vdata.saveObject.mchPublicKey
        }
        req.updateById(API_URL_MCH_APP, vdata.appId, vdata.saveObject).then((res) => {
          $infoBox.message.success(t('修改成功'))
          vdata.open = false
          props.callbackFunc() // 刷新列表
        })
      }
    }
  })
}
// 平台公鑰（唯讀，交給商戶驗證平台回應與通知；ADR-0013）
function loadPlatformKey() {
  vdata.platformPublicKey = ''
  req.getById(API_URL_MCH_APP, 'platformPublicKey').then((res) => {
    vdata.platformPublicKey = res.platformPublicKey
  })
}
function copyPlatformKey() {
  navigator.clipboard
    .writeText(vdata.platformPublicKey)
    .then(() => $infoBox.message.success(t('已複製平台公鑰')))
    .catch(() => $infoBox.message.warning(t('無法自動複製，請手動選取文字複製')))
}
function onClose() {
  vdata.open = false
}

defineExpose({ show })
</script>

<style lang="less" scoped></style>
