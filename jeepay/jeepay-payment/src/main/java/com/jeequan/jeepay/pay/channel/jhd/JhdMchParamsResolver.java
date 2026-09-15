package com.jeequan.jeepay.pay.channel.jhd;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.jhd.JhdNormalMchParams;
import com.jeequan.jeepay.pay.channel.jhd.JhdClient.JhdException;
import com.jeequan.jeepay.pay.channel.jhd.JhdClient.ErrorType;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Resolves JHD params through JeePay's cache-aware native configuration service。 */
@Component
public class JhdMchParamsResolver {

    private final ConfigContextQueryService configContextQueryService;

    public JhdMchParamsResolver(ConfigContextQueryService configContextQueryService) {
        this.configContextQueryService = configContextQueryService;
    }

    public JhdNormalMchParams resolve(MchAppConfigContext context) throws JhdException {
        if (context == null || StringUtils.isAnyBlank(context.getMchNo(), context.getAppId())) {
            throw configurationError("JHD 商戶綁定缺失");
        }

        final NormalMchParams nativeParams;
        try {
            nativeParams = configContextQueryService.queryNormalMchParams(
                    context.getMchNo(), context.getAppId(), CS.IF_CODE.JHD);
        } catch (RuntimeException e) {
            throw new JhdException(ErrorType.CONFIGURATION, "JHD 商戶設定格式錯誤", e);
        }
        if (!(nativeParams instanceof JhdNormalMchParams)) {
            throw configurationError("JHD 商戶設定缺失");
        }

        JhdNormalMchParams params = (JhdNormalMchParams) nativeParams;
        if (StringUtils.isAnyBlank(params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw configurationError("JHD 商戶設定不完整");
        }
        JhdClient.resolveBaseUrl(params.getEnvironment());
        return params;
    }

    private static JhdException configurationError(String message) {
        return new JhdException(ErrorType.CONFIGURATION, message);
    }
}
