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

/** 手續費規則變更紀錄：費率屬動錢設定，每次新增／修改／刪除都留下前後值。 */
@Schema(description = "手續費規則變更紀錄")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_fee_rule_log")
public class FeeRuleLog extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<FeeRuleLog> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final String ACTION_SAVE = "SAVE";
    public static final String ACTION_DELETE = "DELETE";

    @Schema(title = "logId", description = "紀錄ID")
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    @Schema(title = "wayCode", description = "支付方式代碼")
    private String wayCode;

    @Schema(title = "targetType", description = "對象類型")
    private String targetType;

    @Schema(title = "targetId", description = "對象ID")
    private String targetId;

    @Schema(title = "layer", description = "費率層")
    private String layer;

    @Schema(title = "action", description = "動作: SAVE/DELETE")
    private String action;

    @Schema(title = "beforeValue", description = "變更前（JSON）")
    private String beforeValue;

    @Schema(title = "afterValue", description = "變更後（JSON）")
    private String afterValue;

    @Schema(title = "operatorUid", description = "操作者用戶ID")
    private Long operatorUid;

    @Schema(title = "operatorName", description = "操作者姓名")
    private String operatorName;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;
}
