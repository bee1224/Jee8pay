package com.jeequan.jeepay.service.wallet;

import com.jeequan.jeepay.core.entity.PayOrderFee;
import com.jeequan.jeepay.core.entity.WalletAccount;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 一筆訂單的結算拆帳（ADR-0010），純計算、不接觸 DB。
 *
 * 訂單金額全數分配到各帳戶，合計恆等於訂單金額：
 *   商戶 = 金額 - 商戶手續費
 *   團長 = 團長費、直屬隊長 = 隊長費
 *   上游渠道 = 渠道費（渠道成本，記在 CHANNEL:ifCode 帳戶以便對帳）
 *   平台 = 商戶手續費 - 渠道費 - 團長費 - 隊長費（含平臺費與未分配的差額；四層超過商戶手續費時為負，由平台吸收並已在快照標記）
 * 同一帳戶出現多次時合併為一筆，避免同一訂單對同一帳戶重複記帳。
 */
public final class SettlementSplit {

    private SettlementSplit() {
    }

    public static final class Share {
        private final String ownerType;
        private final String ownerId;
        private final long amount;

        Share(String ownerType, String ownerId, long amount) {
            this.ownerType = ownerType;
            this.ownerId = ownerId;
            this.amount = amount;
        }

        public String getOwnerType() { return ownerType; }
        public String getOwnerId() { return ownerId; }
        public long getAmount() { return amount; }
    }

    public static List<Share> of(PayOrderFee fee, String ifCode) {
        long amount = nz(fee.getAmount());
        long mchFee = nz(fee.getMchFeeAmount());
        long channel = nz(fee.getChannelFee());
        long sr = StringUtils.isBlank(fee.getSrAgentNo()) ? 0 : nz(fee.getSrAgentFee());
        long ag = StringUtils.isBlank(fee.getAgentNo()) ? 0 : nz(fee.getAgentFee());

        Map<String, long[]> merged = new LinkedHashMap<>();
        add(merged, WalletAccount.OWNER_MCH, fee.getMchNo(), amount - mchFee);
        add(merged, WalletAccount.OWNER_AGENT, fee.getSrAgentNo(), sr);
        add(merged, WalletAccount.OWNER_AGENT, fee.getAgentNo(), ag);
        add(merged, WalletAccount.OWNER_CHANNEL, StringUtils.defaultIfBlank(ifCode, "UNKNOWN"), channel);
        add(merged, WalletAccount.OWNER_PLATFORM, WalletAccount.PLATFORM_ID, mchFee - channel - sr - ag);

        List<Share> shares = new ArrayList<>();
        for (Map.Entry<String, long[]> e : merged.entrySet()) {
            if (e.getValue()[0] != 0) {
                String[] key = e.getKey().split("\u0000", 2);
                shares.add(new Share(key[0], key[1], e.getValue()[0]));
            }
        }
        return shares;
    }

    private static void add(Map<String, long[]> merged, String ownerType, String ownerId, long amount) {
        if (StringUtils.isBlank(ownerId) || amount == 0) {
            return;
        }
        merged.computeIfAbsent(ownerType + "\u0000" + ownerId, k -> new long[1])[0] += amount;
    }

    private static long nz(Long v) {
        return Objects.requireNonNullElse(v, 0L);
    }
}
