package com.jeequan.jeepay.pay.channel.chi;

import com.jeequan.jeepay.core.model.params.chi.ChiNormalMchParams;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** CHI allowlisted log-value sanitizer. Raw Provider payloads must never be passed here for logging. */
public final class ChiLogSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final int MAX_LENGTH = 256;
    private static final Pattern CREDENTIAL_KEY = Pattern.compile(
            "(?i)authorization|api[_-]?password|password|app[_-]?secret|client[_-]?secret|secret|"
                    + "access[_-]?token|refresh[_-]?token|id[_-]?token|token|cust[_-]?id");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bbearer\\s+");
    private static final Pattern CONTROL = Pattern.compile("[\\r\\n\\t\\p{Cntrl}]");

    private ChiLogSanitizer() {
    }

    public static String sanitize(String value, ChiNormalMchParams params) {
        try {
            String sanitized = StringUtils.defaultString(value);
            sanitized = redactAssignments(sanitized);
            sanitized = redactBearerTokens(sanitized);
            if (params != null) {
                sanitized = replaceKnownValue(sanitized, params.getCustId());
                sanitized = replaceKnownValue(sanitized, params.getApiPassword());
            }
            sanitized = CONTROL.matcher(sanitized).replaceAll(" ");
            return sanitized.length() <= MAX_LENGTH ? sanitized : sanitized.substring(0, MAX_LENGTH);
        } catch (RuntimeException ignored) {
            return REDACTED;
        }
    }

    private static String replaceKnownValue(String value, String secret) {
        return StringUtils.isBlank(secret) ? value : value.replace(secret, REDACTED);
    }

    private static String redactAssignments(String value) {
        StringBuilder result = new StringBuilder(value.length());
        Matcher matcher = CREDENTIAL_KEY.matcher(value);
        int cursor = 0;
        int searchFrom = 0;
        while (matcher.find(searchFrom)) {
            if (!isKeyBoundary(value, matcher.start(), matcher.end())) {
                searchFrom = matcher.end();
                continue;
            }

            int separator = skipKeyQuoteAndWhitespace(value, matcher.end());
            if (separator >= value.length() || (value.charAt(separator) != ':' && value.charAt(separator) != '=')) {
                searchFrom = matcher.end();
                continue;
            }

            int valueStart = skipWhitespace(value, separator + 1);
            Quote quote = quoteAt(value, valueStart);
            if (quote != Quote.NONE) {
                valueStart += quote.length;
            }
            int valueEnd = quote == Quote.NONE
                    ? unquotedValueEnd(value, valueStart, matcher.group())
                    : quotedValueEnd(value, valueStart, quote);

            result.append(value, cursor, valueStart).append(REDACTED);
            cursor = valueEnd;
            searchFrom = valueEnd;
        }
        return result.append(value, cursor, value.length()).toString();
    }

    private static boolean isKeyBoundary(String value, int start, int end) {
        boolean left = start == 0 || !isKeyCharacter(value.charAt(start - 1));
        boolean right = end == value.length() || !isKeyCharacter(value.charAt(end));
        return left && right;
    }

    private static boolean isKeyCharacter(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-';
    }

    private static int skipKeyQuoteAndWhitespace(String value, int index) {
        if (startsWith(value, index, "\\\"")) {
            index += 2;
        } else if (index < value.length() && value.charAt(index) == '"') {
            index++;
        }
        return skipWhitespace(value, index);
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static Quote quoteAt(String value, int index) {
        if (startsWith(value, index, "\\\"")) {
            return Quote.ESCAPED;
        }
        return index < value.length() && value.charAt(index) == '"' ? Quote.PLAIN : Quote.NONE;
    }

    private static int quotedValueEnd(String value, int start, Quote quote) {
        for (int index = start; index < value.length(); index++) {
            if (quote == Quote.ESCAPED && startsWith(value, index, "\\\"")) {
                return index;
            }
            if (quote == Quote.PLAIN && value.charAt(index) == '"' && !isEscaped(value, index)) {
                return index;
            }
        }
        return value.length();
    }

    private static boolean isEscaped(String value, int index) {
        int slashes = 0;
        for (int cursor = index - 1; cursor >= 0 && value.charAt(cursor) == '\\'; cursor--) {
            slashes++;
        }
        return slashes % 2 != 0;
    }

    private static int unquotedValueEnd(String value, int start, String key) {
        int cursor = start;
        if ("authorization".equalsIgnoreCase(key) && regionMatches(value, cursor, "Bearer")) {
            cursor += "Bearer".length();
            cursor = skipWhitespace(value, cursor);
        }
        while (cursor < value.length() && !isUnquotedDelimiter(value.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static boolean isUnquotedDelimiter(char ch) {
        return Character.isWhitespace(ch) || ch == ',' || ch == ';' || ch == '}' || ch == ']'
                || ch == '"' || ch == '\\';
    }

    private static String redactBearerTokens(String value) {
        StringBuilder result = new StringBuilder(value.length());
        Matcher matcher = BEARER.matcher(value);
        int cursor = 0;
        while (matcher.find(cursor)) {
            int tokenEnd = matcher.end();
            while (tokenEnd < value.length() && !isUnquotedDelimiter(value.charAt(tokenEnd))) {
                tokenEnd++;
            }
            result.append(value, cursor, matcher.end()).append(REDACTED);
            cursor = tokenEnd;
        }
        return result.append(value, cursor, value.length()).toString();
    }

    private static boolean startsWith(String value, int index, String expected) {
        return index >= 0 && index + expected.length() <= value.length()
                && value.startsWith(expected, index);
    }

    private static boolean regionMatches(String value, int index, String expected) {
        return index >= 0 && index + expected.length() <= value.length()
                && value.regionMatches(true, index, expected, 0, expected.length());
    }

    private enum Quote {
        NONE(0), PLAIN(1), ESCAPED(2);

        private final int length;

        Quote(int length) {
            this.length = length;
        }
    }
}
