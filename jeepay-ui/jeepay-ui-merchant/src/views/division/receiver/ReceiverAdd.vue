<template>
  <a-drawer
    v-if="vdata.visible"
    v-model:open="vdata.visible"
    @close="onClose"
    :closable="true"
    :maskClosable="false"
    :body-style="{ paddingBottom: '80px' }"
    :drawer-style="{ backgroundColor: '#f0f2f5' }"
    width="80%"
  >
    <a-descriptions title="綁定分帳接收者帳號">
      <a-descriptions-item label="當前應用">
        <span style="color: red">{{ vdata.appInfo.appName }} [{{ vdata.appInfo.appId }}]</span>
      </a-descriptions-item>

      <a-descriptions-item label="選擇要加入到的帳號分組">
        <a-select
          style="width: 210px"
          placeholder="帳號分組"
          v-model:value="vdata.selectedReceiverGroupId"
        >
          <a-select-option
            v-for="item in vdata.allReceiverGroup"
            :key="item.receiverGroupId"
            :value="item.receiverGroupId"
          >
            {{ item.receiverGroupName }}
          </a-select-option>
        </a-select>
      </a-descriptions-item>
    </a-descriptions>
    <a-divider></a-divider>

    <a-card title="微信帳號" v-show="vdata.appSupportIfCodes.indexOf('wxpay') >= 0">
      <template #extra>
        <a-button style="background: green; color: white" @click="addReceiverRow('wxpay')">
          添加【微信官方】分帳接收帳號
        </a-button>
      </template>

      <a-table
        :scroll="{ x: 'max-content' }"
        :columns="vdata.accTableColumns"
        :data-source="vdata.receiverTableData.filter((item) => item.ifCode == 'wxpay')"
        :pagination="false"
        rowKey="rowKey"
      >
        <template #bodyCell="{ column, record, index }">
          <!-- 账号类型 -->
          <template v-if="column.key === 'reqBindState'">
            <div style="color: salmon" v-show="record.reqBindState == 0">
              <a-icon type="info-circle" />
              待綁定
            </div>
            <div style="color: green" v-show="record.reqBindState == 1">
              <a-icon type="check-circle" />
              綁定成功
            </div>
            <div style="color: red" v-show="record.reqBindState == 2">
              <a-icon type="close-circle" />
              綁定異常
            </div>
          </template>

          <!-- 账号别名 -->
          <template v-if="column.key === 'receiverAlias'">
            <a-input
              v-model:value="record.receiverAlias"
              style="width: 150px"
              placeholder="(選填)預設為帳號"
            />
          </template>

          <!-- 账号类型 -->
          <template v-if="column.key === 'accType'">
            <a-select
              style="width: 110px"
              v-model:value="record.accType"
              placeholder="帳號類型"
              default-value="0"
            >
              <a-select-option value="0">個人</a-select-option>
              <a-select-option value="1">微信商戶</a-select-option>
            </a-select>
          </template>

          <!-- 接收方账号 -->
          <template v-if="column.key === 'accNo'">
            <a-input
              v-model:value="record.accNo"
              style="width: 150px"
              placeholder="請輸入接收方帳號"
            />
            <a-button
              type="link"
              v-if="record.accType == 0"
              @click="showChannelUserModal('wxpay', record)"
            >
              掃碼獲取
            </a-button>
          </template>

          <!-- 接收方姓名 -->
          <template v-if="column.key === 'accName'">
            <a-input v-model:value="record.accName" placeholder="請輸入接收方姓名" />
          </template>

          <!-- 分账关系 -->
          <template v-if="column.key === 'relationType'">
            <a-select
              style="width: 110px"
              labelInValue
              placeholder="分帳關係類型"
              :defaultValue="{ key: 'PARTNER' }"
              @change="changeRelationType(record, $event)"
            >
              <a-select-option key="PARTNER">合作夥伴</a-select-option>
              <a-select-option key="SERVICE_PROVIDER">服務商</a-select-option>
              <a-select-option key="STORE">門店</a-select-option>
              <a-select-option key="STAFF">員工</a-select-option>
              <a-select-option key="STORE_OWNER">店主</a-select-option>
              <a-select-option key="HEADQUARTER">總部</a-select-option>
              <a-select-option key="BRAND">品牌方</a-select-option>
              <a-select-option key="DISTRIBUTOR">分銷商</a-select-option>
              <a-select-option key="USER">使用者</a-select-option>
              <a-select-option key="SUPPLIER">供應商</a-select-option>
              <a-select-option key="CUSTOM">自訂</a-select-option>
            </a-select>
          </template>

          <!-- 关系名称 -->
          <template v-if="column.key === 'relationTypeName'">
            <a-input
              :disabled="record.relationType !== 'CUSTOM'"
              v-model:value="record.relationTypeName"
            />
          </template>

          <!-- 默认分账比例 -->
          <template v-if="column.key === 'divisionProfit'">
            <a-input v-model:value="record.divisionProfit" style="width: 65px" />
            %
          </template>

          <template v-if="column.key === 'op'">
            <a-button type="link" @click="delRow(record)">刪除</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <br />
    <a-card title="支付寶帳號" v-show="vdata.appSupportIfCodes.indexOf('alipay') >= 0">
      <template #extra>
        <a-button style="background: dodgerblue; color: white" @click="addReceiverRow('alipay')">
          添加【支付寶官方】分帳接收帳號
        </a-button>
      </template>
      <a-table
        :scroll="{ x: 'max-content' }"
        :columns="vdata.accTableColumns"
        :data-source="vdata.receiverTableData.filter((item) => item.ifCode == 'alipay')"
        :pagination="false"
        rowKey="rowKey"
      >
        <template #bodyCell="{ column, record, index }">
          <!-- 账号类型 -->
          <template v-if="column.key === 'reqBindState'">
            <div style="color: salmon" v-show="record.reqBindState == 0">
              <a-icon type="info-circle" />
              待綁定
            </div>
            <div style="color: green" v-show="record.reqBindState == 1">
              <a-icon type="check-circle" />
              綁定成功
            </div>
            <div style="color: red" v-show="record.reqBindState == 2">
              <a-icon type="close-circle" />
              綁定異常
            </div>
          </template>

          <!-- 账号别名 -->
          <template v-if="column.key === 'receiverAlias'">
            <a-input
              v-model:value="record.receiverAlias"
              style="width: 150px"
              placeholder="(選填)預設為帳號"
            />
          </template>

          <!-- 账号类型 -->
          <template v-if="column.key === 'accType'">
            <a-select
              style="width: 110px"
              v-model:value="record.accType"
              placeholder="帳號類型"
              default-value="0"
            >
              <a-select-option value="0">個人</a-select-option>
              <a-select-option value="1">商戶</a-select-option>
            </a-select>
          </template>

          <!-- 接收方账号 -->
          <template v-if="column.key === 'accNo'">
            <a-input
              v-model:value="record.accNo"
              style="width: 150px"
              placeholder="請輸入接收方帳號"
            />
            <a-button
              type="link"
              v-if="record.accType == 0"
              @click="showChannelUserModal('alipay', record)"
            >
              掃碼獲取
            </a-button>
          </template>

          <!-- 接收方姓名 -->
          <template v-if="column.key === 'accName'">
            <a-input v-model:value="record.accName" placeholder="請輸入接收方姓名" />
          </template>

          <!-- 分账关系 -->
          <template v-if="column.key === 'relationType'">
            <a-select
              style="width: 110px"
              labelInValue
              placeholder="分帳關係類型"
              :defaultValue="{ key: 'PARTNER' }"
              @change="changeRelationType(record, $event)"
            >
              <a-select-option key="PARTNER">合作夥伴</a-select-option>
              <a-select-option key="SERVICE_PROVIDER">服務商</a-select-option>
              <a-select-option key="STORE">門店</a-select-option>
              <a-select-option key="STAFF">員工</a-select-option>
              <a-select-option key="STORE_OWNER">店主</a-select-option>
              <a-select-option key="HEADQUARTER">總部</a-select-option>
              <a-select-option key="BRAND">品牌方</a-select-option>
              <a-select-option key="DISTRIBUTOR">分銷商</a-select-option>
              <a-select-option key="USER">使用者</a-select-option>
              <a-select-option key="SUPPLIER">供應商</a-select-option>
              <a-select-option key="CUSTOM">自訂</a-select-option>
            </a-select>
          </template>

          <!-- 关系名称 -->
          <template v-if="column.key === 'relationTypeName'">
            <a-input
              :disabled="record.relationType !== 'CUSTOM'"
              v-model:value="record.relationTypeName"
            />
          </template>

          <!-- 默认分账比例 -->
          <template v-if="column.key === 'divisionProfit'">
            <a-input v-model:value="record.divisionProfit" style="width: 65px" />
            %
          </template>

          <template v-if="column.key === 'op'">
            <a-button type="link" @click="delRow(record)">刪除</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <div class="drawer-btn-center">
      <a-button type="primary" :style="{ marginRight: '8px' }" @click="reqBatchBindReceiver(0)">
        發起綁定請求
      </a-button>
      <a-button @click="onClose">關閉</a-button>
    </div>

    <ChannelUserModal
      ref="channelUserModal"
      @changeChannelUserId="changeChannelUserIdFunc($event)"
    />
  </a-drawer>
