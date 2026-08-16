<template>
  <page-header-wrapper>
    <a-card>
      <div class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <jeepay-text-up v-model:value="searchData.mchNo" :placeholder="'商戶號'" />
            <jeepay-text-up v-model:value="searchData.isvNo" :placeholder="'服務商號'" />
            <jeepay-text-up v-model:value="searchData.mchName" :placeholder="'商戶名稱'" />
            <a-select
              v-model:value="searchData.state"
              placeholder="商戶狀態"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="0">停用</a-select-option>
              <a-select-option value="1">啟用</a-select-option>
            </a-select>
            <a-select
              v-model:value="searchData.type"
              placeholder="商戶類型"
              class="table-head-layout"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="1">普通商戶</a-select-option>
              <a-select-option value="2">特約商戶</a-select-option>
            </a-select>
            <span class="table-page-search-submitButtons" style="flex-grow: 0; flex-shrink: 0">
              <a-button type="primary" :loading="vdata.btnLoading" @click="queryFunc">
                查詢
              </a-button>
              <a-button style="margin-left: 8px" @click="searchData = {}">重置</a-button>
            </span>
          </div>
        </a-form>
      </div>

      <!-- 列表渲染 -->
      <JeepayTable
        ref="infoTable"
        :init-data="true"
        :req-table-data-func="reqTableDataFunc"
        :table-columns="tableColumns"
        :search-data="searchData"
        row-key="mchNo"
        @btnLoadClose="vdata.btnLoading = false"
      >
        <template #opRow>
          <a-button type="primary" @click="addFunc" v-if="$access('ENT_MCH_INFO_ADD')">
            新建
          </a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'mchName'">
            <a v-if="!$access('ENT_MCH_INFO_VIEW')" @click="detailFunc(record.mchNo)">
              {{ record.mchName }}
            </a>
            <a v-if="$access('ENT_MCH_INFO_VIEW')" @click="detailFunc(record.mchNo)">
              {{ record.mchName }}
            </a>
          </template>

          <template v-if="column.key === 'state'">
            <a-badge
              :status="record.state === 0 ? 'error' : 'processing'"
              :text="record.state === 0 ? '停用' : '啟用'"
            />
          </template>

          <template v-if="column.key === 'type'">
            <a-tag :color="record.type === 1 ? 'green' : 'orange'">
              {{ record.type === 1 ? '普通商戶' : '特約商戶' }}
            </a-tag>
          </template>

          <template v-if="column.key === 'operation'">
            <a-button
              type="link"
              @click="editFunc(record.mchNo)"
              v-if="$access('ENT_MCH_INFO_EDIT')"
            >
              修改
            </a-button>
            <a-button
              type="link"
              @click="mchAppConfig(record.mchNo)"
              v-if="$access('ENT_MCH_APP_CONFIG')"
            >
              應用設定
            </a-button>
            <a-button
              type="link"
              style="color: red"
              @click="delFunc(record.mchNo)"
              v-if="$access('ENT_MCH_INFO_DEL')"
            >
              刪除
            </a-button>
          </template>
        </template>
      </JeepayTable>
    </a-card>
    <!-- 新增页面组件  -->
    <InfoAddOrEdit ref="infoAddOrEdit" :callback-func="searchFunc" />
    <!-- 新增页面组件  -->
    <InfoDetail ref="infoDetail" :callback-func="searchFunc" />
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { API_URL_MCH_LIST, req, reqLoad } from '@/api/manage'
import InfoAddOrEdit from './AddOrEdit.vue'
import InfoDetail from './Detail.vue'
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const infoDetail = ref()
const infoAddOrEdit = ref()
const infoTable = ref()

// eslint-disable-next-line no-unused-vars
let tableColumns = reactive([
  { key: 'mchName', fixed: 'left', width: '200px', title: '商戶名稱' },
  { key: 'mchNo', title: '商戶號', dataIndex: 'mchNo' },
  { key: 'isvNo', title: '服務商號', dataIndex: 'isvNo' },
  { key: 'state', title: '狀態', width: '130px' },
  { key: 'type', title: '商戶類型', width: '130px' },
  { key: 'createdAt', dataIndex: 'createdAt', title: '建立日期' },
  { key: 'operation', title: '操作', width: '260px', fixed: 'right', align: 'center' },
])

const vdata: any = reactive({
  btnLoading: false,
})

let searchData: any = ref({})

function queryFunc() {
  vdata.btnLoading = true
  infoTable.value.refTable(true)
}
// 请求table接口数据
function reqTableDataFunc(params) {
  return req.list(API_URL_MCH_LIST, params)
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
function detailFunc(recordId) {
  // 商户详情页
  infoDetail.value.show(recordId)
}
// 删除商户
function delFunc(recordId) {
  $infoBox.confirmDanger('確認刪除？', '該操作將刪除商戶下所有設定及用戶資訊', () => {
    reqLoad.delById(API_URL_MCH_LIST, recordId).then((res) => {
      infoTable.value.refTable(true)
      $infoBox.message.success('刪除成功')
    })
  })
}
function mchAppConfig(recordId) {
  // 应用配置
  router.push({
    path: '/apps',
    query: { mchNo: recordId },
  })
}
</script>
