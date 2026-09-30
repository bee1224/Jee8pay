package com.jeequan.jeepay.service.route;

import com.jeequan.jeepay.core.entity.WayRoute;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 通道路由的純選擇邏輯（ADR-0011），不接觸 DB，便於測試。
 * 順序：商戶專屬規則優先（有任何專屬規則就不看通用規則）→ 硬條件篩選（啟用、金額、時段、商戶已開通該通道）→ 依權重加權隨機。
 */
public final class WayRouter {

    private WayRouter() {
    }

    public static final class Decision {
        private final String chosen;
        private final List<WayRoute> candidates;

        Decision(String chosen, List<WayRoute> candidates) {
            this.chosen = chosen;
            this.candidates = candidates;
        }

        /** 選中的實際代碼；沒有任何候選時為 null。 */
        public String getChosen() { return chosen; }
        public List<WayRoute> getCandidates() { return candidates; }
    }

    /**
     * @param rules            該別名的所有規則
     * @param passageAvailable 商戶應用是否已開通某實際代碼
     * @param random           給定上界 n，回傳 [0, n) 的隨機整數（測試可注入固定值）
     */
    public static Decision select(List<WayRoute> rules, String mchNo, long amount, LocalTime now,
                                  Predicate<String> passageAvailable, IntUnaryOperator random) {
        List<WayRoute> enabled = rules.stream().filter(r -> r.getState() != null && r.getState() == 1).collect(Collectors.toList());
        List<WayRoute> scoped = enabled.stream().filter(r -> mchNo.equals(r.getMchNo())).collect(Collectors.toList());
        if (scoped.isEmpty()) {
            scoped = enabled.stream().filter(r -> StringUtils.isEmpty(r.getMchNo())).collect(Collectors.toList());
        }
        List<WayRoute> candidates = new ArrayList<>();
        for (WayRoute r : scoped) {
            if (inAmount(r, amount) && inTime(r, now) && passageAvailable.test(r.getTargetWayCode())) {
                candidates.add(r);
            }
        }
        if (candidates.isEmpty()) {
            return new Decision(null, candidates);
        }
        int total = candidates.stream().mapToInt(WayRouter::weight).sum();
        int pick = random.applyAsInt(total);
        for (WayRoute r : candidates) {
            pick -= weight(r);
            if (pick < 0) {
                return new Decision(r.getTargetWayCode(), candidates);
            }
        }
        return new Decision(candidates.get(candidates.size() - 1).getTargetWayCode(), candidates);
    }

    static int weight(WayRoute r) {
        return r.getWeight() == null ? 1 : Math.max(1, Math.min(9, r.getWeight()));
    }

    static boolean inAmount(WayRoute r, long amount) {
        long min = r.getMinAmount() == null ? 0 : r.getMinAmount();
        long max = r.getMaxAmount() == null ? 0 : r.getMaxAmount();
        return amount >= min && (max == 0 || amount <= max);
    }

    /** 時段 [起, 迄)；迄早於起表示跨午夜（例如 22:00–06:00）。任一為空表示全天。 */
    static boolean inTime(WayRoute r, LocalTime now) {
        if (StringUtils.isAnyBlank(r.getTimeStart(), r.getTimeEnd())) {
            return true;
        }
        LocalTime start = LocalTime.parse(r.getTimeStart());
        LocalTime end = LocalTime.parse(r.getTimeEnd());
        if (start.equals(end)) {
            return true;
        }
        return start.isBefore(end) ? !now.isBefore(start) && now.isBefore(end) : !now.isBefore(start) || now.isBefore(end);
    }
}
