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

import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.utils.SpringBeansUtil;
import com.jeequan.jeepay.pay.channel.IPayOrderCloseService;
import com.jeequan.jeepay.pay.ctrl.ApiController;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.rqrs.payorder.ClosePayOrderRQ;
import com.jeequan.jeepay.pay.rqrs.payorder.ClosePayOrderRS;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * 關閉訂單 controller
 *
 * @author xiaoyu
 * @site https://www.jeequan.com
 * @date 2022/1/25 9:19
 */
@Slf4j
@RestController
public class CloseOrderController extends ApiController {

    @Autowired private PayOrderService payOrderService;
    @Autowired private ConfigContextQueryService configContextQueryService;

    /**
     * @author: xiaoyu
     * @date: 2022/1/25 9:19
     * @describe: 關閉訂單
     */
    @RequestMapping("/api/pay/close")
    public ApiRes closeOrder(){

        //獲取參數 & 簽章驗證
        ClosePayOrderRQ rq = getRQByWithMchSign(ClosePayOrderRQ.class);

        if(StringUtils.isAllEmpty(rq.getMchOrderNo(), rq.getPayOrderId())){
            throw new BizException("mchOrderNo 和 payOrderId 不能同時為空");
        }

        PayOrder payOrder = payOrderService.queryMchOrder(rq.getMchNo(), rq.getPayOrderId(), rq.getMchOrderNo());
        if(payOrder == null){
            throw new BizException("訂單不存在");
        }

        if (payOrder.getState() != PayOrder.STATE_INIT && payOrder.getState() != PayOrder.STATE_ING) {
            throw new BizException("當前訂單不可關閉");
        }

        ClosePayOrderRS bizRes = new ClosePayOrderRS();

        // 訂單生成狀態  直接修改訂單狀態
        if (payOrder.getState() == PayOrder.STATE_INIT) {
            payOrderService.updateInit2Close(payOrder.getPayOrderId());
            bizRes.setChannelRetMsg(ChannelRetMsg.confirmSuccess(null));
            return ApiRes.okWithSign(bizRes, configContextQueryService.queryMchApp(rq.getMchNo(), rq.getAppId()).getAppSecret());
        }

        try {

            String payOrderId = payOrder.getPayOrderId();

            //查询支付介面是否存在
            IPayOrderCloseService closeService = SpringBeansUtil.getBean(payOrder.getIfCode() + "PayOrderCloseService", IPayOrderCloseService.class);

            // 支付通道介面实现不存在
            if(closeService == null){
                log.error("{} interface not exists!", payOrder.getIfCode());
                return null;
            }

            //查询出商戶應用的設定資訊
            MchAppConfigContext mchAppConfigContext = configContextQueryService.queryMchInfoAndAppInfo(payOrder.getMchNo(), payOrder.getAppId());

            ChannelRetMsg channelRetMsg = closeService.close(payOrder, mchAppConfigContext);
            if(channelRetMsg == null){
                log.error("channelRetMsg is null");
                return null;
            }

            log.info("關閉訂單[{}]结果为：{}", payOrderId, channelRetMsg);

            // 關閉訂單 成功
            if(channelRetMsg.getChannelState() == ChannelRetMsg.ChannelState.CONFIRM_SUCCESS) {
                payOrderService.updateIng2Close(payOrderId);
            }else {
                return ApiRes.customFail(channelRetMsg.getChannelErrMsg());
            }

            bizRes.setChannelRetMsg(channelRetMsg);
        } catch (Exception e) {  // 關閉訂單異常
            log.error("error payOrderId = {}", payOrder.getPayOrderId(), e);
            return null;
        }

        return ApiRes.okWithSign(bizRes, configContextQueryService.queryMchApp(rq.getMchNo(), rq.getAppId()).getAppSecret());
    }

}
