package com.jeequan.jeepay.service.impl;

import com.jeequan.jeepay.service.utils.UpperCasePasswordEncoder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPortalServicePasswordTest {

    @Test
    void randomPasswordIsEightCharsWithLetterDigitAndSymbol() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String pwd = AgentPortalService.randomPassword();
            assertEquals(8, pwd.length());
            assertTrue(pwd.matches("^[A-HJ-NP-Z2-9!@#$%&*?]{8}$"), pwd);
            assertTrue(pwd.matches(".*[A-Z].*"), pwd);
            assertTrue(pwd.matches(".*[2-9].*"), pwd);
            assertTrue(pwd.matches(".*[!@#$%&*?].*"), pwd);
            seen.add(pwd);
        }
        assertTrue(seen.size() > 490, "隨機密碼不應大量重複");
    }

    @Test
    void randomPasswordSurvivesTheCaseInsensitiveEncoder() {
        String pwd = AgentPortalService.randomPassword();
        UpperCasePasswordEncoder encoder = new UpperCasePasswordEncoder();
        String hash = encoder.encode(pwd);
        assertTrue(encoder.matches(pwd, hash));
        assertTrue(encoder.matches(pwd.toLowerCase(), hash));
    }
}
