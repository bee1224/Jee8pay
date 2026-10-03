package com.jeequan.jeepay.pay.channel.fyz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.ResponseException;
import com.jeequan.jeepay.core.model.params.fyz.FyzNormalMchParams;
import com.jeequan.jeepay.pay.channel.AbstractChannelNoticeService;
import com.jeequan.jeepay.pay.channel.fyz.FyzClient.FyzException;
import com.jeequan.jeepay.pay.channel.fyz.FyzPayOrderQueryService.ValidatedQuery;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.service.impl.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

/** FYZ APN parser、identity binding 与 authenticated Query reconciliation。 */
@Slf4j
@Service
public class FyzChannelNoticeService extends AbstractChannelNoticeService {

    private static final String REQUEST_ORDER_ID = FyzChannelNoticeService.class.getName() + ".orderId";
    private static final String REQUEST_TRANS_ID = FyzChannelNoticeService.class.getName() + ".transId";

    private final FyzPayOrderQueryService queryService;
    private final PayOrderService payOrderService;
    private final FyzMchParamsResolver paramsResolver;

    public FyzChannelNoticeService(FyzPayOrderQueryService queryService, PayOrderService payOrderService,
                                    FyzMchParamsResolver paramsResolver) {
        this.queryService = queryService;
        this.payOrderService = payOrderService;
        this.paramsResolver = paramsResolver;
    }

    @Override
    public String getIfCode() {
        return CS.IF_CODE.FYZ;
    }

