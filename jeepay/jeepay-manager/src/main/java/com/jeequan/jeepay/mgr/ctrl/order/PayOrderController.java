/*
 * Copyright (c) 2021-2031, 河北计全科技有限公司 (https://www.jeequan.com & jeequan@126.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeequan.jeepay.mgr.ctrl.order;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.PayWay;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.utils.SeqKit;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.core.entity.AgentInfo;
import com.jeequan.jeepay.service.impl.AgentInfoService;
import com.jeequan.jeepay.service.impl.AgentPortalService;
import com.jeequan.jeepay.service.impl.MchAppService;
import com.jeequan.jeepay.service.sign.PayApiClient;
import com.jeequan.jeepay.service.impl.PayOrderService;
import com.jeequan.jeepay.service.impl.PayWayService;
import com.jeequan.jeepay.service.impl.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付订单类
 *
 * @author pangxiaoyu
 * @site https://www.jeequan.com
 * @date 2021-06-07 07:15
 */
@Tag(name = "订单管理（支付类）")
@RestController
@RequestMapping("/api/payOrder")
public class PayOrderController extends CommonCtrl {

    @Autowired private PayOrderService payOrderService;
    @Autowired private PayWayService payWayService;
    @Autowired private SysConfigService sysConfigService;
    @Autowired private MchAppService mchAppService;
    @Autowired private PayApiClient payApiClient;
    @Autowired private AgentInfoService agentInfoService;
    @Autowired private AgentPortalService agentPortalService;

    /** 有帶 agentNo 時回傳該團長轄區的商戶號（可能為空清單）；沒帶回傳 null */
    private List<String> agentScope() {
        String agentNo = getValString("agentNo");
        if (StringUtils.isBlank(agentNo)) {
            return null;
        }
        AgentInfo agent = agentInfoService.getById(agentNo);
        return agent == null ? List.of() : agentPortalService.directMchNos(agent);
    }

