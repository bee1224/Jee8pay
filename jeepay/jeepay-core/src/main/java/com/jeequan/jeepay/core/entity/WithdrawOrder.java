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
 * 提現單（ADR-0010）。申請時凍結；平台人工匯款後標記已撥款才真正出帳；駁回或取消則解凍。
 */
@Schema(description = "提現單")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_withdraw_order")
public class WithdrawOrder extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<WithdrawOrder> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final byte STATE_PENDING = 0;
    public static final byte STATE_PAID = 1;
    public static final byte STATE_REJECTED = 2;
    public static final byte STATE_CANCELLED = 3;

    @Schema(title = "withdrawId", description = "提現單號")
    @TableId(value = "withdraw_id", type = IdType.INPUT)
    private String withdrawId;

    @Schema(title = "ownerType", description = "擁有者類型: MCH/AGENT")
    private String ownerType;

    @Schema(title = "ownerId", description = "擁有者ID")
    private String ownerId;

    @Schema(title = "reqNo", description = "申請端冪等鍵（同一擁有者唯一）")
    private String reqNo;

    @Schema(title = "amount", description = "申請金額（自餘額扣除），單位分")
    private Long amount;

    @Schema(title = "fee", description = "提現手續費，單位分")
    private Long fee;

    @Schema(title = "actualAmount", description = "實際匯款金額 = 申請金額 - 手續費，單位分")
    private Long actualAmount;

    @Schema(title = "bankName", description = "銀行名稱")
    private String bankName;

    @Schema(title = "bankCode", description = "銀行代碼")
    private String bankCode;

    @Schema(title = "branch", description = "分行")
    private String branch;

    @Schema(title = "accountNo", description = "帳號")
    private String accountNo;

    @Schema(title = "accountName", description = "戶名")
    private String accountName;

    @Schema(title = "riskFlags", description = "風控提示（逗號分隔）")
    private String riskFlags;

    @Schema(title = "state", description = "狀態: 0-待審核, 1-已撥款, 2-已駁回, 3-已取消")
    private Byte state;

    @Schema(title = "applyUid", description = "申請人用戶ID")
    private Long applyUid;

    @Schema(title = "applyName", description = "申請人")
    private String applyName;

    @Schema(title = "reviewerUid", description = "審核人用戶ID")
    private Long reviewerUid;

    @Schema(title = "reviewerName", description = "審核人")
    private String reviewerName;

    @Schema(title = "paidRef", description = "匯款單號／交易序號")
    private String paidRef;

    @Schema(title = "reviewRemark", description = "審核說明")
    private String reviewRemark;

    @Schema(title = "createdAt", description = "申請時間")
    private Date createdAt;

    @Schema(title = "reviewedAt", description = "審核時間")
    private Date reviewedAt;
}
