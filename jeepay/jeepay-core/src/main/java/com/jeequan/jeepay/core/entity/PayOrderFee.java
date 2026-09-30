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
 * 訂單四層手續費快照（ADR-0009 第二階段）。
 * 下單當下依 t_fee_rule 解析後寫入且之後不再變動，日後調整費率不影響既有訂單；
 * 以旁表存放而不修改 t_pay_order，避免改動訂單主檔與狀態機。
 */
@Schema(description = "訂單手續費快照")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_pay_order_fee")
public class PayOrderFee extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<PayOrderFee> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @Schema(title = "payOrderId", description = "支付訂單號")
    @TableId(value = "pay_order_id", type = IdType.INPUT)
    private String payOrderId;

    @Schema(title = "mchNo", description = "商戶號")
    private String mchNo;

    @Schema(title = "wayCode", description = "支付方式代碼")
    private String wayCode;

    @Schema(title = "amount", description = "訂單金額，單位分")
    private Long amount;

    @Schema(title = "mchFeeAmount", description = "下單時商戶手續費（支付通道費率），單位分")
    private Long mchFeeAmount;

    @Schema(title = "agentNo", description = "直屬代理號（下單當下）")
    private String agentNo;

    @Schema(title = "srAgentNo", description = "高級代理號（下單當下）")
    private String srAgentNo;

    @Schema(title = "referrerAgentNo", description = "推薦人代理號（下單當下）")
    private String referrerAgentNo;

    @Schema(title = "platformFee", description = "平臺費，單位分")
    private Long platformFee;

    @Schema(title = "channelFee", description = "渠道費，單位分")
    private Long channelFee;

    @Schema(title = "srAgentFee", description = "高代費，單位分")
    private Long srAgentFee;

    @Schema(title = "agentFee", description = "代理費，單位分")
    private Long agentFee;

    @Schema(title = "totalFee", description = "四層合計，單位分")
    private Long totalFee;

    @Schema(title = "exceedsMchFee", description = "四層合計是否超過商戶手續費: 0-否, 1-是（需人工檢查費率設定）")
    private Byte exceedsMchFee;

    @Schema(title = "detail", description = "各層費率、固定金額與規則來源（JSON）")
    private String detail;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;
}