</template>

<script setup lang="tsx">
// eslint-disable-next-line no-unused-vars
import { genRowKey } from '@/utils/util'
import ChannelUserModal from '@/components/ChannelUser/ChannelUserModal.vue'
import {
  API_URL_DIVISION_RECEIVER,
  API_URL_DIVISION_RECEIVER_GROUP,
  req,
  getIfCodeByAppId,
} from '@/api/manage'
import { reactive, getCurrentInstance, ref } from 'vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties

// eslint-disable-next-line no-unused-vars
const accTableColumns = [
  { key: 'reqBindState', title: '狀態', scopedSlots: { customRender: 'reqBindStateSlot' } },
  { key: 'receiverAlias', title: '帳號別名', scopedSlots: { customRender: 'receiverAliasSlot' } },
  { key: 'accType', title: '帳號類型', scopedSlots: { customRender: 'accTypeSlot' } },
  { key: 'accNo', width: '300px', title: '接收方帳號', scopedSlots: { customRender: 'accNoSlot' } },
  {
    key: 'accName',
    width: '180px',
    title: '接收方姓名',
    scopedSlots: { customRender: 'accNameSlot' },
  },
  { key: 'relationType', title: '分帳關係', scopedSlots: { customRender: 'relationTypeSlot' } },
  {
    key: 'relationTypeName',
    width: '200px',
    title: '關係名稱',
    scopedSlots: { customRender: 'relationTypeNameSlot' },
  },
  {
    key: 'divisionProfit',
    title: '預設分帳比例',
    scopedSlots: { customRender: 'divisionProfitSlot' },
  },
  { key: 'op', title: '操作', scopedSlots: { customRender: 'opSlot' } },
]

