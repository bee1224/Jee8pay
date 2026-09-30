package com.jeequan.jeepay.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jeequan.jeepay.core.entity.WalletAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface WalletAccountMapper extends BaseMapper<WalletAccount> {

    /** 交易內鎖定帳戶列，之後的餘額判斷與記帳都以此為準，避免併發提現超扣。 */
    @Select("SELECT * FROM t_wallet_account WHERE account_id = #{accountId} FOR UPDATE")
    WalletAccount lockById(@Param("accountId") Long accountId);

    /** 以增量更新餘額，不做「讀出再寫回」。 */
    @Update("UPDATE t_wallet_account SET balance = balance + #{avail}, frozen = frozen + #{frozen},"
            + " total_in = total_in + #{totalIn}, total_out = total_out + #{totalOut} WHERE account_id = #{accountId}")
    int applyDelta(@Param("accountId") Long accountId, @Param("avail") long avail, @Param("frozen") long frozen,
                   @Param("totalIn") long totalIn, @Param("totalOut") long totalOut);
}
