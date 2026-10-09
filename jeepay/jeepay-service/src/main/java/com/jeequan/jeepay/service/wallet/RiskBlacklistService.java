package com.jeequan.jeepay.service.wallet;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.RiskBlacklist;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.mapper.RiskBlacklistMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 風控黑名單（ADR-0010）：GLOBAL 全平台，或以團長為範圍，由該代理轄下共用。 */
@Service
public class RiskBlacklistService extends ServiceImpl<RiskBlacklistMapper, RiskBlacklist> {

    public RiskBlacklist add(RiskBlacklist input, Long uid, String name) {
        String type = input.getListType();
        if (!RiskBlacklist.TYPE_BANK_ACCOUNT.equals(type) && !RiskBlacklist.TYPE_ACCOUNT_NAME.equals(type)
                && !RiskBlacklist.TYPE_PHONE.equals(type)) {
            throw new BizException("不支援的黑名單類型");
        }
        String value = normalize(type, input.getListValue());
        if (value.isEmpty()) {
            throw new BizException("請填寫黑名單內容");
        }
        RiskBlacklist row = new RiskBlacklist().setListType(type).setListValue(value)
                .setScope(StringUtils.defaultIfBlank(input.getScope(), RiskBlacklist.SCOPE_GLOBAL))
                .setRemark(StringUtils.abbreviate(StringUtils.trimToNull(input.getRemark()), 128))
                .setCreatedUid(uid).setCreatedBy(name);
        try {
            save(row);
        } catch (DuplicateKeyException e) {
            throw new BizException("此黑名單已存在");
        }
        return row;
    }

    /** 回傳命中的類型描述；未命中回傳 null。 */
    public String match(List<String> scopes, String accountNo, String accountName) {
        List<String> effective = new ArrayList<>(scopes == null ? List.of() : scopes);
        if (!effective.contains(RiskBlacklist.SCOPE_GLOBAL)) {
            effective.add(RiskBlacklist.SCOPE_GLOBAL);
        }
        String no = normalize(RiskBlacklist.TYPE_BANK_ACCOUNT, accountNo);
        String name = normalize(RiskBlacklist.TYPE_ACCOUNT_NAME, accountName);
        if (!no.isEmpty() && count(RiskBlacklist.gw().eq(RiskBlacklist::getListType, RiskBlacklist.TYPE_BANK_ACCOUNT)
                .eq(RiskBlacklist::getListValue, no).in(RiskBlacklist::getScope, effective)) > 0) {
            return "收款帳號";
        }
        if (!name.isEmpty() && count(RiskBlacklist.gw().eq(RiskBlacklist::getListType, RiskBlacklist.TYPE_ACCOUNT_NAME)
                .eq(RiskBlacklist::getListValue, name).in(RiskBlacklist::getScope, effective)) > 0) {
            return "戶名";
        }
        return null;
    }

    /** 帳號與電話去除空白與破折號；戶名只去頭尾與中間空白。 */
    static String normalize(String type, String value) {
        String v = StringUtils.trimToEmpty(value);
        return RiskBlacklist.TYPE_ACCOUNT_NAME.equals(type) ? v.replaceAll("\\s+", "") : v.replaceAll("[\\s-]", "");
    }
}