const defaultReceiverTemplate = {
  reqBindState: 0, // 默认待绑定
  receiverAlias: '',
  receiverGroupId: '',
  appId: '',
  ifCode: '',
  accType: '0',
  accNo: null,
  accName: '',
  relationType: 'PARTNER', // 默认合作伙伴, 需要同时更改select的defaultValue
  relationTypeName: '合作夥伴',
  divisionProfit: '',
}

const props = defineProps({
  callbackFunc: {
    type: Function,
    default: () => ({}),
  },
})
const vdata: any = reactive({
  visible: false, // 是否显示抽屉
  appInfo: null, // 应用app信息
  accTableColumns: accTableColumns, // 表头模板（微信支付宝公用）

  allReceiverGroup: [], // 当前商户所有的接收账号的分组情况
  selectedReceiverGroupId: '', // 当前选择的分组ID
  appSupportIfCodes: [], // 应用支持的支付方式
  receiverTableData: [], // 微信支付的分账用户列表集合
})

const channelUserModal = ref()

// 弹层打开事件
function show(appInfo) {
  vdata.appSupportIfCodes = [] // 初始化
  vdata.receiverTableData = [] // 置空表格

  // 请求接口，获取所有分组信息，只有此处进行pageSize=-1传参
  req.list(API_URL_DIVISION_RECEIVER_GROUP, { pageSize: -1 }).then((res) => {
    vdata.allReceiverGroup = res.records
    if (vdata.allReceiverGroup && vdata.allReceiverGroup.length > 0) {
      // 默认选中第一个 & 更新列表
      vdata.selectedReceiverGroupId = vdata.allReceiverGroup[0].receiverGroupId
    }
  })

  // 查询支持的分账接口
  getIfCodeByAppId(appInfo.appId).then((res) => {
    vdata.appSupportIfCodes = res
  })

  vdata.appInfo = appInfo // 应用信息

  vdata.visible = true // 显示弹层
}
// 抽屉关闭
function onClose() {
  props.callbackFunc() // 刷新列表
  vdata.visible = false
}
// 删除某一行
function delRow(item) {
  const index = vdata.receiverTableData.indexOf(item)
  if (index > -1) {
    vdata.receiverTableData.splice(index, 1)
  }
}
function changeRelationType(record, value) {
  record.relationType = value.key
  if (value.key !== 'CUSTOM') {
    record.relationTypeName = value.label[0].children
  } else {
    record.relationTypeName = ''
  }
}

