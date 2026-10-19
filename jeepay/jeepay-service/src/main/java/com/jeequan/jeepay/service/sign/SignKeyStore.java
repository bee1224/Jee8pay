package com.jeequan.jeepay.service.sign;

import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.utils.MlDsaKit;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * 平台 ML-DSA-65 金鑰（ADR-0013）。
 *
 * 兩組金鑰分開：
 * - 平台簽名金鑰：簽平台給商戶的同步回應與非同步通知，商戶以平台公鑰驗證。
 * - 平台內部金鑰：後台（manager、merchant 服務）代商戶呼叫支付服務時使用，支付服務以內部公鑰驗證。
 *   與平台簽名金鑰分開，商戶手上的已簽通知就無法被拿來當成內部請求重放。
 *
 * 金鑰內容由部署端以 Docker secrets 掛進 /run/secrets/（configtree），檔名即屬性名；不進 Git、不進資料庫。
 * 各服務只掛自己需要的那幾把，沒掛的在用到時才報錯。
 */
@Component
public class SignKeyStore {

    @Value("${isys.sign.platform-private-key:}")
    private String platformPrivateKeyPem;

    @Value("${isys.sign.platform-public-key:}")
    private String platformPublicKeyPem;

    @Value("${isys.sign.internal-private-key:}")
    private String internalPrivateKeyPem;

    @Value("${isys.sign.internal-public-key:}")
    private String internalPublicKeyPem;

    private volatile PrivateKey platformPrivateKey;
    private volatile PublicKey platformPublicKey;
    private volatile PrivateKey internalPrivateKey;
    private volatile PublicKey internalPublicKey;

    public PrivateKey platformPrivateKey() {
        if (platformPrivateKey == null) {
            platformPrivateKey = MlDsaKit.parsePrivateKey(required(platformPrivateKeyPem, "平台簽名私鑰"));
        }
        return platformPrivateKey;
    }

    public PublicKey platformPublicKey() {
        if (platformPublicKey == null) {
            platformPublicKey = MlDsaKit.parsePublicKey(required(platformPublicKeyPem, "平台簽名公鑰"));
        }
        return platformPublicKey;
    }

    /** 給商戶下載、貼到自己系統的平台公鑰（PEM） **/
    public String platformPublicKeyPem() {
        return MlDsaKit.toPem(platformPublicKey());
    }

    public PrivateKey internalPrivateKey() {
        if (internalPrivateKey == null) {
            internalPrivateKey = MlDsaKit.parsePrivateKey(required(internalPrivateKeyPem, "平台內部私鑰"));
        }
        return internalPrivateKey;
    }

    public PublicKey internalPublicKey() {
        if (internalPublicKey == null) {
            internalPublicKey = MlDsaKit.parsePublicKey(required(internalPublicKeyPem, "平台內部公鑰"));
        }
        return internalPublicKey;
    }

    private static String required(String pem, String name) {
        if (StringUtils.isBlank(pem)) {
            throw new BizException("系統尚未設定" + name + "，請聯絡平台");
        }
        return pem;
    }
}
