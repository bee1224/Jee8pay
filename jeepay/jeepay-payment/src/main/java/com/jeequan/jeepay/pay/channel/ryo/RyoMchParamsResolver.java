package com.jeequan.jeepay.pay.channel.ryo;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.ryo.RyoNormalMchParams;
import com.jeequan.jeepay.pay.channel.ryo.RyoClient.RyoException;
import com.jeequan.jeepay.pay.channel.ryo.RyoClient.ErrorType;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Resolves RYO params through JeePay's cache-aware native configuration service。 */
@Component
public class RyoMchParamsResolver {

    private final ConfigContextQueryService configContextQueryService;

    public RyoMchParamsResolver(ConfigContextQueryService configContextQueryService) {
        this.configContextQueryService = configContextQueryService;
    }

    public RyoNormalMchParams resolve(MchAppConfigContext context) throws RyoException {
        if (context == null || StringUtils.isAnyBlank(context.getMchNo(), context.getAppId())) {
            throw configurationError("RYO 商戶綁定缺失");
        }

        final NormalMchParams nativeParams;
        try {
            nativeParams = configContextQueryService.queryNormalMchParams(
                    context.getMchNo(), context.getAppId(), CS.IF_CODE.RYO);
        } catch (RuntimeException e) {
            throw new RyoException(ErrorType.CONFIGURATION, "RYO 商戶設定格式錯誤", e);
        }
        if (!(nativeParams instanceof RyoNormalMchParams)) {
            throw configurationError("RYO 商戶設定缺失");
        }

        RyoNormalMchParams params = (RyoNormalMchParams) nativeParams;
        if (StringUtils.isAnyBlank(params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw configurationError("RYO 商戶設定不完整");
        }
        RyoClient.resolveBaseUrl(params.getEnvironment());
        return params;
    }

    private static RyoException configurationError(String message) {
        return new RyoException(ErrorType.CONFIGURATION, message);
    }
}
