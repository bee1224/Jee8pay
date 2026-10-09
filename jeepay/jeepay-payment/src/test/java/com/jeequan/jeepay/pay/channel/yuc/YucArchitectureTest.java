package com.jeequan.jeepay.pay.channel.yuc;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.yuc.YucNormalMchParams;
import com.jeequan.jeepay.pay.channel.yuc.payway.YucIbon;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class YucArchitectureTest {

    @Test
    void nativeBeanAndPaywayReflectionNamesMatchContract() {
        assertEquals("yucPaymentService", Introspector.decapitalize(YucPaymentService.class.getSimpleName()));
        assertEquals("yucPayOrderQueryService",
                Introspector.decapitalize(YucPayOrderQueryService.class.getSimpleName()));
        assertEquals("yucChannelNoticeService",
                Introspector.decapitalize(YucChannelNoticeService.class.getSimpleName()));
        assertEquals("YucIbon", YucIbon.class.getSimpleName());
        assertEquals("yuc", CS.IF_CODE.YUC);
        assertEquals("YUC_IBON", CS.PAY_WAY_CODE.YUC_IBON);
    }

    @Test
    void normalMchParamsFactoryLoadsYucConfigurationAndMasksPassword() {
        NormalMchParams loaded = NormalMchParams.factory("yuc",
                "{\"environment\":\"TEST\",\"custId\":\"test-user\","
                        + "\"apiPassword\":\"test-api-password\"}");

        assertInstanceOf(YucNormalMchParams.class, loaded);
        assertFalse(loaded.deSenData().contains("test-api-password"));
        assertTrue(loaded.deSenData().contains("********"));
    }

    @Test
    void channelAdapterDoesNotOwnMerchantNotifyOrCoreStateMachine() {
        assertTrue(Arrays.stream(YucChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayMchNotifyService.class));
        assertTrue(Arrays.stream(YucChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayOrderProcessService.class));
    }
}
