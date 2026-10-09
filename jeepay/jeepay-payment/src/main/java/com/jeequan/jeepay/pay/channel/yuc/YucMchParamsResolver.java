package com.jeequan.jeepay.pay.channel.yuc;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.yuc.YucNormalMchParams;
import com.jeequan.jeepay.pay.channel.yuc.YucClient.YucException;
import com.jeequan.jeepay.pay.channel.yuc.YucClient.ErrorType;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Resolves YUC params through JeePay's cache-aware native configuration service。 */
@Component
public class YucMchParamsResolver {

    private final ConfigContextQueryService configContextQueryService;

    public YucMchParamsResolver(ConfigContextQueryService configContextQueryService) {
        this.configContextQueryService = configContextQueryService;
    }

    public YucNormalMchParams resolve(MchAppConfigContext context) throws YucException {
        if (context == null || StringUtils.isAnyBlank(context.getMchNo(), context.getAppId())) {
            throw configurationError("YUC 商戶綁定缺失");
        }

        final NormalMchParams nativeParams;
        try {
            nativeParams = configContextQueryService.queryNormalMchParams(
                    context.getMchNo(), context.getAppId(), CS.IF_CODE.YUC);
        } catch (RuntimeException e) {
            throw new YucException(ErrorType.CONFIGURATION, "YUC 商戶設定格式錯誤", e);
        }
        if (!(nativeParams instanceof YucNormalMchParams)) {
            throw configurationError("YUC 商戶設定缺失");
        }

        YucNormalMchParams params = (YucNormalMchParams) nativeParams;
        if (StringUtils.isAnyBlank(params.getEnvironment(), params.getCustId(), params.getApiPassword())) {
            throw configurationError("YUC 商戶設定不完整");
        }
        YucClient.resolveBaseUrl(params.getEnvironment());
        return params;
    }

    private static YucException configurationError(String message) {
        return new YucException(ErrorType.CONFIGURATION, message);
    }
}
