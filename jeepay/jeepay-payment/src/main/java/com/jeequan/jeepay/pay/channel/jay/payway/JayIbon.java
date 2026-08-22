package com.jeequan.jeepay.pay.channel.jay.payway;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.model.params.jay.JayNormalMchParams;
import com.jeequan.jeepay.pay.channel.jay.JayClient;
import com.jeequan.jeepay.pay.channel.jay.JayClient.JayException;
import com.jeequan.jeepay.pay.channel.jay.JayClient.CollectResponse;
import com.jeequan.jeepay.pay.channel.jay.JayClient.ErrorType;
import com.jeequan.jeepay.pay.channel.jay.JayIbonOrderRS;
import com.jeequan.jeepay.pay.channel.jay.JayKit;
import com.jeequan.jeepay.pay.channel.jay.JayLogSanitizer;
import com.jeequan.jeepay.pay.channel.jay.JayMchParamsResolver;
import com.jeequan.jeepay.pay.channel.jay.JayPaymentService;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRQ;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** JAY ibon CVS 建单与 ambiguous-response recovery。 */
@Service
@Slf4j
public class JayIbon extends JayPaymentService {

    static final String QUERY_NOT_FOUND_MESSAGE = "找不到此筆代繳資訊";
    private static final int ORDER_DETAIL_MAX_LENGTH = 150;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private final JayClient client;
    private final JayMchParamsResolver paramsResolver;

    public JayIbon(JayClient client, JayMchParamsResolver paramsResolver) {
        this.client = client;
        this.paramsResolver = paramsResolver;
    }

