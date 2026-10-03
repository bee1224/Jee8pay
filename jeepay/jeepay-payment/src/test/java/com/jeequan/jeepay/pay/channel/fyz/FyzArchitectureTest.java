package com.jeequan.jeepay.pay.channel.fyz;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.fyz.FyzNormalMchParams;
import com.jeequan.jeepay.pay.channel.fyz.payway.FyzIbon;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FyzArchitectureTest {

    @Test
    void nativeBeanAndPaywayReflectionNamesMatchContract() {
        assertEquals("fyzPaymentService", Introspector.decapitalize(FyzPaymentService.class.getSimpleName()));
        assertEquals("fyzPayOrderQueryService",
                Introspector.decapitalize(FyzPayOrderQueryService.class.getSimpleName()));
        assertEquals("fyzChannelNoticeService",
                Introspector.decapitalize(FyzChannelNoticeService.class.getSimpleName()));
        assertEquals("FyzIbon", FyzIbon.class.getSimpleName());
        assertEquals("fyz", CS.IF_CODE.FYZ);
        assertEquals("FYZ_IBON", CS.PAY_WAY_CODE.FYZ_IBON);
    }

    @Test
    void normalMchParamsFactoryLoadsFyzConfigurationAndMasksPassword() {
        NormalMchParams loaded = NormalMchParams.factory("fyz",
                "{\"environment\":\"TEST\",\"custId\":\"test-user\","
                        + "\"apiPassword\":\"test-api-password\"}");

        assertInstanceOf(FyzNormalMchParams.class, loaded);
        assertFalse(loaded.deSenData().contains("test-api-password"));
        assertTrue(loaded.deSenData().contains("********"));
    }

    @Test
    void channelAdapterDoesNotOwnMerchantNotifyOrCoreStateMachine() {
        assertTrue(Arrays.stream(FyzChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayMchNotifyService.class));
        assertTrue(Arrays.stream(FyzChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayOrderProcessService.class));
    }
}
