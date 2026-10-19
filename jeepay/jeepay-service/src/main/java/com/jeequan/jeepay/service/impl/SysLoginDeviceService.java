package com.jeequan.jeepay.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.SysLoginDevice;
import com.jeequan.jeepay.service.mapper.SysLoginDeviceMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Set;

/** 登入裝置紀錄：資料由用戶端提供，一律截斷長度、檢查範圍後才存。 */
@Service
public class SysLoginDeviceService extends ServiceImpl<SysLoginDeviceMapper, SysLoginDevice> {

    private static final Set<String> GEO_STATES = Set.of("GRANTED", "DENIED", "UNAVAILABLE", "TIMEOUT", "UNSUPPORTED");

    /** 同一帳號 1 分鐘內只記一筆，避免重複回報灌資料。回傳是否有新增。 */
    public boolean record(SysLoginDevice d) {
        Date oneMinuteAgo = new Date(System.currentTimeMillis() - 60_000);
        if (count(SysLoginDevice.gw().eq(SysLoginDevice::getSysUserId, d.getSysUserId()).ge(SysLoginDevice::getCreatedAt, oneMinuteAgo)) > 0) {
            return false;
        }
        d.setUserAgent(StringUtils.left(d.getUserAgent(), 512));
        d.setDeviceModel(StringUtils.left(StringUtils.trimToNull(d.getDeviceModel()), 128));
        d.setDeviceOs(StringUtils.left(StringUtils.trimToNull(d.getDeviceOs()), 64));
        d.setDeviceId(d.getDeviceId() != null && d.getDeviceId().matches("[A-Za-z0-9-]{8,64}") ? d.getDeviceId() : null);
        if (!GEO_STATES.contains(d.getGeoState())) {
            d.setGeoState("UNSUPPORTED");
        }
        if (!SysLoginDevice.GEO_GRANTED.equals(d.getGeoState()) || !inRange(d.getLatitude(), 90) || !inRange(d.getLongitude(), 180)) {
            d.setLatitude(null).setLongitude(null).setAccuracy(null);
        } else {
            d.setLatitude(d.getLatitude().setScale(7, RoundingMode.HALF_UP)).setLongitude(d.getLongitude().setScale(7, RoundingMode.HALF_UP));
            if (d.getAccuracy() != null && (d.getAccuracy() < 0 || d.getAccuracy() > 1_000_000)) {
                d.setAccuracy(null);
            }
        }
        d.setCreatedAt(new Date());
        return save(d);
    }

    public List<SysLoginDevice> recentByUser(Long sysUserId, int limit) {
        return list(SysLoginDevice.gw().eq(SysLoginDevice::getSysUserId, sysUserId).orderByDesc(SysLoginDevice::getId).last("LIMIT " + limit));
    }

    public List<SysLoginDevice> recentByAgent(String agentNo, int limit) {
        return list(SysLoginDevice.gw().eq(SysLoginDevice::getSysType, "MGR").eq(SysLoginDevice::getBelongInfoId, agentNo)
                .orderByDesc(SysLoginDevice::getId).last("LIMIT " + limit));
    }

    private static boolean inRange(BigDecimal v, int max) {
        return v != null && v.abs().compareTo(BigDecimal.valueOf(max)) <= 0;
    }
}
