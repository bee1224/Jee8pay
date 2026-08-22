package com.jeequan.jeepay.pay.channel.jay;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.PayInterfaceConfig;
import com.jeequan.jeepay.core.model.params.jay.JayNormalMchParams;
import com.jeequan.jeepay.pay.channel.jay.JayClient.JayException;
import com.jeequan.jeepay.pay.model.MchAppConfigContext;
import com.jeequan.jeepay.pay.service.ConfigContextQueryService;
import com.jeequan.jeepay.pay.service.ConfigContextService;
import com.jeequan.jeepay.service.impl.PayInterfaceConfigService;
import com.jeequan.jeepay.service.impl.SysConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class JayMchParamsResolverTest {

    private ConfigContextService cacheService;
    private PayInterfaceConfigService dbService;
    private ConfigContextQueryService nativeQueryService;
    private JayMchParamsResolver resolver;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new Configuration(), "jay-test"), PayInterfaceConfig.class);
        cacheService = mock(ConfigContextService.class);
        dbService = mock(PayInterfaceConfigService.class);
        nativeQueryService = new ConfigContextQueryService();
        ReflectionTestUtils.setField(nativeQueryService, "configContextService", cacheService);
        ReflectionTestUtils.setField(nativeQueryService, "payInterfaceConfigService", dbService);
        resolver = new JayMchParamsResolver(nativeQueryService);
    }

    @AfterEach
    void restoreCacheMode() {
        SysConfigService.IS_USE_CACHE = false;
    }

    @Test
    void cacheEnabledLoadsJayParamsFromNativeContext() throws Exception {
        SysConfigService.IS_USE_CACHE = true;
        MchAppConfigContext binding = binding("M-TEST", "APP-TEST");
        MchAppConfigContext cached = binding("M-TEST", "APP-TEST");
        cached.getNormalMchParamsMap().put(CS.IF_CODE.JAY, validParams());
        when(cacheService.getMchAppConfigContext("M-TEST", "APP-TEST")).thenReturn(cached);

        assertEquals("TEST", resolver.resolve(binding).getEnvironment());
        verifyNoInteractions(dbService);
    }

    @Test
    void cacheDisabledLoadsJayParamsFromCanonicalDatabaseRow() throws Exception {
        SysConfigService.IS_USE_CACHE = false;
        when(dbService.getOne(any())).thenReturn(configRow(validJson()));

        JayNormalMchParams loaded = resolver.resolve(binding("M-TEST", "APP-TEST"));

        assertEquals("TEST", loaded.getEnvironment());
        assertEquals("test-user", loaded.getCustId());
        assertEquals("test-api-password", loaded.getApiPassword());
        verifyNoInteractions(cacheService);
    }

    @Test
    void missingProviderParamsFailClosed() {
        SysConfigService.IS_USE_CACHE = false;
        when(dbService.getOne(any())).thenReturn(null);

        assertThrows(JayException.class, () -> resolver.resolve(binding("M-TEST", "APP-TEST")));
    }

    @Test
    void malformedProviderParamsFailClosedWithoutCredentialOutput(CapturedOutput output) {
        SysConfigService.IS_USE_CACHE = false;
        String secret = "must-not-appear-in-output";
        when(dbService.getOne(any())).thenReturn(configRow(
                "{\"environment\":\"PRODUCTION\",\"custId\":\"test-user\","
                        + "\"apiPassword\":\"" + secret + "\",\"broken\":"));

        JayException error = assertThrows(JayException.class,
                () -> resolver.resolve(binding("M-TEST", "APP-TEST")));

        assertEquals("JAY 商戶設定格式錯誤", error.getMessage());
        assertFalse(output.getAll().contains(secret));
        assertFalse(error.getMessage().contains(secret));
    }

    @Test
    void incompleteProviderParamsFailClosed() {
        SysConfigService.IS_USE_CACHE = false;
        when(dbService.getOne(any())).thenReturn(configRow(
                "{\"environment\":\"PRODUCTION\",\"custId\":\"test-user\"}"));

        JayException error = assertThrows(JayException.class,
                () -> resolver.resolve(binding("M-TEST", "APP-TEST")));

        assertEquals("JAY 商戶設定不完整", error.getMessage());
    }

    @Test
    void wrongMerchantBindingCannotReuseAnotherAppsParams() {
        SysConfigService.IS_USE_CACHE = true;
        MchAppConfigContext expected = binding("M-EXPECTED", "APP-EXPECTED");
        expected.getNormalMchParamsMap().put(CS.IF_CODE.JAY, validParams());
        when(cacheService.getMchAppConfigContext("M-EXPECTED", "APP-EXPECTED")).thenReturn(expected);

        assertThrows(JayException.class,
                () -> resolver.resolve(binding("M-OTHER", "APP-OTHER")));
        verify(cacheService).getMchAppConfigContext("M-OTHER", "APP-OTHER");
    }

    private static MchAppConfigContext binding(String mchNo, String appId) {
        MchAppConfigContext context = new MchAppConfigContext();
        context.setMchNo(mchNo);
        context.setAppId(appId);
        return context;
    }

    private static PayInterfaceConfig configRow(String json) {
        return new PayInterfaceConfig()
                .setInfoType(CS.INFO_TYPE_MCH_APP)
                .setInfoId("APP-TEST")
                .setIfCode(CS.IF_CODE.JAY)
                .setIfParams(json)
                .setState(CS.YES);
    }

    private static JayNormalMchParams validParams() {
        JayNormalMchParams params = new JayNormalMchParams();
        params.setEnvironment(JayNormalMchParams.ENVIRONMENT_TEST);
        params.setCustId("test-user");
        params.setApiPassword("test-api-password");
        return params;
    }

    private static String validJson() {
        return "{\"environment\":\"TEST\",\"custId\":\"test-user\","
                + "\"apiPassword\":\"test-api-password\"}";
    }
}
