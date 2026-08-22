package com.jeequan.jeepay.pay.channel.ryo;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.ryo.RyoNormalMchParams;
import com.jeequan.jeepay.pay.channel.ryo.payway.RyoIbon;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RyoArchitectureTest {

    @Test
    void nativeBeanAndPaywayReflectionNamesMatchContract() {
        assertEquals("ryoPaymentService", Introspector.decapitalize(RyoPaymentService.class.getSimpleName()));
        assertEquals("ryoPayOrderQueryService",
                Introspector.decapitalize(RyoPayOrderQueryService.class.getSimpleName()));
        assertEquals("ryoChannelNoticeService",
                Introspector.decapitalize(RyoChannelNoticeService.class.getSimpleName()));
        assertEquals("RyoIbon", RyoIbon.class.getSimpleName());
        assertEquals("ryo", CS.IF_CODE.RYO);
        assertEquals("RYO_IBON", CS.PAY_WAY_CODE.RYO_IBON);
    }

    @Test
    void normalMchParamsFactoryLoadsRyoConfigurationAndMasksPassword() {
        NormalMchParams loaded = NormalMchParams.factory("ryo",
                "{\"environment\":\"TEST\",\"custId\":\"test-user\","
                        + "\"apiPassword\":\"test-api-password\"}");

        assertInstanceOf(RyoNormalMchParams.class, loaded);
        assertFalse(loaded.deSenData().contains("test-api-password"));
        assertTrue(loaded.deSenData().contains("********"));
    }

    @Test
    void channelAdapterDoesNotOwnMerchantNotifyOrCoreStateMachine() {
        assertTrue(Arrays.stream(RyoChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayMchNotifyService.class));
        assertTrue(Arrays.stream(RyoChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayOrderProcessService.class));
    }
}
