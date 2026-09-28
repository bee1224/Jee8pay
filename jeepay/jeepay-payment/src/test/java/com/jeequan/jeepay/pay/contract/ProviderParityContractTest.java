package com.jeequan.jeepay.pay.contract;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 黑貓 PAY 四個上游（RYO/JAY/CHI/JHD）依 ADR-0002 刻意各自一份 adapter，而非共用實作；
 * 本測試守住「將上游代號正規化後逐字元相同」，避免只改其中一家而其他三家悄悄漂移。
 * 若差異是有意為之，須先更新 docs/providers 說明並調整本測試，而不是繞過。
 */
class ProviderParityContractTest {

    private static final String REFERENCE = "ryo";
    private static final int FILES_PER_PROVIDER = 19;
    private static final List<String> ROOTS = List.of(
            "jeepay-payment/src/main/java/com/jeequan/jeepay/pay/channel/%s",
            "jeepay-payment/src/test/java/com/jeequan/jeepay/pay/channel/%s",
            "jeepay-core/src/main/java/com/jeequan/jeepay/core/model/params/%s");

    @ParameterizedTest
    @ValueSource(strings = {"jay", "chi", "jhd"})
    void providerAdapterMatchesReferenceAfterNormalization(String provider) throws IOException {
        Map<String, String> reference = normalizedSources(REFERENCE);
        Map<String, String> candidate = normalizedSources(provider);

        assertEquals(FILES_PER_PROVIDER, reference.size(), "reference provider file count");
        assertEquals(reference.keySet(), candidate.keySet(), provider + " must have the same files as " + REFERENCE);

        for (Map.Entry<String, String> entry : reference.entrySet()) {
            String expected = entry.getValue();
            String actual = candidate.get(entry.getKey());
            if (!expected.equals(actual)) {
                fail(provider + " drifted from " + REFERENCE + " in " + entry.getKey() + ": " + firstDifference(expected, actual));
            }
        }
    }

    private static Map<String, String> normalizedSources(String provider) throws IOException {
        Path jeepayRoot = findJeepayRoot();
        Map<String, String> sources = new TreeMap<>();
        for (String root : ROOTS) {
            Path directory = jeepayRoot.resolve(String.format(root, provider));
            if (!Files.isDirectory(directory)) {
                continue;
            }
            List<Path> files;
            try (Stream<Path> walk = Files.walk(directory)) {
                files = walk.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
            }
            for (Path file : files) {
                String key = String.format(root, "*") + "/" + normalize(directory.relativize(file).toString().replace('\\', '/'), provider);
                sources.put(key, normalize(Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n"), provider));
            }
        }
        return sources;
    }

    /** 只替換「前後不是同類字母」的代號，避免誤傷 architecture 之類內含 chi 的單字。 */
    static String normalize(String text, String provider) {
        String lower = provider.toLowerCase();
        String upper = provider.toUpperCase();
        String capital = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        return text
                .replaceAll("(?<![A-Z])" + upper + "(?![A-Z])", "@PROVIDER@")
                .replaceAll(capital + "(?![a-z])", "@Provider@")
                .replaceAll("(?<![a-z])" + lower + "(?![a-z])", "@provider@");
    }

    private static String firstDifference(String expected, String actual) {
        String[] left = expected.split("\n", -1);
        String[] right = actual.split("\n", -1);
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            if (!left[i].equals(right[i])) {
                return "line " + (i + 1) + " expected <" + left[i].trim() + "> but was <" + right[i].trim() + ">";
            }
        }
        return "line count " + left.length + " vs " + right.length;
    }

    private static Path findJeepayRoot() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            if (Files.isDirectory(directory.resolve("jeepay-payment")) && Files.isDirectory(directory.resolve("jeepay-core"))) {
                return directory;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("jeepay root (containing jeepay-payment and jeepay-core) not found");
    }
}
