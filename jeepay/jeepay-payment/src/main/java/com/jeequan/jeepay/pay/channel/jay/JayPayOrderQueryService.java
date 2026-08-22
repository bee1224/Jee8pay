package com.jeequan.jeepay.pay.channel.jay;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.model.params.jay.JayNormalMchParams;
import com.jeequan.jeepay.pay.channel.IPayOrderQueryService;
import com.jeequan.jeepay.pay.channel.jay.JayClient.JayException;
import com.jeequan.jeepay.pay.channel.jay.JayClient.ErrorType;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/** JAY CvsOrderQuery adapter；仅回传 ChannelRetMsg，由 native reissue flow 转态。 */
@Service
public class JayPayOrderQueryService implements IPayOrderQueryService {

    private final JayClient client;
    private final JayMchParamsResolver paramsResolver;

    public JayPayOrderQueryService(JayClient client, JayMchParamsResolver paramsResolver) {
        this.client = client;
        this.paramsResolver = paramsResolver;
    }

    @Override
    public String getIfCode() {
        return CS.IF_CODE.JAY;
    }

    @Override
    public ChannelRetMsg query(PayOrder payOrder, MchAppConfigContext mchAppConfigContext) {
        try {
            JayNormalMchParams params = paramsResolver.resolve(mchAppConfigContext);
            return queryValidated(payOrder, params).toChannelRetMsg(payOrder.getChannelOrderNo());
        } catch (JayException e) {
            return ChannelRetMsg.unknown(e.getMessage());
        }
    }

    ValidatedQuery queryValidated(PayOrder payOrder, JayNormalMchParams params) throws JayException {
        JSONObject response = client.query(params, payOrder.getPayOrderId());
        if (!"OK".equals(response.getString("status"))) {
            String message = response.getString("msg");
            throw new JayException(ErrorType.BUSINESS,
                    StringUtils.defaultIfBlank(message, "JAY Query 請求被拒絕"));
        }

        try {
            if (!payOrder.getPayOrderId().equals(response.getString("cust_order_no"))) {
                throw new IllegalArgumentException("cust_order_no mismatch");
            }
            String responseCustId = response.getString("cust_id");
            if (responseCustId != null && !params.getCustId().equals(responseCustId)) {
                throw new IllegalArgumentException("cust_id mismatch");
            }
            String responseTransId = response.getString("trans_id");
            if (StringUtils.isNotBlank(responseTransId)
                    && StringUtils.isNotBlank(payOrder.getChannelOrderNo())
                    && !payOrder.getChannelOrderNo().equals(responseTransId)) {
                throw new IllegalArgumentException("trans_id mismatch");
            }

            long localAmount = JayKit.toJayTwdAmount(payOrder.getAmount());
            long orderAmount = JayKit.parseWholeTwd(response.get("order_amount"), "order_amount", false);
            if (localAmount != orderAmount) {
                throw new IllegalArgumentException("order_amount mismatch");
            }
            long billAmount = JayKit.parseWholeTwd(response.get("bill_amount"), "bill_amount", false);
            String processCode = response.getString("process_code");
            if (StringUtils.isBlank(processCode)) {
                throw new IllegalArgumentException("process_code 為必填");
            }
            ChannelRetMsg.ChannelState state = JayKit.mapProcessCode(processCode);

            Long payAmount = null;
            if (response.get("pay_amount") != null) {
                payAmount = JayKit.parseWholeTwd(response.get("pay_amount"), "pay_amount", true);
            }
            if (state == ChannelRetMsg.ChannelState.CONFIRM_SUCCESS
                    && (payAmount == null || payAmount != billAmount)) {
                throw new IllegalArgumentException("paid amount mismatch");
            }
            return new ValidatedQuery(response, processCode, state, orderAmount, billAmount, payAmount);
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new JayException(ErrorType.MALFORMED, "JAY Query response validation failed", e);
        }
    }

    static final class ValidatedQuery {
        private final JSONObject response;
        private final String processCode;
        private final ChannelRetMsg.ChannelState state;
        private final long orderAmount;
        private final long billAmount;
        private final Long payAmount;

        private ValidatedQuery(JSONObject response, String processCode, ChannelRetMsg.ChannelState state,
                               long orderAmount, long billAmount, Long payAmount) {
            this.response = response;
            this.processCode = processCode;
            this.state = state;
            this.orderAmount = orderAmount;
            this.billAmount = billAmount;
            this.payAmount = payAmount;
        }

        ChannelRetMsg toChannelRetMsg(String existingChannelOrderNo) {
            ChannelRetMsg result;
            if (state == ChannelRetMsg.ChannelState.CONFIRM_SUCCESS) {
                result = ChannelRetMsg.confirmSuccess(existingChannelOrderNo);
            } else if (state == ChannelRetMsg.ChannelState.CONFIRM_FAIL) {
                result = ChannelRetMsg.confirmFail(existingChannelOrderNo);
            } else if (state == ChannelRetMsg.ChannelState.WAITING) {
                result = ChannelRetMsg.waiting();
            } else {
                result = ChannelRetMsg.unknown("JAY process_code 無安全轉態");
            }
            result.setChannelOriginResponse(sanitizedResponse());
            return result;
        }

        private String sanitizedResponse() {
            JSONObject sanitized = new JSONObject(true);
            sanitized.put("status", response.getString("status"));
            sanitized.put("cust_order_no", response.getString("cust_order_no"));
            sanitized.put("process_code", processCode);
            sanitized.put("order_amount", orderAmount);
            sanitized.put("bill_amount", billAmount);
            sanitized.put("pay_amount", payAmount);
            return sanitized.toJSONString();
        }

        String getProcessCode() {
            return processCode;
        }

        ChannelRetMsg.ChannelState getState() {
            return state;
        }

        long getBillAmount() {
            return billAmount;
        }

        Long getPayAmount() {
            return payAmount;
        }
    }
}
