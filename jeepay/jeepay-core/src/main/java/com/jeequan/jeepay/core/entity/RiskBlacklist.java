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
 * 風控黑名單（ADR-0010）：提現收款帳號、戶名或電話命中即拒絕提現。scope 為 GLOBAL 或代理號（代理範圍共用）。
 */
@Schema(description = "風控黑名單")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_risk_blacklist")
public class RiskBlacklist extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<RiskBlacklist> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    public static final String TYPE_BANK_ACCOUNT = "BANK_ACCOUNT";
    public static final String TYPE_ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String TYPE_PHONE = "PHONE";
    public static final String SCOPE_GLOBAL = "GLOBAL";

    @Schema(title = "id", description = "ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(title = "listType", description = "類型: BANK_ACCOUNT/ACCOUNT_NAME/PHONE")
    private String listType;

    @Schema(title = "listValue", description = "值（帳號去除空白與破折號）")
    private String listValue;

    @Schema(title = "scope", description = "範圍: GLOBAL 或代理號")
    private String scope;

    @Schema(title = "remark", description = "備註")
    private String remark;

    @Schema(title = "createdUid", description = "建立者用戶ID")
    private Long createdUid;

    @Schema(title = "createdBy", description = "建立者")
    private String createdBy;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;
}
