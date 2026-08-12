package com.jeequan.jeepay.pay.channel.ccat;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.pay.rqrs.payorder.UnifiedOrderRS;
import lombok.Data;

/** Merchant-facing ibon payment instructions carried by UnifiedOrderRS extension methods。 */
@Data
public class CcatIbonOrderRS extends UnifiedOrderRS {

    private String ibonShopId;
    private String ibonCode;
    private String expireDate;
    private Long billAmount;
    private String shortUrl;

    @Override
    public String buildPayDataType() {
        return "ccatIbon";
    }

    @Override
    public String buildPayData() {
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
