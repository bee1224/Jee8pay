<template>
  <page-header-wrapper>
    <a-card>
      <div class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <jeepay-text-up placeholder="應用AppId" v-model:value="vdata.searchData.appId" />
            <jeepay-text-up placeholder="應用名稱" v-model:value="vdata.searchData.appName" />
            <a-select
              v-model:value="vdata.searchData.state"
              placeholder="狀態"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>

              <a-select-option value="0">禁用</a-select-option>
              <a-select-option value="1">啟用</a-select-option>
            </a-select>
            <span class="table-page-search-submitButtons" style="flex-grow: 0; flex-shrink: 0">
              <a-button type="primary" @click="queryFunc" :loading="vdata.btnLoading">
                查詢
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
        rowKey="appId"
      >
        <template #opRow>
          <a-button v-if="$access('ENT_MCH_APP_ADD')" type="primary" @click="addFunc">
            新建
          </a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'appId'">
            <b>{{ record.appId }}</b>
          </template>
          <!-- 自定义插槽 -->
          <template v-if="column.key === 'state'">
            <a-badge
              :status="record.state === 0 ? 'error' : 'processing'"
              :text="record.state === 0 ? '禁用' : '啟用'"
            />
          </template>
          <template v-if="column.key === 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                type="link"
                v-if="$access('ENT_MCH_APP_EDIT')"
                @click="editFunc(record.appId)"
              >
                修改
              </a-button>
              <a-button
                type="link"
                v-if="$access('ENT_MCH_PAY_CONFIG_LIST') && record.state"
                @click="showPayIfConfigList(record.appId)"
              >
                支付配置
              </a-button>
              <a-button type="link" v-if="$access('ENT_MCH_PAY_TEST') && record.state">
                <router-link :to="{ name: 'ENT_MCH_PAY_TEST', params: { appId: record.appId } }">
                  支付測試
                </router-link>
              </a-button>
              <a-button type="link" v-if="$access('ENT_MCH_TRANSFER') && record.state">
                <router-link :to="{ name: 'ENT_MCH_TRANSFER', params: { appId: record.appId } }">
                  發起轉帳
                </router-link>
              </a-button>
              <a-button
                type="link"
                v-if="$access('ENT_MCH_APP_DEL')"
                style="color: red"
                @click="delFunc(record.appId)"
              >
                刪除
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
    </a-card>
    <!-- 新增应用  -->
    <MchAppAddOrEdit ref="mchAppAddOrEditRef" :callbackFunc="searchFunc" />
    <!-- 支付参数配置页面组件  -->
    <MchPayIfConfigList ref="mchPayIfConfigList" />
  </page-header-wrapper>
</template>

<script setup lang="ts">
import { API_URL_MCH_APP, req } from '@/api/manage'
import MchAppAddOrEdit from './AddOrEdit.vue'
import MchPayIfConfigList from './MchPayIfConfigList.vue'
import { reactive, ref, getCurrentInstance } from 'vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

// eslint-disable-next-line no-unused-vars
const tableColumns = [
  {
    key: 'appId',
    fixed: 'left',
    width: '320px',
    title: '應用AppId',
  },
  { key: 'appName', title: '應用名稱', dataIndex: 'appName' },
  { key: 'state', title: '狀態', scopedSlots: { customRender: 'stateSlot' } },
  { key: 'createdAt', dataIndex: 'createdAt', title: '建立日期' },
  {
    key: 'op',
    title: '操作',
    width: '260px',
    fixed: 'right',
    align: 'center',
  },
]

const infoTable = ref()
const mchAppAddOrEditRef = ref()
const mchPayIfConfigList = ref()

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
  return req.list(API_URL_MCH_APP, params)
}
function searchFunc() {
  // 点击【查询】按钮点击事件
  infoTable.value.refTable(true)
}
function addFunc() {
  // 业务通用【新增】 函数
  mchAppAddOrEditRef.value.show()
}
function editFunc(recordId) {
  // 业务通用【修改】 函数
  mchAppAddOrEditRef.value.show(recordId)
}
function delFunc(appId) {
  $infoBox.confirmDanger('確認刪除？', '', () => {
    req.delById(API_URL_MCH_APP, appId).then((res) => {
      $infoBox.message.success('刪除成功！')
      searchFunc()
    })
  })
}
function showPayIfConfigList(recordId) {
  // 支付参数配置
  mchPayIfConfigList.value.show(recordId)
}
</script>

<style lang="less" scoped></style>
