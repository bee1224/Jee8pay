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
 * 錢包帳戶（ADR-0010）。可用餘額與凍結金額分桶；餘額只能經 WalletService 記帳異動，每次異動都有對應流水。
 */
@Schema(description = "錢包帳戶")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_wallet_account")
public class WalletAccount extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<WalletAccount> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    /** 擁有者類型：商戶、代理、平台（手續費收入）、上游渠道（渠道費成本） */
    public static final String OWNER_MCH = "MCH";
    public static final String OWNER_AGENT = "AGENT";
    public static final String OWNER_PLATFORM = "PLATFORM";
    public static final String OWNER_CHANNEL = "CHANNEL";
    /** 平台帳戶固定的擁有者ID */
    public static final String PLATFORM_ID = "PLATFORM";

    @Schema(title = "accountId", description = "帳戶ID")
    @TableId(value = "account_id", type = IdType.AUTO)
    private Long accountId;

    @Schema(title = "ownerType", description = "擁有者類型: MCH/AGENT/PLATFORM/CHANNEL")
    private String ownerType;

    @Schema(title = "ownerId", description = "擁有者ID（商戶號／代理號／PLATFORM／ifCode）")
    private String ownerId;

    @Schema(title = "balance", description = "可用餘額，單位分")
    private Long balance;

    @Schema(title = "frozen", description = "凍結金額（提現處理中），單位分")
    private Long frozen;

    @Schema(title = "totalIn", description = "累計入帳，單位分")
    private Long totalIn;

    @Schema(title = "totalOut", description = "累計出帳，單位分")
    private Long totalOut;

    @Schema(title = "payoutBankName", description = "提現銀行名稱")
    private String payoutBankName;

    @Schema(title = "payoutBankCode", description = "提現銀行代碼（3 碼）")
    private String payoutBankCode;

    @Schema(title = "payoutBranch", description = "提現分行")
    private String payoutBranch;

    @Schema(title = "payoutAccountNo", description = "提現帳號")
    private String payoutAccountNo;

    @Schema(title = "payoutAccountName", description = "提現戶名")
    private String payoutAccountName;

    @Schema(title = "payoutUpdatedAt", description = "提現帳戶最後變更時間（風控：新變更帳戶提現需人工注意）")
    private Date payoutUpdatedAt;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;

    @Schema(title = "updatedAt", description = "更新時間")
    private Date updatedAt;
}
