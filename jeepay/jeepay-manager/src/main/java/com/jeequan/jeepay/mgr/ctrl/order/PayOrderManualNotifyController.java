/*
 * Jee8pay 專屬：人工手動回調（非 upstream JeePay 功能）。
 *
 * 用途：繞過支付流程，直接把訂單「目前狀態」以標準 Merchant Notify 格式通知到 notifyUrl，
 * 供外部系統商做回調接收測試（驗簽、ACK、冪等、狀態處理）。
 *
 * 重要：通知內容是訂單目前的真實狀態（例如 CLOSED(6) 訂單關閉），不是偽造 SUCCESS；
 * 外部商戶只能在 state=2（SUCCESS）時上分，本功能不得被當作「偽造支付成功」使用。
 *
 * 實作：與原生 PayMchNotifyService.payOrderNotify 相同路徑 —— 建立/重發 MchNotifyRecord
 * 並推送 PayOrderMchNotifyMQ，由 payment 模組既有接收器負責 HTTP POST 與最多 6 次重試，
 * 完全沿用原生通知基礎設施，不修改 core 的 state transition / notify 邏輯。
 */
package com.jeequan.jeepay.mgr.ctrl.order;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.components.mq.model.PayOrderMchNotifyMQ;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchNotifyRecord;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.utils.JeepayKit;
import com.jeequan.jeepay.core.utils.StringKit;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.MchAppService;
import com.jeequan.jeepay.service.impl.MchNotifyRecordService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人工手動回調
 *
 * @author Jee8pay
 * @date 2026-08-18
 */
@Tag(name = "人工手動回調")
@RestController
@RequestMapping("api/payOrder/manualNotify")
public class PayOrderManualNotifyController extends CommonCtrl {

    private static final Logger log = LoggerFactory.getLogger(PayOrderManualNotifyController.class);

    @Autowired private PayOrderService payOrderService;
    @Autowired private MchAppService mchAppService;
    @Autowired private MchNotifyRecordService mchNotifyRecordService;
    @Autowired private IMQSender mqSender;

    @Operation(summary = "人工手動回調訂單通知（繞過支付）")
    @MethodLog(remark = "人工手動回調訂單通知")
    @PreAuthorize("hasAuthority('ENT_PAY_ORDER_MANUAL_NOTIFY')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ApiRes manualNotify() {

        String payOrderId = getValStringRequired("payOrderId");
        PayOrder order = payOrderService.getById(payOrderId);
        if (order == null) {
            throw new BizException("訂單不存在");
        }
        if (StringUtils.isEmpty(order.getNotifyUrl())) {
            throw new BizException("訂單未設定異步通知地址（notifyUrl）");
        }
        MchApp mchApp = mchAppService.getById(order.getAppId());
        if (mchApp == null || StringUtils.isEmpty(mchApp.getAppSecret())) {
            throw new BizException("無法取得商戶 App Secret，請檢查商戶應用設定");
        }

        // 1. 組通知 URL（與原生 createNotifyUrl 相同：非 null 訂單欄位 + reqTime + sign）
        JSONObject params = buildNotifyParams(order);
        params.put("reqTime", System.currentTimeMillis());
        params.put("sign", JeepayKit.getSign(params, mchApp.getAppSecret()));
        String notifyUrl = StringKit.appendUrlQuery(order.getNotifyUrl(), params);

        // 2. 建立或重置 MchNotifyRecord 為「通知中」
        MchNotifyRecord record = mchNotifyRecordService.findByPayOrder(payOrderId);
        if (record == null) {
            record = new MchNotifyRecord();
            record.setOrderId(order.getPayOrderId());
            record.setOrderType(MchNotifyRecord.TYPE_PAY_ORDER);
            record.setMchNo(order.getMchNo());
            record.setMchOrderNo(order.getMchOrderNo());
            record.setIsvNo(order.getIsvNo());
            record.setAppId(order.getAppId());
            record.setNotifyUrl(notifyUrl);
            record.setResResult("");
            record.setNotifyCount(0);
            record.setState(MchNotifyRecord.STATE_ING);
            mchNotifyRecordService.save(record);
        } else {
            record.setNotifyUrl(notifyUrl);
            record.setResResult("");
            record.setNotifyCount(0);
            record.setState(MchNotifyRecord.STATE_ING);
            mchNotifyRecordService.updateById(record);
        }

        // 3. 推送 MQ，由 payment 接收器負責 HTTP POST 與重試
        mqSender.send(PayOrderMchNotifyMQ.build(record.getNotifyId()));

        JSONObject result = new JSONObject();
        result.put("notifyId", record.getNotifyId());
        result.put("payOrderId", order.getPayOrderId());
        result.put("state", order.getState());
        result.put("notifyUrl", notifyUrl);
        log.info("人工回調已推送 payOrderId={}, notifyId={}, state={}", payOrderId, record.getNotifyId(), order.getState());
        return ApiRes.ok(result);
    }

    /** 與 QueryPayOrderRS.buildByPayOrder 一致的訂單欄位（僅非 null） */
    private JSONObject buildNotifyParams(PayOrder order) {
        JSONObject p = new JSONObject();
        putNotNull(p, "payOrderId", order.getPayOrderId());
        putNotNull(p, "mchNo", order.getMchNo());
        putNotNull(p, "appId", order.getAppId());
        putNotNull(p, "mchOrderNo", order.getMchOrderNo());
        putNotNull(p, "ifCode", order.getIfCode());
        putNotNull(p, "wayCode", order.getWayCode());
        putNotNull(p, "amount", order.getAmount());
        putNotNull(p, "currency", order.getCurrency());
        putNotNull(p, "state", order.getState());
        putNotNull(p, "clientIp", order.getClientIp());
        putNotNull(p, "subject", order.getSubject());
        putNotNull(p, "body", order.getBody());
        putNotNull(p, "channelOrderNo", order.getChannelOrderNo());
        putNotNull(p, "errCode", order.getErrCode());
        putNotNull(p, "errMsg", order.getErrMsg());
        putNotNull(p, "extParam", order.getExtParam());
        if (order.getSuccessTime() != null) {
            putNotNull(p, "successTime", order.getSuccessTime().getTime());
        }
        if (order.getCreatedAt() != null) {
            putNotNull(p, "createdAt", order.getCreatedAt().getTime());
        }
        return p;
    }

    private void putNotNull(JSONObject p, String key, Object value) {
        if (value != null) {
            p.put(key, value);
        }
    }
}
