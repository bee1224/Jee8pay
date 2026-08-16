<template>
  <page-header-wrapper>
    <a-card>
      <div v-if="$access('ENT_UR_USER_SEARCH')" class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <jeepay-text-up :placeholder="'用戶ID'" v-model:value="vdata.searchData.sysUserId" />
            <jeepay-text-up :placeholder="'用戶姓名'" v-model:value="vdata.searchData.realname" />
            <span class="table-page-search-submitButtons">
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
        @btnLoadClose="vdata.btnLoading = false"
        ref="infoTable"
        :initData="true"
        :reqTableDataFunc="reqTableDataFunc"
        :tableColumns="vdata.tableColumns"
        :searchData="vdata.searchData"
        rowKey="sysUserId"
      >
        <template #opRow>
          <a-button
            v-if="$access('ENT_UR_USER_ADD')"
            type="primary"
            @click="addFunc"
            class="mg-b-30"
          >
            新建
          </a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'avatarUrl'">
            <a-avatar size="default" :src="record.avatarUrl" />
          </template>

          <template v-if="column.key === 'state'">
            <JeepayTableColState
              :state="record.state"
              :showSwitchType="$access('ENT_UR_USER_EDIT')"
              :onChange="
                (state) => {
                  return updateState(record.sysUserId, state)
                }
              "
            />
          </template>

          <template v-if="column.key === 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                type="link"
                v-if="$access('ENT_UR_USER_UPD_ROLE')"
                @click="roleDist(record.sysUserId)"
              >
                變更角色
              </a-button>
              <a-button
                v-if="$access('ENT_UR_USER_EDIT')"
                type="link"
                @click="editFunc(record.sysUserId)"
              >
                修改
              </a-button>
              <a-button
                v-if="$access('ENT_UR_USER_DELETE')"
                danger
                type="link"
                @click="delFunc(record.sysUserId)"
              >
                刪除
              </a-button>
            </JeepayTableColumns>
          </template>
        </template>
      </JeepayTable>
    </a-card>

    <!-- 新增 / 修改 页面组件  -->
    <InfoAddOrEdit ref="infoAddOrEdit" :callbackFunc="searchFunc" />

    <!-- 分配角色 页面组件  -->
    <RoleDist ref="roleDistRef" />
  </page-header-wrapper>
</template>
<script setup lang="ts">
import { API_URL_SYS_USER_LIST, req, reqLoad } from '@/api/manage'
import InfoAddOrEdit from './AddOrEdit.vue'
import RoleDist from './RoleDist.vue'
import { reactive, getCurrentInstance, ref } from 'vue'

// 导入全局函数
const { $infoBox, $SYS_NAME_MAP, $access } =
  getCurrentInstance()!.appContext.config.globalProperties

const tableColumns = [
  { title: '用戶ID', dataIndex: 'sysUserId', fixed: 'left' },
  { title: '姓名', dataIndex: 'realname' },
  {
    title: '性別',
    dataIndex: 'sex',
    customRender: ({ text, record, index, column }) => {
      return record.sex === 1 ? '男' : record.sex === 2 ? '女' : '未知'
    },
  },
  { title: '頭像', key: 'avatarUrl', scopedSlots: { customRender: 'avatarSlot' } },
  { title: '編號', dataIndex: 'userNo' },
  { title: '手機號', dataIndex: 'telphone' },
  {
    title: '超管',
    dataIndex: 'isAdmin',
    customRender: ({ text, record, index, column }) => {
      return record.isAdmin === 1 ? '是' : '否'
    },
  },
  { title: '狀態', key: 'state', align: 'center' },
  { title: '建立時間', dataIndex: 'createdAt' },
  { title: '修改時間', dataIndex: 'updatedAt' },
  {
    key: 'op',
    title: '操作',
    width: 200,
    fixed: 'right',
    align: 'center',
    scopedSlots: { customRender: 'opSlot' },
  },
]

const vdata: any = reactive({
  tableColumns: tableColumns,
  searchData: {},
  btnLoading: false,
})

const infoTable = ref()
const infoAddOrEdit = ref()
const roleDistRef = ref()

// 请求table接口数据
function reqTableDataFunc(params) {
  return req.list(API_URL_SYS_USER_LIST, params)
}

function searchFunc() {
  // 点击【查询】按钮点击事件
  vdata.btnLoading = true // 打开查询按钮的loading
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

function delFunc(recordId) {
  // 业务通用【删除】 函数
  $infoBox.confirmDanger('確認刪除？', '', () => {
    return req.delById(API_URL_SYS_USER_LIST, recordId).then((res) => {
      $infoBox.message.success('刪除成功！')
      infoTable.value.refTable(false)
    })
  })
}

function roleDist(recordId) {
  roleDistRef.value.show(recordId)
}

function updateState(recordId, state) {
  // 【更新状态】
  const title = state === 1 ? '確認[啟用]該用戶？' : '確認[停用]該用戶？'
  const content =
    state === 1 ? '啟用後用戶可進行登入等一系列操作' : '停用後該用戶將立即退出系統並不可再次登入'

  return new Promise((resolve, reject) => {
    $infoBox.confirmDanger(
      title,
      content,
      () => {
        return reqLoad
          .updateById(API_URL_SYS_USER_LIST, recordId, { state: state })
          .then((res) => {
            searchFunc()
            resolve(res)
          })
          .catch((err) => reject(err))
      },
      () => {
        reject(new Error())
      }
    )
  })
}
</script>
