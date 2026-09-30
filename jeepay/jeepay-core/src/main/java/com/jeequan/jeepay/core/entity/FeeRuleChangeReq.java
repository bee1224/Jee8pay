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
import java.math.BigDecimal;
import java.util.Date;

/** 平臺費／渠道費變更申請（雙人覆核）：申請人不得自行核准，核准後才寫入 t_fee_rule。 */
@Schema(description = "費率變更申請")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_fee_rule_change_req")
public class FeeRuleChangeReq extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<FeeRuleChangeReq> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final byte STATE_PENDING = 0;
    public static final byte STATE_APPROVED = 1;
    public static final byte STATE_REJECTED = 2;

    @Schema(title = "reqId", description = "申請ID")
    @TableId(value = "req_id", type = IdType.AUTO)
    private Long reqId;

    @Schema(title = "action", description = "動作: SAVE/DELETE")
    private String action;

    @Schema(title = "wayCode", description = "支付方式代碼")
    private String wayCode;

    @Schema(title = "targetType", description = "對象類型: DEFAULT/MCH")
    private String targetType;

    @Schema(title = "targetId", description = "對象ID")
    private String targetId;

    @Schema(title = "layer", description = "費率層: PLATFORM/CHANNEL")
    private String layer;

    @Schema(title = "rate", description = "申請費率（比率）")
    private BigDecimal rate;

    @Schema(title = "fixedAmount", description = "申請單筆固定金額，單位分")
    private Long fixedAmount;

    @Schema(title = "state", description = "狀態: 0-待覆核, 1-已核准, 2-已駁回")
    private Byte state;

    @Schema(title = "requesterUid", description = "申請人用戶ID")
    private Long requesterUid;

    @Schema(title = "requesterName", description = "申請人姓名")
    private String requesterName;

    @Schema(title = "reviewerUid", description = "覆核人用戶ID")
    private Long reviewerUid;

    @Schema(title = "reviewerName", description = "覆核人姓名")
    private String reviewerName;

    @Schema(title = "reviewRemark", description = "覆核意見")
    private String reviewRemark;

    @Schema(title = "createdAt", description = "申請時間")
    private Date createdAt;

    @Schema(title = "reviewedAt", description = "覆核時間")
    private Date reviewedAt;
}
