import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 商戶 API 串接範例（ML-DSA-65，ADR-0013），只用 JDK 24 以上的標準函式庫，不需要任何套件。
 *
 * 用法（金鑰請先用 deploy/jee8pay-v2-dev/scripts/GenMlDsaKeys.java 產生，或任何支援 FIPS 204 的工具）：
 *
 *   export UAT_BASE_URL=https://api-v2-dev.nnviopp.com
 *   export UAT_MERCHANT_ID=<商戶號>
 *   export UAT_APP_ID=<應用 AppId>
 *   export UAT_PRIVATE_KEY_FILE=<商戶私鑰 PEM 路徑>
 *   export UAT_PLATFORM_PUBLIC_KEY_FILE=<後台複製的平台公鑰 PEM 路徑>
 *   export UAT_NOTIFY_URL=https://<你的接收端>/callback
 *
 *   java MerchantApiExample.java create [RYO_IBON|JAY_IBON|CHI_IBON|JHD_IBON] [金額，預設 4000 = TWD40]
 *   java MerchantApiExample.java query <mchOrderNo>
 *   java MerchantApiExample.java print-create [wayCode]       只印出已簽名的 JSON，可貼到 Talend 等工具（5 分鐘內有效）
 *   java MerchantApiExample.java verify-notify <通知內容檔>    檔案內容為平台 POST 過來的 form body（a=1&b=2...）
 *
 * 安全：本程式不會印出私鑰；私鑰只應存在商戶自己的系統。
 */
public class MerchantApiExample {

    static final String SIGN_TYPE = "ML-DSA-65";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("用法：create | query <mchOrderNo> | print-create | verify-notify <檔案>");
            System.exit(2);
        }
        switch (args[0]) {
            case "create" -> send("/api/pay/unifiedOrder", createRequest(arg(args, 1, "RYO_IBON"), Long.parseLong(arg(args, 2, "4000"))));
            case "print-create" -> System.out.println(toJson(signed(createRequest(arg(args, 1, "RYO_IBON"), 4000L))));
            case "query" -> send("/api/pay/query", queryRequest(args[1]));
            case "verify-notify" -> verifyNotify(Files.readString(Path.of(args[1])).trim());
            default -> {
                System.err.println("未知指令：" + args[0]);
                System.exit(2);
            }
        }
    }

    // ---------------- 簽名規則（與平台相同） ----------------

    /** 除 sign 外、值不為 null 或空字串的參數，依參數名稱排序，組成 k1=v1&k2=v2，UTF-8 **/
    static String canonical(Map<String, ?> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (!"sign".equals(k) && v != null && !v.toString().isEmpty()) {
                sorted.put(k, v.toString());
            }
        });
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> sb.append(sb.length() == 0 ? "" : "&").append(k).append('=').append(v));
        return sb.toString();
    }

    static String sign(Map<String, ?> params, PrivateKey key) throws Exception {
        Signature s = Signature.getInstance("ML-DSA-65");
        s.initSign(key);
        s.update(canonical(params).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    static boolean verify(Map<String, ?> params, String sign, PublicKey key) throws Exception {
        Signature s = Signature.getInstance("ML-DSA-65");
        s.initVerify(key);
        s.update(canonical(params).getBytes(StandardCharsets.UTF_8));
        return s.verify(Base64.getDecoder().decode(sign));
    }

    // ---------------- 請求 ----------------

    static Map<String, Object> createRequest(String wayCode, long amount) {
        Map<String, Object> p = base();
        p.put("mchOrderNo", "UAT-" + System.currentTimeMillis());
        p.put("wayCode", wayCode);
        p.put("amount", amount);
        p.put("currency", "TWD");
        p.put("subject", "ibon UAT 測試");
        p.put("body", "ML-DSA-65 串接測試");
        p.put("notifyUrl", env("UAT_NOTIFY_URL"));
        p.put("channelExtra", "{\"payerName\":\"王小明\",\"payerPostcode\":\"100\",\"payerAddress\":\"台北市測試路1號\","
                + "\"payerMobile\":\"0900000000\",\"payerEmail\":\"uat@example.test\"}");
        return p;
    }

    static Map<String, Object> queryRequest(String mchOrderNo) {
        Map<String, Object> p = base();
        p.put("mchOrderNo", mchOrderNo);
        return p;
    }

    static Map<String, Object> base() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("version", "1.0");
        p.put("signType", SIGN_TYPE);
        p.put("reqTime", String.valueOf(System.currentTimeMillis()));
        p.put("mchNo", env("UAT_MERCHANT_ID"));
        p.put("appId", env("UAT_APP_ID"));
        return p;
    }

    static Map<String, Object> signed(Map<String, Object> p) throws Exception {
        p.put("sign", sign(p, privateKey(env("UAT_PRIVATE_KEY_FILE"))));
        return p;
    }

    static void send(String path, Map<String, Object> params) throws Exception {
        String body = toJson(signed(params));
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(env("UAT_BASE_URL") + path))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP " + res.statusCode());
        System.out.println(res.body());

        // 回應的 sign 只簽 data；以平台公鑰驗證（數字以原文字參與待簽字串）
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) new Json(res.body()).value();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) envelope.get("data");
        String sign = (String) envelope.get("sign");
        if (data != null && sign != null) {
            boolean ok = verify(data, sign, publicKey(env("UAT_PLATFORM_PUBLIC_KEY_FILE")));
            System.out.println("RESPONSE_SIGNATURE=" + (ok ? "VALID" : "INVALID"));
            if (!ok) {
                System.exit(1);
            }
        }
    }

    // ---------------- 通知 ----------------

    static void verifyNotify(String formBody) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : formBody.split("&")) {
            int i = pair.indexOf('=');
            params.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        boolean ok = SIGN_TYPE.equals(params.get("signType"))
                && verify(params, params.get("sign"), publicKey(env("UAT_PLATFORM_PUBLIC_KEY_FILE")));
        // 驗簽通過後還要比對金額、商戶號與自己的訂單，並確認 reqTime 在合理範圍內；處理完回應純文字 SUCCESS
        System.out.println("NOTIFY_SIGNATURE=" + (ok ? "VALID" : "INVALID"));
        System.exit(ok ? 0 : 1);
    }

    // ---------------- 金鑰 ----------------

    static PrivateKey privateKey(String file) throws Exception {
        return KeyFactory.getInstance("ML-DSA-65").generatePrivate(new PKCS8EncodedKeySpec(der(Files.readString(Path.of(file)))));
    }

    static PublicKey publicKey(String file) throws Exception {
        return KeyFactory.getInstance("ML-DSA-65").generatePublic(new X509EncodedKeySpec(der(Files.readString(Path.of(file)))));
    }

    static byte[] der(String pem) {
        return Base64.getDecoder().decode(pem.replaceAll("-----(BEGIN|END)[A-Z ]*-----", "").replaceAll("\\s", ""));
    }

    // ---------------- 極簡 JSON（範例不依賴套件；正式系統請用 JSON 套件） ----------------

    static String toJson(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder("{");
        m.forEach((k, v) -> {
            if (sb.length() > 1) sb.append(',');
            sb.append('"').append(k).append("\":");
            sb.append(v instanceof Number ? v.toString() : "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
        });
        return sb.append('}').toString();
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

    static String arg(String[] a, int i, String def) {
        return a.length > i ? a[i] : def;
    }

    static String env(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            System.err.println("MISSING_ENV=" + name);
            System.exit(2);
        }
        return v;
    }
}
