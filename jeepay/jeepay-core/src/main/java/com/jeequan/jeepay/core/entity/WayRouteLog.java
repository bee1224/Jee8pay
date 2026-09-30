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
 * 路由決策紀錄：每次別名下單都記錄候選與結果，供排查「為什麼走了這個通道」。
 */
@Schema(description = "路由決策紀錄")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_way_route_log")
public class WayRouteLog extends BaseModel implements Serializable {

    public static final LambdaQueryWrapper<WayRouteLog> gw() {
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID = 1L;

    @Schema(title = "logId", description = "紀錄ID")
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    @Schema(title = "mchNo", description = "商戶號")
    private String mchNo;

    @Schema(title = "appId", description = "應用ID")
    private String appId;

    @Schema(title = "mchOrderNo", description = "商戶訂單號")
    private String mchOrderNo;

    @Schema(title = "aliasWayCode", description = "別名代碼")
    private String aliasWayCode;

    @Schema(title = "amount", description = "訂單金額，單位分")
    private Long amount;

    @Schema(title = "chosenWayCode", description = "選中的支付方式（無可用時為空）")
    private String chosenWayCode;

    @Schema(title = "candidates", description = "通過篩選的候選與權重（JSON）")
    private String candidates;

    @Schema(title = "createdAt", description = "建立時間")
    private Date createdAt;
}
