package com.jeequan.jeepay.pay.channel.chi;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.chi.ChiNormalMchParams;
import com.jeequan.jeepay.pay.channel.chi.payway.ChiIbon;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ChiArchitectureTest {

    @Test
    void nativeBeanAndPaywayReflectionNamesMatchContract() {
        assertEquals("chiPaymentService", Introspector.decapitalize(ChiPaymentService.class.getSimpleName()));
        assertEquals("chiPayOrderQueryService",
                Introspector.decapitalize(ChiPayOrderQueryService.class.getSimpleName()));
        assertEquals("chiChannelNoticeService",
                Introspector.decapitalize(ChiChannelNoticeService.class.getSimpleName()));
        assertEquals("ChiIbon", ChiIbon.class.getSimpleName());
        assertEquals("chi", CS.IF_CODE.CHI);
        assertEquals("CHI_IBON", CS.PAY_WAY_CODE.CHI_IBON);
    }

    @Test
    void normalMchParamsFactoryLoadsChiConfigurationAndMasksPassword() {
        NormalMchParams loaded = NormalMchParams.factory("chi",
                "{\"environment\":\"TEST\",\"custId\":\"test-user\","
                        + "\"apiPassword\":\"test-api-password\"}");

        assertInstanceOf(ChiNormalMchParams.class, loaded);
        assertFalse(loaded.deSenData().contains("test-api-password"));
        assertTrue(loaded.deSenData().contains("********"));
    }

    @Test
    void channelAdapterDoesNotOwnMerchantNotifyOrCoreStateMachine() {
        assertTrue(Arrays.stream(ChiChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayMchNotifyService.class));
        assertTrue(Arrays.stream(ChiChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayOrderProcessService.class));
    }
}
