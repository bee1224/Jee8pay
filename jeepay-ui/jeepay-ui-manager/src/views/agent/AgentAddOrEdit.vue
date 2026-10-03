<template>
  <a-drawer
    :maskClosable="false"
    v-model:open="vdata.open"
    :title="vdata.isAdd ? '新增代理' : '修改代理'"
    @close="onClose"
    :body-style="{ paddingBottom: '80px' }"
    width="40%"
  >
    <a-form ref="infoFormModel" :model="vdata.saveObject" layout="vertical" :rules="vdata.rules">
      <a-row justify="space-between" type="flex">
        <a-col :span="10">
          <a-form-item label="代理名稱" name="agentName">
            <a-input placeholder="請輸入代理名稱" v-model:value="vdata.saveObject.agentName" />
          </a-form-item>
        </a-col>
        <a-col :span="10">
          <a-form-item label="代理層級" name="agentLevel">
            <!-- 層級與上級建立後不可變更（物化路徑一致性） -->
            <a-radio-group v-model:value="vdata.saveObject.agentLevel" :disabled="!vdata.isAdd">
              <a-radio :value="1">高級代理</a-radio>
              <a-radio :value="2">一般代理</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row v-if="vdata.saveObject.agentLevel === 2" justify="space-between" type="flex">
        <a-col :span="22">
          <a-form-item label="上級高級代理" name="parentAgentNo">
            <a-select
              v-model:value="vdata.saveObject.parentAgentNo"
              placeholder="請選擇上級高級代理"
              :disabled="!vdata.isAdd"
              show-search
              option-filter-prop="label"
              :options="vdata.seniorOptions"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row v-if="vdata.isAdd" justify="space-between" type="flex">
        <a-col :span="22">
          <a-form-item label="登入帳號" name="loginUsername" extra="代理登入營運平台用的帳號；儲存後系統產生 8 碼隨機密碼並顯示一次。">
            <a-input placeholder="4～32 碼英文、數字或底線" v-model:value="vdata.saveObject.loginUsername" autocomplete="off" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row justify="space-between" type="flex">
        <a-col :span="10">
          <a-form-item label="聯絡人姓名" name="contactName">
            <a-input placeholder="請輸入聯絡人姓名" v-model:value="vdata.saveObject.contactName" />
          </a-form-item>
        </a-col>
        <a-col :span="10">
          <a-form-item label="聯絡人手機號" name="contactTel">
            <a-input placeholder="09 開頭 10 碼" v-model:value="vdata.saveObject.contactTel" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row justify="space-between" type="flex">
        <a-col :span="10">
          <a-form-item label="聯絡人信箱" name="contactEmail">
            <a-input placeholder="請輸入聯絡人信箱" v-model:value="vdata.saveObject.contactEmail" />
          </a-form-item>
        </a-col>
        <a-col :span="10">
          <a-form-item label="狀態" name="state">
            <a-radio-group v-model:value="vdata.saveObject.state">
              <a-radio :value="1">啟用</a-radio>
              <a-radio :value="0">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="備註" name="remark">
        <a-textarea v-model:value="vdata.saveObject.remark" placeholder="請輸入備註" />
      </a-form-item>
    </a-form>
    <div class="drawer-btn-center">
      <a-button @click="onClose" style="margin-right: 8px">取消</a-button>
      <a-button type="primary" @click="handleOkFunc" :loading="vdata.btnLoading">儲存</a-button>
    </div>
  </a-drawer>
  <AgentCredentialModal ref="credentialModal" />
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, req } from '@/api/manage'
import { reactive, ref, getCurrentInstance } from 'vue'
import AgentCredentialModal from './AgentCredentialModal.vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const props = defineProps({
  callbackFunc: { type: Function, default: () => {} },
})

const infoFormModel = ref()
const credentialModal = ref()

const vdata: any = reactive({
  btnLoading: false,
  isAdd: true,
  saveObject: {},
  recordId: null,
  open: false,
  seniorOptions: [],
  rules: {
    agentName: [{ required: true, message: '請輸入代理名稱', trigger: 'blur' }],
    agentLevel: [{ required: true, message: '請選擇代理層級', trigger: 'change' }],
    parentAgentNo: [{ required: true, message: '請選擇上級高級代理', trigger: 'change' }],
    loginUsername: [{ required: true, pattern: /^[A-Za-z0-9_]{4,32}$/, message: '請輸入 4～32 碼英文、數字或底線', trigger: 'blur' }],
    // 新增時必填：登入帳號需要綁定手機號
    contactTel: [{ required: true, pattern: /^09\d{8}$/, message: '請輸入正確的手機號（09 開頭 10 碼）', trigger: 'blur' }],
    contactEmail: [
      { required: false, pattern: /^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z0-9]{2,6}$/, message: '請輸入正確的信箱', trigger: 'blur' },
    ],
  },
})

function loadSeniorOptions() {
  req.list(API_URL_AGENT_INFO, { agentLevel: 1, pageSize: -1 }).then((res) => {
    vdata.seniorOptions = (res.records || []).map((a) => ({
      value: a.agentNo,
      label: `${a.agentName}（${a.agentNo}）${a.state === 0 ? '［停用］' : ''}`,
      disabled: a.state === 0,
    }))
  })
}

function show(recordId) {
  vdata.isAdd = !recordId
  vdata.rules.contactTel[0].required = vdata.isAdd
  vdata.saveObject = { state: 1, agentLevel: 1 }
  if (infoFormModel.value) {
    infoFormModel.value.resetFields()
  }
  loadSeniorOptions()
  if (!vdata.isAdd) {
    vdata.recordId = recordId
    req.getById(API_URL_AGENT_INFO, recordId).then((res) => {
      vdata.saveObject = res
    })
  }
  vdata.open = true
}

function handleOkFunc() {
  infoFormModel.value.validate().then(() => {
    vdata.btnLoading = true
    const done = (msg) => {
      $infoBox.message.success(msg)
      vdata.open = false
      props.callbackFunc()
    }
    const request = vdata.isAdd
      ? req.add(API_URL_AGENT_INFO, vdata.saveObject).then((res) => {
          done('新增成功')
          // 後端有開通登入帳號時回傳一次性初始密碼
          if (res && res.initPassword) credentialModal.value.show(res)
        })
      : req.updateById(API_URL_AGENT_INFO, vdata.recordId, vdata.saveObject).then(() => done('修改成功'))
    request.finally(() => {
      vdata.btnLoading = false
    })
  })
}

function onClose() {
  vdata.open = false
}

defineExpose({ show })
</script>
