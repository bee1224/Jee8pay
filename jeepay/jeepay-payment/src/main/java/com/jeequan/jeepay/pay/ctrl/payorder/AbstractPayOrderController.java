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

import cn.hutool.core.date.DateUtil;
import com.jeequan.jeepay.components.mq.model.PayOrderReissueMQ;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchInfo;
import com.jeequan.jeepay.core.entity.MchPayPassage;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.model.DBApplicationConfig;
import com.jeequan.jeepay.core.model.QRCodeParams;
import com.jeequan.jeepay.core.utils.AmountUtil;
import com.jeequan.jeepay.core.utils.SeqKit;
import com.jeequan.jeepay.core.utils.SpringBeansUtil;
import com.jeequan.jeepay.core.utils.StringKit;
import com.jeequan.jeepay.pay.channel.IPaymentService;
import com.jeequan.jeepay.pay.ctrl.ApiController;
import com.jeequan.jeepay.pay.exception.ChannelException;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRQ;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRS;
import com.jeequan.jeepay.pay.rqrs.payorder.payway.QrCashierOrderRQ;
import com.jeequan.jeepay.pay.rqrs.payorder.payway.QrCashierOrderRS;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import com.jeequan.jeepay.service.impl.MchPayPassageService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import com.jeequan.jeepay.service.impl.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;

/*
* 创建支付訂單抽象类
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:26
*/
@Slf4j
public abstract class AbstractPayOrderController extends ApiController {

    @Autowired private MchPayPassageService mchPayPassageService;
    @Autowired private PayOrderService payOrderService;
    @Autowired private ConfigContextQueryService configContextQueryService;
    @Autowired private PayOrderProcessService payOrderProcessService;
    @Autowired private SysConfigService sysConfigService;
    @Autowired private IMQSender mqSender;


    /** 统一下单 (新建訂單模式) **/
    protected ApiRes unifiedOrder(String wayCode, UnifiedOrderRQ bizRQ){
        return unifiedOrder(wayCode, bizRQ, null);
    }

