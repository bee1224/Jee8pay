<template>
  <page-header-wrapper>
    <a-card>
      <div v-if="history" class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <a-range-picker
              v-model:value="vdata.date"
              class="table-head-layout range-picker-full"
              :show-time="{ format: 'HH:mm:ss' }"
              format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disabledDate"
              @change="onChange"
            >
              <a-icon slot="suffixIcon" type="sync" />
            </a-range-picker>
            <jeepay-text-up
              v-model:value="vdata.searchData.unionOrderId"
              :placeholder="'支付/商戶/渠道訂單號'"
              :msg="vdata.searchData.unionOrderId"
            />
            <!--            <jeepay-text-up :placeholder="'支付订单号'" :msg="searchData.payOrderId" v-model:value="searchData.payOrderId" />-->
            <!--            <jeepay-text-up :placeholder="'商户订单号'" :msg="searchData.mchOrderNo" v-model:value="searchData.mchOrderNo" />-->
            <jeepay-text-up v-model:value="vdata.searchData.mchNo" :placeholder="'商戶號'" />
            <jeepay-text-up v-model:value="vdata.searchData.isvNo" :placeholder="'服務商號'" />
            <jeepay-text-up v-model:value="vdata.searchData.appId" :placeholder="'應用AppId'" />

            <a-select
              v-model:value="vdata.searchData.wayCode"
              placeholder="支付方式"
              v-if="$access('ENT_PAY_ORDER_SEARCH_PAY_WAY')"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option
                v-for="item in vdata.payWayList"
                :key="item.wayCode"
                :value="item.wayCode"
              >
                {{ item.wayName }}
              </a-select-option>
            </a-select>
            <a-select
              v-model:value="vdata.searchData.state"
              placeholder="支付狀態"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="0">訂單生成</a-select-option>
              <a-select-option value="1">支付中</a-select-option>
              <a-select-option value="2">支付成功</a-select-option>
              <a-select-option value="3">支付失敗</a-select-option>
              <a-select-option value="4">已撤銷</a-select-option>
              <a-select-option value="5">已退款</a-select-option>
              <a-select-option value="6">訂單關閉</a-select-option>
            </a-select>
            <a-select
              v-model:value="vdata.searchData.notifyState"
              placeholder="回調狀態"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="0">未發送</a-select-option>
              <a-select-option value="1">已發送</a-select-option>
            </a-select>
            <a-select
              v-model:value="vdata.searchData.divisionState"
              placeholder="分帳狀態"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="0">未發生分帳</a-select-option>
              <a-select-option value="1">等待分帳任務處理</a-select-option>
              <a-select-option value="2">分帳處理中</a-select-option>
              <a-select-option value="3">分帳任務已結束（狀態請看分帳記錄）</a-select-option>
            </a-select>
            <span class="table-page-search-submitButtons">
              <a-button type="primary" :loading="vdata.btnLoading" @click="queryFunc">
                搜尋
              </a-button>
              <a-button
                style="margin-left: 8px"
                @click="resetFunc"
              >
                重置
              </a-button>
              <a-button v-if="vdata.applied && $access('ENT_EXPORT_CENTER')" style="margin-left: 8px" :loading="vdata.exporting" :disabled="!vdata.total" @click="exportFunc">匯出</a-button>
              <a-button v-if="$access('ENT_EXPORT_CENTER')" style="margin-left: 8px" @click="exportDrawer.open()">匯出紀錄</a-button>
            </span>
          </div>
        </a-form>
      </div>
      <div v-else class="today-hint">僅顯示今日訂單；查詢其他日期、篩選或匯出請到「歷史查詢 → 代收查詢」。</div>

      <!-- 列表渲染 -->
      <a-empty v-if="history && !vdata.applied" style="padding: 60px 0" description="請先設定查詢條件，再按「搜尋」" />
      <JeepayTable
        v-show="!history || vdata.applied"
        ref="infoTable"
        :init-data="!history"
        :req-table-data-func="reqTableDataFunc"
        :table-columns="vdata.tableColumns"
        :search-data="vdata.searchData"
        row-key="payOrderId"
        :table-row-cross-color="true"
        @btnLoadClose="vdata.btnLoading = false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'mchFeeAmount'">
            {{ (record.currency || 'TWD').toUpperCase() }} {{ (record.mchFeeAmount / 100).toFixed(2) }}
          </template>

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
                      : record.state === 6
                        ? ''
                        : 'volcano'
              "
            >
              {{
                record.state === 0
                  ? '訂單生成'
                  : record.state === 1
                    ? '支付中'
                    : record.state === 2
                      ? '支付成功'
                      : record.state === 3
                        ? '支付失敗'
                        : record.state === 4
                          ? '已撤銷'
                          : record.state === 5
                            ? '已退款'
                            : record.state === 6
                              ? '訂單關閉'
                              : '未知'
              }}
            </a-tag>
          </template>
          <template v-if="column.key === 'notifyState'">
            <a-badge
              :status="record.notifyState === 1 ? 'processing' : 'error'"
              :text="record.notifyState === 1 ? '已發送' : '未發送'"
            />
          </template>
          <template v-if="column.key === 'orderNo'">
            <div class="order-list">
              <p>
                <span class="order-label" style="color: #729ed5; background: #e7f5f7">支付</span>{{ record.payOrderId }}
              </p>
              <p>
                <span class="order-label" style="color: #56cf56; background: #d8eadf">商戶</span><a-tooltip
                  v-if="record.mchOrderNo.length > record.payOrderId.length"
                  placement="bottom"
                  style="font-weight: normal"
                >
                  <template #title>
                    <span>{{ record.mchOrderNo }}</span>
                  </template>{{ changeStr2ellipsis(record.mchOrderNo, record.payOrderId.length) }}</a-tooltip><span v-else style="font-weight: normal">{{ record.mchOrderNo }}</span>
              </p>
              <p v-if="record.channelOrderNo">
                <span class="order-label" style="color: #fff; background: #e09c4d">渠道</span><a-tooltip
                  v-if="record.channelOrderNo.length > record.payOrderId.length"
                  placement="bottom"
                  style="font-weight: normal"
                >
                  <template #title>
                    <span>{{ record.channelOrderNo }}</span>
                  </template>{{ changeStr2ellipsis(record.channelOrderNo, record.payOrderId.length) }}</a-tooltip><span v-else style="font-weight: normal">{{ record.channelOrderNo }}</span>
              </p>
            </div>
          </template>
          <template v-if="column.key === 'op'">
            <!-- 操作列插槽：僅詳情；人工回調/退款在訂單詳情最下方，避免誤觸 -->
            <JeepayTableColumns>
              <a-button
                v-if="$access('ENT_PAY_ORDER_VIEW')"
                type="link"
                @click="detailFunc(record.payOrderId)"
              >
                詳情
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
      <ExportJobsDrawer v-if="history" ref="exportDrawer" :job-types="['PAY_ORDER']" />
    </a-card>
    <!-- 退款弹出框 -->
    <refund-modal ref="refundModalInfo" :callback-func="searchFunc" />
    <!-- 日志详情抽屉 -->
    <template>
      <a-drawer
        width="50%"
        placement="right"
        :closable="true"
        v-model:open="vdata.visible"
        :title="vdata.visible === true ? '訂單詳情' : ''"
        @close="onClose"
      >
        <a-row justify="space-between" type="flex">
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="所屬系統">
                {{
                  vdata.detailData.mchType === 1
                    ? '普通商戶'
                    : vdata.detailData.mchType === 2
                      ? '特約商戶'
                      : '未知'
                }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="服務商號">
                {{ vdata.detailData.isvNo }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="支付訂單號">
                <a-tag color="purple">
                  {{ vdata.detailData.payOrderId }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="商戶號">
                {{ vdata.detailData.mchNo }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="商戶訂單號">
                {{ vdata.detailData.mchOrderNo }}
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
              <a-descriptions-item label="支付金額">
                <a-tag color="green">
                  {{ vdata.detailData.amount / 100 }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="手續費">
                <a-tag color="pink">
                  {{ vdata.detailData.mchFeeAmount / 100 }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="商家費率">
                {{ (vdata.detailData.mchFeeRate * 100).toFixed(2) }}%
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <!-- ADR-0009：下單當下的四層手續費快照 -->
          <a-col :sm="24">
            <a-descriptions>
              <a-descriptions-item label="四層手續費">
                <template v-if="vdata.feeSnapshot">
                  平臺 {{ vdata.feeSnapshot.platformFee / 100 }}／渠道 {{ vdata.feeSnapshot.channelFee / 100 }}／高代
                  {{ vdata.feeSnapshot.srAgentFee / 100 }}／代理 {{ vdata.feeSnapshot.agentFee / 100 }}／推薦
                  {{ (vdata.feeSnapshot.referrerFee || 0) / 100 }}，合計
                  {{ vdata.feeSnapshot.totalFee / 100 }} 元
                  <a-tag v-if="vdata.feeSnapshot.exceedsMchFee === 1" color="red">超過商戶手續費</a-tag>
                  <span v-if="vdata.feeSnapshot.agentNo">（代理 {{ vdata.feeSnapshot.agentNo }}）</span>
                </template>
                <span v-else style="color: #999">無快照</span>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="訂單狀態">
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
                      ? '訂單生成'
                      : vdata.detailData.state === 1
                        ? '支付中'
                        : vdata.detailData.state === 2
                          ? '支付成功'
                          : vdata.detailData.state === 3
                            ? '支付失敗'
                            : vdata.detailData.state === 4
                              ? '已撤銷'
                              : vdata.detailData.state === 5
                                ? '已退款'
                                : vdata.detailData.state === 6
                                  ? '訂單關閉'
                                  : '未知'
                  }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="回調狀態">
                <a-tag :color="vdata.detailData.notifyState === 1 ? 'green' : 'volcano'">
                  {{
                    vdata.detailData.notifyState === 0
                      ? '未發送'
                      : vdata.detailData.notifyState === 1
                        ? '已發送'
                        : '未知'
                  }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="應用APPID">
                {{ vdata.detailData.appId }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="支付錯誤碼">
                {{ vdata.detailData.errCode }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="支付錯誤描述">
                {{ vdata.detailData.errMsg }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="訂單失效時間">
                {{ vdata.detailData.expiredTime }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="支付成功時間">
                {{ vdata.detailData.successTime }}
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
          <a-divider />
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="商品標題">
                {{ vdata.detailData.subject }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="商品描述">
                {{ vdata.detailData.body }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="介面代碼">
                {{ vdata.detailData.ifCode }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="貨幣代碼">
                {{ vdata.detailData.currency }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="支付方式">
                {{ vdata.detailData.wayCode }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="客戶端IP">
                {{ vdata.detailData.clientIp }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="用戶標識">
                {{ vdata.detailData.channelUser }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="渠道訂單號">
                {{ vdata.detailData.channelOrderNo }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="異步通知地址">
                {{ vdata.detailData.notifyUrl }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="頁面跳轉地址">
                {{ vdata.detailData.returnUrl }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="退款狀態">
                <a-tag
                  :color="
                    vdata.detailData.refundState === 0
                      ? 'blue'
                      : vdata.detailData.refundState === 1
                        ? 'orange'
                        : vdata.detailData.refundState === 2
                          ? 'green'
                          : 'volcano'
                  "
                >
                  {{
                    vdata.detailData.refundState === 0
                      ? '未發起'
                      : vdata.detailData.refundState === 1
                        ? '部分退款'
                        : vdata.detailData.refundState === 2
                          ? '全額退款'
                          : '未知'
                  }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="退款次數">
                {{ vdata.detailData.refundTimes }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="退款總額">
                <a-tag v-if="vdata.detailData.refundAmount" color="cyan">
                  {{ vdata.detailData.refundAmount / 100 }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-divider />
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="訂單分帳模式">
                <span v-if="vdata.detailData.divisionMode == 0">該筆訂單不允許分帳</span>
                <span v-else-if="vdata.detailData.divisionMode == 1">
                  支付成功按設定自動完成分帳
                </span>
                <span v-else-if="vdata.detailData.divisionMode == 2">
                  商戶手動分帳(解凍商戶金額)
                </span>
                <span v-else>未知</span>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="分帳狀態">
                <a-tag v-if="vdata.detailData.divisionState == 0" color="blue">未發生分帳</a-tag>
                <a-tag v-else-if="vdata.detailData.divisionState == 1" color="orange">待分帳</a-tag>
                <a-tag v-else-if="vdata.detailData.divisionState == 2" color="red">
                  分帳處理中
                </a-tag>
                <a-tag v-else-if="vdata.detailData.divisionState == 3" color="green">
                  任務已結束
                </a-tag>
                <a-tag v-else color="#f50">未知</a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="最新分帳發起時間">
                {{ vdata.detailData.divisionLastTime }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>
        <a-divider />

        <a-row justify="start" type="flex">
          <a-col :sm="24">
            <a-form layout="vertical">
              <a-form-item label="擴充參數:">
                <a-textarea
                  v-model:value="vdata.detailData.extParam"
                  disabled="disabled"
                  style="height: 100px; color: black"
                />
              </a-form-item>
            </a-form>
          </a-col>
        </a-row>

        <!-- 底部操作：人工回調 / 退款（放在最下面，避免誤觸） -->
        <a-divider />
        <a-row justify="start" type="flex">
          <a-col :sm="24">
            <a-button
              v-if="$access('ENT_PAY_ORDER_MANUAL_NOTIFY')"
              type="primary"
              danger
              @click="manualNotifyFunc(vdata.detailData)"
            >
              人工回調
            </a-button>
            <a-button
              v-if="$access('ENT_PAY_ORDER_REFUND')"
              v-show="vdata.detailData.state === 2 && vdata.detailData.refundState !== 2"
              type="primary"
              danger
              style="margin-left: 8px"
              :disabled="!REFUND_CAPABLE"
              @click="openFunc(vdata.detailData, vdata.detailData.payOrderId)"
            >
              退款
            </a-button>
          </a-col>
        </a-row>
      </a-drawer>
    </template>
  </page-header-wrapper>
</template>
<script setup lang="ts">
import RefundModal from './RefundModal.vue' // 退款弹出框
import { API_URL_PAY_ORDER_FEE, API_URL_PAY_ORDER_LIST, API_URL_PAY_ORDER_MANUAL_NOTIFY, API_URL_PAYWAYS_LIST, req } from '@/api/manage'
import moment from 'moment'
import { submitExport } from '@/utils/exportJob'
import ExportJobsDrawer from '@/components/ExportJobs/ExportJobsDrawer.vue'
import { reactive, ref, getCurrentInstance, onMounted, watch } from 'vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

// CCAT 目前尚無退款能力（Provider capability 未包含退款）：
// 退款按鈕先「停用但保留顯示」，避免誤導可操作；待退款功能上線後改為 true 即可恢復。
const REFUND_CAPABLE = false

// eslint-disable-next-line no-unused-vars
const tableColumns = [
  {
    key: 'amount',
    title: '支付金額',
    ellipsis: true,
    width: 108,
    fixed: 'left',
    scopedSlots: { customRender: 'amountSlot' },
  },
  {
    key: 'mchFeeAmount',
    dataIndex: 'mchFeeAmount',
    title: '手續費',
    width: 100,
  },
  { key: 'mchName', title: '商戶名稱', dataIndex: 'mchName', ellipsis: true, width: 100 },
  { key: 'orderNo', title: '訂單號', scopedSlots: { customRender: 'orderSlot' }, width: 210 },
  // { key: 'payOrderId', title: '支付订单号', dataIndex: 'payOrderId' },
  // { key: 'mchOrderNo', title: '商户订单号', dataIndex: 'mchOrderNo' },
  { key: 'wayName', title: '支付方式', dataIndex: 'wayName', width: 120 },
  { key: 'state', title: '支付狀態', scopedSlots: { customRender: 'stateSlot' }, width: 100 },
  {
    key: 'notifyState',
    title: '回調狀態',
    scopedSlots: { customRender: 'notifySlot' },
    width: 100,
  },
  { key: 'createdAt', dataIndex: 'createdAt', title: '建立日期', width: 170 },
  {
    key: 'op',
    title: '操作',
    width: 120,
    fixed: 'right',
    align: 'center',
    scopedSlots: { customRender: 'opSlot' },
  },
]

const infoTable = ref()
const refundModalInfo = ref()

// history=true 為「歷史查詢 → 代收查詢」（完整篩選與匯出）；否則為訂單管理，只顯示今日訂單
const props = defineProps({ history: { type: Boolean, default: false } })
const exportDrawer = ref()

const vdata: any = reactive({
  applied: null, // 歷史查詢：最近一次送出搜尋時的條件；null 表示尚未搜尋
  total: 0, // 歷史查詢：最近一次搜尋的總筆數
  exporting: false,
  date: '',

  btnLoading: false,
  tableColumns: tableColumns,
  searchData: {},
  createdStart: '', // 选择开始时间
  createdEnd: '', // 选择结束时间
  visible: false,
  detailData: {},
  feeSnapshot: null,
  payWayList: [],

  dateOneFlag: false,
  dateOneFunction: () => {},
})

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
      return $infoBox.message.warning('請先選擇日期區間，或輸入訂單號')
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
    $infoBox.message.modalError('訂單無可退款金額', '')
  }
  refundModalInfo.value.show(recordId)
}
function detailFunc(recordId) {
  req.getById(API_URL_PAY_ORDER_LIST, recordId).then((res) => {
    vdata.detailData = res
  })
  vdata.feeSnapshot = null
  req.getById(API_URL_PAY_ORDER_FEE, recordId).then((res) => {
    vdata.feeSnapshot = res
  })
  vdata.visible = true
}

// 人工手動回調：繞過支付，把訂單目前狀態直接通知到異步通知地址（外部商戶回調測試用）
function manualNotifyFunc(record) {
  const stateText = record.state === 6 ? '訂單關閉' : 'state=' + record.state
  $infoBox.confirmDanger(
    '確認人工回調？',
    `將訂單 ${record.payOrderId}（${stateText}）以標準 Merchant Notify 格式直接通知到異步通知地址。通知內容為訂單目前狀態，外部商戶不會以非 SUCCESS 上分。`,
    () => {
      req
        .add(API_URL_PAY_ORDER_MANUAL_NOTIFY, { payOrderId: record.payOrderId })
        .then((res) => {
          $infoBox.message.success(`已推送通知，notifyId=${res && res.notifyId}`)
        })
    }
  )
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
  vdata.visible = false
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
const UNSUPPORTED_NAMES = { unionOrderId: '訂單號', isvNo: '服務商號', appId: '應用AppId', notifyState: '回調狀態', divisionState: '分帳狀態' }
const EXPORT_MAX_ROWS = 200000

// 背景匯出：只匯出最近一次搜尋的結果，完成後在「匯出紀錄」下載
function exportFunc() {
  if (vdata.exporting) return
  if (!vdata.applied) {
    return $infoBox.message.warning('請先搜尋，確認結果後再匯出')
  }
  if (JSON.stringify(cleanParams(vdata.searchData)) !== JSON.stringify(vdata.applied)) {
    return $infoBox.message.warning('查詢條件已變更，請先重新搜尋再匯出')
  }
  if (!vdata.total) {
    return $infoBox.message.warning('目前的查詢沒有資料可匯出')
  }
  const unsupported = Object.keys(vdata.applied).filter((k) => !EXPORT_KEYS.includes(k))
  if (unsupported.length) {
    const names = unsupported.map((k) => UNSUPPORTED_NAMES[k] || k).join('、')
    return $infoBox.message.warning(`匯出不支援以「${names}」篩選，請清除後重新搜尋再匯出`)
  }
  if (!(vdata.applied.createdStart && vdata.applied.createdEnd)) {
    return $infoBox.message.warning('匯出需要指定日期區間，請選擇後重新搜尋')
  }
  if (vdata.total > EXPORT_MAX_ROWS) {
    return $infoBox.message.warning(`查詢結果共 ${vdata.total} 筆，超過單次匯出上限 ${EXPORT_MAX_ROWS} 筆，請縮小日期區間`)
  }
  $infoBox.confirmPrimary(
    '確認匯出',
    `將匯出 ${vdata.applied.createdStart} ～ ${vdata.applied.createdEnd} 的查詢結果，共 ${vdata.total} 筆。`,
    () => {
      vdata.exporting = true
      submitExport('PAY_ORDER', vdata.applied)
        .then(() => {
          $infoBox.message.success('已建立匯出，完成後可在匯出紀錄下載')
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
/* 建立時間區間選取器：放寬以完整顯示起訖時間 */
.range-picker-full {
  max-width: 480px;
}
.order-list {
  -webkit-text-size-adjust: none;
  font-size: 12px;
  display: flex;
  flex-direction: column;

  p {
    white-space: nowrap;
    margin: 5px 0;
    .order-label {
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
