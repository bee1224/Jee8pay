<template>
  <page-header-wrapper>
    <a-card>
      <div class="table-page-search-wrapper">
        <a-form layout="inline" style="margin-bottom: 30px">
          <a-row :gutter="16">
            <a-col :sm="18">
              <a-row :gutter="16">
                <a-col :md="6">
                  <a-form-item label="">
                    <a-select
                      v-model:value="vdata.querySysType"
                      :placeholder="$t('選擇系統選單')"
                      @change="refTable"
                      class="table-head-layout"
                    >
                      <a-select-option value="MGR">{{ $t('顯示選單：營運平台') }}</a-select-option>
                      <a-select-option value="MCH">{{ $t('顯示選單：商戶系統') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-col>
          </a-row>
        </a-form>
      </div>

      <JeepayTable
        ref="infoTable"
        :init-data="false"
        :table-columns="vdata.tableColumns"
        :req-table-data-func="reqTableDataFunc"
        :pagination="false"
        :loading="vdata.loading"
        rowKey="entId"
        :scroll="{ x: 1450 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <JeepayTableColState
              :state="record.state"
              :showSwitchType="$access('ENT_UR_ROLE_ENT_EDIT')"
              :onChange="
                (state) => {
                  return updateState(record.entId, state)
                }
              "
            />
          </template>
          <template v-if="column.key === 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                v-if="$access('ENT_UR_ROLE_ENT_EDIT')"
                type="link"
                @click="editFunc(record.entId)"
              >
                {{ $t('修改') }}
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
    </a-card>

    <!-- 新增 / 修改 页面组件  -->
    <InfoAddOrEdit ref="infoAddOrEdit" :callbackFunc="refTable" />
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { getEntTree, API_URL_ENT_LIST, reqLoad } from '@/api/manage'
import InfoAddOrEdit from './AddOrEdit.vue'
import { reactive, getCurrentInstance, ref, onMounted } from 'vue'
import { t } from '@/i18n'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

const tableColumns = [
  { title: t('資源權限ID'), dataIndex: 'entId' }, // key为必填项，用于标志该列的唯一
  { title: t('資源名稱'), dataIndex: 'entName' },
  { title: t('圖示'), dataIndex: 'menuIcon' },
  { title: t('路徑'), dataIndex: 'menuUri' },
  { title: t('元件名稱'), dataIndex: 'componentName' },
  { title: t('類型'), dataIndex: 'entType' },
  { title: t('狀態'), key: 'state', align: 'center' },
  { title: t('排序'), dataIndex: 'entSort' },
  { title: t('修改時間'), dataIndex: 'updatedAt' },
  {
    title: t('操作'),
    width: '100px',
    fixed: 'right',
    align: 'center',
    key: 'op',
  },
]

const infoAddOrEdit = ref()
const infoTable = ref()

const vdata = reactive({
  querySysType: 'MGR', // 默认查询运营平台
  tableColumns: tableColumns,
  dataSource: [],
  loading: false,
})

function refTable() {
  vdata.loading = true
  getEntTree(vdata.querySysType).then((res) => {
    vdata.dataSource = res
    vdata.loading = false

    infoTable.value.refTable(true)
  })
}

function reqTableDataFunc() {
  return Promise.resolve({
    current: 0,
    total: 0,
    records: vdata.dataSource,
    hasNext: false,
  })
}

onMounted(() => refTable())

function updateState(recordId, state) {
  return reqLoad
    .updateById(API_URL_ENT_LIST, recordId, { state: state, sysType: vdata.querySysType })
    .then((res) => {
      $infoBox.message.success(t('更新成功'))
      refTable() // 刷新页面
    })
}

function editFunc(recordId) {
  // 业务通用【修改】 函数
  infoAddOrEdit.value.show(recordId, vdata.querySysType)
}
</script>