    /** 统一下单 **/
    protected ApiRes unifiedOrder(String wayCode, UnifiedOrderRQ bizRQ, PayOrder payOrder){

        // 响应数据
        UnifiedOrderRS bizRS = null;

        //是否新訂單模式 [  一般介面都为新訂單模式，  由于QR_CASHIER支付方式，需要先 在DB插入一个新訂單， 导致此处需要特殊判断下。 如果已存在则直接更新，否则为插入。  ]
        boolean isNewOrder = payOrder == null;

        try {

            if(payOrder != null){ //当訂單存在时，封装公共參數。

                if(payOrder.getState() != PayOrder.STATE_INIT){
                    throw new BizException("訂單狀態異常");
                }

                payOrder.setWayCode(wayCode); // 需要将訂單更新 支付方式
                payOrder.setChannelUser(bizRQ.getChannelUserId()); //更新渠道用戶資訊
                bizRQ.setMchNo(payOrder.getMchNo());
                bizRQ.setAppId(payOrder.getAppId());
                bizRQ.setMchOrderNo(payOrder.getMchOrderNo());
                bizRQ.setWayCode(wayCode);
                bizRQ.setAmount(payOrder.getAmount());
                bizRQ.setCurrency(payOrder.getCurrency());
                bizRQ.setClientIp(payOrder.getClientIp());
                bizRQ.setSubject(payOrder.getSubject());
                bizRQ.setNotifyUrl(payOrder.getNotifyUrl());
                bizRQ.setReturnUrl(payOrder.getReturnUrl());
                bizRQ.setChannelExtra(payOrder.getChannelExtra());
                bizRQ.setExtParam(payOrder.getExtParam());
                bizRQ.setDivisionMode(payOrder.getDivisionMode());
            }

            String mchNo = bizRQ.getMchNo();
            String appId = bizRQ.getAppId();

            // 只有新訂單模式，进行校验
            if(isNewOrder && payOrderService.count(PayOrder.gw().eq(PayOrder::getMchNo, mchNo).eq(PayOrder::getMchOrderNo, bizRQ.getMchOrderNo())) > 0){
                throw new BizException("商戶訂單["+bizRQ.getMchOrderNo()+"]已存在");
            }

            if(StringUtils.isNotEmpty(bizRQ.getNotifyUrl()) && !StringKit.isAvailableUrl(bizRQ.getNotifyUrl())){
                throw new BizException("異步通知地址協定僅支援 http:// 或 https:// !");
            }
            if(StringUtils.isNotEmpty(bizRQ.getReturnUrl()) && !StringKit.isAvailableUrl(bizRQ.getReturnUrl())){
                throw new BizException("同步通知地址協定僅支援 http:// 或 https:// !");
            }

            //獲取支付參數 (缓存数据) 和 商戶資訊
            MchAppConfigContext mchAppConfigContext = configContextQueryService.queryMchInfoAndAppInfo(mchNo, appId);
            if(mchAppConfigContext == null){
                throw new BizException("獲取商戶應用資訊失敗");
            }

            MchInfo mchInfo = mchAppConfigContext.getMchInfo();
            MchApp mchApp = mchAppConfigContext.getMchApp();

            if(mchApp == null || mchApp.getState() != CS.YES){
                throw new BizException("商戶應用狀態不可用");
            }

            //收银台支付并且只有新訂單需要走这里，  收银台二次下单的wayCode应该为实际支付方式。
            if(isNewOrder && CS.PAY_WAY_CODE.QR_CASHIER.equals(wayCode)){

                //生成訂單
                payOrder = genPayOrder(bizRQ, mchInfo, mchApp, null, null);
                String payOrderId = payOrder.getPayOrderId();
                //訂單入库 訂單狀態： 生成狀態  此时没有和任何上游渠道产生交互。
                payOrderService.save(payOrder);

                QrCashierOrderRS qrCashierOrderRS = new QrCashierOrderRS();
                QrCashierOrderRQ qrCashierOrderRQ = (QrCashierOrderRQ)bizRQ;

                DBApplicationConfig dbApplicationConfig = sysConfigService.getDBApplicationConfig();

                String payUrl = dbApplicationConfig.genUniJsapiPayUrl(QRCodeParams.TYPE_PAY_ORDER, payOrderId);
                if(CS.PAY_DATA_TYPE.CODE_IMG_URL.equals(qrCashierOrderRQ.getPayDataType())){ //二维码地址
                    qrCashierOrderRS.setCodeImgUrl(dbApplicationConfig.genScanImgUrl(payUrl));

                }else{ //默认都为跳转地址方式
                    qrCashierOrderRS.setPayUrl(payUrl);
                }

                return packageApiResByPayOrder(bizRQ, qrCashierOrderRS, payOrder);
            }

            // 根据支付方式， 查询出 该商戶 可用的支付介面
            MchPayPassage mchPayPassage = mchPayPassageService.findMchPayPassage(mchAppConfigContext.getMchNo(), mchAppConfigContext.getAppId(), wayCode);
            if(mchPayPassage == null){
                throw new BizException("商戶應用不支援該支付方式");
            }

            //獲取支付介面
            IPaymentService paymentService = checkMchWayCodeAndGetService(mchAppConfigContext, mchPayPassage);
            String ifCode = paymentService.getIfCode();

            //生成訂單
            if(isNewOrder){
                payOrder = genPayOrder(bizRQ, mchInfo, mchApp, ifCode, mchPayPassage);
            }else{
                payOrder.setIfCode(ifCode);

                // 查询支付方式的费率，并 在更新ing时更新费率資訊
                payOrder.setMchFeeRate(mchPayPassage.getRate());
                payOrder.setMchFeeAmount(AmountUtil.calPercentageFee(payOrder.getAmount(), payOrder.getMchFeeRate())); //商戶手续费,单位分
            }

            //预先校验
            String errMsg = paymentService.preCheck(bizRQ, payOrder);
            if(StringUtils.isNotEmpty(errMsg)){
                throw new BizException(errMsg);
            }

            String newPayOrderId = paymentService.customPayOrderId(bizRQ, payOrder, mchAppConfigContext);


            if(isNewOrder){
                if(StringUtils.isNotBlank(newPayOrderId)){ // 自定义訂單号
                    payOrder.setPayOrderId(newPayOrderId);
                }
                //訂單入库 訂單狀態： 生成狀態  此时没有和任何上游渠道产生交互。
                payOrderService.save(payOrder);
            }

            //调起上游支付介面
            bizRS = (UnifiedOrderRS) paymentService.pay(bizRQ, payOrder, mchAppConfigContext);

            //處理上游返回数据
            this.processChannelMsg(bizRS.getChannelRetMsg(), payOrder);

            return packageApiResByPayOrder(bizRQ, bizRS, payOrder);

        } catch (BizException e) {
            return ApiRes.customFail(e.getMessage());

        } catch (ChannelException e) {

            //處理上游返回数据
            this.processChannelMsg(e.getChannelRetMsg(), payOrder);

            if(e.getChannelRetMsg().getChannelState() == ChannelRetMsg.ChannelState.SYS_ERROR ){
                return ApiRes.customFail(e.getMessage());
            }

            return this.packageApiResByPayOrder(bizRQ, bizRS, payOrder);


        } catch (Exception e) {
            log.error("系統異常：{}", e);
            return ApiRes.customFail("系統異常");
        }
    }

