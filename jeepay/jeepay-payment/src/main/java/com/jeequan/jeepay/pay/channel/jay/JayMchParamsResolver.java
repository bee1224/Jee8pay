package com.jeequan.jeepay.pay.channel.jay;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.jay.JayNormalMchParams;
import com.jeequan.jeepay.pay.channel.jay.JayClient.JayException;
import com.jeequan.jeepay.pay.channel.jay.JayClient.ErrorType;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Resolves JAY params through JeePay's cache-aware native configuration service。 */
@Component
public class JayMchParamsResolver {

    private final ConfigContextQueryService configContextQueryService;

    public JayMchParamsResolver(ConfigContextQueryService configContextQueryService) {
        this.configContextQueryService = configContextQueryService;
    }

    public JayNormalMchParams resolve(MchAppConfigContext context) throws JayException {
        if (context == null || StringUtils.isAnyBlank(context.getMchNo(), context.getAppId())) {
            throw configurationError("JAY 商戶綁定缺失");
        }

        final NormalMchParams nativeParams;
        try {
            nativeParams = configContextQueryService.queryNormalMchParams(
                    context.getMchNo(), context.getAppId(), CS.IF_CODE.JAY);
        } catch (RuntimeException e) {
            throw new JayException(ErrorType.CONFIGURATION, "JAY 商戶設定格式錯誤", e);
        }
        if (!(nativeParams instanceof JayNormalMchParams)) {
            throw configurationError("JAY 商戶設定缺失");
        }

        JayNormalMchParams params = (JayNormalMchParams) nativeParams;
        if (StringUtils.isAnyBlank(params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw configurationError("JAY 商戶設定不完整");
        }
        JayClient.resolveBaseUrl(params.getEnvironment());
        return params;
    }

    private static JayException configurationError(String message) {
        return new JayException(ErrorType.CONFIGURATION, message);
    }
}
