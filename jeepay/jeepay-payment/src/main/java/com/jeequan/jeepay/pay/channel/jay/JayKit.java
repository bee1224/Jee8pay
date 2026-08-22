package com.jeequan.jeepay.pay.channel.jay;

import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;

/** JAY boundary 的金額、狀態与 checksum 纯函数。 */
public final class JayKit {

    private static final Pattern WHOLE_TWD = Pattern.compile("[0-9]+");
    private static final Pattern CHECKSUM = Pattern.compile("[0-9a-fA-F]{32}");
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private JayKit() {
    }

    public static long toJayTwdAmount(long jeepayAmount) {
        if (jeepayAmount <= 0) {
            throw new IllegalArgumentException("金額必須大於 0");
        }
        if (jeepayAmount % 100 != 0) {
            throw new IllegalArgumentException("金額必須為整數 TWD 元");
        }
        return jeepayAmount / 100;
    }

    public static long parseWholeTwd(Object value, String field, boolean allowZero) {
        if (value == null) {
            throw new IllegalArgumentException(field + " 為必填");
        }
        String raw = String.valueOf(value);
        if (!WHOLE_TWD.matcher(raw).matches()) {
            throw new IllegalArgumentException(field + " 必須為零位小數整數");
        }
        BigInteger parsed = new BigInteger(raw);
        if (parsed.compareTo(LONG_MAX) > 0 || (!allowZero && parsed.signum() == 0)) {
            throw new IllegalArgumentException(field + " 超出範圍");
        }
        return parsed.longValueExact();
    }

    public static long wholeTwdToMinorUnits(Object value, String field, boolean allowZero) {
        return Math.multiplyExact(parseWholeTwd(value, field, allowZero), 100L);
    }

    public static String checksum(String apiId, String transId, String amount, String status, String nonce) {
        String canonical = apiId + ':' + transId + ':' + amount + ':' + status + ':' + nonce;
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(32);
            for (byte item : digest) {
                result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is unavailable", e);
        }
    }

    public static boolean verifyChecksum(String apiId, String transId, String amount, String status,
                                         String nonce, String suppliedChecksum) {
        if (suppliedChecksum == null || !CHECKSUM.matcher(suppliedChecksum).matches()) {
            return false;
        }
        byte[] expected = checksum(apiId, transId, amount, status, nonce).getBytes(StandardCharsets.US_ASCII);
        byte[] supplied = suppliedChecksum.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, supplied);
    }

    public static ChannelRetMsg.ChannelState mapProcessCode(String processCode) {
        if ("0".equals(processCode) || "1".equals(processCode) || "3".equals(processCode)) {
            return ChannelRetMsg.ChannelState.WAITING;
        }
        if ("4".equals(processCode) || "7".equals(processCode) || "8".equals(processCode)) {
            return ChannelRetMsg.ChannelState.CONFIRM_SUCCESS;
        }
        if ("5".equals(processCode) || "6".equals(processCode)) {
            return ChannelRetMsg.ChannelState.CONFIRM_FAIL;
        }
        return ChannelRetMsg.ChannelState.UNKNOWN;
    }
}
