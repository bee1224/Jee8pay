package com.jeequan.jeepay.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jeequan.jeepay.core.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 已關閉訂單的補查進度（C5）：旁表記錄查過幾次、最後結果，不修改 t_pay_order。 */
@Schema(description = "關閉訂單補查紀錄")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_pay_order_audit")
public class PayOrderAudit extends BaseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(title = "payOrderId", description = "支付訂單號")
    @TableId(value = "pay_order_id", type = IdType.INPUT)
    private String payOrderId;

    @Schema(title = "auditCount", description = "已補查次數")
    private Integer auditCount;

    @Schema(title = "lastResult", description = "最後一次查詢結果（ChannelState）")
    private String lastResult;

    @Schema(title = "reopened", description = "是否因查到已付款而轉回支付成功: 0-否, 1-是")
    private Byte reopened;

    @Schema(title = "lastAuditAt", description = "最後補查時間")
    private Date lastAuditAt;
}
