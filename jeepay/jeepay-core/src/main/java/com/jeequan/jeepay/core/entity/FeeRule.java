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

/**
 * 四層手續費規則（ADR-0009）。每條規則 = 支付方式 × 對象（平台預設／代理／單一商戶）× 費率層。
 * 平臺費與渠道費只能由平台設定（DEFAULT 或 MCH 覆寫）；高代費掛在高級代理、代理費掛在一般代理，皆可被 MCH 覆寫。
 */
@Schema(description = "四層手續費規則表")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_fee_rule")
public class FeeRule extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<FeeRule> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final String TARGET_DEFAULT = "DEFAULT";
    public static final String TARGET_AGENT = "AGENT";
    public static final String TARGET_MCH = "MCH";

    public static final String LAYER_PLATFORM = "PLATFORM";
    public static final String LAYER_CHANNEL = "CHANNEL";
    public static final String LAYER_SR_AGENT = "SR_AGENT";
    public static final String LAYER_AGENT = "AGENT";

    @Schema(title = "ruleId", description = "規則ID")
    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;

    @Schema(title = "wayCode", description = "支付方式代碼")
    private String wayCode;

    @Schema(title = "targetType", description = "對象類型: DEFAULT/AGENT/MCH")
    private String targetType;

    @Schema(title = "targetId", description = "對象ID（DEFAULT 為空字串）")
    private String targetId;

    @Schema(title = "layer", description = "費率層: PLATFORM/CHANNEL/SR_AGENT/AGENT")
    private String layer;

    @Schema(title = "rate", description = "費率（比例，0.006 即 0.6%）")
    private BigDecimal rate;

    @Schema(title = "fixedAmount", description = "單筆固定金額（分）")
    private Long fixedAmount;

    @Schema(title = "state", description = "狀態: 0-停用, 1-啟用")
    private Byte state;

    @Schema(title = "updatedUid", description = "最後修改者用戶ID")
    private Long updatedUid;

    @Schema(title = "updatedBy", description = "最後修改者姓名")
    private String updatedBy;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;

    @Schema(title = "updatedAt", description = "更新時間")
    private Date updatedAt;
}
