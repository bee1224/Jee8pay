import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 商戶 API 黑箱回歸（ML-DSA-65，ADR-0013），取代原本 MD5 版的 run-d01-blackbox.py。需要 JDK 24 以上。
 *
 * 只送「一定會在呼叫 Provider 之前被拒絕」的請求與查單，不會建立真實 ibon 訂單。
 *
 * 環境變數：
 *   UAT_INTERNAL_BASE_URL         支付服務網址，預設 http://127.0.0.1:19216
 *   UAT_MERCHANT_ID / UAT_APP_ID  測試商戶與應用
 *   UAT_PRIVATE_KEY_FILE          該應用的商戶私鑰（PEM）
 *   UAT_PLATFORM_PUBLIC_KEY_FILE  平台簽名公鑰（PEM）
 *   UAT_WAY_CODE                  RYO_IBON（預設）/ JAY_IBON / CHI_IBON / JHD_IBON
 *   UAT_BASELINE_MCH_ORDER_NO、UAT_BASELINE_PAY_ORDER_ID
 *                                 選填：已成功（state=2、amount=4000）的既有訂單；有設定才做查單基準與重複建單檢查
 *
 * 用法：java D01Blackbox.java
 */
public class D01Blackbox {

    static final String BASE = envOr("UAT_INTERNAL_BASE_URL", "http://127.0.0.1:19216");
    static final String MCH_NO = env("UAT_MERCHANT_ID");
    static final String APP_ID = env("UAT_APP_ID");
    static final String WAY_CODE = envOr("UAT_WAY_CODE", "RYO_IBON");
    static final String PREFIX = WAY_CODE.replace("_IBON", "");
    static final String BASE_MCH_ORDER = System.getenv("UAT_BASELINE_MCH_ORDER_NO");
    static final String BASE_PAY_ORDER = System.getenv("UAT_BASELINE_PAY_ORDER_ID");

    static PrivateKey merchantKey;
    static PublicKey platformKey;
    static int passed = 0;

