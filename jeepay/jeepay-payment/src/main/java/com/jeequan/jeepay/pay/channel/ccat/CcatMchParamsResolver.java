package com.jeequan.jeepay.pay.channel.ccat;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.ccat.CcatNormalMchParams;
import com.jeequan.jeepay.pay.channel.ccat.CcatClient.CcatException;
import com.jeequan.jeepay.pay.channel.ccat.CcatClient.ErrorType;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Resolves CCAT params through JeePay's cache-aware native configuration service。 */
@Component
public class CcatMchParamsResolver {

    private final ConfigContextQueryService configContextQueryService;

    public CcatMchParamsResolver(ConfigContextQueryService configContextQueryService) {
        this.configContextQueryService = configContextQueryService;
    }

    public CcatNormalMchParams resolve(MchAppConfigContext context) throws CcatException {
        if (context == null || StringUtils.isAnyBlank(context.getMchNo(), context.getAppId())) {
            throw configurationError("CCAT merchant binding is missing");
        }

        final NormalMchParams nativeParams;
        try {
            nativeParams = configContextQueryService.queryNormalMchParams(
                    context.getMchNo(), context.getAppId(), CS.IF_CODE.CCAT);
        } catch (RuntimeException e) {
            throw new CcatException(ErrorType.CONFIGURATION, "CCAT merchant configuration is malformed", e);
        }
        if (!(nativeParams instanceof CcatNormalMchParams)) {
            throw configurationError("CCAT merchant configuration is missing");
        }

        CcatNormalMchParams params = (CcatNormalMchParams) nativeParams;
        if (StringUtils.isAnyBlank(params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw configurationError("CCAT merchant configuration is incomplete");
        }
        CcatClient.resolveBaseUrl(params.getEnvironment());
        return params;
    }

    private static CcatException configurationError(String message) {
        return new CcatException(ErrorType.CONFIGURATION, message);
    }
}
