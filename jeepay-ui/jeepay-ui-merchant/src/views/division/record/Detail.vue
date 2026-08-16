<!-- 详情抽屉 -->
<template>
  <a-drawer
    width="50%"
    :closable="true"
    v-model:open="vdata.visible"
    title="記錄詳情"
    @close="vdata.visible = false"
  >
    <a-row justify="space-between" type="flex">
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳記錄ID">
            {{ vdata.detailData.recordId }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="商戶號">{{ vdata.detailData.mchNo }}</a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="應用ID">{{ vdata.detailData.appId }}</a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="支付接口代碼">
            {{ vdata.detailData.ifCode }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="系統支付訂單號">
            {{ vdata.detailData.payOrderId }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="支付訂單渠道支付訂單號">
            {{ vdata.detailData.payOrderChannelOrderNo }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="訂單金額">
            {{ vdata.detailData.payOrderAmount / 100 }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳基數">
            {{ vdata.detailData.payOrderDivisionAmount / 100 }} （訂單金額-手續費-退款金額）
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="系統分帳批次號">
            {{ vdata.detailData.batchOrderId }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="上游分帳批次號">
            {{ vdata.detailData.channelBatchOrderId }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="狀態">
            <a-tag v-if="vdata.detailData.state === 0" :key="vdata.detailData.state" color="orange">
              分帳中
            </a-tag>
            <a-tag v-if="vdata.detailData.state === 1" :key="vdata.detailData.state" color="blue">
              分帳成功
            </a-tag>
            <a-tag
              v-if="vdata.detailData.state === 2"
              :key="vdata.detailData.state"
              color="volcano"
            >
              分帳失敗
            </a-tag>
            <a-tag v-if="vdata.detailData.state === 3" :key="vdata.detailData.state" color="purple">
              已受理
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳接收者ID">
            {{ vdata.detailData.receiverId }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="收款帳號組ID">
            {{ vdata.detailData.receiverGroupId }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="收款帳號別名">
            {{ vdata.detailData.receiverAlias }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳接收帳號類型">
            {{ vdata.detailData.accType == 0 ? '個人' : '商戶' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳接收帳號">
            {{ vdata.detailData.accNo }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳接收帳號名稱">
            {{ vdata.detailData.accName }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳關係類型">
            {{ vdata.detailData.relationType }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳關係類型名稱">
            {{ vdata.detailData.relationTypeName }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="實際分帳比例">
            {{ (vdata.detailData.divisionProfit * 100).toFixed(2) }}%
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="分帳金額">
            {{ vdata.detailData.calDivisionAmount / 100 }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="建立時間">
            {{ vdata.detailData.createdAt }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item label="更新時間">
            {{ vdata.detailData.updatedAt }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
    </a-row>
    <a-divider />
    <a-row justify="start" type="flex">
      <a-col :sm="24">
        <a-form-item label="上游返回資料包">
          <a-textarea
            disabled="disabled"
            style="height: 100px; color: black"
            v-model:value="vdata.detailData.channelRespResult"
          />
        </a-form-item>
      </a-col>
    </a-row>
  </a-drawer>
</template>
<script lang="ts" setup>
import { API_URL_PAY_ORDER_DIVISION_RECORD_LIST, req } from '@/api/manage'
import { reactive } from 'vue'

const vdata: any = reactive({
  visible: false,
  detailData: {},
})

function show(recordId) {
  req.getById(API_URL_PAY_ORDER_DIVISION_RECORD_LIST, recordId).then((res) => {
    vdata.detailData = res
  })
  vdata.visible = true
}

defineExpose({ show })
</script>
