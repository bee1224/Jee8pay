<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <a-drawer
    v-model:open="vdata.open"
    :title="true ? '商戶詳情' : ''"
    :body-style="{ paddingBottom: '80px' }"
    width="40%"
    @close="onClose"
  >
    <a-row justify="space-between" type="flex">
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="商戶號">
            {{ vdata.detailData.mchNo }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="商戶名稱">
            {{ vdata.detailData.mchName }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="登入名">
            {{ vdata.detailData.loginUserName }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="商戶簡稱">
            {{ vdata.detailData.mchShortName }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col v-if="vdata.detailData.type === 2" :sm="12">
        <a-descriptions>
          <a-descriptions-item label="服務商號">
            {{ vdata.detailData.isvNo }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col v-if="vdata.detailData.type === 2" :sm="12">
        <a-descriptions>
          <a-descriptions-item label="服務商名稱">
            {{ vdata.isvName }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="聯絡人姓名">
            {{ vdata.detailData.contactName }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="商戶類型">
            {{ vdata.detailData.type === 1 ? '普通商戶' : '特約商戶' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="聯絡人手機號">
            {{ vdata.detailData.contactTel }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="狀態">
            <a-tag :color="vdata.detailData.state === 1 ? 'green' : 'volcano'">
              {{
                vdata.detailData.state === 0
                  ? '停用'
                  : vdata.detailData.state === 1
                    ? '啟用'
                    : '未知'
              }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="聯絡人郵箱">
            {{ vdata.detailData.contactEmail }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
    </a-row>
    <a-row justify="start" type="flex">
      <a-col :sm="24">
        <a-form-item label="備註">
          <a-textarea
            v-model:value="vdata.detailData.remark"
            disabled="disabled"
            style="height: 50px"
          />
        </a-form-item>
      </a-col>
    </a-row>
  </a-drawer>
</template>

<script lang="ts" setup>
import { API_URL_MCH_LIST, API_URL_ISV_LIST, req } from '@/api/manage'
import { reactive, ref } from 'vue'

const vdata: any = reactive({
  btnLoading: false,
  detailData: {}, // 数据对象
  recordId: null, // 更新对象ID
  open: false, // 是否显示弹层/抽屉
  isvList: null, // 服务商下拉列表
  isvName: '', // 服务商名称
})

function show(recordId) {
  // 弹层打开事件
  vdata.detailData = { state: 1, type: 1 } // 数据清空

  vdata.recordId = recordId
  req.getById(API_URL_MCH_LIST, recordId).then((res) => {
    vdata.detailData = res
  })

  req.list(API_URL_ISV_LIST, { pageSize: null }).then((res) => {
    // 服务商下拉选择列表
    vdata.isvList = res.records
    for (let i = 0; i < vdata.isvList.length; i++) {
      if (vdata.detailData.isvNo === vdata.isvList[i].isvNo) {
        vdata.isvName = vdata.isvList[i].isvName
      }
    }
  })

  vdata.open = true
}

function onClose() {
  vdata.open = false
}

defineExpose({ show })
</script>
