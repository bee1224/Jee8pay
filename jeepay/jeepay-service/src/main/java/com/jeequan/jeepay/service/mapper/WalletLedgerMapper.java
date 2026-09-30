package com.jeequan.jeepay.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jeequan.jeepay.core.entity.WalletLedger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface WalletLedgerMapper extends BaseMapper<WalletLedger> {

    /** 日結：依入帳日與帳戶類型彙總訂單結算與沖回（台北時間，DB 時區見 ADR-0006）。 */
    @Select("SELECT DATE(created_at) AS day, owner_type AS ownerType, biz_type AS bizType, COUNT(DISTINCT biz_id) AS orders, SUM(amount) AS amount"
            + " FROM t_wallet_ledger WHERE biz_type IN ('ORDER_SETTLE', 'ORDER_REVERSE') AND created_at >= #{start} AND created_at < #{end}"
            + " GROUP BY DATE(created_at), owner_type, biz_type ORDER BY day")
    List<Map<String, Object>> settleDaily(@Param("start") Date start, @Param("end") Date end);
}