// 显示获取用户ID的弹层
function showChannelUserModal(ifCode, record) {
  channelUserModal.value.showModal(vdata.appInfo.appId, ifCode, record)
}

// 接收到当前渠道用户ID信息
function changeChannelUserIdFunc({ channelUserId, extObject }) {
  extObject.accNo = channelUserId
}

// 添加一行账号信息
function addReceiverRow(ifCode) {
  if (!vdata.selectedReceiverGroupId) {
    return $infoBox.message.error('請選選擇要加入的分組')
  }
  vdata.receiverTableData.push(
    Object.assign({}, defaultReceiverTemplate, {
      rowKey: genRowKey(),
      ifCode: ifCode,
      appId: vdata.appInfo.appId,
    })
  )
}

// 单条绑定 返回是否成功
function reqBatchBindReceiver(i) {
  if (vdata.receiverTableData.length <= 0) {
    return $infoBox.message.error('請先添加帳號')
  }

  // 完成了所有的绑定操作
  if (i >= vdata.receiverTableData.length) {
    return $infoBox.message.success('已完成所有帳號的綁定操作')
  }

  // 当前的账号
  const currentReceiver = vdata.receiverTableData[i]
  currentReceiver.receiverGroupId = vdata.selectedReceiverGroupId // 设置分组ID

  if (currentReceiver.reqBindState === 1) {
    // 已经绑定成功， 不在重复发起
    return reqBatchBindReceiver(++i) // 递归继续绑定
  }

  if (!currentReceiver.accNo) {
    return $infoBox.message.error(`第${i + 1}筆： 接收方帳號不能為空`)
  }

  if (currentReceiver.relationType === 'CUSTOM' && !currentReceiver.relationTypeName) {
    return $infoBox.message.error(`第${i + 1}筆： 自訂類型時接收方帳號名稱不能為空`)
  }

  if (
    !currentReceiver.divisionProfit ||
    currentReceiver.divisionProfit <= 0 ||
    currentReceiver.divisionProfit > 100
  ) {
    return $infoBox.message.error(`第${i + 1}筆： 預設分帳比例請設定在[0.01% ~ 100% ] 之間`)
  }

  req
    .add(API_URL_DIVISION_RECEIVER, currentReceiver)
    .then((apiRes) => {
      // 绑定成功
      if (apiRes.bindState === 1) {
        reqBatchBindReceiver(++i) // 递归继续绑定
        currentReceiver.reqBindState = 1 // 成功
      } else {
        currentReceiver.reqBindState = 2 // 异常
        $infoBox.modalError(
          `第${i + 1}筆： 綁定異常`,
          <div>
            <div>錯誤碼：{apiRes.errCode}</div>
            <div>錯誤訊息：{apiRes.errMsg}</div>
          </div>
        )
      }
    })
    .catch(() => {
      currentReceiver.reqBindState = 2 // 异常
    })
}

defineExpose({ show })
</script>
