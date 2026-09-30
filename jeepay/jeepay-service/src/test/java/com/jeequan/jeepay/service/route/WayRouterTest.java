package com.jeequan.jeepay.service.route;

import com.jeequan.jeepay.core.entity.WayRoute;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WayRouterTest {

    private static final LocalTime NOON = LocalTime.of(12, 0);

    @Test
    void weightedPickFollowsCumulativeWeights() {
        List<WayRoute> rules = List.of(r("RYO_IBON", "", 3), r("JAY_IBON", "", 1));
        // 總權重 4：0,1,2 → RYO；3 → JAY
        assertEquals("RYO_IBON", WayRouter.select(rules, "M1", 1000, NOON, t -> true, n -> 2).getChosen());
        assertEquals("JAY_IBON", WayRouter.select(rules, "M1", 1000, NOON, t -> true, n -> 3).getChosen());
    }

    @Test
    void merchantSpecificRulesOverrideGlobalRules() {
        List<WayRoute> rules = List.of(r("RYO_IBON", "", 9), r("CHI_IBON", "M1", 1));
        assertEquals("CHI_IBON", WayRouter.select(rules, "M1", 1000, NOON, t -> true, n -> 0).getChosen());
        assertEquals("RYO_IBON", WayRouter.select(rules, "M2", 1000, NOON, t -> true, n -> 0).getChosen());
    }

    @Test
    void hardFiltersAmountTimePassageAndState() {
        WayRoute small = r("RYO_IBON", "", 1).setMaxAmount(20_000L);
        WayRoute big = r("JAY_IBON", "", 1).setMinAmount(20_001L);
        WayRoute night = r("CHI_IBON", "", 1).setTimeStart("22:00").setTimeEnd("06:00");
        WayRoute off = r("JHD_IBON", "", 9).setState((byte) 0);
        List<WayRoute> rules = List.of(small, big, night, off);

        assertEquals("RYO_IBON", WayRouter.select(rules, "M1", 20_000, NOON, t -> true, n -> 0).getChosen());
        assertEquals("JAY_IBON", WayRouter.select(rules, "M1", 30_000, NOON, t -> true, n -> 0).getChosen());
        // 23:00 跨午夜時段內：金額 20000 時候選為 RYO 與 CHI
        assertEquals(Set.of("RYO_IBON", "CHI_IBON"), Set.copyOf(WayRouter.select(rules, "M1", 20_000, LocalTime.of(23, 0), t -> true, n -> 0)
                .getCandidates().stream().map(WayRoute::getTargetWayCode).toList()));
        // 商戶沒開通 RYO：小額沒有候選
        assertNull(WayRouter.select(rules, "M1", 20_000, NOON, t -> !t.equals("RYO_IBON"), n -> 0).getChosen());
    }

    private static WayRoute r(String target, String mchNo, int weight) {
        return new WayRoute().setAliasWayCode("IBON").setTargetWayCode(target).setMchNo(mchNo).setWeight(weight)
                .setMinAmount(0L).setMaxAmount(0L).setState((byte) 1);
    }
}
