<template>
  <page-header-wrapper>
    <a-card>
      <div class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <a-range-picker
              class="table-head-layout"
              @change="onChange"
              v-model:value="vdata.date"
              :show-time="{ format: 'HH:mm:ss' }"
              format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disabledDate"
            >
              <a-icon slot="suffixIcon" type="sync" />
            </a-range-picker>
            <jeepay-text-up :placeholder="$t('訂單ID')" v-model:value="vdata.searchData.orderId" />
            <jeepay-text-up
              :placeholder="$t('商戶訂單號')"
              v-model:value="vdata.searchData.mchOrderNo"
            />
            <jeepay-text-up :placeholder="$t('商戶號')" v-model:value="vdata.searchData.mchNo" />
            <jeepay-text-up :placeholder="$t('服務商號')" v-model:value="vdata.searchData.isvNo" />
            <jeepay-text-up :placeholder="$t('應用AppId')" v-model:value="vdata.searchData.appId" />
            <a-select
              v-model:value="vdata.searchData.state"
              :placeholder="$t('通知狀態')"
              class="table-head-layout"
            >
              <a-select-option value="">{{ $t('全部') }}</a-select-option>
              <a-select-option value="1">{{ $t('通知中') }}</a-select-option>
              <a-select-option value="2">{{ $t('通知成功') }}</a-select-option>
              <a-select-option value="3">{{ $t('通知失敗') }}</a-select-option>
            </a-select>
            <a-select
              v-model:value="vdata.searchData.orderType"
              :placeholder="$t('訂單類型')"
              class="table-head-layout"
            >
              <a-select-option value="">{{ $t('全部') }}</a-select-option>
              <a-select-option value="1">{{ $t('支付') }}</a-select-option>
              <a-select-option value="2">{{ $t('退款') }}</a-select-option>
              <a-select-option value="3">{{ $t('轉帳') }}</a-select-option>
            </a-select>

            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="queryFunc" :loading="vdata.btnLoading">
                {{ $t('搜尋') }}
              </a-button>
              <a-button
                style="margin-left: 8px"
                @click="
                  () => {
                    vdata.searchData = {}
                    vdata.date = ''
                  }
                "
              >
                {{ $t('重置') }}
              </a-button>
            </span>
          </div>
        </a-form>
      </div>

      <!-- 列表渲染 -->
      <JeepayTable
        @btnLoadClose="vdata.btnLoading = false"
        ref="infoTable"
        :closable="true"
        :initData="true"
        :reqTableDataFunc="reqTableDataFunc"
        :tableColumns="vdata.tableColumns"
        :searchData="vdata.searchData"
        :rowSelection="rowSelection"
        rowKey="orderId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <a-tag
              :key="record.orderId"
              :color="record.state === 1 ? 'orange' : record.state === 2 ? 'green' : 'volcano'"
            >
              <template v-if="record.state == 1">{{ $t('通知中') }}</template>
              <template v-else-if="record.state == 2">{{ $t('通知成功') }}</template>
              <template v-else-if="record.state == 3">{{ $t('通知失敗') }}</template>
              <template v-else>{{ $t('未知') }}</template>
            </a-tag>
          </template>
          <template v-if="column.key === 'orderType'">
            <a-tag v-if="record.orderType == 1" color="green">{{ $t('支付') }}</a-tag>
            <a-tag v-else-if="record.orderType == 1" color="volcano">{{ $t('退款') }}</a-tag>
            <a-tag v-else-if="record.orderType == 1" color="blue">{{ $t('轉帳') }}</a-tag>
            <a-tag v-else color="orange">{{ $t('未知') }}</a-tag>
          </template>
          <template v-if="column.key === 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                type="link"
                v-if="$access('ENT_MCH_NOTIFY_VIEW')"
                @click="detailFunc(record.notifyId)"
              >
                {{ $t('詳情') }}
              </a-button>
              <a-button
                type="link"
                v-if="$access('ENT_MCH_NOTIFY_RESEND') && record.state === 3"
                @click="resendFunc(record.notifyId)"
              >
                {{ $t('重發通知') }}
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
    </a-card>
    <!-- 日志详情抽屉 -->
    <template>
      <a-drawer
        width="40%"
        placement="right"
        :closable="true"
        v-model:open="vdata.open"
        :title="vdata.open === true ? $t('商戶通知詳情') : ''"
        @close="onClose"
      >
        <a-row justify="space-between" type="flex">
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('訂單ID')">
                <a-tag color="purple">
                  {{ vdata.detailData.orderId }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('服務商號')">
                {{ vdata.detailData.isvNo }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('商戶訂單號')">
                {{ vdata.detailData.mchOrderNo }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('商戶號')">
                {{ vdata.detailData.mchNo }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('訂單類型')">
                <a-tag v-if="vdata.detailData.orderType == 1" color="green">{{ $t('支付') }}</a-tag>
                <a-tag v-else-if="vdata.detailData.orderType == 1" color="volcano">{{ $t('退款') }}</a-tag>
                <a-tag v-else-if="vdata.detailData.orderType == 1" color="blue">{{ $t('轉帳') }}</a-tag>
                <a-tag v-else color="orange">{{ $t('未知') }}</a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('應用APPID')">
                {{ vdata.detailData.appId }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('通知狀態')">
                <a-tag v-if="vdata.detailData.state == 1" color="orange">{{ $t('通知中') }}</a-tag>
                <a-tag v-else-if="vdata.detailData.state == 2" color="green">{{ $t('通知成功') }}</a-tag>
                <a-tag v-else-if="vdata.detailData.state == 3" color="volcano">{{ $t('通知失敗') }}</a-tag>
                <a-tag v-else color="volcano">{{ $t('未知') }}</a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('通知次數')">
                {{ vdata.detailData.notifyCount }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="24">
            <a-descriptions>
              <a-descriptions-item :label="$t('最後通知時間')">
                {{ vdata.detailData.lastNotifyTime }}
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

          <a-col :sm="24">
            <a-form layout="vertical">
              <a-form-item :label="$t('通知地址:')">
                <a-textarea
                  disabled="disabled"
                  style="height: 100px; color: black"
                  v-model:value="vdata.detailData.notifyUrl"
                />
              </a-form-item>
            </a-form>
          </a-col>
          <a-col :sm="24">
            <a-form layout="vertical">
              <a-form-item :label="$t('回應結果:')">
                <a-textarea
                  disabled="disabled"
                  style="height: 100px; color: black"
                  v-model:value="vdata.detailData.resResult"
                />
              </a-form-item>
            </a-form>
          </a-col>
        </a-row>
      </a-drawer>
    </template>
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { API_URL_MCH_NOTIFY_LIST, req, mchNotifyResend } from '@/api/manage'
import moment from 'moment'
import { reactive, computed, ref, getCurrentInstance } from 'vue'
import { t } from '@/i18n'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

// eslint-disable-next-line no-unused-vars
const tableColumns = [
  { key: 'orderId', title: t('訂單ID'), fixed: 'left', dataIndex: 'orderId' },
  { key: 'mchOrderNo', title: t('商戶訂單號'), dataIndex: 'mchOrderNo' },
  { key: 'state', title: t('通知狀態'), width: '130px', scopedSlots: { customRender: 'stateSlot' } },
  {
    key: 'orderType',
    title: t('訂單類型'),
    width: '130px',
    scopedSlots: { customRender: 'orderTypeSlot' },
  },
  { key: 'createdAt', dataIndex: 'createdAt', title: t('建立日期') },
  {
    key: 'op',
    title: t('操作'),
    fixed: 'right',
    align: 'center',
    scopedSlots: { customRender: 'opSlot' },
  },
]

const vdata: any = reactive({
  date: '',

  btnLoading: true,
  tableColumns: tableColumns,
  searchData: {},
  selectedIds: [], // 选中的数据
  createdStart: '', // 选择开始时间
  createdEnd: '', // 选择结束时间
  open: false,
  detailData: {},
  moment,
})

const infoTable = ref()

const rowSelection = computed(() => {
  const that = this
  return {
    onChange: (selectedRowKeys, selectedRows) => {
      vdata.selectedIds = [] // 清空选中数组
      selectedRows.forEach(function (data: any) {
        // 赋值选中参数
        vdata.selectedIds.push(data.payOrderId)
      })
    },
  }
})

function queryFunc() {
  vdata.btnLoading = true
  infoTable.value.refTable(true)
}

// 请求table接口数据
function reqTableDataFunc(params) {
  return req.list(API_URL_MCH_NOTIFY_LIST, params)
}
function searchFunc() {
  // 点击【查询】按钮点击事件
  infoTable.value.refTable(true)
}

function detailFunc(recordId) {
  req.getById(API_URL_MCH_NOTIFY_LIST, recordId).then((res) => {
    vdata.detailData = res
  })
  vdata.open = true
  console.log(vdata.open, 'vdata.open')
}

function onChange(date, dateString) {
  vdata.searchData.createdStart = dateString[0] // 开始时间
  vdata.searchData.createdEnd = dateString[1] // 结束时间
}

function disabledDate(current) {
  // 今日之后日期不可选
  return current && current > moment().endOf('day')
}
function onClose() {
  vdata.open = false
}

function resendFunc(notifyId) {
  // 重发通知
  $infoBox.confirmPrimary(t('確認重發通知？'), '', () => {
    mchNotifyResend(notifyId).then((res) => {
      $infoBox.message.success(t('任務更新成功，請稍後檢視最新狀態！'))
      searchFunc()
    })
  })
}
</script>
