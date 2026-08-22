package com.jeequan.jeepay.pay.channel.jay;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.jay.JayNormalMchParams;
import com.jeequan.jeepay.pay.channel.jay.payway.JayIbon;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class JayArchitectureTest {

    @Test
    void nativeBeanAndPaywayReflectionNamesMatchContract() {
        assertEquals("jayPaymentService", Introspector.decapitalize(JayPaymentService.class.getSimpleName()));
        assertEquals("jayPayOrderQueryService",
                Introspector.decapitalize(JayPayOrderQueryService.class.getSimpleName()));
        assertEquals("jayChannelNoticeService",
                Introspector.decapitalize(JayChannelNoticeService.class.getSimpleName()));
        assertEquals("JayIbon", JayIbon.class.getSimpleName());
        assertEquals("jay", CS.IF_CODE.JAY);
        assertEquals("JAY_IBON", CS.PAY_WAY_CODE.JAY_IBON);
    }

    @Test
    void normalMchParamsFactoryLoadsJayConfigurationAndMasksPassword() {
        NormalMchParams loaded = NormalMchParams.factory("jay",
                "{\"environment\":\"TEST\",\"custId\":\"test-user\","
                        + "\"apiPassword\":\"test-api-password\"}");

        assertInstanceOf(JayNormalMchParams.class, loaded);
        assertFalse(loaded.deSenData().contains("test-api-password"));
        assertTrue(loaded.deSenData().contains("********"));
    }

    @Test
    void channelAdapterDoesNotOwnMerchantNotifyOrCoreStateMachine() {
        assertTrue(Arrays.stream(JayChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayMchNotifyService.class));
        assertTrue(Arrays.stream(JayChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayOrderProcessService.class));
    }
}
