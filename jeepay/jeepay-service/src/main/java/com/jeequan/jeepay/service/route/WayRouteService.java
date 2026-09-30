package com.jeequan.jeepay.service.route;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.PayWay;
import com.jeequan.jeepay.core.entity.WayRoute;
import com.jeequan.jeepay.core.entity.WayRouteLog;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.impl.MchPayPassageService;
import com.jeequan.jeepay.service.impl.PayWayService;
import com.jeequan.jeepay.service.mapper.WayRouteLogMapper;
import com.jeequan.jeepay.service.mapper.WayRouteMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 通道路由（ADR-0011）：別名代碼 → 實際支付方式代碼。
 * 不是別名（沒有任何規則）時回傳 null，下單流程照舊，對既有以實際代碼下單的商戶零影響。
 */
@Slf4j
@Service
public class WayRouteService extends ServiceImpl<WayRouteMapper, WayRoute> {

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    @Autowired private WayRouteLogMapper wayRouteLogMapper;
    @Autowired private MchPayPassageService mchPayPassageService;
    @Autowired private PayWayService payWayService;

    public String resolve(String mchNo, String appId, String mchOrderNo, String wayCode, Long amount) {
        if (StringUtils.isBlank(wayCode)) {
            return null;
        }
        List<WayRoute> rules = list(WayRoute.gw().eq(WayRoute::getAliasWayCode, wayCode));
        if (rules.isEmpty()) {
            return null;
        }
        WayRouter.Decision d = WayRouter.select(rules, StringUtils.defaultString(mchNo), amount == null ? 0 : amount,
                LocalTime.now(TAIPEI), target -> mchPayPassageService.findMchPayPassage(mchNo, appId, target) != null,
                bound -> ThreadLocalRandom.current().nextInt(bound));
        JSONArray cands = new JSONArray();
        d.getCandidates().forEach(r -> {
            JSONObject c = new JSONObject(true);
            c.put("wayCode", r.getTargetWayCode());
            c.put("weight", WayRouter.weight(r));
            c.put("routeId", r.getRouteId());
            cands.add(c);
        });
        try {
            wayRouteLogMapper.insert(new WayRouteLog().setMchNo(mchNo).setAppId(appId).setMchOrderNo(mchOrderNo)
                    .setAliasWayCode(wayCode).setAmount(amount).setChosenWayCode(d.getChosen()).setCandidates(cands.toJSONString()));
        } catch (Exception e) {
            // 決策紀錄失敗不影響下單
            log.error("路由決策紀錄寫入失敗 mchNo={} mchOrderNo={}", mchNo, mchOrderNo, e);
        }
        if (d.getChosen() == null) {
            throw new BizException("目前沒有可用的支付通道");
        }
        return d.getChosen();
    }

    public WayRoute saveRoute(WayRoute input, String operator) {
        String alias = StringUtils.trimToEmpty(input.getAliasWayCode()).toUpperCase();
        String target = StringUtils.trimToEmpty(input.getTargetWayCode());
        if (!alias.matches("[A-Z0-9_]{2,20}")) {
            throw new BizException("別名代碼限 2～20 碼大寫英數或底線");
        }
        if (payWayService.count(PayWay.gw().eq(PayWay::getWayCode, alias)) > 0) {
            throw new BizException("別名不可與既有支付方式代碼相同");
        }
        if (payWayService.count(PayWay.gw().eq(PayWay::getWayCode, target)) == 0) {
            throw new BizException("實際支付方式不存在");
        }
        long min = input.getMinAmount() == null ? 0 : input.getMinAmount();
        long max = input.getMaxAmount() == null ? 0 : input.getMaxAmount();
        if (min < 0 || max < 0 || (max > 0 && max < min)) {
            throw new BizException("金額區間不正確");
        }
        int weight = input.getWeight() == null ? 1 : input.getWeight();
        if (weight < 1 || weight > 9) {
            throw new BizException("權重須為 1～9");
        }
        String ts = StringUtils.trimToNull(input.getTimeStart());
        String te = StringUtils.trimToNull(input.getTimeEnd());
        if ((ts == null) != (te == null) || (ts != null && !(ts.matches("([01]\\d|2[0-3]):[0-5]\\d") && te.matches("([01]\\d|2[0-3]):[0-5]\\d")))) {
            throw new BizException("時段格式為 HH:mm，起迄需同時填寫或同時留空");
        }
        WayRoute route = new WayRoute().setRouteId(input.getRouteId()).setAliasWayCode(alias).setTargetWayCode(target)
                .setMchNo(StringUtils.trimToEmpty(input.getMchNo())).setMinAmount(min).setMaxAmount(max).setWeight(weight)
                .setTimeStart(ts).setTimeEnd(te).setState(input.getState() == null ? (byte) 1 : input.getState())
                .setRemark(StringUtils.abbreviate(StringUtils.trimToNull(input.getRemark()), 128)).setUpdatedBy(operator);
        saveOrUpdate(route);
        return route;
    }
}
