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
            <jeepay-text-up
              :placeholder="'轉帳/商戶/渠道訂單號'"
              v-model:value="vdata.searchData.unionOrderId"
            />
            <!--            <jeepay-text-up :placeholder="'转账订单号'" :msg="searchData.transferId" v-model:value="searchData.transferId" />-->
            <!--            <jeepay-text-up :placeholder="'商户订单号'" :msg="searchData.mchOrderNo" v-model:value="searchData.mchOrderNo" />-->
            <!--            <jeepay-text-up :placeholder="'渠道支付订单号'" :msg="searchData.channelOrderNo" v-model:value="searchData.channelOrderNo" />-->
            <jeepay-text-up :placeholder="'商戶號'" v-model:value="vdata.searchData.mchNo" />
            <jeepay-text-up :placeholder="'應用AppId'" v-model:value="vdata.searchData.appId" />
            <a-select
              v-model:value="vdata.searchData.state"
              class="table-head-layout"
              placeholder="轉帳狀態"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="0">訂單生成</a-select-option>
              <a-select-option value="1">轉帳中</a-select-option>
              <a-select-option value="2">轉帳成功</a-select-option>
              <a-select-option value="3">轉帳失敗</a-select-option>
            </a-select>
            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="queryFunc" :loading="vdata.btnLoading">
                搜尋
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
                重置
              </a-button>
            </span>
          </div>
        </a-form>
      </div>

      <!-- 列表渲染 -->
      <JeepayTable
        @btnLoadClose="vdata.btnLoading = false"
        ref="infoTable"
        :initData="true"
        :reqTableDataFunc="reqTableDataFunc"
        :tableColumns="vdata.tableColumns"
        :searchData="vdata.searchData"
        rowKey="transferId"
        :tableRowCrossColor="true"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            <b>{{ (record.currency || 'TWD').toUpperCase() }} {{ record.amount / 100 }}</b>
          </template>
          <!-- 自定义插槽 -->
          <template v-if="column.key === 'state'">
            <a-tag
              :key="record.state"
              :color="
                record.state === 0
                  ? 'blue'
                  : record.state === 1
                    ? 'orange'
                    : record.state === 2
                      ? 'green'
                      : 'volcano'
              "
            >
              {{
                record.state === 0
                  ? '訂單生成'
                  : record.state === 1
                    ? '轉帳中'
                    : record.state === 2
                      ? '轉帳成功'
                      : record.state === 3
                        ? '轉帳失敗'
                        : record.state === 4
                          ? '任務關閉'
                          : '未知'
              }}
            </a-tag>
          </template>
          <template v-if="column.key === 'orderNo'">
            <div class="order-list">
              <p>
                <span style="color: #729ed5; background: #e7f5f7">轉帳</span>
                {{ record.transferId }}
              </p>
              <p>
                <span style="color: #56cf56; background: #d8eadf">商戶</span>
                <a-tooltip
                  placement="bottom"
                  style="font-weight: normal"
                  v-if="record.mchOrderNo.length > record.transferId.length"
                >
                  <template slot="title">
                    <span>{{ record.mchOrderNo }}</span>
                  </template>
                  {{ changeStr2ellipsis(record.mchOrderNo, record.transferId.length) }}
                </a-tooltip>
                <span style="font-weight: normal" v-else>{{ record.mchOrderNo }}</span>
              </p>
              <p v-if="record.channelOrderNo">
                <span style="color: #fff; background: #e09c4d">渠道</span>
                <a-tooltip
                  placement="bottom"
                  style="font-weight: normal"
                  v-if="record.channelOrderNo.length > record.transferId.length"
                >
                  <template slot="title">
                    <span>{{ record.channelOrderNo }}</span>
                  </template>
                  {{ changeStr2ellipsis(record.channelOrderNo, record.transferId.length) }}
                </a-tooltip>
                <span style="font-weight: normal" v-else>{{ record.channelOrderNo }}</span>
              </p>
            </div>
          </template>
          <template v-if="column.key === 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                type="link"
                v-if="$access('ENT_TRANSFER_ORDER_VIEW')"
                @click="detailFunc(record.transferId)"
              >
                詳情
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
    </a-card>

    <!-- 订单详情 页面组件  -->
    <TransferOrderDetail ref="transferOrderDetail" />
  </page-header-wrapper>
</template>
<script setup lang="ts">
import TransferOrderDetail from './TransferOrderDetail.vue'
import { API_URL_TRANSFER_ORDER_LIST, req } from '@/api/manage'
import moment from 'moment'
import { reactive, ref, getCurrentInstance } from 'vue'
const { $infoBox, $access, $hasAgentEnt } = getCurrentInstance()!.appContext.config.globalProperties

// eslint-disable-next-line no-unused-vars
const tableColumns = [
  { title: '轉帳金額', key: 'amount', width: 108 },
  { title: '商戶名稱', dataIndex: 'mchName' },
  { key: 'orderNo', title: '訂單號', scopedSlots: { customRender: 'orderSlot' }, width: 260 },
  // { title: '渠道订单号', dataIndex: 'channelOrderNo' },
  { title: '收款帳號', dataIndex: 'accountNo', width: 200 },
  { title: '收款人姓名', dataIndex: 'accountName' },
  { title: '轉帳備註', dataIndex: 'transferDesc' },
  { title: '狀態', key: 'state', width: 100 },
  { title: '建立日期', dataIndex: 'createdAt' },
  {
    title: '操作',
    width: '100px',
    fixed: 'right',
    align: 'center',
    key: 'op',
  },
]

const infoTable = ref()
const transferOrderDetail = ref()

const vdata: any = reactive({
  date: '',

  btnLoading: false,
  tableColumns: tableColumns,
  searchData: {},
  createdStart: '', // 选择开始时间
  createdEnd: '', // 选择结束时间
})
function queryFunc() {
  vdata.btnLoading = true
  infoTable.value.refTable(true)
}
// 请求table接口数据
function reqTableDataFunc(params) {
  return req.list(API_URL_TRANSFER_ORDER_LIST, params)
}
function searchFunc() {
  // 点击【查询】按钮点击事件
  infoTable.value.refTable(true)
}
function detailFunc(recordId) {
  transferOrderDetail.value.show(recordId)
}
function onChange(date, dateString) {
  vdata.searchData.createdStart = dateString[0] // 开始时间
  vdata.searchData.createdEnd = dateString[1] // 结束时间
}
function disabledDate(current) {
  // 今日之后日期不可选
  return current && current > moment().endOf('day')
}
function changeStr2ellipsis(orderNo, baseLength) {
  const halfLengh = Math.floor(baseLength / 2)
  return (
    orderNo.substring(0, halfLengh - 1) +
    '...' +
    orderNo.substring(orderNo.length - halfLengh, orderNo.length)
  )
}
</script>
<style lang="less" scoped>
.order-list {
  -webkit-text-size-adjust: none;
  font-size: 12px;
  display: flex;
  flex-direction: column;

  p {
    margin: 5px 0;
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
