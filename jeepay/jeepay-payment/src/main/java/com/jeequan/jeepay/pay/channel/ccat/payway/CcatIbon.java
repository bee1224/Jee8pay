package com.jeequan.jeepay.pay.channel.ccat.payway;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.model.params.ccat.CcatNormalMchParams;
import com.jeequan.jeepay.pay.channel.ccat.CcatClient;
import com.jeequan.jeepay.pay.channel.ccat.CcatClient.CcatException;
import com.jeequan.jeepay.pay.channel.ccat.CcatClient.ErrorType;
import com.jeequan.jeepay.pay.channel.ccat.CcatIbonOrderRS;
import com.jeequan.jeepay.pay.channel.ccat.CcatKit;
import com.jeequan.jeepay.pay.channel.ccat.CcatMchParamsResolver;
import com.jeequan.jeepay.pay.channel.ccat.CcatPaymentService;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.AbstractRS;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRQ;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** CCAT ibon CVS 建单与 ambiguous-response recovery。 */
@Service
public class CcatIbon extends CcatPaymentService {

    static final String QUERY_NOT_FOUND_MESSAGE = "找不到此筆代繳資訊";
    private static final int ORDER_DETAIL_MAX_LENGTH = 150;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    private final CcatClient client;
    private final CcatMchParamsResolver paramsResolver;

    public CcatIbon(CcatClient client, CcatMchParamsResolver paramsResolver) {
        this.client = client;
        this.paramsResolver = paramsResolver;
    }

