package com.jeequan.jeepay.pay.channel.ccat;

import com.jeequan.jeepay.core.model.params.ccat.CcatNormalMchParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CcatLogSanitizerTest {

    private static final String SECRET = "REAL_SECRET";

    @Test
    void redactsJsonQuotedToken() {
        assertSecretAbsent("{\"Token\":\"" + SECRET + "\"}");
        assertSecretAbsent("{\"Token\": \"" + SECRET + "\"}");
    }

    @Test
    void redactsJsonQuotedAuthorizationBearerToken() {
        assertSecretAbsent("{\"Authorization\":\"Bearer " + SECRET + "\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\\\"Token\\\":\\\"REAL_SECRET\\\"}",
            "{\\\"Authorization\\\":\\\"Bearer REAL_SECRET\\\"}"
    })
    void redactsEscapedNestedJsonSecrets(String value) {
        assertSecretAbsent(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Token=REAL_SECRET",
            "Authorization=Bearer REAL_SECRET",
            "Authorization: Bearer REAL_SECRET",
            "Bearer REAL_SECRET"
    })
    void redactsAssignmentHeaderAndBearerSecrets(String value) {
        assertSecretAbsent(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"token\":\"REAL_SECRET\"}",
            "{\"TOKEN\":\"REAL_SECRET\"}",
            "{\"authorization\":\"Bearer REAL_SECRET\"}",
            "apiPassword=REAL_SECRET",
            "PASSWORD=REAL_SECRET",
            "App-Secret: REAL_SECRET",
            "client_secret=REAL_SECRET",
            "secret=REAL_SECRET"
    })
    void redactsCaseVariantsAndPasswordOrSecretCredentials(String value) {
        assertSecretAbsent(value);
    }

    @Test
    void preservesSafeProviderMessage() {
        String message = "Payment instruction is pending; query again later";

        String sanitized = CcatLogSanitizer.sanitize(message, null);

        assertEquals(message, sanitized);
        assertTrue(sanitized.contains("pending"));
    }

    @Test
    void sanitizerFailureDoesNotFallBackToRawValue() {
        CcatNormalMchParams params = mock(CcatNormalMchParams.class);
        when(params.getCustId()).thenThrow(new IllegalStateException("synthetic sanitizer failure"));

        String sanitized = CcatLogSanitizer.sanitize("safe-prefix " + SECRET, params);

        assertEquals("[REDACTED]", sanitized);
        assertFalse(sanitized.contains(SECRET));
    }

    private static void assertSecretAbsent(String value) {
        String sanitized = CcatLogSanitizer.sanitize(value, null);
        assertFalse(sanitized.contains(SECRET), sanitized);
        assertTrue(sanitized.contains("[REDACTED]"), sanitized);
    }
}