    @Override
    public String preCheck(UnifiedOrderRQ bizRQ, PayOrder payOrder) {
        try {
            if (!"TWD".equalsIgnoreCase(payOrder.getCurrency())) {
                return "JAY ibon 僅支援 TWD";
            }
            JayKit.toJayTwdAmount(payOrder.getAmount());
            parsePayer(bizRQ.getChannelExtra());
            if (payOrder.getExpiredTime() == null) {
                return "JAY ibon 缴费期限不能為空";
            }
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Override
    public AbstractRS pay(UnifiedOrderRQ bizRQ, PayOrder payOrder, MchAppConfigContext mchAppConfigContext) {
        JayIbonOrderRS result = new JayIbonOrderRS();
        Instant requestTimestamp = Instant.now();
        JayNormalMchParams params = null;
        try {
            params = paramsResolver.resolve(mchAppConfigContext);
            JSONObject request = buildAppendRequest(bizRQ, payOrder, params, getNotifyUrl());
            CreateResult response = executeCreate(params, payOrder, request);
            populateWaitingResult(result, payOrder, response.response.getBody());
            logCreate(payOrder, requestTimestamp, params, response.response, null,
                    "SUCCESS", response.reconciliation);
        } catch (JayException e) {
            result.setChannelRetMsg(errorResult(e));
            logCreate(payOrder, requestTimestamp, params, null, e, outcome(e),
                    isAmbiguous(e) ? "SCHEDULED_QUERY" : "NONE");
        } catch (IllegalArgumentException e) {
            JayException configuration = new JayException(ErrorType.CONFIGURATION, e.getMessage());
            result.setChannelRetMsg(errorResult(configuration));
            logCreate(payOrder, requestTimestamp, params, null, configuration, "REJECTED", "NONE");
        }
        return result;
    }

    JSONObject buildAppendRequest(UnifiedOrderRQ bizRQ, PayOrder payOrder, JayNormalMchParams params,
                                  String notifyUrl) throws JayException {
        if (params == null || StringUtils.isAnyBlank(
                params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw new JayException(ErrorType.CONFIGURATION, "JAY 商戶設定不完整");
        }
        JayClient.resolveBaseUrl(params.getEnvironment());
        JSONObject payer = parsePayer(bizRQ.getChannelExtra());
        long orderAmount = JayKit.toJayTwdAmount(payOrder.getAmount());
        if (payOrder.getExpiredTime() == null) {
            throw new IllegalArgumentException("JAY ibon 缴费期限不能為空");
        }

        JSONObject request = new JSONObject(true);
        request.put("cmd", "CvsOrderAppend");
        request.put("cust_id", params.getCustId());
        request.put("cust_order_no", payOrder.getPayOrderId());
        request.put("order_amount", orderAmount);
        request.put("expire_date", payOrder.getExpiredTime().toInstant().atZone(TAIPEI).toLocalDate().format(DATE_FORMAT));
        request.put("payer_name", payer.getString("payerName"));
        request.put("payer_postcode", payer.getString("payerPostcode"));
        request.put("payer_address", payer.getString("payerAddress"));
        request.put("payer_mobile", payer.getString("payerMobile"));
        request.put("payer_email", payer.getString("payerEmail"));
        request.put("payment_type", 0);
        request.put("payment_acquirerType", 2);
        request.put("apn_url", notifyUrl);
        request.put("order_detail", boundedOrderDetail(payOrder));
        return request;
    }

    CreateResult executeCreate(JayNormalMchParams params, PayOrder payOrder, JSONObject request)
            throws JayException {
        try {
            CollectResponse response = client.append(params, request);
            requireProviderOk(response);
            validateCreateResponse(response.getBody(), payOrder, params, response);
            return new CreateResult(response, "NONE");
        } catch (JayException e) {
            if (!isAmbiguous(e)) {
                throw e;
            }
            return recoverAfterAmbiguity(params, payOrder, e);
        }
    }

    private CreateResult recoverAfterAmbiguity(JayNormalMchParams params, PayOrder payOrder,
                                               JayException createFailure)
            throws JayException {
        CollectResponse query;
        try {
            query = client.queryWithMetadata(params, payOrder.getPayOrderId());
        } catch (JayException queryFailure) {
            throw withReconciliation(createFailure, "QUERY_ERROR");
        }
        if (isProviderOk(query.getBody())) {
            validateCreateResponse(query.getBody(), payOrder, params, query);
            return new CreateResult(query, "QUERY_CONFIRMED");
        }
        String reconciliation = isExactNotFound(query.getBody()) ? "QUERY_NOT_FOUND" : "QUERY_INCONCLUSIVE";
        throw withReconciliation(createFailure, reconciliation);
    }

    private static void validateCreateResponse(JSONObject response, PayOrder payOrder, JayNormalMchParams params,
                                               CollectResponse metadata)
            throws JayException {
        try {
            if (!payOrder.getPayOrderId().equals(response.getString("cust_order_no"))) {
                throw new IllegalArgumentException("cust_order_no mismatch");
            }
            String responseCustId = response.getString("cust_id");
            if (responseCustId != null && !params.getCustId().equals(responseCustId)) {
                throw new IllegalArgumentException("cust_id mismatch");
            }
            long responseAmount = JayKit.wholeTwdToMinorUnits(response.get("order_amount"), "order_amount", false);
            if (responseAmount != payOrder.getAmount()) {
                throw new IllegalArgumentException("order_amount mismatch");
            }
            JayKit.parseWholeTwd(response.get("bill_amount"), "bill_amount", false);
            boolean hasCode = StringUtils.isNoneBlank(response.getString("ibon_shopid"), response.getString("ibon_code"));
            boolean hasShortUrl = StringUtils.isNotBlank(response.getString("short_url"));
            if (!hasCode && !hasShortUrl) {
                throw new IllegalArgumentException("ibon payment instruction is missing");
            }
            if (StringUtils.isBlank(response.getString("expire_date"))) {
                throw new IllegalArgumentException("expire_date is missing");
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new JayException(ErrorType.MALFORMED, "JAY Create response validation failed", e,
                    metadata.getHttpStatus(), metadata.getLatencyMillis(), allowlistedProviderFields(response));
        }
    }

    private static void populateWaitingResult(JayIbonOrderRS result, PayOrder payOrder, JSONObject response) {
        result.setIbonShopId(response.getString("ibon_shopid"));
        result.setIbonCode(response.getString("ibon_code"));
        result.setExpireDate(response.getString("expire_date"));
        result.setBillAmount(JayKit.parseWholeTwd(response.get("bill_amount"), "bill_amount", false));
        result.setShortUrl(response.getString("short_url"));

        ChannelRetMsg channel = ChannelRetMsg.waiting();
        channel.setNeedQuery(true);
        channel.setChannelAttach(result.buildPayData());
        result.setChannelRetMsg(channel);
    }

    private static ChannelRetMsg errorResult(JayException error) {
        if (error.getType() == ErrorType.BUSINESS) {
            return ChannelRetMsg.confirmFail("JAY_BUSINESS", "JAY Provider 拒絕請求");
        }
        ChannelRetMsg result = new ChannelRetMsg();
        if (error.getType() == ErrorType.CONFIGURATION || error.getType() == ErrorType.AUTHENTICATION) {
            result.setChannelState(ChannelRetMsg.ChannelState.SYS_ERROR);
        } else {
            result.setChannelState(ChannelRetMsg.ChannelState.UNKNOWN);
            result.setNeedQuery(true);
        }
        result.setChannelErrCode("JAY_" + error.getType().name());
        result.setChannelErrMsg(error.getMessage());
        return result;
    }

    private static JSONObject parsePayer(String channelExtra) {
        JSONObject payer;
        try {
            payer = JSON.parseObject(StringUtils.defaultIfBlank(channelExtra, "{}"));
        } catch (JSONException e) {
            throw new IllegalArgumentException("JAY channelExtra 格式錯誤");
        }
        if (payer == null || StringUtils.isAnyBlank(
                payer.getString("payerName"), payer.getString("payerPostcode"),
                payer.getString("payerAddress"), payer.getString("payerMobile"),
                payer.getString("payerEmail"))) {
            throw new IllegalArgumentException("JAY channelExtra 缺少繳款人資料");
        }
        return payer;
    }

    private static String boundedOrderDetail(PayOrder payOrder) {
        String detail = StringUtils.defaultString(payOrder.getSubject());
        if (StringUtils.isNotBlank(payOrder.getBody())) {
            detail += " " + payOrder.getBody();
        }
        if (StringUtils.isBlank(detail)) {
            throw new IllegalArgumentException("JAY order_detail 為必填");
        }
        return detail.length() <= ORDER_DETAIL_MAX_LENGTH ? detail : detail.substring(0, ORDER_DETAIL_MAX_LENGTH);
    }

    private static void requireProviderOk(CollectResponse response) throws JayException {
        if (!isProviderOk(response.getBody())) {
            throw providerError(response);
        }
    }

    private static boolean isProviderOk(JSONObject response) {
        return response != null && "OK".equals(response.getString("status"));
    }

    private static boolean isExactNotFound(JSONObject response) {
        return response != null && "ERROR".equals(response.getString("status"))
                && QUERY_NOT_FOUND_MESSAGE.equals(response.getString("msg"));
    }

    private static JayException providerError(CollectResponse response) {
        String message = response.getBody() == null ? null : response.getBody().getString("msg");
        return new JayException(ErrorType.BUSINESS,
                StringUtils.defaultIfBlank(message, "JAY Provider 拒絕請求"), null,
                response.getHttpStatus(), response.getLatencyMillis(), allowlistedProviderFields(response.getBody()));
    }

    private static boolean isAmbiguous(JayException error) {
        return error.getType() == ErrorType.AMBIGUOUS || error.getType() == ErrorType.MALFORMED;
    }

    private static JayException withReconciliation(JayException error, String reconciliation) {
        JSONObject fields = error.getProviderFields() == null
                ? new JSONObject(true) : new JSONObject(error.getProviderFields());
        fields.put("reconciliation", reconciliation);
        return new JayException(error.getType(), error.getMessage(), error.getCause(),
                error.getHttpStatus(), error.getLatencyMillis(), fields);
    }

    private static JSONObject allowlistedProviderFields(JSONObject source) {
        if (source == null) {
            return null;
        }
        JSONObject allowed = new JSONObject(true);
        copyIfPresent(source, allowed, "status");
        copyIfPresent(source, allowed, "process_code");
        copyIfPresent(source, allowed, "result_code");
        copyIfPresent(source, allowed, "code");
        copyIfPresent(source, allowed, "msg");
        copyIfPresent(source, allowed, "message");
        copyIfPresent(source, allowed, "error");
        copyIfPresent(source, allowed, "error_description");
        copyIfPresent(source, allowed, "trans_id");
        copyIfPresent(source, allowed, "transaction_id");
        return allowed;
    }

    private static void copyIfPresent(JSONObject source, JSONObject target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private static void logCreate(PayOrder payOrder, Instant requestTimestamp, JayNormalMchParams params,
                                  CollectResponse response, JayException error, String outcome,
                                  String defaultReconciliation) {
        JSONObject fields = error == null ? allowlistedProviderFields(response.getBody()) : error.getProviderFields();
        String reconciliation = value(fields, "reconciliation");
        if (StringUtils.isBlank(reconciliation)) {
            reconciliation = defaultReconciliation;
        }
        String providerMessage = firstValue(fields, "msg", "message", "error_description", "error");
        if (StringUtils.isBlank(providerMessage) && error != null) {
            providerMessage = error.getMessage();
        }
        Integer httpStatus = error == null ? Integer.valueOf(response.getHttpStatus()) : error.getHttpStatus();
        Long latencyMillis = error == null ? Long.valueOf(response.getLatencyMillis()) : error.getLatencyMillis();
        log.info("event=JAY_CREATE operation=CREATE outcome={} payOrderId={} mchOrderNo={} "
                        + "requestTimestamp={} amountTwd={} httpStatus={} providerStatus={} providerCode={} "
                        + "providerMessage={} providerReference={} latencyMs={} reconciliation={}",
                outcome, JayLogSanitizer.sanitize(payOrder.getPayOrderId(), params),
                JayLogSanitizer.sanitize(payOrder.getMchOrderNo(), params),
                requestTimestamp, JayKit.toJayTwdAmount(payOrder.getAmount()), value(httpStatus),
                JayLogSanitizer.sanitize(value(fields, "status"), params),
                JayLogSanitizer.sanitize(firstValue(fields, "process_code", "result_code", "code"), params),
                JayLogSanitizer.sanitize(providerMessage, params),
                JayLogSanitizer.sanitize(firstValue(fields, "trans_id", "transaction_id"), params),
                value(latencyMillis), reconciliation);
    }

    private static String outcome(JayException error) {
        if (error.getType() == ErrorType.BUSINESS || error.getType() == ErrorType.AUTHENTICATION
                || error.getType() == ErrorType.CONFIGURATION) {
            return "REJECTED";
        }
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof IOException || cause instanceof InterruptedException) {
                return "TRANSPORT_ERROR";
            }
            cause = cause.getCause();
        }
        return "AMBIGUOUS";
    }

    private static String firstValue(JSONObject fields, String... keys) {
        for (String key : keys) {
            String candidate = value(fields, key);
            if (StringUtils.isNotBlank(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String value(JSONObject fields, String key) {
        return fields == null ? null : fields.getString(key);
    }

    private static String value(Object value) {
        return value == null ? "UNAVAILABLE" : String.valueOf(value);
    }

    static final class CreateResult {
        private final CollectResponse response;
        private final String reconciliation;

        private CreateResult(CollectResponse response, String reconciliation) {
            this.response = response;
            this.reconciliation = reconciliation;
        }

        JSONObject body() {
            return response.getBody();
        }
    }
}
