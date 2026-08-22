package com.jeequan.jeepay.pay.channel.ryo;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRS;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/** Merchant-facing ibon payment instructions carried by UnifiedOrderRS extension methods。 */
@Data
public class RyoIbonOrderRS extends UnifiedOrderRS {

    private String ibonShopId;
    private String ibonCode;
    private String expireDate;
    private Long billAmount;
    private String shortUrl;

    @Override
    public String buildPayDataType() {
        return "ryoIbon";
    }

    @Override
    public String buildPayData() {
        boolean hasPaymentCode = StringUtils.isNoneBlank(ibonShopId, ibonCode);
        if (!hasPaymentCode && StringUtils.isBlank(shortUrl)) {
            throw new IllegalStateException("RYO payment instruction is unavailable");
        }
        JSONObject result = new JSONObject(true);
        result.put("ibonShopId", ibonShopId);
        result.put("ibonCode", ibonCode);
        result.put("paymentCode", ibonShopId == null || ibonCode == null ? null : ibonShopId + ibonCode);
        result.put("expireDate", expireDate);
        result.put("billAmount", billAmount);
        result.put("shortUrl", shortUrl);
        return result.toJSONString();
    }
}
