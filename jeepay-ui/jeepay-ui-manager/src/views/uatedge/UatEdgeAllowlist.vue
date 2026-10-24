<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>{{ $t('UAT Edge 白名單') }}</template>

      <a-alert
        type="info"
        show-icon
        :message="$t('套用狀態：') + (vdata.applyStatus || $t('尚未套用'))"
        style="margin-bottom: 16px"
      />
      <a-alert
        type="warning"
        show-icon
        :message="$t('白名單儲存後約 1 分鐘內由系統自動套用到 edge（nginx reload），不需人工部署。系統保留 IP（Talend 測試機）不可刪除。')"
        style="margin-bottom: 16px"
      />

      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item label="IP / CIDR">
          <a-input
            v-model:value="vdata.form.ip"
            :placeholder="$t('例：1.2.3.4 或 2001:b011::/32')"
            style="width: 280px"
          />
        </a-form-item>
        <a-form-item :label="$t('備註')">
          <a-input
            v-model:value="vdata.form.remark"
            :placeholder="$t('例：辦公室測試機')"
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="vdata.btnLoading" @click="addFunc">
            {{ $t('新增') }}
          </a-button>
          <a-button style="margin-left: 8px" @click="queryFunc">
            {{ $t('重新整理') }}
          </a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="vdata.records"
        :pagination="false"
        row-key="ip"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'op'">
            <a-tooltip v-if="isProtected(record.ip)" :title="$t('系統保留 IP 不可刪除')">
              <a-button type="link" danger disabled>{{ $t('刪除') }}</a-button>
            </a-tooltip>
            <a-button v-else type="link" danger @click="removeFunc(record.ip)">
              {{ $t('刪除') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { API_URL_UAT_EDGE_ALLOWLIST, req } from '@/api/manage'
import { getCurrentInstance, reactive } from 'vue'
import { t } from '@/i18n'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

// 與後端 PROTECTED_IPS 對應：Talend 測試機不可刪除
const PROTECTED_IPS = ['34.92.245.74', '34.92.52.162']

const columns = [
  { key: 'ip', title: 'IP / CIDR', dataIndex: 'ip', width: 300 },
  { key: 'remark', title: t('備註'), dataIndex: 'remark' },
  { key: 'op', title: t('操作'), width: 120, align: 'center' },
]

const vdata: any = reactive({
  records: [],
  applyStatus: '',
  btnLoading: false,
  form: { ip: '', remark: '' },
})

function queryFunc() {
  vdata.btnLoading = true
  req
    .list(API_URL_UAT_EDGE_ALLOWLIST, {})
    .then((res) => {
      vdata.records = (res && res.records) || []
      vdata.applyStatus = (res && res.applyStatus) || ''
    })
    .finally(() => {
      vdata.btnLoading = false
    })
}

function addFunc() {
  if (!vdata.form.ip) {
    $infoBox.message.warning(t('請輸入 IP'))
    return
  }
  req
    .add(API_URL_UAT_EDGE_ALLOWLIST + '/add', {
      ip: vdata.form.ip.trim(),
      remark: vdata.form.remark.trim(),
    })
    .then(() => {
      $infoBox.message.success(t('已新增，約 1 分鐘內自動套用'))
      vdata.form.ip = ''
      vdata.form.remark = ''
      queryFunc()
    })
}

function removeFunc(ip) {
  $infoBox.confirmDanger(t('確認刪除？'), t('將從白名單移除 {0}', [ip]), () => {
    req.add(API_URL_UAT_EDGE_ALLOWLIST + '/remove', { ip }).then(() => {
      $infoBox.message.success(t('已刪除，約 1 分鐘內自動套用'))
      queryFunc()
    })
  })
}

function isProtected(ip) {
  return PROTECTED_IPS.includes(ip)
}

queryFunc()
</script>
