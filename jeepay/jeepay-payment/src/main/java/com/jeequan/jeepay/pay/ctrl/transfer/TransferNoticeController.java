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
package com.jeequan.jeepay.pay.ctrl.transfer;

import com.jeequan.jeepay.core.ctrls.AbstractCtrl;
import com.jeequan.jeepay.core.entity.TransferOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.exception.ResponseException;
import com.jeequan.jeepay.core.utils.SpringBeansUtil;
import com.jeequan.jeepay.pay.channel.ITransferNoticeService;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.service.impl.TransferOrderService;
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

/**
* 轉帳異步通知入口Controller
*
* @author zx
* @site https://www.jeequan.com
* @date 2022/12/30 10:26
*/
@Slf4j
@Controller
public class TransferNoticeController extends AbstractCtrl {

    @Autowired private TransferOrderService transferOrderService;
    @Autowired private ConfigContextQueryService configContextQueryService;
    @Autowired private PayMchNotifyService payMchNotifyService;


    /** 異步回調入口 **/
    @ResponseBody
    @RequestMapping(value= {"/api/transfer/notify/{ifCode}", "/api/transfer/notify/{ifCode}/{transferId}"})
    public ResponseEntity doNotify(HttpServletRequest request, @PathVariable("ifCode") String ifCode, @PathVariable(value = "transferId", required = false) String urlOrderId){

        String transferId = null;
        String logPrefix = "进入[" +ifCode+ "]轉帳回調：urlOrderId：["+ StringUtils.defaultIfEmpty(urlOrderId, "") + "] ";
        log.info("===== {} =====" , logPrefix);

        try {

            // 參數有误
            if(StringUtils.isEmpty(ifCode)){
                return ResponseEntity.badRequest().body("ifCode is empty");
            }

            //查询轉帳介面是否存在
            ITransferNoticeService transferNotifyService = SpringBeansUtil.getBean(ifCode + "TransferNoticeService", ITransferNoticeService.class);

            // 支付通道轉帳介面实现不存在
            if(transferNotifyService == null){
                log.error("{}, transfer interface not exists ", logPrefix);
                return ResponseEntity.badRequest().body("[" + ifCode + "] transfer interface not exists");
            }

            // 解析轉帳单号 和 請求參數
            MutablePair<String, Object> mutablePair = transferNotifyService.parseParams(request, urlOrderId);
            if(mutablePair == null){ // 解析数据失敗， 响应已處理
                log.error("{}, mutablePair is null ", logPrefix);
                throw new BizException("解析数据異常！"); //需要实现类自行抛出ResponseException, 不应该在这抛此異常。
            }

            // 解析到轉帳单号
            transferId = mutablePair.left;
            log.info("{}, 解析数据为：transferId:{}, params:{}", logPrefix, transferId, mutablePair.getRight());

            // 獲取轉帳单号 和 轉帳单数据
            TransferOrder transferOrder = transferOrderService.getById(transferId);

            // 轉帳单不存在
            if(transferOrder == null){
                log.error("{}, 轉帳单不存在. transferId={} ", logPrefix, transferId);
                return transferNotifyService.doNotifyOrderNotExists(request);
            }

            //查询出商戶應用的設定資訊
            MchAppConfigContext mchAppConfigContext = configContextQueryService.queryMchInfoAndAppInfo(transferOrder.getMchNo(), transferOrder.getAppId());

            //调起介面的回調判断
            ChannelRetMsg notifyResult = transferNotifyService.doNotice(request, mutablePair.getRight(), transferOrder, mchAppConfigContext);

            // 返回null 表明出现異常， 无需處理通知下游等操作。
            if(notifyResult == null || notifyResult.getChannelState() == null || notifyResult.getResponseEntity() == null){
                log.error("{}, 處理回調事件異常  notifyResult data error, notifyResult ={} ",logPrefix, notifyResult);
                throw new BizException("處理回調事件異常！"); //需要实现类自行抛出ResponseException, 不应该在这抛此異常。
            }

            // 轉帳单是 【轉帳中狀態】
            if(transferOrder.getState() == TransferOrder.STATE_ING) {
                if(notifyResult.getChannelState() == ChannelRetMsg.ChannelState.CONFIRM_SUCCESS) {
                    // 轉帳成功
                    transferOrderService.updateIng2Success(transferId, notifyResult.getChannelOrderId());
                    payMchNotifyService.transferOrderNotify(transferOrderService.getById(transferId));

                }else if(notifyResult.getChannelState() == ChannelRetMsg.ChannelState.CONFIRM_FAIL){
                    // 轉帳失敗
                    transferOrderService.updateIng2Fail(transferId, notifyResult.getChannelOrderId(), notifyResult.getChannelErrCode(), notifyResult.getChannelErrMsg());
                    payMchNotifyService.transferOrderNotify(transferOrderService.getById(transferId));
                }
            }

            log.info("===== {}, 轉帳单通知完成。 transferId={}, parseState = {} =====", logPrefix, transferId, notifyResult.getChannelState());

            return notifyResult.getResponseEntity();

        } catch (BizException e) {
            log.error("{}, transferId={}, BizException", logPrefix, transferId, e);
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (ResponseException e) {
            log.error("{}, transferId={}, ResponseException", logPrefix, transferId, e);
            return e.getResponseEntity();

        } catch (Exception e) {
            log.error("{}, transferId={}, 系統異常", logPrefix, transferId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
