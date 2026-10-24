<!-- 订单详情抽屉 -->
<template>
  <a-drawer
    width="50%"
    placement="right"
    :closable="true"
    v-model:open="vdata.isShow"
    :title="$t('轉帳訂單詳情')"
    @close="vdata.isShow = false"
  >
    <a-row justify="space-between" type="flex">
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('商戶類型')">
            {{
              vdata.detailData.mchType === 1
                ? $t('普通商戶')
                : vdata.detailData.mchType === 2
                  ? $t('特約商戶')
                  : $t('未知')
            }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('商戶號')">{{ vdata.detailData.mchNo }}</a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('商戶名稱')">{{ vdata.detailData.mchName }}</a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('應用APPID')">{{ vdata.detailData.appId }}</a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('服務商號')">{{ vdata.detailData.isvNo }}</a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('轉帳訂單號')">
            <a-tag color="purple">{{ vdata.detailData.transferId }}</a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-col>

      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('商戶轉帳單號')">
            {{ vdata.detailData.mchOrderNo }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('渠道訂單號')">
            {{ vdata.detailData.channelOrderNo }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>

      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('金額')">
            <a-tag color="green">{{ vdata.detailData.amount / 100 }}</a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('貨幣代碼')">
            {{ vdata.detailData.currency }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('收款帳號')">
            <a-tag color="green">{{ vdata.detailData.accountNo }}</a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('收款人姓名')">
            {{ vdata.detailData.accountName }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('轉帳備註')">
            {{ vdata.detailData.transferDesc }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('訂單狀態')">
            <a-tag
              :color="
                vdata.detailData.state === 0
                  ? 'blue'
                  : vdata.detailData.state === 1
                    ? 'orange'
                    : vdata.detailData.state === 2
                      ? 'green'
                      : 'volcano'
              "
            >
              {{
                vdata.detailData.state === 0
                  ? $t('訂單生成')
                  : vdata.detailData.state === 1
                    ? $t('轉帳中')
                    : vdata.detailData.state === 2
                      ? $t('轉帳成功')
                      : vdata.detailData.state === 3
                        ? $t('轉帳失敗')
                        : vdata.detailData.state === 4
                          ? $t('任務關閉')
                          : $t('未知')
              }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('轉帳成功時間')">
            {{ vdata.detailData.successTime }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('建立時間')">
            {{ vdata.detailData.createdAt }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('更新時間')">
            {{ vdata.detailData.updatedAt }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-divider />
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('介面代碼')">{{ vdata.detailData.ifCode }}</a-descriptions-item>
        </a-descriptions>
      </a-col>

      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('入帳類型')">
            {{ vdata.detailData.entryType }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12">
        <a-descriptions>
          <a-descriptions-item :label="$t('客戶端IP')">
            {{ vdata.detailData.clientIp }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="24">
        <a-descriptions>
          <a-descriptions-item :label="$t('異步通知地址')">
            {{ vdata.detailData.notifyUrl }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
    </a-row>
    <a-row justify="space-between" type="flex" v-if="vdata.userH5ConfirmUrl && vdata.userH5ConfirmQrImgUrl">
      <a-divider />
      <a-col :sm="24" >
        <a-descriptions>
          <a-descriptions-item :label="$t('領取連結')">
            {{ vdata.userH5ConfirmUrl }}
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :sm="12" >
        <a-descriptions>
          <a-descriptions-item :label="$t('領取QR Code')">
            <div style="width: 100%; text-align: center">
              <img :src="vdata.userH5ConfirmQrImgUrl" alt="" />
              <p>{{ $t('請使用微信掃碼領取') }}</p>
            </div>
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
    </a-row>
    
    <a-divider />
    <a-col :sm="12">
      <a-descriptions>
        <a-descriptions-item :label="$t('渠道訂單號')">
          {{ vdata.detailData.channelOrderNo }}
        </a-descriptions-item>
      </a-descriptions>
    </a-col>
    <a-col :sm="12">
      <a-descriptions>
        <a-descriptions-item :label="$t('渠道錯誤碼')">{{ vdata.detailData.errCode }}</a-descriptions-item>
      </a-descriptions>
    </a-col>
    <a-col :sm="12">
      <a-descriptions>
        <a-descriptions-item :label="$t('渠道錯誤描述')">
          {{ vdata.detailData.errMsg }}
        </a-descriptions-item>
      </a-descriptions>
    </a-col>
    <a-form layout="vertical">
      <a-col :sm="24">
        <a-form-item :label="$t('渠道額外參數:')">
          <a-textarea
            disabled="disabled"
            style="height: 100px; color: black"
            v-model:value="vdata.detailData.channelExtra"
          />
        </a-form-item>
      </a-col>
      <a-divider />
      <a-col :sm="24">
        <a-form-item :label="$t('擴充參數:')">
          <a-textarea
            disabled="disabled"
            style="height: 100px; color: black"
            v-model:value="vdata.detailData.extParam"
          />
        </a-form-item>
      </a-col>
    </a-form>
  </a-drawer>
</template>
<script setup lang="ts">
import { API_URL_TRANSFER_ORDER_LIST, req } from '@/api/manage'
import { reactive } from 'vue'

const vdata: any = reactive({
  detailData: {},
  userH5ConfirmUrl: null, // 转账确认领取链接地址
  userH5ConfirmQrImgUrl: null, // 转账确认领取二维码地址
  isShow: false, // 是否显示弹层/抽屉
  recordId: null, // 更新对象ID
})

function show(recordId) {
  vdata.userH5ConfirmUrl = null
  vdata.userH5ConfirmQrImgUrl = null
  req.getById(API_URL_TRANSFER_ORDER_LIST, recordId).then((res) => {
    vdata.detailData = res
    if (res.channelResData && res.ifCode === 'wxpay') {
        let channelResData =JSON.parse(res.channelResData)
        if(channelResData && channelResData.userH5ConfirmQrImgUrl){ // 用户确认模式
          vdata.userH5ConfirmUrl = channelResData.userH5ConfirmUrl
          vdata.userH5ConfirmQrImgUrl = channelResData.userH5ConfirmQrImgUrl
        }
    }
  })
  vdata.isShow = true
}

defineExpose({ show })
</script>
