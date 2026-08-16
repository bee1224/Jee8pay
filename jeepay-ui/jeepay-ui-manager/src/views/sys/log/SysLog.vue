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
            <jeepay-text-up :placeholder="'用戶ID'" v-model:value="vdata.searchData.userId" />
            <jeepay-text-up :placeholder="'用戶名稱'" v-model:value="vdata.searchData.userName" />
            <a-select
              v-model:value="vdata.searchData.sysType"
              placeholder="所屬系統"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="MGR">營運平台</a-select-option>
              <a-select-option value="MCH">商戶系統</a-select-option>
            </a-select>

            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="queryhFunc" :loading="vdata.btnLoading">
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
        <div>
          <a-button
            v-if="$access('ENT_SYS_LOG_DEL')"
            type="primary"
            danger
            @click="delFunc"
            class="mg-b-30"
          >
            刪除
          </a-button>
        </div>
      </div>

      <!-- 列表渲染 -->
      <JeepayTable
        @btnLoadClose="vdata.btnLoading = false"
        ref="infoTable"
        :initData="true"
        :row-selection="{ onChange: onSelectChange }"
        :reqTableDataFunc="reqTableDataFunc"
        :tableColumns="tableColumns"
        :searchData="vdata.searchData"
        rowKey="sysLogId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userName'">
            <b>{{ record.userName }}</b>
          </template>

          <template v-if="column.key === 'sysType'">
            <a-tag
              :key="record.sysType"
              :color="
                record.sysType === 'MGR' ? 'green' : record.sysType === 'MCH' ? 'geekblue' : 'loser'
              "
            >
              {{
                record.sysType === 'MGR'
                  ? '營運平台'
                  : record.sysType === 'MCH'
                    ? '商戶系統'
                    : '其他'
              }}
            </a-tag>
          </template>

          <template v-if="column.key === 'op'">
            <a-button
              type="link"
              v-if="$access('ENT_SYS_LOG_VIEW')"
              @click="detailFunc(record.sysLogId)"
            >
              詳情
            </a-button>
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
        v-model:open="vdata.visible"
        :title="vdata.visible === true ? '日誌詳情' : ''"
        @close="onClose"
      >
        <a-row justify="space-between" type="flex">
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="用戶ID">
                {{ vdata.detailData.userId }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="用戶IP">
                {{ vdata.detailData.userIp }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="用戶名稱">
                <b>
                  {{ vdata.detailData.userName }}
                </b>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="12">
            <a-descriptions>
              <a-descriptions-item label="所屬系統">
                <a-tag
                  :key="vdata.detailData.sysType"
                  :color="
                    vdata.detailData.sysType === 'MGR'
                      ? 'green'
                      : vdata.detailData.sysType === 'MCH'
                        ? 'geekblue'
                        : 'loser'
                  "
                >
                  {{
                    vdata.detailData.sysType === 'MGR'
                      ? '營運平台'
                      : vdata.detailData.sysType === 'MCH'
                        ? '商戶系統'
                        : '其他'
                  }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>
        <a-divider />
        <a-row justify="space-between" type="flex">
          <a-col :sm="24">
            <a-descriptions>
              <a-descriptions-item label="操作描述">
                {{ vdata.detailData.methodRemark }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="24">
            <a-descriptions>
              <a-descriptions-item label="請求方法">
                {{ vdata.detailData.methodName }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
          <a-col :sm="24">
            <a-descriptions>
              <a-descriptions-item label="請求地址">
                {{ vdata.detailData.reqUrl }}
              </a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>
        <a-row justify="start" type="flex">
          <a-col :sm="24">
            <a-form-item label="請求參數">
              <a-textarea
                disabled="disabled"
                style="background-color: black; color: #ffffff; height: 100px"
                v-model:value="vdata.detailData.optReqParam"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row justify="start" type="flex">
          <a-col :sm="24">
            <a-form-item label="回應參數">
              <a-textarea
                disabled="disabled"
                style="background-color: black; color: #ffffff; height: 150px"
                v-model:value="vdata.detailData.optResInfo"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-drawer>
    </template>
  </page-header-wrapper>
</template>

<script setup lang="ts">
// import JeepayTableColumns from '@/components/JeepayTable/JeepayTableColumns.vue'
import { API_URL_SYS_LOG, req } from '@/api/manage'
import moment from 'moment'
import { message, Modal } from 'ant-design-vue'
import { ref, reactive, computed, getCurrentInstance } from 'vue'

const { $infoBox, $access, $hasAgentEnt } = getCurrentInstance()!.appContext.config.globalProperties

const infoTable = ref()

// eslint-disable-next-line no-unused-vars
const tableColumns = reactive([
  {
    key: 'userName',
    title: '用戶名稱',
    fixed: 'left',
    scopedSlots: { customRender: 'userNameSlot' },
  },
  { key: 'userId', title: '用戶ID', dataIndex: 'userId' },
  { key: 'userIp', title: '用戶IP', dataIndex: 'userIp' },
  { key: 'sysType', title: '所屬系統', scopedSlots: { customRender: 'sysTypeSlot' } },
  { key: 'methodRemark', title: '操作描述', ellipsis: true, dataIndex: 'methodRemark' },
  { key: 'createdAt', dataIndex: 'createdAt', title: '建立日期' },
  {
    key: 'op',
    title: '操作',
    width: '100px',
    fixed: 'right',
    align: 'center',
    scopedSlots: { customRender: 'opSlot' },
  },
])

const vdata: any = reactive({
  date: '',

  searchData: {},
  selectedIds: [],
  createdStart: '',
  createdEnd: '',
  visible: false,
  detailData: {},
  btnLoading: false,
})

// 请求table接口数据
function reqTableDataFunc(params) {
  return req.list(API_URL_SYS_LOG, params)
}

function delFunc() {
  if (vdata.selectedIds.length === 0) {
    message.error('請選擇要刪除的日誌')
    return false
  }
  Modal.confirm({
    title: '確認刪除' + vdata.selectedIds.length + '筆日誌嗎？',
    okType: 'danger',
    onOk() {
      req.delById(API_URL_SYS_LOG, vdata.selectedIds).then((res) => {
        vdata.selectedIds = [] // 清空选中数组
        infoTable.value.refTable(true)
        message.success('刪除成功')
      })
    },
  })
}
function searchFunc() {
  // 点击【查询】按钮点击事件
  infoTable.value.refTable(true)
}

function onSelectChange(selectedRowKeys) {
  vdata.selectedIds = selectedRowKeys // 清空选中数组
}

function detailFunc(recordId) {
  req.getById(API_URL_SYS_LOG, recordId).then((res) => {
    vdata.detailData = res
  })
  vdata.visible = true
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
function queryhFunc() {
  infoTable.value.refTable(true)
}
</script>
