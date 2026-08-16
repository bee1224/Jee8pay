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
package com.jeequan.jeepay.pay.ctrl.payorder;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.ctrls.AbstractCtrl;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.exception.ResponseException;
import com.jeequan.jeepay.core.utils.SpringBeansUtil;
import com.jeequan.jeepay.pay.channel.IChannelNoticeService;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.ConfigContextService;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/*
* 渠道侧的通知入口Controller 【分为同步跳转（doReturn）和異步回調(doNotify) 】
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:26
*/
@Slf4j
@Controller
public class ChannelNoticeController extends AbstractCtrl {

    @Autowired private PayOrderService payOrderService;
    @Autowired private ConfigContextQueryService configContextQueryService;
    @Autowired private PayMchNotifyService payMchNotifyService;
    @Autowired private PayOrderProcessService payOrderProcessService;

    /**
     * 同步通知入口
     *
     * payOrderId 前缀为 ONLYJUMP_，表示直接跳转
     **/
    @RequestMapping(value= {"/api/pay/return/{ifCode}", "/api/pay/return/{ifCode}/{payOrderId}"})
    public String doReturn(HttpServletRequest request, @PathVariable("ifCode") String ifCode, @PathVariable(value = "payOrderId", required = false) String urlOrderId){

        String payOrderId = null;
        String logPrefix = "进入[" +ifCode+ "]支付同步跳转：urlOrderId：["+ StringUtils.defaultIfEmpty(urlOrderId, "") + "] ";
        log.info("===== {} =====" , logPrefix);

        try {

            // 參數有误
            if(StringUtils.isEmpty(ifCode)){
                return this.toReturnPage("ifCode is empty");
            }

            //查询支付介面是否存在
            IChannelNoticeService payNotifyService = SpringBeansUtil.getBean(ifCode + "ChannelNoticeService", IChannelNoticeService.class);

            // 支付通道介面实现不存在
            if(payNotifyService == null){
                log.error("{}, interface not exists ", logPrefix);
                return this.toReturnPage("[" + ifCode + "] interface not exists");
            }

            // 仅做跳转，直接跳转訂單的returnUrl
            if (StringUtils.isNotBlank(urlOrderId) && urlOrderId.startsWith(CS.PAY_RETURNURL_FIX_ONLY_JUMP_PREFIX)) {
                onlyJump(urlOrderId, logPrefix);
                return null;
            }

            // 解析訂單号 和 請求參數
            MutablePair<String, Object> mutablePair = payNotifyService.parseParams(request, urlOrderId, IChannelNoticeService.NoticeTypeEnum.DO_RETURN);
            if(mutablePair == null){ // 解析数据失敗， 响应已處理
                log.error("{}, mutablePair is null ", logPrefix);
                throw new BizException("解析数据異常！"); //需要实现类自行抛出ResponseException, 不应该在这抛此異常。
            }

            //解析到訂單号
            payOrderId = mutablePair.left;
            log.info("{}, 解析数据为：payOrderId:{}, params:{}", logPrefix, payOrderId, mutablePair.getRight());

            if(StringUtils.isNotEmpty(urlOrderId) && !urlOrderId.equals(payOrderId)){
                log.error("{}, 訂單号不匹配. urlOrderId={}, payOrderId={} ", logPrefix, urlOrderId, payOrderId);
                throw new BizException("訂單号不匹配！");
            }

            //獲取訂單号 和 訂單数据
            PayOrder payOrder = payOrderService.getById(payOrderId);

            // 訂單不存在
            if(payOrder == null){
                log.error("{}, 訂單不存在. payOrderId={} ", logPrefix, payOrderId);
                return this.toReturnPage("支付訂單不存在");
            }

            //查询出商戶應用的設定資訊
            MchAppConfigContext mchAppConfigContext = configContextQueryService.queryMchInfoAndAppInfo(payOrder.getMchNo(), payOrder.getAppId());

            //调起介面的回調判断
            ChannelRetMsg notifyResult = payNotifyService.doNotice(request, mutablePair.getRight(), payOrder, mchAppConfigContext, IChannelNoticeService.NoticeTypeEnum.DO_RETURN);

            // 返回null 表明出现異常， 无需處理通知下游等操作。
            if(notifyResult == null || notifyResult.getChannelState() == null || notifyResult.getResponseEntity() == null){
                log.error("{}, 處理回調事件異常  notifyResult data error, notifyResult ={} ",logPrefix, notifyResult);
                throw new BizException("處理回調事件異常！"); //需要实现类自行抛出ResponseException, 不应该在这抛此異常。
            }

            //判断訂單狀態
            if(notifyResult.getChannelState() == ChannelRetMsg.ChannelState.CONFIRM_SUCCESS) {
                payOrder.setState(PayOrder.STATE_SUCCESS);
            }else if(notifyResult.getChannelState() == ChannelRetMsg.ChannelState.CONFIRM_FAIL) {
                payOrder.setState(PayOrder.STATE_FAIL);
            }

            boolean hasReturnUrl = StringUtils.isNotBlank(payOrder.getReturnUrl());
            log.info("===== {}, 訂單通知完成。 payOrderId={}, parseState = {}, hasReturnUrl={} =====", logPrefix, payOrderId, notifyResult.getChannelState(), hasReturnUrl);

            //包含通知地址时
            if(hasReturnUrl){
                // 重定向
                response.sendRedirect(payMchNotifyService.createReturnUrl(payOrder, mchAppConfigContext.getMchApp().getAppSecret()));
                return null;
            }else{

                //跳转到支付成功页面
                return this.toReturnPage(null);
            }

        } catch (BizException e) {
            log.error("{}, payOrderId={}, BizException", logPrefix, payOrderId, e);
            return this.toReturnPage(e.getMessage());

        } catch (ResponseException e) {
            log.error("{}, payOrderId={}, ResponseException", logPrefix, payOrderId, e);
            return this.toReturnPage(e.getMessage());

        } catch (Exception e) {
            log.error("{}, payOrderId={}, 系統異常", logPrefix, payOrderId, e);
            return this.toReturnPage(e.getMessage());
        }
    }

