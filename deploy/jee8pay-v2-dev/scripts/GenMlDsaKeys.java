import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * 產生 ML-DSA-65 金鑰對（ADR-0013），需要 JDK 24 以上。
 *
 * 用法：java GenMlDsaKeys.java <輸出目錄> <檔名前綴>
 * 產出：<前綴>-private-key.pem（PKCS#8，權限 600）與 <前綴>-public-key.pem（X.509）。
 * 私鑰檔已存在時拒絕覆寫，避免誤換掉正在使用的金鑰。
 *
 * 伺服器上沒有 JDK 時，用容器執行（私鑰不離開伺服器）：
 *   docker run --rm -v "$PWD:/w" -v <secrets 目錄>:/out eclipse-temurin:25-jdk-noble \
 *     java /w/GenMlDsaKeys.java /out sign-platform
 */
public class GenMlDsaKeys {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("用法：java GenMlDsaKeys.java <輸出目錄> <檔名前綴>");
            System.exit(2);
        }
        Path dir = Path.of(args[0]);
        Path priv = dir.resolve(args[1] + "-private-key.pem");
        Path pub = dir.resolve(args[1] + "-public-key.pem");
        if (Files.exists(priv)) {
            System.err.println("私鑰已存在，不覆寫：" + priv);
            System.exit(1);
        }

        KeyPair kp = KeyPairGenerator.getInstance("ML-DSA-65").generateKeyPair();

        Files.writeString(priv, pem("PRIVATE KEY", kp.getPrivate().getEncoded()), StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW);
        try {
            Files.setPosixFilePermissions(priv, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // Windows 等非 POSIX 檔案系統：請自行限制私鑰檔權限
        }
        Files.writeString(pub, pem("PUBLIC KEY", kp.getPublic().getEncoded()), StandardCharsets.US_ASCII);
        System.out.println("已產生 " + priv + " 與 " + pub);
    }

    private static String pem(String type, byte[] der) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }
}
