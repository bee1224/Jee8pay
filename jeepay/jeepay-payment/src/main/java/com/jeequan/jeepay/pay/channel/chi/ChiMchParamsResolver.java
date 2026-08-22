package com.jeequan.jeepay.pay.channel.chi;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.chi.ChiNormalMchParams;
import com.jeequan.jeepay.pay.channel.chi.ChiClient.ChiException;
import com.jeequan.jeepay.pay.channel.chi.ChiClient.ErrorType;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Resolves CHI params through JeePay's cache-aware native configuration service。 */
@Component
public class ChiMchParamsResolver {

    private final ConfigContextQueryService configContextQueryService;

    public ChiMchParamsResolver(ConfigContextQueryService configContextQueryService) {
        this.configContextQueryService = configContextQueryService;
    }

    public ChiNormalMchParams resolve(MchAppConfigContext context) throws ChiException {
        if (context == null || StringUtils.isAnyBlank(context.getMchNo(), context.getAppId())) {
            throw configurationError("CHI 商戶綁定缺失");
        }

        final NormalMchParams nativeParams;
        try {
            nativeParams = configContextQueryService.queryNormalMchParams(
                    context.getMchNo(), context.getAppId(), CS.IF_CODE.CHI);
        } catch (RuntimeException e) {
            throw new ChiException(ErrorType.CONFIGURATION, "CHI 商戶設定格式錯誤", e);
        }
        if (!(nativeParams instanceof ChiNormalMchParams)) {
            throw configurationError("CHI 商戶設定缺失");
        }

        ChiNormalMchParams params = (ChiNormalMchParams) nativeParams;
        if (StringUtils.isAnyBlank(params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw configurationError("CHI 商戶設定不完整");
        }
        ChiClient.resolveBaseUrl(params.getEnvironment());
        return params;
    }

    private static ChiException configurationError(String message) {
        return new ChiException(ErrorType.CONFIGURATION, message);
    }
}
