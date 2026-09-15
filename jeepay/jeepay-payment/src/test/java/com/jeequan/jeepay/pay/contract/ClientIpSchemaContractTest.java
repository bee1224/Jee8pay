package com.jeequan.jeepay.pay.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientIpSchemaContractTest {

    private static final int IPV6_TEXT_MAX_LENGTH = 45;
    private static final Pattern CLIENT_IP_COLUMN = Pattern.compile(
            "`client_ip`\\s+VARCHAR\\((\\d+)\\)", Pattern.CASE_INSENSITIVE);

    @Test
    void allOrderClientIpColumnsCanStoreFullIpv6Text() throws IOException {
        String schema = Files.readString(findSchema());
        Matcher matcher = CLIENT_IP_COLUMN.matcher(schema);
        List<Integer> lengths = new ArrayList<>();
        while (matcher.find()) {
            lengths.add(Integer.parseInt(matcher.group(1)));
        }

        assertEquals(3, lengths.size(), "payment/refund/transfer client_ip columns");
        assertTrue(lengths.stream().allMatch(length -> length >= IPV6_TEXT_MAX_LENGTH),
                "all client_ip columns must store a full IPv6 address: " + lengths);
    }

    private static Path findSchema() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            Path schema = directory.resolve("docs/sql/init.sql");
            if (Files.isRegularFile(schema)) {
                return schema;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("docs/sql/init.sql not found");
    }
}
