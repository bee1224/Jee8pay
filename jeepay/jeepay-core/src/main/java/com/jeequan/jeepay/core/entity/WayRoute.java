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
 * 通道路由規則（ADR-0011）：商戶以別名代碼（例如 IBON）下單時，依金額區間、時段與權重，在已開通的實際通道代碼中選一個。mch_no 為空字串表示全部商戶；有商戶專屬規則時只用專屬規則。
 */
@Schema(description = "通道路由規則")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_way_route")
public class WayRoute extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<WayRoute> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @Schema(title = "routeId", description = "規則ID")
    @TableId(value = "route_id", type = IdType.AUTO)
    private Long routeId;

    @Schema(title = "aliasWayCode", description = "別名代碼（商戶下單用）")
    private String aliasWayCode;

    @Schema(title = "targetWayCode", description = "實際支付方式代碼")
    private String targetWayCode;

    @Schema(title = "mchNo", description = "商戶號，空字串表示全部商戶")
    private String mchNo;

    @Schema(title = "minAmount", description = "金額下限（含），單位分，0 表示不限")
    private Long minAmount;

    @Schema(title = "maxAmount", description = "金額上限（含），單位分，0 表示不限")
    private Long maxAmount;

    @Schema(title = "weight", description = "權重 1-9")
    private Integer weight;

    @Schema(title = "timeStart", description = "可用時段起（HH:mm，台北時間），空白表示全天")
    private String timeStart;

    @Schema(title = "timeEnd", description = "可用時段迄（HH:mm，不含），可跨午夜")
    private String timeEnd;

    @Schema(title = "state", description = "狀態: 0-停用, 1-啟用")
    private Byte state;

    @Schema(title = "remark", description = "備註")
    private String remark;

    @Schema(title = "updatedBy", description = "最後修改者")
    private String updatedBy;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;

    @Schema(title = "updatedAt", description = "更新時間")
    private Date updatedAt;
}