    /**
     * @author: pangxiaoyu
     * @date: 2021/6/7 16:15
     * @describe: 订单信息列表
     */
    @Operation(summary = "支付订单信息列表")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "pageNumber", description = "分页页码"),
            @Parameter(name = "pageSize", description = "分页条数"),
            @Parameter(name = "createdStart", description = "日期格式字符串（yyyy-MM-dd HH:mm:ss），时间范围查询--开始时间，查询范围：大于等于此时间"),
            @Parameter(name = "createdEnd", description = "日期格式字符串（yyyy-MM-dd HH:mm:ss），时间范围查询--结束时间，查询范围：小于等于此时间"),
            @Parameter(name = "mchNo", description = "商户号"),
            @Parameter(name = "unionOrderId", description = "支付/商户/渠道订单号"),
            @Parameter(name = "isvNo", description = "服务商号"),
            @Parameter(name = "appId", description = "应用ID"),
            @Parameter(name = "wayCode", description = "支付方式代码"),
            @Parameter(name = "state", description = "支付状态: 0-订单生成, 1-支付中, 2-支付成功, 3-支付失败, 4-已撤销, 5-已退款, 6-订单关闭"),
            @Parameter(name = "notifyState", description = "向下游回调状态, 0-未发送,  1-已发送"),
            @Parameter(name = "notifyOk", description = "0-只看已收款但商戶尚未回覆成功的訂單"),
            @Parameter(name = "divisionState", description = "0-未发生分账, 1-等待分账任务处理, 2-分账处理中, 3-分账任务已结束(不体现状态)")
    })
    @PreAuthorize("hasAuthority('ENT_ORDER_LIST')")
    @RequestMapping(value="", method = RequestMethod.GET)
    public ApiPageRes<PayOrder> list() {

        PayOrder payOrder = getObject(PayOrder.class);
        JSONObject paramJSON = getReqParamJSON();
        LambdaQueryWrapper<PayOrder> wrapper = PayOrder.gw();
        // 上帝看某位團長的訂單：範圍為這位團長這一支（含隊長）的全部商戶
        List<String> scope = agentScope();
        if (scope != null) {
            if (scope.isEmpty()) {
                return ApiPageRes.pages(getIPage());
            }
            wrapper.in(PayOrder::getMchNo, scope);
        }
        // 手機版「未通知商戶」：已收款但商戶尚未回覆成功
        if ("0".equals(paramJSON.getString("notifyOk"))) {
            payOrderService.notNotifiedOk(wrapper);
        }

        IPage<PayOrder> pages = payOrderService.listByPage(getIPage(), payOrder, paramJSON, wrapper);
        payOrderService.fillNotifyResult(pages.getRecords());
        // 得到所有支付方式
        Map<String, String> payWayNameMap = new HashMap<>();
        List<PayWay> payWayList = payWayService.list();
        for (PayWay payWay:payWayList) {
            payWayNameMap.put(payWay.getWayCode(), payWay.getWayName());
        }
        for (PayOrder order:pages.getRecords()) {
            // 存入支付方式名称
            if (StringUtils.isNotEmpty(payWayNameMap.get(order.getWayCode()))) {
                order.addExt("wayName", payWayNameMap.get(order.getWayCode()));
            }else {
                order.addExt("wayName", order.getWayCode());
            }
        }
        return ApiPageRes.pages(pages);
    }

    @Operation(summary = "訂單摘要：期間內各狀態筆數、金額與尚未通知商戶的筆數（手機版訂單頁頂部）")
    @PreAuthorize("hasAuthority('ENT_ORDER_LIST')")
    @RequestMapping(value="/summary", method = RequestMethod.GET)
    public ApiRes summary() {
        List<String> scope = agentScope();
        return ApiRes.ok(payOrderService.summary(scope, getValString("createdStart"), getValString("createdEnd")));
    }

    /**
     * @author: pangxiaoyu
     * @date: 2021/6/7 16:15
     * @describe: 支付订单信息
     */
    @Operation(summary = "支付订单信息详情")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "payOrderId", description = "支付订单号", required = true)
    })
    @PreAuthorize("hasAuthority('ENT_PAY_ORDER_VIEW')")
    @RequestMapping(value="/{payOrderId}", method = RequestMethod.GET)
    public ApiRes<PayOrder> detail(@PathVariable("payOrderId") String payOrderId) {
        PayOrder payOrder = payOrderService.getById(payOrderId);
        if (payOrder == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        return ApiRes.ok(payOrder);
    }


    /**
     * 发起订单退款
     * @author terrfly
     * @site https://www.jeequan.com
     * @date 2021/6/17 16:38
     */
    @Operation(summary = "发起订单退款")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "payOrderId", description = "支付订单号", required = true),
            @Parameter(name = "refundAmount", description = "退款金额", required = true),
            @Parameter(name = "refundReason", description = "退款原因", required = true)
    })
    @MethodLog(remark = "發起訂單退款")
    @PreAuthorize("hasAuthority('ENT_PAY_ORDER_REFUND')")
    @PostMapping("/refunds/{payOrderId}")
    public ApiRes refund(@PathVariable("payOrderId") String payOrderId) {

        Long refundAmount = getRequiredAmountL("refundAmount");
        String refundReason = getValStringRequired("refundReason");

        PayOrder payOrder = payOrderService.getById(payOrderId);
        if (payOrder == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }

        if(payOrder.getState() != PayOrder.STATE_SUCCESS){
            throw new BizException("訂單狀態不正確");
        }

        if(payOrder.getRefundAmount() + refundAmount > payOrder.getAmount()){
            throw new BizException("退款金額超過訂單可退款金額！");
        }


        // 平台不持有商戶私鑰，以平台內部金鑰代商戶發起退款（ADR-0013）
        Map<String, Object> req = new HashMap<>();
        req.put("mchNo", payOrder.getMchNo());
        req.put("appId", payOrder.getAppId());
        req.put("payOrderId", payOrderId);
        req.put("mchRefundNo", SeqKit.genMhoOrderId());
        req.put("refundAmount", refundAmount);
        req.put("refundReason", refundReason);
        req.put("currency", payOrder.getCurrency());
        return ApiRes.ok(payApiClient.call("/api/refund/refundOrder", req));
    }

}
