<template>
  <page-header-wrapper>
    <a-card>
      <div v-if="$access('ENT_DIVISION_RECEIVER_LIST')" class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <a-select
              v-model:value="vdata.searchData.appId"
              placeholder="選擇應用"
              class="table-head-layout"
            >
              <a-select-option key="">全部應用</a-select-option>
              <a-select-option v-for="item in vdata.mchAppList" :key="item.appId">
                {{ item.appName }} [{{ item.appId }}]
              </a-select-option>
            </a-select>

            <jeepay-text-up
              placeholder="分帳接收者ID[精準]"
              v-model:value="vdata.searchData.receiverId"
            />
            <jeepay-text-up
              placeholder="接收者帳號別名[模糊]"
              v-model:value="vdata.searchData.receiverAlias"
            />
            <jeepay-text-up
              placeholder="組ID[精準]"
              v-model:value="vdata.searchData.receiverGroupId"
            />

            <a-select
              class="table-head-layout"
              v-model:value="vdata.searchData.state"
              placeholder="帳號狀態（本系統）"
              default-value=""
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="1">正常分帳</a-select-option>
              <a-select-option value="0">暫停分帳</a-select-option>
            </a-select>

            <span class="table-page-search-submitButtons table-head-layout">
              <a-button type="primary" @click="searchFunc" :loading="vdata.btnLoading">
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
        ref="infoTable"
        :initData="false"
        :reqTableDataFunc="reqTableDataFunc"
        :tableColumns="vdata.tableColumns"
        :searchData="vdata.searchData"
        @btnLoadClose="vdata.btnLoading = false"
        rowKey="receiverId"
      >
        <template #opRow>
          <a-button v-if="$access('ENT_DIVISION_RECEIVER_ADD')" type="primary" @click="addFunc">
            新建
          </a-button>
        </template>

        <!-- 渠道类型 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ifCode'">
            <template v-if="record.ifCode === 'wxpay'">
              <span style="color: green">
                <a-icon type="wechat" />
                微信
              </span>
            </template>
            <template v-else-if="record.ifCode == 'alipay'">
              <span style="color: dodgerblue">
                <a-icon type="alipay-circle" />
                支付寶
              </span>
            </template>
            <template v-else>{{ record.ifCode }}</template>
          </template>

          <!-- 状态（本系统） -->
          <template template v-if="column.key === 'state'">
            <div v-if="record.state == 0"><a-badge status="error" text="暫停分帳" /></div>
            <div v-else-if="record.state == 1"><a-badge status="processing" text="正常分帳" /></div>
            <div v-else><a-badge status="warning" text="未知" /></div>
          </template>

          <template v-if="column.key === 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                type="link"
                v-if="$access('ENT_DIVISION_RECEIVER_EDIT')"
                @click="editFunc(record.receiverId)"
              >
                修改
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>

      <!-- 新增收款账号页面  -->
      <ReceiverAdd ref="receiverAdd" :callbackFunc="searchFunc" />

      <!-- 修改 页面组件  -->
      <ReceiverEdit ref="receiverEdit" :callbackFunc="searchFunc" />
    </a-card>
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { API_URL_DIVISION_RECEIVER, API_URL_MCH_APP, req } from '@/api/manage'
import ReceiverAdd from './ReceiverAdd.vue'
import ReceiverEdit from './ReceiverEdit.vue'
import { reactive, ref, onMounted, getCurrentInstance } from 'vue'

// eslint-disable-next-line no-unused-vars
const tableColumns = [
  { key: 'receiverId', dataIndex: 'receiverId', title: '綁定ID' },
  { key: 'ifCode', title: '渠道類型', scopedSlots: { customRender: 'ifCodeSlot' } },
  { key: 'receiverAlias', dataIndex: 'receiverAlias', title: '帳號別名' },
  { key: 'receiverGroupName', dataIndex: 'receiverGroupName', title: '組名稱' },
  { key: 'accNo', dataIndex: 'accNo', title: '分帳接收帳號' },
  { key: 'accName', dataIndex: 'accName', title: '分帳接收帳號名稱' },
  { key: 'relationTypeName', dataIndex: 'relationTypeName', title: '分帳關係類型' },
  { title: '狀態', key: 'state', scopedSlots: { customRender: 'stateSlot' }, align: 'center' },
  { key: 'bindSuccessTime', dataIndex: 'bindSuccessTime', title: '綁定成功時間' },
  {
    key: 'divisionProfit',
    dataIndex: 'divisionProfit',
    title: '預設分帳比例',
    customRender: ({ text }) => {
      return (text * 100).toFixed(2) + '%'
    },
  },
  {
    key: 'op',
    title: '操作',
    width: '100px',
    fixed: 'right',
    align: 'center',
    scopedSlots: { customRender: 'opSlot' },
  },
]

const vdata: any = reactive({
  tableColumns: tableColumns,
  searchData: { appId: '' },
  btnLoading: false,

  mchAppList: [], // 商户app列表
})

const infoTable = ref()
const receiverAdd = ref()
const receiverEdit = ref()

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

onMounted(() => {
  const that = this // 提前保留this
  // 请求接口，获取所有的appid，只有此处进行pageSize=-1传参
  req.list(API_URL_MCH_APP, { pageSize: -1 }).then((res) => {
    vdata.mchAppList = res.records

    // 默认选中第一个 & 更新列表
    if (vdata.mchAppList && vdata.mchAppList.length > 0) {
      vdata.searchData.appId = vdata.mchAppList[0].appId + ''
      searchFunc()
    }
  })
})
// 请求table接口数据
function reqTableDataFunc(params) {
  return req.list(API_URL_DIVISION_RECEIVER, params)
}

function searchFunc() {
  // 点击【查询】按钮点击事件
  vdata.btnLoading = true // 打开查询按钮上的loading
  infoTable.value.refTable(true)
}

function addFunc() {
  // 业务通用【新增】 函数
  if (vdata.mchAppList.length <= 0) {
    return $infoBox.message.error('當前商戶無任何應用，請先建立應用後再試。')
  }
  if (!vdata.searchData.appId) {
    return $infoBox.message.error('請先選擇應用。')
  }

  // 打开弹层
  receiverAdd.value.show(
    vdata.mchAppList.filter((item) => item.appId === vdata.searchData.appId)[0]
  )
}

function editFunc(recordId) {
  // 业务通用【修改】 函数
  receiverEdit.value.show(recordId)
}
</script>
