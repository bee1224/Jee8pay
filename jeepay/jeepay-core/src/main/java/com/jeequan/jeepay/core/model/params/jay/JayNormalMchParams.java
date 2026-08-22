package com.jeequan.jeepay.core.model.params.jay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/** JAY / 黑猫 PAY 普通商户参数。 */
@Data
public class JayNormalMchParams extends NormalMchParams {

    public static final String ENVIRONMENT_TEST = "TEST";
    public static final String ENVIRONMENT_PRODUCTION = "PRODUCTION";

    /** Provider 环境；必须明确选择 TEST 或 PRODUCTION。 */
    private String environment;

    /** Token username、Collect cust_id 与 APN api_id 共用的契客代号。 */
    private String custId;

    /** Token password。 */
    private String apiPassword;

    @Override
    public String deSenData() {
        JSONObject result = (JSONObject) JSON.toJSON(this);
        if (StringUtils.isNotBlank(apiPassword)) {
            result.put("apiPassword", "********");
        }
        return result.toJSONString();
    }
}