    @Override
    public String preCheck(UnifiedOrderRQ bizRQ, PayOrder payOrder) {
        try {
            if (!"TWD".equalsIgnoreCase(payOrder.getCurrency())) {
                return "CCAT ibon 仅支持 TWD";
            }
            CcatKit.toCcatTwdAmount(payOrder.getAmount());
            parsePayer(bizRQ.getChannelExtra());
            if (payOrder.getExpiredTime() == null) {
                return "CCAT ibon 缴费期限不能为空";
            }
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Override
    public AbstractRS pay(UnifiedOrderRQ bizRQ, PayOrder payOrder, MchAppConfigContext mchAppConfigContext) {
        CcatIbonOrderRS result = new CcatIbonOrderRS();
        try {
            CcatNormalMchParams params = paramsResolver.resolve(mchAppConfigContext);
            JSONObject request = buildAppendRequest(bizRQ, payOrder, params, getNotifyUrl());
            JSONObject response = executeCreate(params, payOrder, request);
            populateWaitingResult(result, payOrder, response);
        } catch (CcatException e) {
            result.setChannelRetMsg(errorResult(e));
        } catch (IllegalArgumentException e) {
            result.setChannelRetMsg(errorResult(new CcatException(ErrorType.CONFIGURATION, e.getMessage())));
        }
        return result;
    }

    JSONObject buildAppendRequest(UnifiedOrderRQ bizRQ, PayOrder payOrder, CcatNormalMchParams params,
                                  String notifyUrl) throws CcatException {
        if (params == null || StringUtils.isAnyBlank(
                params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw new CcatException(ErrorType.CONFIGURATION, "CCAT merchant configuration is incomplete");
        }
        CcatClient.resolveBaseUrl(params.getEnvironment());
        JSONObject payer = parsePayer(bizRQ.getChannelExtra());
        long orderAmount = CcatKit.toCcatTwdAmount(payOrder.getAmount());
        if (payOrder.getExpiredTime() == null) {
            throw new IllegalArgumentException("CCAT ibon 缴费期限不能为空");
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

    JSONObject executeCreate(CcatNormalMchParams params, PayOrder payOrder, JSONObject request)
            throws CcatException {
        try {
            JSONObject response = client.append(params, request);
            requireProviderOk(response);
            validateCreateResponse(response, payOrder, params);
            return response;
        } catch (CcatException e) {
            if (!isAmbiguous(e)) {
                throw e;
            }
            return recoverAfterAmbiguity(params, payOrder, request);
        }
    }

    private JSONObject recoverAfterAmbiguity(CcatNormalMchParams params, PayOrder payOrder, JSONObject request)
            throws CcatException {
        JSONObject query = client.query(params, payOrder.getPayOrderId());
        if (isProviderOk(query)) {
            validateCreateResponse(query, payOrder, params);
            return query;
        }
        if (!isExactNotFound(query)) {
            throw providerError(query);
        }

        JSONObject retry;
        try {
            retry = client.append(params, request);
        } catch (CcatException retryFailure) {
            if (isAmbiguous(retryFailure)) {
                JSONObject finalQuery = client.query(params, payOrder.getPayOrderId());
                if (isProviderOk(finalQuery)) {
                    validateCreateResponse(finalQuery, payOrder, params);
                    return finalQuery;
                }
            }
            throw retryFailure;
        }
        if (isProviderOk(retry)) {
            validateCreateResponse(retry, payOrder, params);
            return retry;
        }

        CcatException retryError = providerError(retry);
        JSONObject finalQuery = client.query(params, payOrder.getPayOrderId());
        if (isProviderOk(finalQuery)) {
            validateCreateResponse(finalQuery, payOrder, params);
            return finalQuery;
        }
        throw retryError;
    }

    private static void validateCreateResponse(JSONObject response, PayOrder payOrder, CcatNormalMchParams params)
            throws CcatException {
        try {
            if (!payOrder.getPayOrderId().equals(response.getString("cust_order_no"))) {
                throw new IllegalArgumentException("cust_order_no mismatch");
            }
            String responseCustId = response.getString("cust_id");
            if (responseCustId != null && !params.getCustId().equals(responseCustId)) {
                throw new IllegalArgumentException("cust_id mismatch");
            }
            long responseAmount = CcatKit.wholeTwdToMinorUnits(response.get("order_amount"), "order_amount", false);
            if (responseAmount != payOrder.getAmount()) {
                throw new IllegalArgumentException("order_amount mismatch");
            }
            CcatKit.parseWholeTwd(response.get("bill_amount"), "bill_amount", false);
            boolean hasCode = StringUtils.isNoneBlank(response.getString("ibon_shopid"), response.getString("ibon_code"));
            boolean hasShortUrl = StringUtils.isNotBlank(response.getString("short_url"));
            if (!hasCode && !hasShortUrl) {
                throw new IllegalArgumentException("ibon payment instruction is missing");
            }
            if (StringUtils.isBlank(response.getString("expire_date"))) {
                throw new IllegalArgumentException("expire_date is missing");
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new CcatException(ErrorType.MALFORMED, "CCAT Create response validation failed", e);
        }
    }

    private static void populateWaitingResult(CcatIbonOrderRS result, PayOrder payOrder, JSONObject response) {
        result.setIbonShopId(response.getString("ibon_shopid"));
        result.setIbonCode(response.getString("ibon_code"));
        result.setExpireDate(response.getString("expire_date"));
        result.setBillAmount(CcatKit.parseWholeTwd(response.get("bill_amount"), "bill_amount", false));
        result.setShortUrl(response.getString("short_url"));

        ChannelRetMsg channel = ChannelRetMsg.waiting();
        channel.setNeedQuery(true);
        channel.setChannelAttach(result.buildPayData());
        result.setChannelRetMsg(channel);
    }

    private static ChannelRetMsg errorResult(CcatException error) {
        ChannelRetMsg result = new ChannelRetMsg();
        if (error.getType() == ErrorType.CONFIGURATION || error.getType() == ErrorType.AUTHENTICATION) {
            result.setChannelState(ChannelRetMsg.ChannelState.SYS_ERROR);
        } else if (error.getType() == ErrorType.BUSINESS) {
            result.setChannelState(ChannelRetMsg.ChannelState.API_RET_ERROR);
        } else {
            result.setChannelState(ChannelRetMsg.ChannelState.UNKNOWN);
            result.setNeedQuery(true);
        }
        result.setChannelErrCode("CCAT_" + error.getType().name());
        result.setChannelErrMsg(error.getMessage());
        return result;
    }

    private static JSONObject parsePayer(String channelExtra) {
        JSONObject payer;
        try {
            payer = JSON.parseObject(StringUtils.defaultIfBlank(channelExtra, "{}"));
        } catch (JSONException e) {
            throw new IllegalArgumentException("CCAT channelExtra 格式错误");
        }
        if (payer == null || StringUtils.isAnyBlank(
                payer.getString("payerName"), payer.getString("payerPostcode"),
                payer.getString("payerAddress"), payer.getString("payerMobile"),
                payer.getString("payerEmail"))) {
            throw new IllegalArgumentException("CCAT channelExtra 缺少缴款人资料");
        }
        return payer;
    }

    private static String boundedOrderDetail(PayOrder payOrder) {
        String detail = StringUtils.defaultString(payOrder.getSubject());
        if (StringUtils.isNotBlank(payOrder.getBody())) {
            detail += " " + payOrder.getBody();
        }
        if (StringUtils.isBlank(detail)) {
            throw new IllegalArgumentException("CCAT order_detail is required");
        }
        return detail.length() <= ORDER_DETAIL_MAX_LENGTH ? detail : detail.substring(0, ORDER_DETAIL_MAX_LENGTH);
    }

    private static void requireProviderOk(JSONObject response) throws CcatException {
        if (!isProviderOk(response)) {
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

    private static CcatException providerError(JSONObject response) {
        String message = response == null ? "CCAT Provider rejected request" : response.getString("msg");
        return new CcatException(ErrorType.BUSINESS,
                StringUtils.defaultIfBlank(message, "CCAT Provider rejected request"));
    }

    private static boolean isAmbiguous(CcatException error) {
        return error.getType() == ErrorType.AMBIGUOUS || error.getType() == ErrorType.MALFORMED;
    }
}