    public static void main(String[] args) throws Exception {
        if (!List.of("RYO_IBON", "JAY_IBON", "CHI_IBON", "JHD_IBON").contains(WAY_CODE)) {
            throw new IllegalArgumentException("UAT_WAY_CODE 必須是 RYO_IBON、JAY_IBON、CHI_IBON 或 JHD_IBON");
        }
        merchantKey = KeyFactory.getInstance("ML-DSA-65").generatePrivate(new PKCS8EncodedKeySpec(der(env("UAT_PRIVATE_KEY_FILE"))));
        platformKey = KeyFactory.getInstance("ML-DSA-65").generatePublic(new X509EncodedKeySpec(der(env("UAT_PLATFORM_PUBLIC_KEY_FILE"))));
        boolean baseline = BASE_MCH_ORDER != null && BASE_PAY_ORDER != null;

        Map<String, Object> baseData = null;
        if (baseline) {
            baseData = assertBaseline("P0_QUERY_BY_MCH_ORDER_NO", query("mchOrderNo", BASE_MCH_ORDER));
            Map<String, Object> byPayId = assertBaseline("P0_QUERY_BY_PAY_ORDER_ID", query("payOrderId", BASE_PAY_ORDER));
            check("P0_QUERY_KEYS_STABLE", stable(baseData).equals(stable(byPayId)), "");
        }

        // 簽名與時效
        Map<String, Object> md5 = auth(Map.of("mchOrderNo", "BB-MD5"));
        md5.put("signType", "MD5");
        md5.put("sign", "0123456789ABCDEF0123456789ABCDEF");
        expectFail("P0_MD5_REJECTED", post("/api/pay/query", md5), "不支援的簽名類型，signType 必須是 ML-DSA-65");

        Map<String, Object> forged = auth(Map.of("mchOrderNo", "BB-FORGED"));
        forged.put("signType", "PLATFORM-ML-DSA-65");
        sign(forged, merchantKey);
        expectFail("P0_PLATFORM_SIGNTYPE_WITH_MERCHANT_KEY", post("/api/pay/query", forged), "簽章驗證失敗");

        Map<String, Object> wrongKey = auth(Map.of("mchOrderNo", "BB-WRONG-KEY"));
        sign(wrongKey, KeyPairGenerator.getInstance("ML-DSA-65").generateKeyPair().getPrivate());
        expectFail("P0_WRONG_KEY", post("/api/pay/query", wrongKey), "簽章驗證失敗");

        Map<String, Object> stale = auth(Map.of("mchOrderNo", "BB-STALE"));
        stale.put("reqTime", "1577808000000"); // 2020-01-01，超出 5 分鐘
        sign(stale, merchantKey);
        expectFail("P0_STALE_REQTIME", post("/api/pay/query", stale), "請求時間戳已過期");

        Map<String, Object> nonNumeric = auth(Map.of("mchOrderNo", "BB-NONNUM"));
        nonNumeric.put("reqTime", "now");
        sign(nonNumeric, merchantKey);
        expectFail("P0_NON_NUMERIC_REQTIME", post("/api/pay/query", nonNumeric), "請求時間戳已過期");

        Map<String, Object> tampered = signed(auth(Map.of("mchOrderNo", "BB-TAMPER")));
        tampered.put("mchOrderNo", "BB-TAMPER-MODIFIED");
        expectFail("P0_TAMPERED_SIGNATURE", post("/api/pay/query", tampered), "簽章驗證失敗");

        expectFail("P0_MISSING_QUERY_IDENTIFIERS", post("/api/pay/query", signed(auth(Map.of()))), "mchOrderNo 和 payOrderId 不能同時為空");

        if (baseline) {
            expectFail("P0_DUPLICATE_CREATE", post("/api/pay/unifiedOrder", signed(create(BASE_MCH_ORDER, Map.of()))),
                    "商戶訂單[" + BASE_MCH_ORDER + "]已存在");
        }

        // 建單參數錯誤：必須在呼叫 Provider 前被拒絕，且不留下訂單
        String stamp = String.valueOf(System.currentTimeMillis());
        Map<String, Object> missingEmail = payer();
        missingEmail.remove("payerEmail");
        Object[][] cases = {
                {"P1_INVALID_AMOUNT", "AMOUNT", Map.of("amount", 4050), "金額必須為整數 TWD 元"},
                {"P1_INVALID_CURRENCY", "CURRENCY", Map.of("currency", "USD"), PREFIX + " ibon 僅支援 TWD"},
                {"P1_MALFORMED_CHANNEL_EXTRA", "MALFORMED", Map.of("channelExtra", "{not-json"), PREFIX + " channelExtra 格式錯誤"},
                {"P1_MISSING_PAYER_FIELD", "PAYER", Map.of("channelExtra", toJson(missingEmail)), PREFIX + " channelExtra 缺少繳款人資料"},
                {"P1_UNSUPPORTED_WAYCODE", "WAYCODE", Map.of("wayCode", "ALI_JSAPI"), "不支援的支付方式"},
                {"P1_MISSING_SUBJECT", "SUBJECT", Map.of("subject", ""), "商品標題不能為空"},
        };
        for (Object[] c : cases) {
            String mchOrderNo = "BB-" + stamp + "-" + c[1];
            @SuppressWarnings("unchecked")
            Map<String, Object> overrides = (Map<String, Object>) c[2];
            expectFail((String) c[0], post("/api/pay/unifiedOrder", signed(create(mchOrderNo, overrides))), (String) c[3]);
            expectFail(c[0] + "_NO_ORDER", query("mchOrderNo", mchOrderNo), "訂單不存在");
        }

        if (baseline) {
            Map<String, Object> last = assertBaseline("FINAL_EXISTING_ORDER_UNCHANGED", query("mchOrderNo", BASE_MCH_ORDER));
            check("FINAL_KEYS_STABLE", stable(baseData).equals(stable(last)), "");
        }
        System.out.println("D01_BLACKBOX_SUITE=PASS checks=" + passed + " wayCode=" + WAY_CODE
                + " baseline=" + (baseline ? "YES" : "NO") + " providerCreateCallsExpected=0 realPaymentTriggered=NO");
    }

    // ---------------- 請求組裝 ----------------

