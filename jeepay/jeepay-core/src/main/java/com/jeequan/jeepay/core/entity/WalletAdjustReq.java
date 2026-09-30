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
 * 人工調帳申請（雙人覆核）：申請人不得自行核准，核准後才記帳。
 */
@Schema(description = "人工調帳申請")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_wallet_adjust_req")
public class WalletAdjustReq extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<WalletAdjustReq> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final byte STATE_PENDING = 0;
    public static final byte STATE_APPROVED = 1;
    public static final byte STATE_REJECTED = 2;

    @Schema(title = "reqId", description = "申請ID")
    @TableId(value = "req_id", type = IdType.AUTO)
    private Long reqId;

    @Schema(title = "accountId", description = "帳戶ID")
    private Long accountId;

    @Schema(title = "ownerType", description = "擁有者類型")
    private String ownerType;

    @Schema(title = "ownerId", description = "擁有者ID")
    private String ownerId;

    @Schema(title = "amount", description = "調整金額（正為加、負為減），單位分")
    private Long amount;

    @Schema(title = "reason", description = "調帳原因")
    private String reason;

    @Schema(title = "state", description = "狀態: 0-待覆核, 1-已核准, 2-已駁回")
    private Byte state;

    @Schema(title = "requesterUid", description = "申請人用戶ID")
    private Long requesterUid;

    @Schema(title = "requesterName", description = "申請人")
    private String requesterName;

    @Schema(title = "reviewerUid", description = "覆核人用戶ID")
    private Long reviewerUid;

    @Schema(title = "reviewerName", description = "覆核人")
    private String reviewerName;

    @Schema(title = "reviewRemark", description = "覆核意見")
    private String reviewRemark;

    @Schema(title = "createdAt", description = "申請時間")
    private Date createdAt;

    @Schema(title = "reviewedAt", description = "覆核時間")
    private Date reviewedAt;
}
