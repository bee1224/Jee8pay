package com.jeequan.jeepay.pay.channel.jhd;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.jhd.JhdNormalMchParams;
import com.jeequan.jeepay.pay.channel.jhd.payway.JhdIbon;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class JhdArchitectureTest {

    @Test
    void nativeBeanAndPaywayReflectionNamesMatchContract() {
        assertEquals("jhdPaymentService", Introspector.decapitalize(JhdPaymentService.class.getSimpleName()));
        assertEquals("jhdPayOrderQueryService",
                Introspector.decapitalize(JhdPayOrderQueryService.class.getSimpleName()));
        assertEquals("jhdChannelNoticeService",
                Introspector.decapitalize(JhdChannelNoticeService.class.getSimpleName()));
        assertEquals("JhdIbon", JhdIbon.class.getSimpleName());
        assertEquals("jhd", CS.IF_CODE.JHD);
        assertEquals("JHD_IBON", CS.PAY_WAY_CODE.JHD_IBON);
    }

    @Test
    void normalMchParamsFactoryLoadsJhdConfigurationAndMasksPassword() {
        NormalMchParams loaded = NormalMchParams.factory("jhd",
                "{\"environment\":\"TEST\",\"custId\":\"test-user\","
                        + "\"apiPassword\":\"test-api-password\"}");

        assertInstanceOf(JhdNormalMchParams.class, loaded);
        assertFalse(loaded.deSenData().contains("test-api-password"));
        assertTrue(loaded.deSenData().contains("********"));
    }

    @Test
    void channelAdapterDoesNotOwnMerchantNotifyOrCoreStateMachine() {
        assertTrue(Arrays.stream(JhdChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayMchNotifyService.class));
        assertTrue(Arrays.stream(JhdChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayOrderProcessService.class));
    }
}