    private PayOrder genPayOrder(UnifiedOrderRQ rq, MchInfo mchInfo, MchApp mchApp, String ifCode, MchPayPassage mchPayPassage){

        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(SeqKit.genPayOrderId()); //生成訂單ID
        payOrder.setMchNo(mchInfo.getMchNo()); //商戶號
        payOrder.setIsvNo(mchInfo.getIsvNo()); //服務商号
        payOrder.setMchName(mchInfo.getMchShortName()); //商戶名称（简称）
        payOrder.setMchType(mchInfo.getType()); //商戶类型
        payOrder.setMchOrderNo(rq.getMchOrderNo()); //商戶訂單号
        payOrder.setAppId(mchApp.getAppId()); //商戶應用appId
        payOrder.setIfCode(ifCode); //介面代码
        payOrder.setWayCode(rq.getWayCode()); //支付方式
        payOrder.setAmount(rq.getAmount()); //訂單金額

        if(mchPayPassage != null){
            payOrder.setMchFeeRate(mchPayPassage.getRate()); //商戶手续费费率快照
        }else{
            payOrder.setMchFeeRate(BigDecimal.ZERO); //预下单模式， 按照0计算入库， 后续进行更新
        }

        payOrder.setMchFeeAmount(AmountUtil.calPercentageFee(payOrder.getAmount(), payOrder.getMchFeeRate())); //商戶手续费,单位分

        payOrder.setCurrency(rq.getCurrency()); //币种
        payOrder.setState(PayOrder.STATE_INIT); //訂單狀態, 默认訂單生成狀態
        payOrder.setClientIp(StringUtils.defaultIfEmpty(rq.getClientIp(), getClientIp())); //客户端IP
        payOrder.setSubject(rq.getSubject()); //商品標題
        payOrder.setBody(rq.getBody()); //商品描述資訊
//        payOrder.setChannelExtra(rq.getChannelExtra()); //特殊渠道发起的附件额外參數,  是否应该删除该字段了？？ 比如authCode不应该记录， 只是在传输阶段存在的吧？  之前的为了在payOrder对象需要传参。
        payOrder.setChannelUser(rq.getChannelUserId()); //渠道用戶标志
        payOrder.setExtParam(rq.getExtParam()); //商戶扩展參數
        payOrder.setNotifyUrl(rq.getNotifyUrl()); //異步通知地址
        payOrder.setReturnUrl(rq.getReturnUrl()); //页面跳转地址

        // 分帳模式
        payOrder.setDivisionMode(ObjectUtils.defaultIfNull(rq.getDivisionMode(), PayOrder.DIVISION_MODE_FORBID));

        Date nowDate = new Date();

        //訂單过期时间 单位： 秒
        if(rq.getExpiredTime() != null){
            payOrder.setExpiredTime(DateUtil.offsetSecond(nowDate, rq.getExpiredTime()));
        }else{
            payOrder.setExpiredTime(DateUtil.offsetHour(nowDate, 2)); //訂單过期时间 默认两个小时
        }

        payOrder.setCreatedAt(nowDate); //訂單创建时间
        return payOrder;
    }


    /**
     * 校验： 商戶的支付方式是否可用
     * 返回： 支付介面
     * **/
    private IPaymentService checkMchWayCodeAndGetService(MchAppConfigContext mchAppConfigContext, MchPayPassage mchPayPassage){

        // 介面代码
        String ifCode = mchPayPassage.getIfCode();
        IPaymentService paymentService = SpringBeansUtil.getBean(ifCode + "PaymentService", IPaymentService.class);
        if(paymentService == null){
            throw new BizException("無此支付通道介面");
        }

        if(!paymentService.isSupport(mchPayPassage.getWayCode())){
            throw new BizException("介面不支援該支付方式");
        }

        if(mchAppConfigContext.getMchType() == MchInfo.TYPE_NORMAL){ //普通商戶

            if(configContextQueryService.queryNormalMchParams(mchAppConfigContext.getMchNo(), mchAppConfigContext.getAppId(), ifCode) == null){
                throw new BizException("商戶應用參數未設定");
            }
        }else if(mchAppConfigContext.getMchType() == MchInfo.TYPE_ISVSUB){ //特約商戶

            if(configContextQueryService.queryIsvsubMchParams(mchAppConfigContext.getMchNo(), mchAppConfigContext.getAppId(), ifCode) == null){
                throw new BizException("特約商戶參數未設定");
            }

            if(configContextQueryService.queryIsvParams(mchAppConfigContext.getMchInfo().getIsvNo(), ifCode) == null){
                throw new BizException("服務商參數未設定");
            }
        }

        return paymentService;

    }


