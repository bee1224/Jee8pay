package com.jeequan.jeepay.pay.channel.ccat;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.params.NormalMchParams;
import com.jeequan.jeepay.core.model.params.ccat.CcatNormalMchParams;
import com.jeequan.jeepay.pay.channel.ccat.payway.CcatIbon;
import com.jeequan.jeepay.pay.service.PayMchNotifyService;
import com.jeequan.jeepay.pay.service.PayOrderProcessService;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CcatArchitectureTest {

    @Test
    void nativeBeanAndPaywayReflectionNamesMatchContract() {
        assertEquals("ccatPaymentService", Introspector.decapitalize(CcatPaymentService.class.getSimpleName()));
        assertEquals("ccatPayOrderQueryService",
                Introspector.decapitalize(CcatPayOrderQueryService.class.getSimpleName()));
        assertEquals("ccatChannelNoticeService",
                Introspector.decapitalize(CcatChannelNoticeService.class.getSimpleName()));
        assertEquals("CcatIbon", CcatIbon.class.getSimpleName());
        assertEquals("ccat", CS.IF_CODE.CCAT);
        assertEquals("CCAT_IBON", CS.PAY_WAY_CODE.CCAT_IBON);
    }

    @Test
    void normalMchParamsFactoryLoadsCcatConfigurationAndMasksPassword() {
        NormalMchParams loaded = NormalMchParams.factory("ccat",
                "{\"environment\":\"TEST\",\"custId\":\"test-user\","
                        + "\"apiPassword\":\"test-api-password\"}");

        assertInstanceOf(CcatNormalMchParams.class, loaded);
        assertFalse(loaded.deSenData().contains("test-api-password"));
        assertTrue(loaded.deSenData().contains("********"));
    }

    @Test
    void channelAdapterDoesNotOwnMerchantNotifyOrCoreStateMachine() {
        assertTrue(Arrays.stream(CcatChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayMchNotifyService.class));
        assertTrue(Arrays.stream(CcatChannelNoticeService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == PayOrderProcessService.class));
    }
}
