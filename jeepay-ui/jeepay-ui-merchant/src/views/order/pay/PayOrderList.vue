<template>
  <page-header-wrapper>
    <a-card>
      <div v-if="history" class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <a-form-item label="" class="table-head-layout">
              <a-range-picker
                @change="onChange"
                v-model:value="vdata.date"
                :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss"
                :disabled-date="disabledDate"
              >
                <a-icon slot="suffixIcon" type="sync" />
              </a-range-picker>
            </a-form-item>
            <jeepay-text-up
              :placeholder="$t('支付/商戶/渠道訂單號')"
              v-model:value="vdata.searchData.unionOrderId"
            />
            <!--            <jeepay-text-up :placeholder="'支付订单号'" :msg="vdata.searchData.payOrderId" v-model:value="vdata.searchData.payOrderId" />-->
            <!--            <jeepay-text-up :placeholder="'商户订单号'" :msg="vdata.searchData.mchOrderNo" v-model:value="vdata.searchData.mchOrderNo" />-->
            <jeepay-text-up :placeholder="$t('應用AppId')" v-model:value="vdata.searchData.appId" />

            <a-select
              class="table-head-layout"
              v-if="$access('ENT_PAY_ORDER_SEARCH_PAY_WAY')"
              v-model:value="vdata.searchData.wayCode"
              :placeholder="$t('支付方式')"
            >
              <a-select-option value="">{{ $t('全部') }}</a-select-option>
              <a-select-option
                :key="item.wayCode"
                v-for="item in vdata.payWayList"
                :value="item.wayCode"
              >
                {{ item.wayName }}
              </a-select-option>
            </a-select>
            <a-select
              v-model:value="vdata.searchData.state"
              :placeholder="$t('支付狀態')"
              class="table-head-layout"
            >
              <a-select-option value="">{{ $t('全部') }}</a-select-option>
              <a-select-option value="0">{{ $t('訂單生成') }}</a-select-option>
              <a-select-option value="1">{{ $t('支付中') }}</a-select-option>
              <a-select-option value="2">{{ $t('支付成功') }}</a-select-option>
              <a-select-option value="3">{{ $t('支付失敗') }}</a-select-option>
              <a-select-option value="4">{{ $t('已撤銷') }}</a-select-option>
              <a-select-option value="5">{{ $t('已退款') }}</a-select-option>
              <a-select-option value="6">{{ $t('訂單關閉') }}</a-select-option>
            </a-select>

            <a-select
              v-model:value="vdata.searchData.divisionState"
              :placeholder="$t('分帳狀態')"
              class="table-head-layout"
            >
              <a-select-option value="">{{ $t('全部') }}</a-select-option>
              <a-select-option value="0">{{ $t('未發生分帳') }}</a-select-option>
              <a-select-option value="1">{{ $t('等待分帳任務處理') }}</a-select-option>
              <a-select-option value="2">{{ $t('分帳處理中') }}</a-select-option>
              <a-select-option value="3">{{ $t('分帳任務已結束（狀態請看分帳記錄）') }}</a-select-option>
            </a-select>

            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="queryFunc" :loading="vdata.btnLoading">
                {{ $t('搜尋') }}
              </a-button>
              <a-button
                style="margin-left: 8px"
                @click="resetFunc"
              >
                {{ $t('重置') }}
              </a-button>
              <a-button v-if="vdata.applied && $access('ENT_MCH_EXPORT_CENTER')" style="margin-left: 8px" :loading="vdata.exporting" :disabled="!vdata.total" @click="exportFunc">{{ $t('匯出') }}</a-button>
              <a-button v-if="$access('ENT_MCH_EXPORT_CENTER')" style="margin-left: 8px" @click="exportDrawer.open()">{{ $t('匯出紀錄') }}</a-button>
            </span>
          </div>
        </a-form>
      </div>
      <div v-else class="today-hint">{{ $t('僅顯示今日訂單；查詢其他日期、篩選或匯出請到「歷史查詢 → 代收查詢」。') }}</div>

      <!-- 列表渲染 -->
      <a-empty v-if="history && !vdata.applied" style="padding: 60px 0" :description="$t('請先設定查詢條件，再按「搜尋」')" />
      <JeepayTable
        v-show="!history || vdata.applied"
        @btnLoadClose="vdata.btnLoading = false"
        ref="infoTable"
        :init-data="!history"
        :reqTableDataFunc="reqTableDataFunc"
        :tableColumns="vdata.tableColumns"
        :searchData="vdata.searchData"
        rowKey="payOrderId"
        :tableRowCrossColor="true"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key == 'amount'">
            <b>{{ (record.currency || 'TWD').toUpperCase() }} {{ record.amount / 100 }}</b>
          </template>
          <!-- 自定义插槽 -->
          <template v-if="column.key == 'refundAmount'">
            {{ (record.currency || 'TWD').toUpperCase() }} {{ record.refundAmount / 100 }}
          </template>
          <!-- 自定义插槽 -->
          <template v-if="column.key == 'state'">
            <a-tag
              :key="record.state"
              :color="
                record.state === 0
                  ? 'blue'
                  : record.state === 1
                    ? 'orange'
                    : record.state === 2
                      ? 'green'
                      : record.state === 6
                        ? ''
                        : 'volcano'
              "
            >
              {{
                record.state === 0
                  ? $t('訂單生成')
                  : record.state === 1
                    ? $t('支付中')
                    : record.state === 2
                      ? $t('支付成功')
                      : record.state === 3
                        ? $t('支付失敗')
                        : record.state === 4
                          ? $t('已撤銷')
                          : record.state === 5
                            ? $t('已退款')
                            : record.state === 6
                              ? $t('訂單關閉')
                              : $t('未知')
              }}
            </a-tag>
          </template>

          <template v-if="column.key == 'mchFeeAmount'">
            {{ (record.currency || 'TWD').toUpperCase() }}
            {{ record.mchFeeAmount && (record.mchFeeAmount / 100).toFixed(2) }}
          </template>

          <template v-if="column.key == 'divisionState'">
            <span v-if="record.divisionState == 0">-</span>
            <a-tag color="orange" v-else-if="record.divisionState == 1">{{ $t('待分帳') }}</a-tag>
            <a-tag color="red" v-else-if="record.divisionState == 2">{{ $t('分帳處理中') }}</a-tag>
            <a-tag color="green" v-else-if="record.divisionState == 3">{{ $t('任務已結束') }}</a-tag>
            <span v-else>{{ $t('未知') }}</span>
          </template>

          <template v-if="column.key == 'orderNo'">
            <div class="order-list">
              <p>
                <span style="color: #729ed5; background: #e7f5f7">{{ $t('支付') }}</span>{{ record.payOrderId }}
              </p>
              <p style="margin-bottom: 0">
                <span style="color: #56cf56; background: #d8eadf">{{ $t('商戶') }}</span><a-tooltip
                  placement="bottom"
                  style="font-weight: normal"
                  v-if="record.mchOrderNo.length > record.payOrderId.length"
                >
                  <template slot="title">
                    <span>{{ record.mchOrderNo }}</span>
                  </template>{{ changeStr2ellipsis(record.mchOrderNo, record.payOrderId.length) }}</a-tooltip><span style="font-weight: normal" v-else>{{ record.mchOrderNo }}</span>
              </p>
              <p v-if="record.channelOrderNo" style="margin-bottom: 0; margin-top: 10px">
                <span style="color: #fff; background: #e09c4d">{{ $t('渠道') }}</span><a-tooltip
                  placement="bottom"
                  style="font-weight: normal"
                  v-if="record.channelOrderNo.length > record.payOrderId.length"
                >
                  <template slot="title">
                    <span>{{ record.channelOrderNo }}</span>
                  </template>{{ changeStr2ellipsis(record.channelOrderNo, record.payOrderId.length) }}</a-tooltip><span style="font-weight: normal" v-else>{{ record.channelOrderNo }}</span>
              </p>
            </div>
          </template>

          <template v-if="column.key == 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                type="link"
                v-if="$access('ENT_PAY_ORDER_VIEW')"
                @click="detailFunc(record.payOrderId)"
              >
                {{ $t('詳情') }}
              </a-button>
              <a-button
                type="link"
                v-if="$access('ENT_PAY_ORDER_REFUND')"
                style="color: red"
                v-show="record.state === 2 && record.refundState !== 2"
                @click="openFunc(record, record.payOrderId)"
              >
                {{ $t('退款') }}
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
      <ExportJobsDrawer v-if="history" ref="exportDrawer" :job-types="['PAY_ORDER']" />
    </a-card>
    <!-- 退款弹出框 -->
    <refund-modal ref="refundModalInfo" :callbackFunc="searchFunc"></refund-modal>
    <!-- 日志详情抽屉 -->
    <template>
      <a-drawer
        width="50%"
        placement="right"
        :closable="true"
        :open="vdata.open"
        :title="vdata.open === true ? $t('訂單詳情') : ''"
        @close="onClose"
      >
        <a-row justify="space-between" type="flex">
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('服務商號')">
                {{ vdata.detailData.isvNo }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('支付訂單號')">
                <a-tag color="purple">
                  {{ vdata.detailData.payOrderId }}
                </a-tag>
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
              <a-descriptions-item :label="$t('商戶訂單號')">
                {{ vdata.detailData.mchOrderNo }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('商戶名稱')">
                {{ vdata.detailData.mchName }}
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
              <a-descriptions-item :label="$t('訂單狀態')">
                <a-tag
                  :color="
                    vdata.detailData.state === 0
                      ? 'blue'
                      : vdata.detailData.state === 1
                        ? 'orange'
                        : vdata.detailData.state === 2
                          ? 'green'
                          : vdata.detailData.state === 6
                            ? ''
                            : 'volcano'
                  "
                >
                  {{
                    vdata.detailData.state === 0
                      ? $t('訂單生成')
                      : vdata.detailData.state === 1
                        ? $t('支付中')
                        : vdata.detailData.state === 2
                          ? $t('支付成功')
                          : vdata.detailData.state === 3
                            ? $t('支付失敗')
                            : vdata.detailData.state === 4
                              ? $t('已撤銷')
                              : vdata.detailData.state === 5
                                ? $t('已退款')
                                : vdata.detailData.state === 6
                                  ? $t('訂單關閉')
                                  : $t('未知')
                  }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('支付金額')">
                <a-tag color="green">
                  {{ vdata.detailData.amount / 100 }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('手續費')">
                <a-tag color="pink">{{ vdata.detailData.mchFeeAmount / 100 }}</a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('商家費率')">
                {{ (vdata.detailData.mchFeeRate * 100).toFixed(2) }}%
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('支付錯誤碼')">
                {{ vdata.detailData.errCode }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('支付錯誤描述')">
                {{ vdata.detailData.errMsg }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('訂單失效時間')">
                {{ vdata.detailData.expiredTime }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('支付成功時間')">
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
              <a-descriptions-item :label="$t('商品標題')">
                {{ vdata.detailData.subject }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('商品描述')">
                {{ vdata.detailData.body }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('接口代碼')">
                {{ vdata.detailData.ifCode }}
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
              <a-descriptions-item :label="$t('支付方式')">
                {{ vdata.detailData.wayCode }}
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
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('使用者標識')">
                {{ vdata.detailData.channelUser }}
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
              <a-descriptions-item :label="$t('異步通知地址')">
                {{ vdata.detailData.notifyUrl }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('頁面跳轉地址')">
                {{ vdata.detailData.returnUrl }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('退款次數')">
                {{ vdata.detailData.refundTimes }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('退款總額')">
                <a-tag color="cyan" v-if="vdata.detailData.refundAmount">
                  {{ vdata.detailData.refundAmount / 100 }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>

          <a-divider />
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('訂單分帳模式')">
                <span v-if="vdata.detailData.divisionMode == 0">{{ $t('該筆訂單不允許分帳') }}</span>
                <span v-else-if="vdata.detailData.divisionMode == 1">
                  {{ $t('支付成功按配置自動完成分帳') }}
                </span>
                <span v-else-if="vdata.detailData.divisionMode == 2">
                  {{ $t('商戶手動分帳(解凍商戶金額)') }}
                </span>
                <span v-else>{{ $t('未知') }}</span>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('分帳狀態')">
                <a-tag color="blue" v-if="vdata.detailData.divisionState == 0">{{ $t('未發生分帳') }}</a-tag>
                <a-tag color="orange" v-else-if="vdata.detailData.divisionState == 1">{{ $t('待分帳') }}</a-tag>
                <a-tag color="red" v-else-if="vdata.detailData.divisionState == 2">
                  {{ $t('分帳處理中') }}
                </a-tag>
                <a-tag color="green" v-else-if="vdata.detailData.divisionState == 3">
                  {{ $t('任務已結束') }}
                </a-tag>
                <a-tag color="#f50" v-else>{{ $t('未知') }}</a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item :label="$t('最新分帳發起時間')">
                {{ vdata.detailData.divisionLastTime }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>
        <a-divider />
        <a-row justify="start" type="flex">
          <a-col :sm="24">
            <a-form layout="vertical">
              <a-form-item :label="$t('擴展參數:')">
                <a-textarea
                  disabled="disabled"
                  style="height: 100px; color: black"
                  v-model:value="vdata.detailData.extParam"
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
import RefundModal from './RefundModal.vue' // 退款弹出框
import { API_URL_PAY_ORDER_LIST, API_URL_PAYWAYS_LIST, req } from '@/api/manage'
import moment from 'moment'
import { submitExport } from '@/utils/exportJob'
import ExportJobsDrawer from '@/components/ExportJobs/ExportJobsDrawer.vue'
import { reactive, ref, getCurrentInstance, onMounted } from 'vue'
import { t } from '@/i18n'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

// eslint-disable-next-line no-unused-vars
const tableColumns = [
  { key: 'amount', title: t('支付金額'), scopedSlots: { customRender: 'amountSlot' } },
  { key: 'refundAmount', title: t('退款金額'), scopedSlots: { customRender: 'refundAmountSlot' } },
  {
    key: 'mchFeeAmount',
    dataIndex: 'mchFeeAmount',
    title: t('手續費'),
  },
  { key: 'orderNo', title: t('訂單號'), width: '260px' },
  // { key: 'payOrderId', title: '支付订单号', dataIndex: 'payOrderId' },
  // { key: 'mchOrderNo', title: '商户订单号', dataIndex: 'mchOrderNo' },
  { key: 'wayName', title: t('支付方式'), dataIndex: 'wayName', width: 150 },
  { key: 'state', title: t('支付狀態'), scopedSlots: { customRender: 'stateSlot' } },
  {
    key: 'divisionState',
    title: t('分帳狀態'),
    scopedSlots: { customRender: 'divisionStateSlot' },
    align: 'center',
  },
  { key: 'createdAt', dataIndex: 'createdAt', title: t('建立日期') },
  {
    key: 'op',
    title: t('操作'),
    width: '120px',
    fixed: 'right',
    align: 'center',
    scopedSlots: { customRender: 'opSlot' },
  },
]

// history=true 為「歷史查詢 → 代收查詢」（完整篩選與匯出）；否則為訂單管理，只顯示今日訂單
const props = defineProps({ history: { type: Boolean, default: false } })
const exportDrawer = ref()

const vdata: any = reactive({
  applied: null, // 歷史查詢：最近一次送出搜尋時的條件；null 表示尚未搜尋
  total: 0, // 歷史查詢：最近一次搜尋的總筆數
  exporting: false,
  btnLoading: false,
  tableColumns: tableColumns,
  searchData: {},
  createdStart: '', // 选择开始时间
  createdEnd: '', // 选择结束时间
  open: false,
  detailData: {},
  payWayList: [],

  date: '',
})

const infoTable = ref()
const refundModalInfo = ref()
onMounted(() => {
  if ($access('ENT_PAY_ORDER_SEARCH_PAY_WAY')) {
    initPayWay()
  }
})
function cleanParams(obj) {
  const r: any = {}
  Object.keys(obj || {}).forEach((k) => {
    if (obj[k] !== undefined && obj[k] !== null && obj[k] !== '') r[k] = obj[k]
  })
  return r
}
function queryFunc() {
  if (props.history) {
    const cond = cleanParams(vdata.searchData)
    // 防呆：不允許空條件查全部歷史資料
    if (!cond.unionOrderId && !(cond.createdStart && cond.createdEnd)) {
      return $infoBox.message.warning(t('請先選擇日期區間，或輸入訂單號'))
    }
    vdata.applied = cond
  }
  vdata.btnLoading = true
  infoTable.value.refTable(true)
}
function resetFunc() {
  vdata.searchData = {}
  vdata.date = ''
  vdata.applied = null
  vdata.total = 0
}
// 请求table接口数据
function reqTableDataFunc(params) {
  if (!props.history) {
    const today = moment().format('YYYY-MM-DD')
    params = { ...params, createdStart: `${today} 00:00:00`, createdEnd: `${today} 23:59:59` }
    return req.list(API_URL_PAY_ORDER_LIST, params)
  }
  // 歷史查詢：換頁也沿用送出搜尋時的條件，不受之後修改欄位影響
  params = { pageNumber: params.pageNumber, pageSize: params.pageSize, ...vdata.applied }
  return req.list(API_URL_PAY_ORDER_LIST, params).then((res) => {
    vdata.total = res.total || 0
    return res
  })
}
function searchFunc() {
  // 点击【查询】按钮点击事件
  infoTable.value.refTable(false)
}
// 打开退款弹出框
function openFunc(record, recordId) {
  if (record.refundState === 2) {
    return $infoBox.modalError(t('訂單無可退款金額'), '')
  }
  refundModalInfo.value.show(recordId)
}
function detailFunc(recordId) {
  req.getById(API_URL_PAY_ORDER_LIST, recordId).then((res) => {
    vdata.detailData = res
  })
  vdata.open = true
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
function initPayWay() {
  req.list(API_URL_PAYWAYS_LIST, { pageSize: -1 }).then((res) => {
    // 支付方式下拉列表
    vdata.payWayList = res.records
  })
}
function changeStr2ellipsis(orderNo, baseLength) {
  const halfLengh = Math.floor(baseLength / 2)
  return (
    orderNo.substring(0, halfLengh - 1) +
    '...' +
    orderNo.substring(orderNo.length - halfLengh, orderNo.length)
  )
}

// 背景匯出只支援這些條件（與後端 ExportService 一致）；其他條件會被忽略，因此有設定時不允許匯出
const EXPORT_KEYS = ['mchNo', 'wayCode', 'state', 'createdStart', 'createdEnd']
const UNSUPPORTED_NAMES = { unionOrderId: t('訂單號'), isvNo: t('服務商號'), appId: t('應用AppId'), notifyState: t('回調狀態'), divisionState: t('分帳狀態') }
const EXPORT_MAX_ROWS = 200000

// 背景匯出：只匯出最近一次搜尋的結果，完成後在「匯出紀錄」下載
function exportFunc() {
  if (vdata.exporting) return
  if (!vdata.applied) {
    return $infoBox.message.warning(t('請先搜尋，確認結果後再匯出'))
  }
  if (JSON.stringify(cleanParams(vdata.searchData)) !== JSON.stringify(vdata.applied)) {
    return $infoBox.message.warning(t('查詢條件已變更，請先重新搜尋再匯出'))
  }
  if (!vdata.total) {
    return $infoBox.message.warning(t('目前的查詢沒有資料可匯出'))
  }
  const unsupported = Object.keys(vdata.applied).filter((k) => !EXPORT_KEYS.includes(k))
  if (unsupported.length) {
    const names = unsupported.map((k) => UNSUPPORTED_NAMES[k] || k).join('、')
    return $infoBox.message.warning(t('匯出不支援以「{0}」篩選，請清除後重新搜尋再匯出', [names]))
  }
  if (!(vdata.applied.createdStart && vdata.applied.createdEnd)) {
    return $infoBox.message.warning(t('匯出需要指定日期區間，請選擇後重新搜尋'))
  }
  if (vdata.total > EXPORT_MAX_ROWS) {
    return $infoBox.message.warning(t('查詢結果共 {0} 筆，超過單次匯出上限 {1} 筆，請縮小日期區間', [vdata.total, EXPORT_MAX_ROWS]))
  }
  $infoBox.confirmPrimary(
    t('確認匯出'),
    t('將匯出 {0} ～ {1} 的查詢結果，共 {2} 筆。', [vdata.applied.createdStart, vdata.applied.createdEnd, vdata.total]),
    () => {
      vdata.exporting = true
      submitExport('PAY_ORDER', vdata.applied)
        .then(() => {
          $infoBox.message.success(t('已建立匯出，完成後可在匯出紀錄下載'))
          exportDrawer.value.open()
        })
        .finally(() => (vdata.exporting = false))
    }
  )
}
</script>
<style lang="less" scoped>
.today-hint {
  margin-bottom: 12px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}
///deep/ .ant-table-fixed{
//  tr{
//    th{
//      padding: 0px 0px;
//    }
//  }
//  }

.order-list {
  -webkit-text-size-adjust: none;
  font-size: 12px;
  display: flex;
  flex-direction: column;

  p {
    white-space: nowrap;
    span {
      display: inline-block;
      font-weight: 800;
      height: 16px;
      line-height: 16px;
      width: 35px;
      border-radius: 5px;
      text-align: center;
      margin-right: 2px;
    }
  }
}
</style>
