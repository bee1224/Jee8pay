package com.jeequan.jeepay.service.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Locale;

/**
 * 密碼不分大小寫的 BCrypt encoder：一律以 toUpperCase(Locale.ROOT) 正規化後再 encode / matches。
 *
 * 既有 credential 必須是「大寫密碼」的 bcrypt hash（新增／重置／登入驗證皆走同一正規化）。
 * 僅影響登入密碼；Merchant API 的 App Secret（MD5 簽名）不受影響、維持大小寫敏感。
 */
public class UpperCasePasswordEncoder extends BCryptPasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return super.encode(normalize(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return super.matches(normalize(rawPassword), encodedPassword);
    }

    private static CharSequence normalize(CharSequence rawPassword) {
        return rawPassword == null ? null : rawPassword.toString().toUpperCase(Locale.ROOT);
    }
}