    /** 處理返回的渠道資訊，并更新訂單狀態
     *  payOrder将对部分資訊进行 赋值操作。
     * **/
    private void processChannelMsg(ChannelRetMsg channelRetMsg, PayOrder payOrder){

        //对象为空 || 上游返回狀態为空， 则无需操作
        if(channelRetMsg == null || channelRetMsg.getChannelState() == null){
            return ;
        }

        String payOrderId = payOrder.getPayOrderId();

        //明确成功
        if(ChannelRetMsg.ChannelState.CONFIRM_SUCCESS == channelRetMsg.getChannelState()) {

            this.updateInitOrderStateThrowException(PayOrder.STATE_SUCCESS, payOrder, channelRetMsg);

            //訂單支付成功，其他业务逻辑
            payOrderProcessService.confirmSuccess(payOrder);

        //明确失敗
        }else if(ChannelRetMsg.ChannelState.CONFIRM_FAIL == channelRetMsg.getChannelState()) {

            this.updateInitOrderStateThrowException(PayOrder.STATE_FAIL, payOrder, channelRetMsg);

        // 上游處理中 || 未知 || 上游介面返回異常  訂單为支付中狀態
        }else if( ChannelRetMsg.ChannelState.WAITING == channelRetMsg.getChannelState() ||
                  ChannelRetMsg.ChannelState.UNKNOWN == channelRetMsg.getChannelState() ||
                  ChannelRetMsg.ChannelState.API_RET_ERROR == channelRetMsg.getChannelState()

        ){
            this.updateInitOrderStateThrowException(PayOrder.STATE_ING, payOrder, channelRetMsg);

        // 系統異常：  訂單不再處理。  为： 生成狀態
        }else if( ChannelRetMsg.ChannelState.SYS_ERROR == channelRetMsg.getChannelState()){

        }else{

            throw new BizException("ChannelState 返回異常！");
        }

        //判断是否需要轮询查单
        if(channelRetMsg.isNeedQuery()){
            mqSender.send(PayOrderReissueMQ.build(payOrderId, 1), 5);
        }

    }


    /** 更新訂單狀態 --》 訂單生成--》 其他狀態  (向外抛出異常) **/
    private void updateInitOrderStateThrowException(byte orderState, PayOrder payOrder, ChannelRetMsg channelRetMsg){

        payOrder.setState(orderState);
        payOrder.setChannelOrderNo(channelRetMsg.getChannelOrderId());
        payOrder.setErrCode(channelRetMsg.getChannelErrCode());
        payOrder.setErrMsg(channelRetMsg.getChannelErrMsg());

        // 聚合码场景 訂單对象存在会员資訊， 不可全部以上游为准。
        if(StringUtils.isNotEmpty(channelRetMsg.getChannelUserId())){
            payOrder.setChannelUser(channelRetMsg.getChannelUserId());
        }

        payOrderProcessService.updateIngAndSuccessOrFailByCreatebyOrder(payOrder, channelRetMsg);

    }


    /** 统一封装訂單数据  **/
    private ApiRes packageApiResByPayOrder(UnifiedOrderRQ bizRQ, UnifiedOrderRS bizRS, PayOrder payOrder){

        // 返回介面数据
        bizRS.setPayOrderId(payOrder.getPayOrderId());
        bizRS.setOrderState(payOrder.getState());
        bizRS.setMchOrderNo(payOrder.getMchOrderNo());

        if(payOrder.getState() == PayOrder.STATE_FAIL){
            bizRS.setErrCode(bizRS.getChannelRetMsg() != null ? bizRS.getChannelRetMsg().getChannelErrCode() : null);
            bizRS.setErrMsg(bizRS.getChannelRetMsg() != null ? bizRS.getChannelRetMsg().getChannelErrMsg() : null);
        }

        return ApiRes.okWithSign(bizRS, configContextQueryService.queryMchApp(bizRQ.getMchNo(), bizRQ.getAppId()).getAppSecret());
    }


}