    /** 異步回調入口 **/
    @ResponseBody
    @RequestMapping(value= {"/api/pay/notify/{ifCode}", "/api/pay/notify/{ifCode}/{payOrderId}"})
    public ResponseEntity doNotify(HttpServletRequest request, @PathVariable("ifCode") String ifCode, @PathVariable(value = "payOrderId", required = false) String urlOrderId){

        String payOrderId = null;
        String logPrefix = "进入[" +ifCode+ "]支付回調：urlOrderId：["+ StringUtils.defaultIfEmpty(urlOrderId, "") + "] ";
        log.info("===== {} =====" , logPrefix);

        try {

            // 參數有误
            if(StringUtils.isEmpty(ifCode)){
                return ResponseEntity.badRequest().body("ifCode is empty");
            }

            //查询支付介面是否存在
            IChannelNoticeService payNotifyService = SpringBeansUtil.getBean(ifCode + "ChannelNoticeService", IChannelNoticeService.class);

            // 支付通道介面实现不存在
            if(payNotifyService == null){
                log.error("{}, interface not exists ", logPrefix);
                return ResponseEntity.badRequest().body("[" + ifCode + "] interface not exists");
            }

            // 解析訂單号 和 請求參數
            MutablePair<String, Object> mutablePair = payNotifyService.parseParams(request, urlOrderId, IChannelNoticeService.NoticeTypeEnum.DO_NOTIFY);
            if(mutablePair == null){ // 解析数据失敗， 响应已處理
                log.error("{}, mutablePair is null ", logPrefix);
                throw new BizException("解析数据異常！"); //需要实现类自行抛出ResponseException, 不应该在这抛此異常。
            }

            //解析到訂單号
            payOrderId = mutablePair.left;
            log.info("{}, 解析数据为：payOrderId:{}, params:{}", logPrefix, payOrderId, mutablePair.getRight());

            if(StringUtils.isNotEmpty(urlOrderId) && !urlOrderId.equals(payOrderId)){
                log.error("{}, 訂單号不匹配. urlOrderId={}, payOrderId={} ", logPrefix, urlOrderId, payOrderId);
                throw new BizException("訂單号不匹配！");
            }

            //獲取訂單号 和 訂單数据
            PayOrder payOrder = payOrderService.getById(payOrderId);

            // 訂單不存在
            if(payOrder == null){
                log.error("{}, 訂單不存在. payOrderId={} ", logPrefix, payOrderId);
                return payNotifyService.doNotifyOrderNotExists(request);
            }

            //查询出商戶應用的設定資訊
            MchAppConfigContext mchAppConfigContext = configContextQueryService.queryMchInfoAndAppInfo(payOrder.getMchNo(), payOrder.getAppId());


            //调起介面的回調判断
            ChannelRetMsg notifyResult = payNotifyService.doNotice(request, mutablePair.getRight(), payOrder, mchAppConfigContext, IChannelNoticeService.NoticeTypeEnum.DO_NOTIFY);

            // 返回null 表明出现異常， 无需處理通知下游等操作。
            if(notifyResult == null || notifyResult.getChannelState() == null || notifyResult.getResponseEntity() == null){
                log.error("{}, 處理回調事件異常  notifyResult data error, notifyResult ={} ",logPrefix, notifyResult);
                throw new BizException("處理回調事件異常！"); //需要实现类自行抛出ResponseException, 不应该在这抛此異常。
            }

            boolean updateOrderSuccess = true; //默认更新成功
            // 訂單是 【支付中狀態】
            if(payOrder.getState() == PayOrder.STATE_ING) {

                //明确成功
                if(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS == notifyResult.getChannelState()) {

                    updateOrderSuccess = payOrderService.updateIng2Success(payOrderId, notifyResult.getChannelOrderId(), notifyResult.getChannelUserId());

                    //明确失敗
                }else if(ChannelRetMsg.ChannelState.CONFIRM_FAIL == notifyResult.getChannelState()) {

                    updateOrderSuccess = payOrderService.updateIng2Fail(payOrderId, notifyResult.getChannelOrderId(), notifyResult.getChannelUserId(), notifyResult.getChannelErrCode(), notifyResult.getChannelErrMsg());
                }

            // ADR-0007：已關閉訂單，仅接受经完整验证的 paid-APN 转回支付成功
            }else if(payOrder.getState() == PayOrder.STATE_CLOSED
                    && ChannelRetMsg.ChannelState.CONFIRM_SUCCESS == notifyResult.getChannelState()) {

                updateOrderSuccess = payOrderService.updateClosed2Success(payOrderId, notifyResult.getChannelOrderId(), notifyResult.getChannelUserId());
            }

            // 更新訂單 異常
            if(!updateOrderSuccess){
                log.error("{}, updateOrderSuccess = {} ",logPrefix, updateOrderSuccess);
                return payNotifyService.doNotifyOrderStateUpdateFail(request);
            }

            //訂單支付成功 其他业务逻辑
            if(notifyResult.getChannelState() == ChannelRetMsg.ChannelState.CONFIRM_SUCCESS){
                payOrderProcessService.confirmSuccess(payOrder);
            }

            log.info("===== {}, 訂單通知完成。 payOrderId={}, parseState = {} =====", logPrefix, payOrderId, notifyResult.getChannelState());

            return notifyResult.getResponseEntity();

        } catch (BizException e) {
            log.error("{}, payOrderId={}, BizException", logPrefix, payOrderId, e);
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (ResponseException e) {
            log.error("{}, payOrderId={}, ResponseException", logPrefix, payOrderId, e);
            return e.getResponseEntity();

        } catch (Exception e) {
            log.error("{}, payOrderId={}, 系統異常", logPrefix, payOrderId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    /*  跳转到支付成功页面 **/
    private String toReturnPage(String errInfo){


        return "cashier/returnPage";
    }

    private void onlyJump(String urlOrderId, String logPrefix) throws IOException {

        if (StringUtils.isNotBlank(urlOrderId) && urlOrderId.startsWith(CS.PAY_RETURNURL_FIX_ONLY_JUMP_PREFIX)) {

            String payOrderId = urlOrderId.substring(CS.PAY_RETURNURL_FIX_ONLY_JUMP_PREFIX.length());

            //獲取訂單号 和 訂單数据
            PayOrder payOrder = payOrderService.getById(payOrderId);

            // 訂單不存在
            if(payOrder == null){
                log.error("{}, 訂單不存在. payOrderId={} ", logPrefix, payOrderId);
                this.toReturnPage("支付訂單不存在");
            }

            //查询出商戶應用的設定資訊
            MchAppConfigContext mchAppConfigContext = configContextQueryService.queryMchInfoAndAppInfo(payOrder.getMchNo(), payOrder.getAppId());

            if (StringUtils.isBlank(payOrder.getReturnUrl())) {
                this.toReturnPage(null);
            }
            response.sendRedirect(payMchNotifyService.createReturnUrl(payOrder, mchAppConfigContext.getMchApp().getAppSecret()));
        }
    }

}
