<template>
  <!-- 渠道測試：替商戶開一筆「管理員測試單」，確認渠道能正常產生繳費資訊（比照競品用戶列表的「渠道測試」收銀台） -->
  <a-modal v-model:open="vdata.open" :title="$t('渠道測試：') + vdata.mchName" :footer="null" width="520px" :mask-closable="false">
    <template v-if="!vdata.result">
      <a-alert type="warning" show-icon style="margin-bottom: 12px"
        :message="$t('這會以商戶身分向第三方開出一筆真的繳費單，並留在訂單列表（標題為「管理員測試單」）。不去繳費的話，到期會自動關閉。')" />
      <a-empty v-if="vdata.loaded && !vdata.ways.length" :description="$t('這個商戶還沒有開通任何支付方式，請先請平台設定通道。')" />
      <a-form v-else layout="vertical">
        <a-form-item :label="$t('支付渠道')" required>
          <a-select v-model:value="vdata.wayKey" :options="vdata.ways.map((w) => ({ value: w.appId + '|' + w.wayCode, label: `${w.wayName}（${w.appName}）` }))" />
        </a-form-item>
        <a-form-item :label="$t('金額（元）')" required>
          <a-input-number v-model:value="vdata.form.amount" :min="1" :max="20000" :precision="0" style="width: 100%" />
        </a-form-item>
        <a-form-item :label="$t('繳款人姓名')" required><a-input v-model:value="vdata.form.payerName" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item :label="$t('手機號')" required><a-input v-model:value="vdata.form.payerMobile" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item :label="$t('電子信箱')" required><a-input v-model:value="vdata.form.payerEmail" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="8"><a-form-item :label="$t('郵遞區號')" required><a-input v-model:value="vdata.form.payerPostcode" /></a-form-item></a-col>
          <a-col :span="16"><a-form-item :label="$t('地址')" required><a-input v-model:value="vdata.form.payerAddress" /></a-form-item></a-col>
        </a-row>
        <div style="text-align: right">
          <a-button @click="vdata.open = false">{{ $t('取消') }}</a-button>
          <a-button type="primary" style="margin-left: 8px" :loading="vdata.saving" @click="submit">{{ $t('送出測試單') }}</a-button>
        </div>
      </a-form>
    </template>
    <template v-else>
      <a-result :status="vdata.result.ok ? 'success' : 'error'" :title="vdata.result.ok ? $t('渠道正常，已產生繳費資訊') : $t('渠道沒有回傳繳費資訊')"
        :sub-title="vdata.result.ok ? '' : vdata.result.errMsg" />
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item :label="$t('支付訂單號')">{{ vdata.result.payOrderId }}</a-descriptions-item>
        <a-descriptions-item :label="$t('商戶訂單號')">{{ vdata.result.mchOrderNo }}</a-descriptions-item>
        <template v-if="vdata.result.pay">
          <a-descriptions-item v-if="vdata.result.pay.paymentCode" :label="$t('ibon 繳費代碼')">
            <span style="font-family: monospace; font-size: 15px">{{ vdata.result.pay.paymentCode }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="vdata.result.pay.billAmount != null" :label="$t('應繳金額（元）')">{{ vdata.result.pay.billAmount }}</a-descriptions-item>
          <a-descriptions-item v-if="vdata.result.pay.expireDate" :label="$t('繳費期限')">{{ vdata.result.pay.expireDate }}</a-descriptions-item>
          <a-descriptions-item v-if="vdata.result.pay.shortUrl" :label="$t('繳費頁面')">
            <a :href="vdata.result.pay.shortUrl" target="_blank" rel="noopener noreferrer">{{ vdata.result.pay.shortUrl }}</a>
          </a-descriptions-item>
        </template>
      </a-descriptions>
      <div style="text-align: right; margin-top: 16px">
        <a-button @click="vdata.result = null">{{ $t('再測一筆') }}</a-button>
        <a-button type="primary" style="margin-left: 8px" @click="vdata.open = false">{{ $t('關閉') }}</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { reactive, getCurrentInstance } from 'vue'
import { t } from '@/i18n'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const vdata: any = reactive({ open: false, mchNo: '', mchName: '', ways: [], loaded: false, wayKey: undefined, saving: false, result: null, form: {} })

function show(record) {
  Object.assign(vdata, {
    open: true, mchNo: record.mchNo, mchName: record.mchName || record.mchNo, ways: [], loaded: false, wayKey: undefined, saving: false, result: null,
    form: { amount: 100, payerName: t('測試繳款人'), payerMobile: '', payerEmail: '', payerPostcode: '', payerAddress: '' },
  })
  req.list(`${API_URL_AGENT_PORTAL}/branch/merchants/${record.mchNo}/testWays`, {}).then((r) => {
    vdata.ways = r || []
    vdata.loaded = true
    if (vdata.ways.length) vdata.wayKey = vdata.ways[0].appId + '|' + vdata.ways[0].wayCode
  })
}
function submit() {
  const f = vdata.form
  if (!vdata.wayKey || !f.amount || !f.payerName || !f.payerMobile || !f.payerEmail || !f.payerPostcode || !f.payerAddress) {
    $infoBox.message.warning(t('請選擇支付渠道並填寫金額與完整的繳款人資料'))
    return
  }
  const [appId, wayCode] = vdata.wayKey.split('|')
  vdata.saving = true
  req
    .add(`${API_URL_AGENT_PORTAL}/branch/merchants/${vdata.mchNo}/channelTest`, { ...f, appId, wayCode })
    .then((res) => {
      let pay = null
      try {
        pay = res.payData ? JSON.parse(res.payData) : null
      } catch (e) {
        pay = null
      }
      // orderState 3 為支付失敗；有繳費代碼或繳費頁面才算渠道正常
      const ok = res.orderState !== 3 && !!pay && !!(pay.paymentCode || pay.shortUrl)
      vdata.result = { ok, payOrderId: res.payOrderId, mchOrderNo: res.mchOrderNo, pay, errMsg: res.errMsg || res.errCode || t('請到訂單列表查看這筆訂單的狀態') }
    })
    .finally(() => (vdata.saving = false))
}
defineExpose({ show })
</script>
