<template>
  <a-modal v-model:open="vdata.open" :title="'代理綁定：' + vdata.mchNo" :confirm-loading="vdata.btnLoading" @ok="saveFunc" ok-text="儲存">
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="直屬代理決定這個商戶的團長費與隊長費歸誰。"
    />
    <a-form layout="vertical">
      <a-form-item label="直屬代理（必填）">
        <a-select
          v-model:value="vdata.agentNo"
          placeholder="請選擇直屬代理"
          show-search
          option-filter-prop="label"
          :options="vdata.agentOptions"
        />
      </a-form-item>
    </a-form>
    <a-button v-if="vdata.bound" danger @click="unbindFunc">解除綁定</a-button>
  </a-modal>
</template>

<script setup lang="ts">
import { API_URL_AGENT_INFO, API_URL_AGENT_MCH_RELA, req } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const props = defineProps({
  callbackFunc: { type: Function, default: () => {} },
})

const vdata: any = reactive({
  open: false,
  btnLoading: false,
  mchNo: '',
  agentNo: undefined,
  bound: false,
  agentOptions: [],
})

function label(a) {
  return `${a.agentLevel === 1 ? '［高級］' : '［一般］'}${a.agentName}（${a.agentNo}）`
}

function show(mchNo) {
  vdata.mchNo = mchNo
  vdata.agentNo = undefined
  vdata.bound = false
  vdata.open = true
  req.list(API_URL_AGENT_INFO, { pageSize: -1 }).then((res) => {
    const agents = res.records || []
    vdata.agentOptions = agents.map((a) => ({ value: a.agentNo, label: label(a), disabled: a.state === 0 }))
  })
  req.getById(API_URL_AGENT_MCH_RELA, mchNo).then((res) => {
    if (res) {
      vdata.bound = true
      vdata.agentNo = res.agentNo
    }
  })
}

function saveFunc() {
  if (!vdata.agentNo) {
    $infoBox.message.warning('請選擇直屬代理')
    return
  }
  vdata.btnLoading = true
  req
    .updateById(API_URL_AGENT_MCH_RELA, vdata.mchNo, { agentNo: vdata.agentNo })
    .then(() => {
      $infoBox.message.success('綁定成功')
      vdata.open = false
      props.callbackFunc()
    })
    .finally(() => {
      vdata.btnLoading = false
    })
}

function unbindFunc() {
  $infoBox.confirmDanger('確認解除綁定？', '解除後該商戶不再計算團長費與隊長費', () => {
    req.delById(API_URL_AGENT_MCH_RELA, vdata.mchNo).then(() => {
      $infoBox.message.success('已解除綁定')
      vdata.open = false
      props.callbackFunc()
    })
  })
}

defineExpose({ show })
</script>