    static Map<String, Object> auth(Map<String, Object> fields) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("version", "1.0");
        p.put("signType", "ML-DSA-65");
        p.put("reqTime", String.valueOf(System.currentTimeMillis()));
        p.put("mchNo", MCH_NO);
        p.put("appId", APP_ID);
        p.putAll(fields);
        return p;
    }

    static Map<String, Object> payer() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("payerName", "黑箱測試");
        p.put("payerPostcode", "100");
        p.put("payerAddress", "台北市測試路1號");
        p.put("payerMobile", "0900000000");
        p.put("payerEmail", "blackbox@example.test");
        return p;
    }

    static Map<String, Object> create(String mchOrderNo, Map<String, Object> overrides) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("mchOrderNo", mchOrderNo);
        f.put("wayCode", WAY_CODE);
        f.put("amount", 4000);
        f.put("currency", "TWD");
        f.put("subject", PREFIX + " ibon black-box negative test");
        f.put("body", "Must be rejected before Provider Create");
        f.put("notifyUrl", "https://merchant.example.test/callback/jeepay");
        f.put("clientIp", "127.0.0.1");
        f.put("expiredTime", 604800);
        f.put("channelExtra", toJson(payer()));
        f.putAll(overrides);
        return auth(f);
    }

    static Map<String, Object> query(String field, String value) throws Exception {
        return post("/api/pay/query", signed(auth(Map.of(field, value))));
    }

    static Map<String, Object> signed(Map<String, Object> p) throws Exception {
        sign(p, merchantKey);
        return p;
    }

    static void sign(Map<String, Object> p, PrivateKey key) throws Exception {
        p.remove("sign");
        Signature s = Signature.getInstance("ML-DSA-65");
        s.initSign(key);
        s.update(canonical(p).getBytes(StandardCharsets.UTF_8));
        p.put("sign", Base64.getEncoder().encodeToString(s.sign()));
    }

    static String canonical(Map<String, ?> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (!"sign".equals(k) && v != null && !v.toString().isEmpty()) sorted.put(k, v.toString());
        });
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> sb.append(sb.length() == 0 ? "" : "&").append(k).append('=').append(v));
        return sb.toString();
    }

    // ---------------- 斷言 ----------------

    @SuppressWarnings("unchecked")
    static Map<String, Object> assertBaseline(String name, Map<String, Object> res) throws Exception {
        check(name, num(res.get("code")) == 0, res.toString());
        Map<String, Object> data = (Map<String, Object>) res.get("data");
        check(name + "_DATA", BASE_PAY_ORDER.equals(data.get("payOrderId")) && BASE_MCH_ORDER.equals(data.get("mchOrderNo"))
                && num(data.get("state")) == 2 && num(data.get("amount")) == 4000 && "TWD".equals(data.get("currency")), data.toString());
        Signature s = Signature.getInstance("ML-DSA-65");
        s.initVerify(platformKey);
        s.update(canonical(data).getBytes(StandardCharsets.UTF_8));
        check(name + "_RESPONSE_SIGNATURE", s.verify(Base64.getDecoder().decode((String) res.get("sign"))), "");
        return data;
    }

    static void expectFail(String name, Map<String, Object> res, String msg) {
        check(name, num(res.get("code")) == 9999 && msg.equals(res.get("msg")) && res.get("data") == null,
                "expected code=9999 msg=" + msg + " got " + res);
    }

    static void check(String name, boolean ok, String detail) {
        if (!ok) {
            System.out.println(name + "=FAIL " + detail);
            System.exit(1);
        }
        passed++;
        System.out.println(name + "=PASS");
    }

    static Map<String, Object> stable(Map<String, Object> d) {
        Map<String, Object> m = new TreeMap<>();
        for (String k : List.of("payOrderId", "mchOrderNo", "amount", "currency", "state", "createdAt")) m.put(k, d.get(k));
        return m;
    }

    static long num(Object o) {
        return o instanceof Number n ? n.longValue() : Long.MIN_VALUE;
    }

    // ---------------- HTTP 與 JSON ----------------

    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @SuppressWarnings("unchecked")
    static Map<String, Object> post(String path, Map<String, Object> body) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(BASE + path)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(body))).build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) {
            System.out.println(path + " HTTP " + r.statusCode() + " " + r.body());
            System.exit(1);
        }
        return (Map<String, Object>) new Json(r.body()).value();
    }

    static String toJson(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder("{");
        m.forEach((k, v) -> {
            if (sb.length() > 1) sb.append(',');
            sb.append(quote(k)).append(':').append(v instanceof Number ? v.toString() : quote(String.valueOf(v)));
        });
        return sb.append('}').toString();
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** 極簡 JSON 解析器：物件、陣列、字串、數字、true/false/null **/
    static final class Json {
        final String s;
        int i = 0;

        Json(String s) { this.s = s; }

        Object value() {
            ws();
            char c = s.charAt(i);
            if (c == '{') {
                Map<String, Object> m = new LinkedHashMap<>();
                i++; ws();
                if (s.charAt(i) == '}') { i++; return m; }
                while (true) {
                    ws(); String k = str(); ws(); i++; // :
                    m.put(k, value()); ws();
                    if (s.charAt(i++) == '}') return m;
                }
            }
            if (c == '[') {
                List<Object> l = new ArrayList<>();
                i++; ws();
                if (s.charAt(i) == ']') { i++; return l; }
                while (true) {
                    l.add(value()); ws();
                    if (s.charAt(i++) == ']') return l;
                }
            }
            if (c == '"') return str();
            if (s.startsWith("null", i)) { i += 4; return null; }
            if (s.startsWith("true", i)) { i += 4; return true; }
            if (s.startsWith("false", i)) { i += 5; return false; }
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(st, i);
            return n.contains(".") || n.contains("e") || n.contains("E") ? (Object) Double.parseDouble(n) : (Object) Long.parseLong(n);
        }

        String str() {
            StringBuilder sb = new StringBuilder();
            i++; // "
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(e);
                }
            }
        }

        void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }

    // ---------------- 其他 ----------------

    static byte[] der(String file) throws Exception {
        return Base64.getDecoder().decode(Files.readString(Path.of(file)).replaceAll("-----(BEGIN|END)[A-Z ]*-----", "").replaceAll("\\s", ""));
    }

    static String env(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            System.err.println("MISSING_ENV=" + name);
            System.exit(2);
        }
        return v;
    }

    static String envOr(String name, String def) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? def : v;
    }
}