    @Override
    public MutablePair<String, Object> parseParams(HttpServletRequest request, String urlOrderId,
                                                   NoticeTypeEnum noticeTypeEnum) {
        try {
            if (noticeTypeEnum != NoticeTypeEnum.DO_NOTIFY || !"POST".equals(request.getMethod())) {
                throw new IllegalArgumentException("FYZ APN requires POST");
            }
            String contentType = request.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
                throw new IllegalArgumentException("FYZ APN requires application/json");
            }
            String body = request.getReader().lines().collect(Collectors.joining());
            FyzNoticePayload payload = FyzNoticePayload.parse(body);
            request.setAttribute(REQUEST_ORDER_ID, payload.orderNo);
            request.setAttribute(REQUEST_TRANS_ID, payload.transId);
            return MutablePair.of(payload.orderNo, payload);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("FYZ APN parse rejected: {}", e.getMessage());
            throw failureException();
        }
    }

    @Override
    public ChannelRetMsg doNotice(HttpServletRequest request, Object params, PayOrder payOrder,
                                  MchAppConfigContext mchAppConfigContext, NoticeTypeEnum noticeTypeEnum) {
        try {
            if (noticeTypeEnum != NoticeTypeEnum.DO_NOTIFY || !(params instanceof FyzNoticePayload)) {
                throw new IllegalArgumentException("FYZ APN payload is invalid");
            }
            FyzNoticePayload payload = (FyzNoticePayload) params;
            if (!payOrder.getPayOrderId().equals(payload.orderNo)) {
                throw new IllegalArgumentException("order_no mismatch");
            }
            if (payOrder.getState() != PayOrder.STATE_ING
                    && payOrder.getState() != PayOrder.STATE_SUCCESS
                    && payOrder.getState() != PayOrder.STATE_FAIL
                    && payOrder.getState() != PayOrder.STATE_CLOSED) {
                throw new IllegalArgumentException("local order state cannot accept APN");
            }

            FyzNormalMchParams fyzParams = paramsResolver.resolve(mchAppConfigContext);
            if (!payload.apiId.equals(fyzParams.getCustId())) {
                throw new IllegalArgumentException("api_id mismatch");
            }
            if (!FyzKit.verifyChecksum(payload.apiId, payload.transId, payload.amount,
                    payload.status, payload.nonce, payload.checksum)) {
                throw new IllegalArgumentException("checksum mismatch");
            }

            ValidatedQuery query = queryService.queryValidated(payOrder, fyzParams);
            validateApnAgainstQuery(payload, query);
            ensureTerminalStateConsistency(payOrder, query.getState());
            ensureTransactionBinding(payOrder, payload.transId);

            ChannelRetMsg result = query.toChannelRetMsg(payload.transId);
            result.setChannelOrderId(payload.transId);
            result.setResponseEntity(successResponse());
            return result;
        } catch (FyzException | IllegalArgumentException e) {
            log.warn("FYZ APN rejected for payOrderId={}: {}", payOrder.getPayOrderId(), e.getMessage());
            throw failureException();
        }
    }

    @Override
    public ResponseEntity doNotifyOrderNotExists(HttpServletRequest request) {
        return failureResponse();
    }

    @Override
    public ResponseEntity doNotifyOrderStateUpdateFail(HttpServletRequest request) {
        Object orderId = request.getAttribute(REQUEST_ORDER_ID);
        Object transId = request.getAttribute(REQUEST_TRANS_ID);
        if (orderId instanceof String && transId instanceof String) {
            PayOrder committed = payOrderService.getById((String) orderId);
            if (committed != null
                    && (committed.getState() == PayOrder.STATE_SUCCESS || committed.getState() == PayOrder.STATE_FAIL)
                    && transId.equals(committed.getChannelOrderNo())) {
                return successResponse();
            }
        }
        return failureResponse();
    }

    private void ensureTransactionBinding(PayOrder payOrder, String transId) {
        if (StringUtils.isNotBlank(payOrder.getChannelOrderNo())) {
            if (!payOrder.getChannelOrderNo().equals(transId)) {
                throw new IllegalArgumentException("trans_id mismatch");
            }
            return;
        }

        if (payOrder.getState() == PayOrder.STATE_ING) {
            return; // ChannelNoticeController 在 native terminal transition 时写入 channelOrderNo。
        }

        boolean bound = payOrderService.update(new LambdaUpdateWrapper<PayOrder>()
                .set(PayOrder::getChannelOrderNo, transId)
                .eq(PayOrder::getPayOrderId, payOrder.getPayOrderId())
                .eq(PayOrder::getState, payOrder.getState())
                .isNull(PayOrder::getChannelOrderNo));
        if (!bound) {
            PayOrder committed = payOrderService.getById(payOrder.getPayOrderId());
            if (committed == null || !transId.equals(committed.getChannelOrderNo())) {
                throw new IllegalArgumentException("trans_id binding failed");
            }
        }
    }

    private static void ensureTerminalStateConsistency(PayOrder payOrder, ChannelRetMsg.ChannelState state) {
        if (payOrder.getState() == PayOrder.STATE_SUCCESS && state != ChannelRetMsg.ChannelState.CONFIRM_SUCCESS) {
            throw new IllegalArgumentException("terminal success state mismatch");
        }
        if (payOrder.getState() == PayOrder.STATE_FAIL && state != ChannelRetMsg.ChannelState.CONFIRM_FAIL) {
            throw new IllegalArgumentException("terminal fail state mismatch");
        }
        // ADR-0007：本地 CLOSED 是未諮詢 Provider 的本地關閉；只有經完整驗證的 paid-APN
        // （authenticated Query 顯示已付款）可轉回 SUCCESS，其餘狀態維持 CLOSED。
        if (payOrder.getState() == PayOrder.STATE_CLOSED
                && state != ChannelRetMsg.ChannelState.CONFIRM_SUCCESS) {
            throw new IllegalArgumentException("closed order only accepts paid APN");
        }
    }

    private static void validateApnAgainstQuery(FyzNoticePayload payload, ValidatedQuery query) {
        long apnAmount = FyzKit.parseWholeTwd(payload.amount, "amount", false);
        if (apnAmount != query.getBillAmount()) {
            throw new IllegalArgumentException("APN amount mismatch");
        }

        boolean statusAgrees;
        switch (payload.status) {
            case "A":
                statusAgrees = query.getState() == ChannelRetMsg.ChannelState.WAITING;
                break;
            case "B":
                statusAgrees = query.getState() == ChannelRetMsg.ChannelState.CONFIRM_SUCCESS
                        && ("4".equals(query.getProcessCode()) || "7".equals(query.getProcessCode())
                        || "8".equals(query.getProcessCode()));
                break;
            case "C":
                statusAgrees = "5".equals(query.getProcessCode());
                break;
            case "D":
                statusAgrees = "6".equals(query.getProcessCode());
                break;
            case "E":
                statusAgrees = "7".equals(query.getProcessCode()) || "8".equals(query.getProcessCode());
                break;
            default:
                statusAgrees = false;
        }
        if (!statusAgrees) {
            throw new IllegalArgumentException("APN status does not agree with Query");
        }

        if ("B".equals(payload.status) || "E".equals(payload.status)) {
            if (payload.payAmount == null || StringUtils.isBlank(payload.payDate)) {
                throw new IllegalArgumentException("paid APN fields are missing");
            }
            long apnPayAmount = FyzKit.parseWholeTwd(payload.payAmount, "pay_amount", false);
            if (query.getPayAmount() == null || apnPayAmount != query.getPayAmount()
                    || apnPayAmount != query.getBillAmount()) {
                throw new IllegalArgumentException("APN paid amount mismatch");
            }
        }
    }

    private static ResponseEntity<String> successResponse() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("OK");
    }

    private static ResponseEntity<String> failureResponse() {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("ERROR");
    }

    private static ResponseException failureException() {
        return new ResponseException(failureResponse());
    }

    static final class FyzNoticePayload {
        private final String apiId;
        private final String transId;
        private final String orderNo;
        private final String amount;
        private final String status;
        private final String nonce;
        private final String checksum;
        private final String payDate;
        private final String payAmount;

        private FyzNoticePayload(JSONObject body) {
            apiId = required(body, "api_id");
            transId = required(body, "trans_id");
            orderNo = required(body, "order_no");
            amount = required(body, "amount");
            status = required(body, "status");
            nonce = required(body, "nonce");
            checksum = required(body, "checksum");
            if (!"2".equals(required(body, "payment_code"))) {
                throw new IllegalArgumentException("payment_code mismatch");
            }
            if (!nonce.matches("[0-9]{10}")) {
                throw new IllegalArgumentException("nonce format is invalid");
            }
            FyzKit.parseWholeTwd(amount, "amount", false);
            payDate = optional(body, "pay_date");
            payAmount = optional(body, "pay_amount");
        }

        static FyzNoticePayload parse(String rawBody) {
            if (StringUtils.isBlank(rawBody)) {
                throw new IllegalArgumentException("FYZ APN body is empty");
            }
            try {
                JSONObject body = JSON.parseObject(rawBody);
                if (body == null) {
                    throw new IllegalArgumentException("FYZ APN body is empty");
                }
                return new FyzNoticePayload(body);
            } catch (JSONException e) {
                throw new IllegalArgumentException("FYZ APN JSON is malformed");
            }
        }

        private static String required(JSONObject body, String name) {
            Object value = body.get(name);
            if (value == null || StringUtils.isBlank(String.valueOf(value))) {
                throw new IllegalArgumentException(name + " 為必填");
            }
            return String.valueOf(value);
        }

        private static String optional(JSONObject body, String name) {
            Object value = body.get(name);
            return value == null ? null : String.valueOf(value);
        }

        @Override
        public String toString() {
            return "FyzNoticePayload{orderNo='" + orderNo + "', status='" + status + "'}";
        }
    }
}
