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
package com.jeequan.jeepay.pay.ctrl.refund;

import com.jeequan.jeepay.core.ctrls.AbstractCtrl;
import com.jeequan.jeepay.core.entity.RefundOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.exception.ResponseException;
import com.jeequan.jeepay.core.utils.SpringBeansUtil;
import com.jeequan.jeepay.pay.channel.IChannelRefundNoticeService;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.ConfigContextService;
import com.jeequan.jeepay.pay.service.RefundOrderProcessService;
import com.jeequan.jeepay.service.impl.RefundOrderService;
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

/*
* 渠道侧的退款通知入口Controller 【異步回調(doNotify) 】
*
* @author jmdhappy
* @site https://www.jeequan.com
* @date 2021/9/25 22:35
*/
@Slf4j
@Controller
public class ChannelRefundNoticeController extends AbstractCtrl {

    @Autowired private RefundOrderService refundOrderService;
    @Autowired private ConfigContextQueryService configContextQueryService;
    @Autowired private RefundOrderProcessService refundOrderProcessService;

    /** 異步回調入口 **/
    @ResponseBody
    @RequestMapping(value= {"/api/refund/notify/{ifCode}", "/api/refund/notify/{ifCode}/{refundOrderId}"})
    public ResponseEntity doNotify(HttpServletRequest request, @PathVariable("ifCode") String ifCode, @PathVariable(value = "refundOrderId", required = false) String urlOrderId){

        String refundOrderId = null;
        String logPrefix = "进入[" +ifCode+ "]退款回調：urlOrderId：["+ StringUtils.defaultIfEmpty(urlOrderId, "") + "] ";
        log.info("===== {} =====" , logPrefix);

        try {

            // 參數有误
            if(StringUtils.isEmpty(ifCode)){
                return ResponseEntity.badRequest().body("ifCode is empty");
            }

            //查询退款介面是否存在
            IChannelRefundNoticeService refundNotifyService = SpringBeansUtil.getBean(ifCode + "ChannelRefundNoticeService", IChannelRefundNoticeService.class);

            // 支付通道介面实现不存在
            if(refundNotifyService == null){
                log.error("{}, interface not exists ", logPrefix);
                return ResponseEntity.badRequest().body("[" + ifCode + "] interface not exists");
            }

            // 解析訂單号 和 請求參數
            MutablePair<String, Object> mutablePair = refundNotifyService.parseParams(request, urlOrderId, IChannelRefundNoticeService.NoticeTypeEnum.DO_NOTIFY);
            if(mutablePair == null){ // 解析数据失敗， 响应已處理
                log.error("{}, mutablePair is null ", logPrefix);
                throw new BizException("解析数据異常！"); //需要实现类自行抛出ResponseException, 不应该在这抛此異常。
            }

            // 解析到訂單号
            refundOrderId = mutablePair.left;
            log.info("{}, 解析数据为：refundOrderId:{}, params:{}", logPrefix, refundOrderId, mutablePair.getRight());

            if(StringUtils.isNotEmpty(urlOrderId) && !urlOrderId.equals(refundOrderId)){
                log.error("{}, 訂單号不匹配. urlOrderId={}, refundOrderId={} ", logPrefix, urlOrderId, refundOrderId);
                throw new BizException("退款單號不匹配！");
            }

            //獲取訂單号 和 訂單数据
            RefundOrder refundOrder = refundOrderService.getById(refundOrderId);

            // 訂單不存在
            if(refundOrder == null){
                log.error("{}, 退款訂單不存在. refundOrder={} ", logPrefix, refundOrder);
                return refundNotifyService.doNotifyOrderNotExists(request);
            }

            //查询出商戶應用的設定資訊
            MchAppConfigContext mchAppConfigContext = configContextQueryService.queryMchInfoAndAppInfo(refundOrder.getMchNo(), refundOrder.getAppId());

            //调起介面的回調判断
            ChannelRetMsg notifyResult = refundNotifyService.doNotice(request, mutablePair.getRight(), refundOrder, mchAppConfigContext, IChannelRefundNoticeService.NoticeTypeEnum.DO_NOTIFY);

            // 返回null 表明出现異常， 无需處理通知下游等操作。
            if(notifyResult == null || notifyResult.getChannelState() == null || notifyResult.getResponseEntity() == null){
                log.error("{}, 處理回調事件異常  notifyResult data error, notifyResult ={} ",logPrefix, notifyResult);
                throw new BizException("處理回調事件異常！"); //需要实现类自行抛出ResponseException, 不应该在这抛此異常。
            }
            // 處理退款訂單
            boolean updateOrderSuccess = refundOrderProcessService.handleRefundOrder4Channel(notifyResult, refundOrder);

            // 更新退款訂單 異常
            if(!updateOrderSuccess){
                log.error("{}, updateOrderSuccess = {} ",logPrefix, updateOrderSuccess);
                return refundNotifyService.doNotifyOrderStateUpdateFail(request);
            }

            log.info("===== {}, 訂單通知完成。 refundOrderId={}, parseState = {} =====", logPrefix, refundOrderId, notifyResult.getChannelState());

            return notifyResult.getResponseEntity();

        } catch (BizException e) {
            log.error("{}, refundOrderId={}, BizException", logPrefix, refundOrderId, e);
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (ResponseException e) {
            log.error("{}, refundOrderId={}, ResponseException", logPrefix, refundOrderId, e);
            return e.getResponseEntity();

        } catch (Exception e) {
            log.error("{}, refundOrderId={}, 系統異常", logPrefix, refundOrderId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
