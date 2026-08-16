<template>
  <page-header-wrapper>
    <a-card>
      <div class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <jeepay-text-up :placeholder="'服務商號'" v-model:value="vdata.searchData.isvNo" />
            <jeepay-text-up :placeholder="'服務商名稱'" v-model:value="vdata.searchData.isvName" />

            <a-select
              v-model:value="vdata.searchData.state"
              placeholder="服務商狀態"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="0">停用</a-select-option>
              <a-select-option value="1">啟用</a-select-option>
            </a-select>
            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="queryFunc" :loading="vdata.btnLoading">
                搜尋
              </a-button>
              <a-button style="margin-left: 8px" @click="() => (vdata.searchData = {})">
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
        rowKey="isvNo"
      >
        <template #opRow>
          <a-button
            v-if="$access('ENT_ISV_INFO_ADD')"
            type="primary"
            @click="addFunc"
            style="margin-bottom: 30px"
          >
            新建
          </a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'isvName'">
            <b>{{ record.isvName }}</b>
          </template>
          <!-- 自定义插槽 -->
          <template v-if="column.key === 'state'">
            <a-badge
              :status="record.state === 0 ? 'error' : 'processing'"
              :text="record.state === 0 ? '停用' : '啟用'"
            />
          </template>
          <template v-if="column.key === 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                type="link"
                v-if="$access('ENT_ISV_INFO_EDIT')"
                @click="editFunc(record.isvNo)"
              >
                修改
              </a-button>
              <a-button
                type="link"
                v-if="$access('ENT_ISV_PAY_CONFIG_LIST')"
                @click="showPayIfConfigList(record.isvNo)"
              >
                支付設定
              </a-button>
              <a-button
                type="link"
                v-if="$access('ENT_ISV_INFO_DEL')"
                style="color: red"
                @click="delFunc(record.isvNo)"
              >
                刪除
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
    </a-card>
    <!-- 新增页面组件  -->
    <InfoAddOrEdit ref="infoAddOrEdit" :callbackFunc="searchFunc" />
    <!-- 支付参数配置页面组件  -->
    <IsvPayIfConfigList ref="isvPayIfConfigList" />
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { API_URL_ISV_LIST, req } from '@/api/manage'
import InfoAddOrEdit from './AddOrEdit.vue'
import IsvPayIfConfigList from './IsvPayIfConfigList.vue'
import { reactive, ref, getCurrentInstance } from 'vue'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const tableColumns = [
  {
    key: 'isvName',
    width: '200px',
    title: '服務商名稱',
    fixed: 'left',
    scopedSlots: { customRender: 'isvNameSlot' },
  },
  { key: 'isvNo', title: '服務商號', dataIndex: 'isvNo' },
  { key: 'state', title: '服務商狀態', scopedSlots: { customRender: 'stateSlot' } },
  { key: 'createdAt', dataIndex: 'createdAt', title: '建立日期' },
  {
    key: 'op',
    title: '操作',
    width: '260px',
    fixed: 'right',
    align: 'center',
    scopedSlots: { customRender: 'opSlot' },
  },
]

const infoTable = ref()
const infoAddOrEdit = ref()
const isvPayIfConfigList = ref()

const vdata: any = reactive({
  btnLoading: false,
  tableColumns: tableColumns,
  searchData: {},
})

function queryFunc() {
  vdata.btnLoading = true
  infoTable.value.refTable(true)
}
// 请求table接口数据
function reqTableDataFunc(params) {
  return req.list(API_URL_ISV_LIST, params)
}

function delFunc(recordId) {
  $infoBox.confirmDanger('確認刪除？', '請確認該服務商下未分配商戶', () => {
    req.delById(API_URL_ISV_LIST, recordId).then((res) => {
      infoTable.value.refTable(false)
      $infoBox.message.success('刪除成功')
    })
  })
}

function searchFunc() {
  // 点击【查询】按钮点击事件
  infoTable.value.refTable(true)
}
function addFunc() {
  // 业务通用【新增】 函数
  infoAddOrEdit.value.show()
}
function editFunc(recordId) {
  // 业务通用【修改】 函数
  infoAddOrEdit.value.show(recordId)
}
function showPayIfConfigList(recordId) {
  // 支付参数配置
  isvPayIfConfigList.value.show(recordId)
}
</script>
