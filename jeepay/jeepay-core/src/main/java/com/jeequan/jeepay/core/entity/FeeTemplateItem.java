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

/** 費率範本明細：（範本、支付方式、費率層）唯一。 */
@Schema(description = "費率範本明細")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_fee_template_item")
public class FeeTemplateItem extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<FeeTemplateItem> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @Schema(title = "itemId", description = "明細ID")
    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    @Schema(title = "templateId", description = "範本ID")
    private Long templateId;

    @Schema(title = "wayCode", description = "支付方式代碼")
    private String wayCode;

    @Schema(title = "layer", description = "費率層: SR_AGENT/AGENT")
    private String layer;

    @Schema(title = "rate", description = "費率（比率）")
    private BigDecimal rate;

    @Schema(title = "fixedAmount", description = "單筆固定金額，單位分")
    private Long fixedAmount;
}
