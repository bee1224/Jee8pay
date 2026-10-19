package com.jeequan.jeepay.core.utils;

import com.jeequan.jeepay.core.exception.BizException;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 商戶 API 簽名（ADR-0013）：ML-DSA-65（FIPS 204，pure 模式，空 context），使用 JDK 內建實作。
 *
 * 待簽字串：除 sign 外、值不為 null 或空字串的參數，依參數名稱的字元順序由小到大排序，
 * 組成 k1=v1&k2=v2（無結尾 &），UTF-8 編碼；sign 為標準 Base64。
 */
public final class MlDsaKit {

    /** 商戶簽名（商戶私鑰簽、平台以商戶公鑰驗），也是平台回應與通知的 signType **/
    public static final String SIGN_TYPE = "ML-DSA-65";

    /** 平台後台代商戶呼叫支付服務（平台內部金鑰） **/
    public static final String SIGN_TYPE_PLATFORM = "PLATFORM-ML-DSA-65";

    private static final String ALGORITHM = "ML-DSA-65";

    private MlDsaKit() {}

    /** 組待簽字串 **/
    public static String canonical(Map<String, ?> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, ?> e : params.entrySet()) {
            if ("sign".equals(e.getKey()) || e.getValue() == null) {
                continue;
            }
            String v = e.getValue().toString();
            if (!v.isEmpty()) {
                sorted.put(e.getKey(), v);
            }
        }
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> sb.append(sb.length() == 0 ? "" : "&").append(k).append('=').append(v));
        return sb.toString();
    }

    public static String sign(Map<String, ?> params, PrivateKey privateKey) {
        try {
            Signature s = Signature.getInstance(ALGORITHM);
            s.initSign(privateKey);
            s.update(canonical(params).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(s.sign());
        } catch (Exception e) {
            throw new IllegalStateException("ML-DSA 簽名失敗", e);
        }
    }

    /** 驗簽；sign 格式錯誤、金鑰不符或簽名不符一律回 false **/
    public static boolean verify(Map<String, ?> params, String sign, PublicKey publicKey) {
        if (sign == null || sign.isEmpty() || publicKey == null) {
            return false;
        }
        try {
            Signature s = Signature.getInstance(ALGORITHM);
            s.initVerify(publicKey);
            s.update(canonical(params).getBytes(StandardCharsets.UTF_8));
            return s.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    /** 解析公鑰（PEM 或純 Base64 的 X.509 SubjectPublicKeyInfo）；只接受 ML-DSA-65 **/
    public static PublicKey parsePublicKey(String pemOrBase64) {
        try {
            return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(der(pemOrBase64)));
        } catch (Exception e) {
            throw new BizException("公鑰格式錯誤，必須是 ML-DSA-65 公鑰（PEM 或 Base64）");
        }
    }

    /** 解析私鑰（PEM 或純 Base64 的 PKCS#8）；只接受 ML-DSA-65 **/
    public static PrivateKey parsePrivateKey(String pemOrBase64) {
        try {
            return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(der(pemOrBase64)));
        } catch (Exception e) {
            throw new IllegalStateException("私鑰格式錯誤，必須是 ML-DSA-65 PKCS#8 私鑰");
        }
    }

    /** 將公鑰整理成標準 PEM（儲存與顯示用） **/
    public static String toPem(PublicKey publicKey) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----\n";
    }

    private static byte[] der(String pemOrBase64) {
        String body = pemOrBase64.replaceAll("-----(BEGIN|END)[A-Z ]*-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
