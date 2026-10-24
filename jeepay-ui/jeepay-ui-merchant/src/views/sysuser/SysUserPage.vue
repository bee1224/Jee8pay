<template>
  <page-header-wrapper>
    <a-card>
      <div v-if="$access('ENT_UR_USER_SEARCH')" class="table-page-search-wrapper">
        <a-form layout="inline" class="table-head-ground">
          <div class="table-layer">
            <jeepay-text-up :placeholder="$t('使用者ID')" v-model:value="vdata.searchData.sysUserId" />
            <jeepay-text-up :placeholder="$t('使用者姓名')" v-model:value="vdata.searchData.realname" />

            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="searchFunc" :loading="vdata.btnLoading">
                {{ $t('查詢') }}
              </a-button>
              <a-button style="margin-left: 8px" @click="() => (vdata.searchData = {})">
                {{ $t('重置') }}
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
          <a-button v-if="$access('ENT_UR_USER_ADD')" type="primary" @click="addFunc">
            {{ $t('新建') }}
          </a-button>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'avatar'">
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

          <template slot="opSlot" v-if="column.key === 'op'">
            <!-- 操作列插槽 -->
            <JeepayTableColumns>
              <a-button
                type="link"
                v-if="$access('ENT_UR_USER_UPD_ROLE')"
                @click="roleDist(record.sysUserId)"
              >
                {{ $t('變更角色') }}
              </a-button>
              <a-button
                type="link"
                v-if="$access('ENT_UR_USER_EDIT')"
                @click="editFunc(record.sysUserId)"
              >
                {{ $t('修改') }}
              </a-button>
              <a-button
                v-if="$access('ENT_UR_USER_DELETE')"
                type="link"
                danger
                @click="delFunc(record.sysUserId)"
              >
                {{ $t('刪除') }}
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
import { t } from '@/i18n'

const tableColumns = [
  { title: t('使用者ID'), dataIndex: 'sysUserId', fixed: 'left' },
  { title: t('姓名'), dataIndex: 'realname' },
  {
    title: t('性別'),
    dataIndex: 'sex',
    customRender: ({ record }) => {
      return record.sex === 1 ? t('男') : record.sex === 2 ? t('女') : t('未知')
    },
  },
  { title: t('頭像'), key: 'avatar', scopedSlots: { customRender: 'avatarSlot' } },
  { title: t('編號'), dataIndex: 'userNo' },
  { title: t('手機號'), dataIndex: 'telphone' },
  {
    title: t('超管'),
    dataIndex: 'isAdmin',
    customRender: ({ record }) => {
      return record.isAdmin === 1 ? t('是') : t('否')
    },
  },
  { title: t('狀態'), key: 'state', scopedSlots: { customRender: 'stateSlot' }, align: 'center' },
  { title: t('建立時間'), dataIndex: 'createdAt' },
  { title: t('修改時間'), dataIndex: 'updatedAt' },
  {
    key: 'op',
    title: t('操作'),
    width: '200px',
    fixed: 'right',
    align: 'center',
    scopedSlots: { customRender: 'opSlot' },
  },
]

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

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

function delFunc(recordId) {
  // 业务通用【删除】 函数
  $infoBox.confirmDanger(t('確認刪除？'), '', () => {
    return req.delById(API_URL_SYS_USER_LIST, recordId).then((res) => {
      $infoBox.message.success(t('刪除成功！'))
      infoTable.value.refTable(false)
    })
  })
}

function editFunc(recordId) {
  // 业务通用【修改】 函数
  infoAddOrEdit.value.show(recordId)
}

function roleDist(recordId) {
  // 【分配权限】 按钮点击事件
  roleDistRef.value.show(recordId)
}
function updateState(recordId, state) {
  // 【更新状态】
  const title = state === 1 ? t('確認[啟用]該使用者？') : t('確認[停用]該使用者？')
  const content =
    state === 1 ? t('啟用後使用者可進行登入等一系列操作') : t('停用後該使用者將立即退出系統並不可再次登入')

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
