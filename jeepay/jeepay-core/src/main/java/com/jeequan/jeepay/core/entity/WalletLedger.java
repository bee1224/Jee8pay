package com.jeequan.jeepay.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.core.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 錢包流水（只增不改）。(account_id, biz_type, biz_id) 唯一，重複記帳會被資料庫擋下，確保結算與提現冪等。
 */
@Schema(description = "錢包流水")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_wallet_ledger")
public class WalletLedger extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<WalletLedger> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final String BIZ_ORDER_SETTLE = "ORDER_SETTLE";
    public static final String BIZ_ORDER_REVERSE = "ORDER_REVERSE";
    public static final String BIZ_WITHDRAW_APPLY = "WITHDRAW_APPLY";
    public static final String BIZ_WITHDRAW_RELEASE = "WITHDRAW_RELEASE";
    public static final String BIZ_WITHDRAW_PAID = "WITHDRAW_PAID";
    public static final String BIZ_WITHDRAW_FEE = "WITHDRAW_FEE";
    public static final String BIZ_ADJUST = "ADJUST";

    @Schema(title = "ledgerId", description = "流水ID")
    @TableId(value = "ledger_id", type = IdType.AUTO)
    private Long ledgerId;

    @Schema(title = "accountId", description = "帳戶ID")
    private Long accountId;

    @Schema(title = "ownerType", description = "擁有者類型")
    private String ownerType;

    @Schema(title = "ownerId", description = "擁有者ID")
    private String ownerId;

    @Schema(title = "bizType", description = "業務類型")
    private String bizType;

    @Schema(title = "bizId", description = "業務單號（訂單號／提現單號／調帳單號）")
    private String bizId;

    @Schema(title = "amount", description = "可用餘額變動（正入負出），單位分")
    private Long amount;

    @Schema(title = "frozenChange", description = "凍結金額變動，單位分")
    private Long frozenChange;

    @Schema(title = "balanceBefore", description = "變動前可用餘額")
    private Long balanceBefore;

    @Schema(title = "balanceAfter", description = "變動後可用餘額")
    private Long balanceAfter;

    @Schema(title = "frozenAfter", description = "變動後凍結金額")
    private Long frozenAfter;

    @Schema(title = "remark", description = "說明")
    private String remark;

    @Schema(title = "operatorUid", description = "操作者用戶ID（系統結算為空）")
    private Long operatorUid;

    @Schema(title = "operatorName", description = "操作者")
    private String operatorName;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;
}
