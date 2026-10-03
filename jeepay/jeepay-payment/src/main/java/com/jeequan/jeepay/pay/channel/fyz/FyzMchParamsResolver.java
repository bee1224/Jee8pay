package com.jeequan.jeepay.pay.channel.fyz;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.fyz.FyzNormalMchParams;
import com.jeequan.jeepay.pay.channel.fyz.FyzClient.FyzException;
import com.jeequan.jeepay.pay.channel.fyz.FyzClient.ErrorType;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Resolves FYZ params through JeePay's cache-aware native configuration service。 */
@Component
public class FyzMchParamsResolver {

    private final ConfigContextQueryService configContextQueryService;

    public FyzMchParamsResolver(ConfigContextQueryService configContextQueryService) {
        this.configContextQueryService = configContextQueryService;
    }

    public FyzNormalMchParams resolve(MchAppConfigContext context) throws FyzException {
        if (context == null || StringUtils.isAnyBlank(context.getMchNo(), context.getAppId())) {
            throw configurationError("FYZ 商戶綁定缺失");
        }

        final NormalMchParams nativeParams;
        try {
            nativeParams = configContextQueryService.queryNormalMchParams(
                    context.getMchNo(), context.getAppId(), CS.IF_CODE.FYZ);
        } catch (RuntimeException e) {
            throw new FyzException(ErrorType.CONFIGURATION, "FYZ 商戶設定格式錯誤", e);
        }
        if (!(nativeParams instanceof FyzNormalMchParams)) {
            throw configurationError("FYZ 商戶設定缺失");
        }

        FyzNormalMchParams params = (FyzNormalMchParams) nativeParams;
        if (StringUtils.isAnyBlank(params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw configurationError("FYZ 商戶設定不完整");
        }
        FyzClient.resolveBaseUrl(params.getEnvironment());
        return params;
    }

    private static FyzException configurationError(String message) {
        return new FyzException(ErrorType.CONFIGURATION, message);
    }
}
